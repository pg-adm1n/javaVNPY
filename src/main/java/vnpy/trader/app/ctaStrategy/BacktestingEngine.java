package vnpy.trader.app.ctaStrategy;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.gte;
import static com.mongodb.client.model.Filters.lt;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Sorts;
import com.peeandgee.ctp.model.Tick;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.ql.exec.vector.BytesColumnVector;
import org.apache.hadoop.hive.ql.exec.vector.DoubleColumnVector;
import org.apache.hadoop.hive.ql.exec.vector.VectorizedRowBatch;
import org.apache.hive.orc.OrcFile;
import org.apache.hive.orc.Reader;
import org.apache.hive.orc.RecordReader;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import vnpy.trader.VtBarData;
import vnpy.trader.VtBaseData;
import vnpy.trader.VtConstant;
import vnpy.trader.VtGlobal;
import vnpy.trader.VtOrderData;
import vnpy.trader.VtTickData;
import vnpy.trader.VtTradeData;
import vnpy.utils.AppException;
import vnpy.utils.Method;

public class BacktestingEngine implements TradingEngine {

    // CTA回测引擎
    // 函数接口和策略引擎保持一样，
    // 从而实现同一套代码从回测到实盘。
    public final static String TICK_MODE = "tick";
    public final static String BAR_MODE = "bar";

    public final static String CSV_TICK_MODE = "csv_tick";

    private String mode; // 回测模式

    private String startDate;
    private int initDays;
    private String endDate;

    private LocalDateTime dataStartDate = null; // 回测数据开始日期，datetime对象
    private LocalDateTime dataEndDate = null; // 回测数据结束日期，datetime对象
    private LocalDateTime strategyStartDate = null; // 策略启动日期（即前面的数据用于初始化），datetime对象

    private double slippage; // 回测时假设的滑点
    private double rate; // 回测时假设的佣金比例（适用于百分比佣金）
    private double size; // 合约大小，默认为1
    private double priceTick; // 价格最小变动

    private MongoClient dbClient; // 数据库客户端
    private FindIterable<?> dbCursor; // 数据库指针

    private List<Object> initData; // 初始化用的数据

    private String dbName; // 回测数据库名
    private String symbol; // 回测集合名

    private CtaTemplate strategy;

    private String engineType;

    // 本地停止单
    private int stopOrderCount; // 编号计数：stopOrderID = STOPORDERPREFIX + str(stopOrderCount)

    // 本地停止单字典, key为stopOrderID，value为stopOrder对象
    private Map<String, StopOrder> stopOrderDict; // 停止单撤销后不会从本字典中删除
    private Map<String, StopOrder> workingStopOrderDict; // 停止单撤销后会从本字典中删除

    private int limitOrderCount = 0; // 限价单编号
    private Map<String, VtOrderData> limitOrderDict;// 限价单字典
    private Map<String, VtOrderData> workingLimitOrderDict;// 活动限价单字典，用于进行撮合用

    private int tradeCount; // 成交编号
    private Map<String, VtTradeData> tradeDict; // 成交字典

    private List<String> logList; // 日志记录

    // 当前最新数据，用于模拟成交用
    private VtTickData tick;
    private VtBarData bar;
    private LocalDateTime dt; // 最新的时间

    // 日线回测结果计算用
    private Map<LocalDate, DailyResult> dailyResultDict;

    public BacktestingEngine() {
        this.engineType = CtaBaseConstant.ENGINETYPE_BACKTESTING; // 引擎类型为回测

        this.strategy = null; // 回测策略
        this.mode = BacktestingEngine.BAR_MODE; // 回测模式，默认为K线

        this.stopOrderCount = 0;
        this.stopOrderDict = new ConcurrentHashMap<String, StopOrder>();
        this.workingStopOrderDict = new ConcurrentHashMap<String, StopOrder>();

        this.limitOrderCount = 0; // 限价单编号
        this.limitOrderDict = new LinkedHashMap<String, VtOrderData>(); // 限价单字典
        this.workingLimitOrderDict = new LinkedHashMap<String, VtOrderData>(); // 活动限价单字典，用于进行撮合用

        this.tradeCount = 0; // 成交编
        this.tradeDict = new LinkedHashMap<String, VtTradeData>(); // 成交字典号

        this.logList = new ArrayList<String>();

        // 日线回测结果计算用
        this.dailyResultDict = new TreeMap<LocalDate, DailyResult>();
    }

    // 输出内容
    private void output(String content) {
        System.out.println(LocalDateTime.now() + "\t" + content);
    }

    // 设置回测模式
    public void setBacktestingMode(String mode) {
        this.mode = mode;
    }

    // 设置回测的启动日期
    public void setStartDate(String startDate) {
        setStartDate(startDate, 10);
    }

    // 设置回测的启动日期
    public void setStartDate(String startDate, int initDays) {
        this.startDate = startDate;
        this.initDays = initDays;

        this.dataStartDate = LocalDate.parse(startDate, DateTimeFormatter.ofPattern("yyyyMMdd")).atStartOfDay();
        this.strategyStartDate = dataStartDate.minusDays(-initDays);
    }

    // 设置回测的结束日期
    public void setEndDate(String endDate) {
        this.endDate = endDate;

        if (!"".equals(endDate) && endDate != null) {
            this.dataEndDate = LocalDate.parse(endDate, DateTimeFormatter.ofPattern("yyyyMMdd")).atStartOfDay();
        }

        // 若不修改时间则会导致不包含dataEndDate当天数据
        this.dataEndDate = this.dataEndDate.withHour(23).withMinute(59).withSecond(59);
    }

    // 设置滑点点数
    public void setSlippage(double slippage) {
        this.slippage = slippage;
    }

    // 设置佣金比例
    public void setRate(double rate) {
        this.rate = rate;
    }

    // 设置合约大小
    public void setSize(double size) {
        this.size = size;
    }

    // 设置价格最小变动
    public void setPriceTick(double priceTick) {
        this.priceTick = priceTick;
    }

    // 设置历史数据所用的数据库
    public void setDatabase(String dbName, String symbol) {
        this.dbName = dbName;
        this.symbol = symbol;
    }

    // 初始化策略
    // setting是策略的参数设置，如果使用类中写好的默认设置则可以不传该参数
    public void initStrategy(Class<?> strategyClass, Map<String, Object> setting) {
        Constructor<?> con;
        try {
            con = strategyClass.getConstructor(BacktestingEngine.class, Map.class);
            this.strategy = (CtaTemplate) con.newInstance(this, setting);
        } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException
            | IllegalArgumentException | InvocationTargetException e) {
            e.printStackTrace();
            throw new AppException("实例化出错");
        }
        this.strategy.setName(this.strategy.getClassName());
    }

    public static Stream getDataFromORCFile(String mainContractPathStr, String startDate, String endDate) {
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", "file:///");
        Path file = new Path(mainContractPathStr);

        // Can be modify as 常量
        final DateTimeFormatter yyyyMMddFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        try {
            Reader reader = OrcFile.createReader(file, OrcFile.readerOptions(conf));

            RecordReader orcFile = reader.rows();

            VectorizedRowBatch batch = reader.getSchema().createRowBatch();

            return Stream.iterate(0, integer -> integer + 1)
                .takeWhile(integer -> {
                    try {
                        // Check if have batch, if have then insert into batch
                        return orcFile.nextBatch(batch);
                    } catch (IOException e) {
                        return false;
                    }
                })
                .flatMap(integer -> {
                    // flat the batch mode to row mode
                    int size1 = batch.size;
                    return Stream.iterate(0, i -> i + 1)
                        .limit(size1)
                        .map(i -> getTick(batch, i));
                })
                .filter(tick1 -> {
                    //TODO:
                    final LocalDate partition1Date = LocalDate.parse(tick1.getPartition1(), yyyyMMddFormatter);
                    // 目前是不包含 如需包含 请自行 LocalDate.parse(startDate,yyyyMMddFormatter).plusDays(1)/LocalDate.parse(endDate,yyyyMMddFormatter).minusDays(1
                    return partition1Date.isAfter(LocalDate.parse(startDate,yyyyMMddFormatter)) && partition1Date.isBefore(LocalDate.parse(endDate,yyyyMMddFormatter).minusDays(1));
                });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Tick getTick(VectorizedRowBatch batch, Integer i) {
        final String contractName = ((BytesColumnVector) batch.cols[0]).toString(i);
        final String tickTime = ((BytesColumnVector) batch.cols[1]).toString(i);
        final DoubleColumnVector priceCol = (DoubleColumnVector) batch.cols[2];
        final double price = priceCol.isRepeating ? priceCol.vector[0] : priceCol.vector[i];

        final DoubleColumnVector cumOpenIntCol = (DoubleColumnVector) batch.cols[3];
        final double cumOpenInt = cumOpenIntCol.isRepeating ? cumOpenIntCol.vector[0] : priceCol.vector[i];

        final DoubleColumnVector openIntCol = (DoubleColumnVector) batch.cols[4];
        final double openInt = openIntCol.isRepeating ? openIntCol.vector[0] : openIntCol.vector[i];

        final DoubleColumnVector turnoverCol = (DoubleColumnVector) batch.cols[5];
        final double turnover = turnoverCol.isRepeating ? turnoverCol.vector[0] : turnoverCol.vector[i];

        final DoubleColumnVector qtyCol = (DoubleColumnVector) batch.cols[6];
        final double qty = qtyCol.isRepeating ? qtyCol.vector[0] : qtyCol.vector[i];

        final DoubleColumnVector bidCol = (DoubleColumnVector) batch.cols[7];
        final double bid = bidCol.isRepeating ? bidCol.vector[0] : bidCol.vector[i];

        final DoubleColumnVector askCol = (DoubleColumnVector) batch.cols[8];
        final double ask = askCol.isRepeating ? askCol.vector[0] : askCol.vector[i];

        final DoubleColumnVector bidQtyCol = (DoubleColumnVector) batch.cols[9];
        final double bidQty = bidQtyCol.isRepeating ? bidQtyCol.vector[0] : bidQtyCol.vector[i];

        final DoubleColumnVector askQtyCol = (DoubleColumnVector) batch.cols[10];
        final double askQty = askQtyCol.isRepeating ? askQtyCol.vector[0] : askQtyCol.vector[i];

        final DoubleColumnVector wprCol = (DoubleColumnVector) batch.cols[11];
        final double wpr = wprCol.isRepeating ? wprCol.vector[0] : wprCol.vector[i];

        final DoubleColumnVector retCol = (DoubleColumnVector) batch.cols[12];
        final double ret = retCol.isRepeating ? retCol.vector[0] : retCol.vector[i];

        final String partition1 = ((BytesColumnVector) batch.cols[13]).toString(i);
        final String partition2 = ((BytesColumnVector) batch.cols[14]).toString(i);
        final String date = ((BytesColumnVector) batch.cols[15]).toString(i);
        final String time = ((BytesColumnVector) batch.cols[16]).toString(i);
        final String ms = ((BytesColumnVector) batch.cols[17]).toString(i);

        final DoubleColumnVector bt1Col = (DoubleColumnVector) batch.cols[18];
        final double bt1 = bt1Col.isRepeating ? bt1Col.vector[0] : bt1Col.vector[i];

        final DoubleColumnVector bt2Col = (DoubleColumnVector) batch.cols[19];
        final double bt2 = bt2Col.isRepeating ? bt2Col.vector[0] : bt2Col.vector[i];

        final DoubleColumnVector st1Col = (DoubleColumnVector) batch.cols[20];
        final double st1 = st1Col.isRepeating ? st1Col.vector[0] : st1Col.vector[i];

        final DoubleColumnVector st2Col = (DoubleColumnVector) batch.cols[21];
        final double st2 = st2Col.isRepeating ? st2Col.vector[0] : st2Col.vector[i];

        //修改 返回的变量类型
        return new Tick(contractName, tickTime, price, cumOpenInt, openInt, turnover, qty, bid, ask, bidQty, askQty, wpr, ret, partition1, partition2, date, time, ms, bt1, bt2, st1,
            st2);
    }

    // 开始跑回测
    public void runBacktesting() {
        // 载入历史数据
        loadHistoryData();

        // 首先根据回测模式，确认要使用的数据类
        Class<?> dataClass = null;
        Method func;
        if (BacktestingEngine.BAR_MODE.equals(this.mode)) {
            dataClass = VtBarData.class;
            func = new Method(this, "newBar", dataClass);
        } else {
            dataClass = VtTickData.class;
            func = new Method(this, "newTick", dataClass);
        }

        this.output("开始回测");

        this.strategy.onInit();
        this.strategy.setInited(true);
        this.output("策略初始化完成");

        this.strategy.setTrading(true);
        this.strategy.onStart();
        this.output("策略启动完成");

        this.output("开始回放数据");

        for (Object d : this.dbCursor) {
            func.invoke(dataClass.cast(d));
        }

        // 新写法
        final Stream data = getDataFromORCFile("rb1610", "20160101", "20160701");
        Class<?> finalDataClass = dataClass;
        data.forEach(o -> func.invoke(finalDataClass.cast(o)));

        this.output("数据回放结束");
    }

    // 载入历史数据
    private void loadHistoryData() {
        this.dbClient = MongoClients.create(
            "mongodb://" + VtGlobal.globalSetting.get("mongoHost") + ":" + VtGlobal.globalSetting.get("mongoPort"));
        CodecRegistry pojoCodecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
            fromProviders(PojoCodecProvider.builder().automatic(true).build()));
        MongoDatabase database = this.dbClient.getDatabase(this.dbName).withCodecRegistry(pojoCodecRegistry);

        this.output("开始载入数据");

        // 首先根据回测模式，确认要使用的数据类
        Class<?> dataClass = null;
        // Method func;
        if (BacktestingEngine.BAR_MODE.equals(this.mode)) {
            dataClass = VtBarData.class;
            // func = new Method(this, "newBar", dataClass);
        } else {
            dataClass = VtTickData.class;
            // func = new Method(this, "newTick", dataClass);
        }

        long count = 0;

        // 载入初始化需要用的数据
        MongoCollection<?> collection = database.getCollection(this.symbol, dataClass);
        Bson flt = and(gte("datetime", this.dataStartDate), lt("datetime", this.strategyStartDate));
        FindIterable<?> initCursor = collection.find(flt).sort(Sorts.ascending("datetime"));
        count = count + collection.countDocuments(flt);

        // 将数据从查询指针中读取出，并生成列表
        this.initData = new ArrayList<Object>(); // 清空initData列表
        for (Object d : initCursor) {
            this.initData.add(d);
        }

        // 载入回测数据
        if (this.dataEndDate == null) {
            flt = gte("datetime", this.strategyStartDate); // 数据过滤条件
        } else {
            flt = and(gte("datetime", this.strategyStartDate), lt("datetime", this.dataEndDate));
        }

        this.dbCursor = collection.find(flt).sort(Sorts.ascending("datetime"));
        count = count + collection.countDocuments(flt);

        this.output("载入完成，数据量：" + count);
    }

    // 新的K线
    public void newBar(VtBarData bar) {
        this.bar = bar;
        this.dt = bar.getDatetime();

        this.crossLimitOrder(); // 先撮合限价单
        this.crossStopOrder(); // 再撮合停止单
        this.strategy.onBar(bar); // 推送K线到策略中

        this.updateDailyClose(bar.getDatetime(), bar.getClose());
    }

    // 新的Tick
    public void newTick(VtTickData tick) {
        this.tick = tick;
        this.dt = tick.getDatetime();

        this.crossLimitOrder();
        this.crossStopOrder();
        this.strategy.onTick(tick);

        this.updateDailyClose(tick.getDatetime(), tick.getLastPrice());
    }

    // 基于最新数据撮合限价单
    private void crossLimitOrder() {
        double buyCrossPrice;
        double sellCrossPrice;
        double buyBestCrossPrice;
        double sellBestCrossPrice;

        boolean buyCross;
        boolean sellCross;

        String tradeID;
        VtTradeData trade;

        // 先确定会撮合成交的价格
        if (BacktestingEngine.BAR_MODE.equals(this.mode)) {
            buyCrossPrice = this.bar.getLow(); // 若买入方向限价单价格高于该价格，则会成交
            sellCrossPrice = this.bar.getHigh(); // 若卖出方向限价单价格低于该价格，则会成交
            buyBestCrossPrice = this.bar.getOpen(); // 在当前时间点前发出的买入委托可能的最优成交价
            sellBestCrossPrice = this.bar.getOpen(); // 在当前时间点前发出的卖出委托可能的最优成交价
        } else {
            buyCrossPrice = this.tick.getAskPrice1();
            sellCrossPrice = this.tick.getBidPrice1();
            buyBestCrossPrice = this.tick.getAskPrice1();
            sellBestCrossPrice = this.tick.getBidPrice1();
        }
        // 遍历限价单字典中的所有限价单
        for (String orderID : this.workingLimitOrderDict.keySet()) {
            VtOrderData order = this.workingLimitOrderDict.get(orderID);
            // 推送委托进入队列（未成交）的状态更新
            if (order.getStatus() == null) {
                order.setStatus(VtConstant.STATUS_NOTTRADED);
                this.strategy.onOrder(order);
            }

            // 判断是否会成交
            buyCross = (VtConstant.DIRECTION_LONG.equals(order.getDirection()) && order.getPrice() >= buyCrossPrice
                && buyCrossPrice > 0);
            // 国内的tick行情在涨停时askPrice1为0，此时买无法成交

            sellCross = (VtConstant.DIRECTION_SHORT.equals(order.getDirection()) && order.getPrice() <= sellCrossPrice
                && sellCrossPrice > 0);
            // 国内的tick行情在跌停时bidPrice1为0，此时卖无法成交

            // 如果发生了成交
            if (buyCross || sellCross) {
                // 推送成交数据
                this.tradeCount += 1;// 成交编号自增1
                tradeID = this.tradeCount + "";
                trade = new VtTradeData();
                trade.setVtSymbol(order.getVtSymbol());
                trade.setTradeID(tradeID);
                trade.setVtTradeID(tradeID);
                trade.setOrderID(order.getOrderID());
                trade.setVtOrderID(order.getOrderID());
                trade.setDirection(order.getDirection());
                trade.setOffset(order.getOffset());

                // 以买入为例：
                // 1. 假设当根K线的OHLC分别为：100, 125, 90, 110
                // 2. 假设在上一根K线结束(也是当前K线开始)的时刻，策略发出的委托为限价105
                // 3. 则在实际中的成交价会是100而不是105，因为委托发出时市场的最优价格是100
                if (buyCross) {
                    trade.setPrice(Math.min(order.getPrice(), buyBestCrossPrice));
                    this.strategy.setPos(this.strategy.getPos() + order.getTotalVolume());
                } else {
                    trade.setPrice(Math.max(order.getPrice(), sellBestCrossPrice));
                    this.strategy.setPos(this.strategy.getPos() - order.getTotalVolume());
                }

                trade.setVolume(order.getTotalVolume());
                trade.setTradeTime(this.dt.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                trade.setDt(this.dt);
                this.strategy.onTrade(trade);

                this.tradeDict.put(tradeID, trade);

                // 推送委托数据
                order.setTradedVolume(order.getTotalVolume());
                order.setStatus(VtConstant.STATUS_ALLTRADED);
                this.strategy.onOrder(order);

                // 从字典中删除该限价单
                if (this.workingLimitOrderDict.containsKey(orderID)) {
                    this.workingLimitOrderDict.remove(orderID);
                }
            }
        }
    }

    // 基于最新数据撮合停止单
    private void crossStopOrder() {
        double buyCrossPrice;
        double sellCrossPrice;
        double bestCrossPrice;

        //boolean buyCross;
        //boolean sellCross;

        //String tradeID;
        //VtTradeData trade;
        //String orderID;
        //VtOrderData order;

        // 先确定会撮合成交的价格，这里和限价单规则相反
        if (BacktestingEngine.BAR_MODE.equals(this.mode)) {
            buyCrossPrice = this.bar.getHigh(); // 若买入方向停止单价格低于该价格，则会成交
            sellCrossPrice = this.bar.getLow(); // 若卖出方向限价单价格高于该价格，则会成交
            bestCrossPrice = this.bar.getOpen(); // 最优成交价，买入停止单不能低于，卖出停止单不能高于
        } else {
            buyCrossPrice = this.tick.getLastPrice();
            sellCrossPrice = this.tick.getLastPrice();
            bestCrossPrice = this.tick.getLastPrice();
        }

        // 遍历停止单字典中的所有停止单
        this.workingStopOrderDict.entrySet().forEach(entry -> {
            //for (String stopOrderID : this.workingStopOrderDict.keySet()) {
            String stopOrderID = entry.getKey();
            StopOrder so = entry.getValue();

            // 判断是否会成交
            boolean buyCross = (VtConstant.DIRECTION_LONG.equals(so.getDirection()) && so.getPrice() <= buyCrossPrice);
            boolean sellCross = (VtConstant.DIRECTION_SHORT.equals(so.getDirection()) && so.getPrice() >= sellCrossPrice);

            // 如果发生了成交
            if (buyCross || sellCross) {
                // 更新停止单状态，并从字典中删除该停止单
                so.setStatus(CtaBaseConstant.STOPORDER_TRIGGERED);
                if (this.workingStopOrderDict.containsKey(stopOrderID)) {
                    this.workingStopOrderDict.remove(stopOrderID);
                }

                // 推送成交数据
                this.tradeCount += 1; // 成交编号自增1
                String tradeID = this.tradeCount + "";
                VtTradeData trade = new VtTradeData();
                trade.setVtSymbol(so.getVtSymbol());
                trade.setTradeID(tradeID);
                trade.setVtTradeID(tradeID);

                if (buyCross) {
                    this.strategy.setPos(this.strategy.getPos() + so.getVolume());
                    trade.setPrice(Math.max(bestCrossPrice, so.getPrice()));
                } else {
                    this.strategy.setPos(this.strategy.getPos() - so.getVolume());
                    trade.setPrice(Math.min(bestCrossPrice, so.getPrice()));
                }

                this.limitOrderCount += 1;
                String orderID = this.limitOrderCount + "";
                trade.setOrderID(orderID);
                trade.setVtOrderID(orderID);
                trade.setDirection(so.getDirection());
                trade.setOffset(so.getOffset());
                trade.setVolume(so.getVolume());
                trade.setTradeTime(this.dt.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                trade.setDt(this.dt);

                this.tradeDict.put(tradeID, trade);

                // 推送委托数据
                VtOrderData order = new VtOrderData();
                order.setVtSymbol(so.getVtSymbol());
                order.setSymbol(so.getVtSymbol());
                order.setOrderID(orderID);
                order.setVtOrderID(orderID);
                order.setDirection(so.getDirection());
                order.setOffset(so.getOffset());
                order.setPrice(so.getPrice());
                order.setTotalVolume(so.getVolume());
                order.setTradedVolume(so.getVolume());
                order.setStatus(VtConstant.STATUS_ALLTRADED);
                order.setOrderTime(trade.getTradeTime());

                this.limitOrderDict.put(orderID, order);

                // 按照顺序推送数据
                this.strategy.onStopOrder(so);
                this.strategy.onOrder(order);
                this.strategy.onTrade(trade);
            }
            //}
        });
    }

    // 更新每日收盘价
    private void updateDailyClose(LocalDateTime dt, double price) {
        LocalDate date = dt.toLocalDate();

        if (!this.dailyResultDict.containsKey(date)) {
            this.dailyResultDict.put(date, new DailyResult(date, price));
        } else {
            this.dailyResultDict.get(date).setClosePrice(price);
        }
    }

    // 取整价格到合约最小价格变动
    private double roundToPriceTick(double price) {
        if (this.priceTick == 0) {
            return price;
        }

        double newPrice = Math.round(price / this.priceTick) * this.priceTick;
        return newPrice;
    }

    // 发停止单（本地实现）
    @Override
    public String[] sendStopOrder(String vtSymbol, String orderType, double price, int volume, CtaTemplate strategy) {
        this.stopOrderCount += 1;
        String stopOrderID = CtaBaseConstant.STOPORDERPREFIX + this.stopOrderCount;

        StopOrder so = new StopOrder();
        so.setVtSymbol(vtSymbol);
        so.setPrice(this.roundToPriceTick(price));
        so.setVolume(volume);
        so.setStrategy(strategy);
        so.setStatus(CtaBaseConstant.STOPORDER_WAITING);
        so.setStopOrderID(stopOrderID);

        if (CtaBaseConstant.CTAORDER_BUY.equals(orderType)) {
            so.setDirection(VtConstant.DIRECTION_LONG);
            so.setOffset(VtConstant.OFFSET_OPEN);
        } else if (CtaBaseConstant.CTAORDER_SELL.equals(orderType)) {
            so.setDirection(VtConstant.DIRECTION_SHORT);
            so.setOffset(VtConstant.OFFSET_CLOSE);
        } else if (CtaBaseConstant.CTAORDER_SHORT.equals(orderType)) {
            so.setDirection(VtConstant.DIRECTION_SHORT);
            so.setOffset(VtConstant.OFFSET_OPEN);
        } else if (CtaBaseConstant.CTAORDER_COVER.equals(orderType)) {
            so.setDirection(VtConstant.DIRECTION_LONG);
            so.setOffset(VtConstant.OFFSET_CLOSE);
        }

        // 保存stopOrder对象到字典中
        this.stopOrderDict.put(stopOrderID, so);
        this.workingStopOrderDict.put(stopOrderID, so);

        // 推送停止单初始更新
        this.strategy.onStopOrder(so);

        return new String[]{stopOrderID};
    }

    // 发单
    @Override
    public String[] sendOrder(String vtSymbol, String orderType, double price, int volume, CtaTemplate ctaTemplate) {
        this.limitOrderCount += 1;
        String orderID = this.limitOrderCount + "";

        VtOrderData order = new VtOrderData();
        order.setVtSymbol(vtSymbol);
        order.setPrice(roundToPriceTick(price));
        order.setTotalVolume(volume);
        order.setOrderID(orderID);
        order.setVtOrderID(orderID);
        order.setOrderTime(this.dt.format(DateTimeFormatter.ofPattern("HH:mm:ss")));

        // CTA委托类型映射
        if (CtaBaseConstant.CTAORDER_BUY.equals(orderType)) {
            order.setDirection(VtConstant.DIRECTION_LONG);
            order.setOffset(VtConstant.OFFSET_OPEN);
        } else if (CtaBaseConstant.CTAORDER_SELL.equals(orderType)) {
            order.setDirection(VtConstant.DIRECTION_SHORT);
            order.setOffset(VtConstant.OFFSET_CLOSE);
        } else if (CtaBaseConstant.CTAORDER_SHORT.equals(orderType)) {
            order.setDirection(VtConstant.DIRECTION_SHORT);
            order.setOffset(VtConstant.OFFSET_OPEN);
        } else if (CtaBaseConstant.CTAORDER_COVER.equals(orderType)) {
            order.setDirection(VtConstant.DIRECTION_LONG);
            order.setOffset(VtConstant.OFFSET_CLOSE);
        }

        // 保存到限价单字典中
        this.workingLimitOrderDict.put(orderID, order);
        this.limitOrderDict.put(orderID, order);

        return new String[]{orderID};
    }

    // 撤销停止单
    @Override
    public void cancelStopOrder(String stopOrderID) {
        // 检查停止单是否存在
        if (this.workingStopOrderDict.containsKey(stopOrderID)) {
            StopOrder so = this.workingStopOrderDict.get(stopOrderID);
            so.setStatus(CtaBaseConstant.STOPORDER_CANCELLED);
            this.workingStopOrderDict.remove(stopOrderID);
            this.strategy.onStopOrder(so);
        }
    }

    // 撤单
    @Override
    public void cancelOrder(String vtOrderID) {
        if (this.workingLimitOrderDict.containsKey(vtOrderID)) {
            VtOrderData order = this.workingLimitOrderDict.get(vtOrderID);

            order.setStatus(VtConstant.STATUS_CANCELLED);
            order.setCancelTime(this.dt.format(DateTimeFormatter.ofPattern("HH:mm:ss")));

            this.strategy.onOrder(order);

            this.workingLimitOrderDict.remove(vtOrderID);
        }
    }

    // 全部撤单
    @Override
    public void cancelAll(String name) {
        // 撤销限价单
        for (String orderID : this.workingLimitOrderDict.keySet()) {
            cancelOrder(orderID);
        }

        // 撤销停止单
        for (String stopOrderID : this.workingStopOrderDict.keySet()) {
            cancelStopOrder(stopOrderID);
        }
    }

    // 考虑到回测中不允许向数据库插入数据，防止实盘交易中的一些代码出错
    @Override
    public void insertData(String dbName, String collectionName, VtBaseData data) {
        return;
    }

    // 读取tick数据
    @Override
    public List<VtTickData> loadTick(String dbName, String collectionName, int days) {
        // 直接返回初始化数据列表中的Tick
        return (List<VtTickData>) (Object) this.initData;
    }

    // 读取bar数据
    @Override
    public List<VtBarData> loadBar(String dbName, String collectionName, int days) {
        // 直接返回初始化数据列表中的Bar
        return (List<VtBarData>) (Object) this.initData;
    }

    // 记录日志
    @Override
    public void writeCtaLog(String content) {
        String log = this.dt + " " + content;
        this.logList.add(log);
    }

    // 发送策略更新事件，回测中忽略
    @Override
    public void putStrategyEvent(String name) {
        return;
    }

    // 获取引擎类型
    @Override
    public String getEngineType() {
        return engineType;
    }

    // 保存同步数据（无效）
    @Override
    public void saveSyncData(CtaTemplate strategy) {
        return;
    }

    // 获取最小价格变动
    @Override
    public double getPriceTick(CtaTemplate ctaTemplate) {
        return this.priceTick;
    }

    // 计算回测结果
    private Map<String, Object> calculateBacktestingResult() {
        this.output("计算回测结果");

        // 检查成交记录
        if (this.tradeDict == null) {
            this.output("成交记录为空，无法计算回测结果");
            return null;
        }
        // 首先基于回测后的成交记录，计算每笔交易的盈亏
        List<TradingResult> resultList = new ArrayList<TradingResult>(); // 交易结果列表

        List<VtTradeData> longTrade = new ArrayList<VtTradeData>(); // 未平仓的多头交易
        List<VtTradeData> shortTrade = new ArrayList<VtTradeData>(); // 未平仓的空头交易

        List<LocalDateTime> tradeTimeList = new ArrayList<LocalDateTime>(); // 每笔成交时间戳
        List<Integer> posList = new ArrayList<Integer>(Collections.nCopies(1, 0)); // 每笔成交后的持仓情况

        //this.tradeDict.entrySet().stream().forEachOrdered((entry)->{
        for (VtTradeData otrade : this.tradeDict.values()) {
            // 复制成交对象，因为下面的开平仓交易配对涉及到对成交数量的修改
            // 若不进行复制直接操作，则计算完后所有成交的数量会变成0
            VtTradeData trade = (VtTradeData) otrade.clone();

            // 多头交易
            if (VtConstant.DIRECTION_LONG.equals(trade.getDirection())) {
                // 如果尚无空头交易
                if (shortTrade.size() == 0) {
                    longTrade.add(trade);
                }
                // 当前多头交易为平空
                else {
                    while (true) {
                        VtTradeData entryTrade = shortTrade.get(0);
                        VtTradeData exitTrade = trade;

                        // 清算开平仓交易
                        int closedVolume = Math.min(exitTrade.getVolume(), entryTrade.getVolume());
                        TradingResult result = new TradingResult(entryTrade.getPrice(), entryTrade.getDt(),
                            exitTrade.getPrice(), exitTrade.getDt(), -closedVolume, this.rate, this.slippage,
                            this.size);
                        resultList.add(result);

                        posList.add(-1);
                        posList.add(0);
                        tradeTimeList.add(result.getEntryDt());
                        tradeTimeList.add(result.getExitDt());

                        // 计算未清算部分
                        entryTrade.setVolume(entryTrade.getVolume() - closedVolume);
                        exitTrade.setVolume(exitTrade.getVolume() - closedVolume);

                        // 如果开仓交易已经全部清算，则从列表中移除
                        if (entryTrade.getVolume() == 0) {
                            shortTrade.remove(0);
                        }

                        // 如果平仓交易已经全部清算，则退出循环
                        if (exitTrade.getVolume() == 0) {
                            break;
                        }

                        // 如果平仓交易未全部清算，
                        if (exitTrade.getVolume() != 0) {
                            // 且开仓交易已经全部清算完，则平仓交易剩余的部分
                            // 等于新的反向开仓交易，添加到队列中
                            if (shortTrade.size() == 0) {
                                longTrade.add(exitTrade);
                                break;
                            }
                            // 如果开仓交易还有剩余，则进入下一轮循环
                            else {
                                // do nothing
                            }
                        }
                    }
                }
            }
            // 空头交易
            else {
                // 如果尚无多头交易
                if (longTrade.size() == 0) {
                    shortTrade.add(trade);
                }
                // 当前空头交易为平多
                else {
                    while (true) {
                        VtTradeData entryTrade = longTrade.get(0);
                        VtTradeData exitTrade = trade;

                        // 清算开平仓交易
                        int closedVolume = Math.min(exitTrade.getVolume(), entryTrade.getVolume());
                        TradingResult result = new TradingResult(entryTrade.getPrice(), entryTrade.getDt(),
                            exitTrade.getPrice(), exitTrade.getDt(), closedVolume, this.rate, this.slippage,
                            this.size);
                        resultList.add(result);

                        posList.add(1);
                        posList.add(0);
                        tradeTimeList.add(result.getEntryDt());
                        tradeTimeList.add(result.getExitDt());

                        // 计算未清算部分
                        entryTrade.setVolume(entryTrade.getVolume() - closedVolume);
                        exitTrade.setVolume(exitTrade.getVolume() - closedVolume);

                        // 如果开仓交易已经全部清算，则从列表中移除
                        if (entryTrade.getVolume() == 0) {
                            longTrade.remove(0);
                        }

                        // 如果平仓交易已经全部清算，则退出循环
                        if (exitTrade.getVolume() == 0) {
                            break;
                        }

                        // 如果平仓交易未全部清算，
                        if (exitTrade.getVolume() != 0) {
                            // 且开仓交易已经全部清算完，则平仓交易剩余的部分
                            // 等于新的反向开仓交易，添加到队列中
                            if (longTrade.size() == 0) {
                                shortTrade.add(exitTrade);
                                break;
                            }
                            // 如果开仓交易还有剩余，则进入下一轮循环
                            else {
                                // pass
                            }
                        }
                    }
                }
            }
        }
        //});

        // 到最后交易日尚未平仓的交易，则以最后价格平仓
        double endPrice;
        if (this.BAR_MODE.equals(this.mode)) {
            endPrice = this.bar.getClose();
        } else {
            endPrice = this.tick.getLastPrice();
        }

        TradingResult rslt;
        for (VtTradeData trade : longTrade) {
            rslt = new TradingResult(trade.getPrice(), trade.getDt(), endPrice, this.dt, trade.getVolume(), this.rate,
                this.slippage, this.size);
            resultList.add(rslt);
        }

        for (VtTradeData trade : shortTrade) {
            rslt = new TradingResult(trade.getPrice(), trade.getDt(), endPrice, this.dt, -trade.getVolume(), this.rate,
                this.slippage, this.size);
            resultList.add(rslt);
        }

        // 检查是否有交易
        if (resultList.size() == 0) {
            this.output("无交易结果");
            return null;
        }

        // 然后基于每笔交易的结果，我们可以计算具体的盈亏曲线和最大回撤等
        double capital = 0; // 资金
        double maxCapital = 0; // 资金最高净值
        double drawdown = 0; // 回撤

        int totalResult = 0; // 总成交数量
        double totalTurnover = 0; // 总成交金额（合约面值）
        double totalCommission = 0; // 总手续费
        double totalSlippage = 0; // 总滑点

        List<LocalDateTime> timeList = new ArrayList<LocalDateTime>(); // 时间序列
        List<Double> pnlList = new ArrayList<Double>(); // 每笔盈亏序列
        List<Double> capitalList = new ArrayList<Double>(); // 盈亏汇总的时间序列
        List<Double> drawdownList = new ArrayList<Double>(); // 回撤的时间序列

        int winningResult = 0; // 盈利次数
        int losingResult = 0; // 亏损次数
        int totalWinning = 0; // 总盈利金额
        int totalLosing = 0; // 总亏损金额

        //resultList.stream().forEachOrdered(result->{
        for (TradingResult result : resultList) {
            capital += result.getPnl();
            maxCapital = Math.max(capital, maxCapital);
            drawdown = capital - maxCapital;

            pnlList.add(result.getPnl());
            timeList.add(result.getExitDt()); // 交易的时间戳使用平仓时间
            capitalList.add(capital);
            drawdownList.add(drawdown);

            totalResult += 1;
            totalTurnover += result.getTurnover();
            totalCommission += result.getCommission();
            totalSlippage += result.getSlippage();

            if (result.getPnl() >= 0) {
                winningResult += 1;
                totalWinning += result.getPnl();
            } else {
                losingResult += 1;
                totalLosing += result.getPnl();
            }
        }
        //});

        // 计算盈亏相关数据
        double winningRate = (double) winningResult / (double) totalResult * 100; // 胜率

        double averageWinning = 0; // 这里把数据都初始化为0
        double averageLosing = 0;
        double profitLossRatio = 0;

        if (winningResult != 0) {
            averageWinning = totalWinning / winningResult; // 平均每笔盈利
        }
        if (losingResult != 0) {
            averageLosing = totalLosing / losingResult; // 平均每笔亏损
        }
        if (averageLosing != 0) {
            profitLossRatio = -averageWinning / averageLosing; // 盈亏比
        }

        // 返回回测结果
        Map<String, Object> d = new HashMap<String, Object>();
        d.put("capital", capital);
        d.put("maxCapital", maxCapital);
        d.put("drawdown", drawdown);
        d.put("totalResult", totalResult);
        d.put("totalTurnover", totalTurnover);
        d.put("totalCommission", totalCommission);
        d.put("totalSlippage", totalSlippage);
        d.put("timeList", timeList);
        d.put("pnlList", pnlList);
        d.put("capitalList", capitalList);
        d.put("drawdownList", drawdownList);
        d.put("winningRate", winningRate);
        d.put("averageWinning", averageWinning);
        d.put("averageLosing", averageLosing);
        d.put("profitLossRatio", profitLossRatio);
        d.put("posList", posList);
        d.put("tradeTimeList", tradeTimeList);
        d.put("resultList", resultList);

        return d;
    }

    // 显示回测结果
    public void showBacktestingResult() {
        Map<String, Object> d = this.calculateBacktestingResult();

        if (d == null) {
            return;
        }
        // 输出
        this.output("------------------------------");
        this.output("第一笔交易：\t" + ((ArrayList<LocalDateTime>) d.get("timeList")).get(0));
        this.output("最后一笔交易：\t" + ((ArrayList<LocalDateTime>) d.get("timeList"))
            .get(((ArrayList<LocalDateTime>) d.get("timeList")).size() - 1));

        this.output("总交易次数：\t" + formatNumber((int) d.get("totalResult")));
        this.output("总盈亏：\t" + formatNumber((double) d.get("capital")));
        this.output("最大回撤: \t" + formatNumber(Collections.min((ArrayList<Double>) d.get("drawdownList"))));

        this.output("平均每笔盈利：\t" + formatNumber((double) d.get("capital") / (int) d.get("totalResult")));
        this.output("平均每笔滑点：\t" + formatNumber((double) d.get("totalSlippage") / (int) d.get("totalResult")));
        this.output("平均每笔佣金：\t" + formatNumber((double) d.get("totalCommission") / (int) d.get("totalResult")));

        this.output("胜率\t\t" + formatNumber((double) d.get("winningRate")) + "%");
        this.output("盈利交易平均值\t" + formatNumber((double) d.get("averageWinning")));
        this.output("亏损交易平均值\t" + formatNumber((double) d.get("averageLosing")));
        this.output("盈亏比：\t" + formatNumber((double) d.get("profitLossRatio")));
    }

    // 格式化数字到字符串
    private String formatNumber(double n) {
        DecimalFormat df = new DecimalFormat("###,###,###,##0.00");
        return df.format(n);
    }

    public static void main(String[] args) {
        // Map<String, Integer> map1 = new HashMap<String, Integer>();
        Map<String, Integer> map1 = new ConcurrentHashMap<String, Integer>();

        for (int i = 0; i < 200000; i++) {
            map1.put("" + i, i);
        }

        map1.entrySet().stream().forEach(entry -> {
            //System.out.println(e);
            entry.getKey();

        });

    }

}
