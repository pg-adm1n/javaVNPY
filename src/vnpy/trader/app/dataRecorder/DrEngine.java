package vnpy.trader.app.dataRecorder;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;

import vnpy.event.Event;
import vnpy.event.EventEngine;
import vnpy.event.EventType;
import vnpy.gson.property.bean.DrSettingBean;
import vnpy.trader.BarGenerator;
import vnpy.trader.MainEngine;
import vnpy.trader.VtAppModule;
import vnpy.trader.VtBarData;
import vnpy.trader.VtLogData;
import vnpy.trader.VtSubscribeReq;
import vnpy.trader.VtTickData;
import vnpy.trader.app.ctaStrategy.CtaBaseConstant;
import vnpy.utils.AppException;
import vnpy.utils.Method;

// 数据记录引擎
public class DrEngine extends VtAppModule{

	private static final String settingFileName = "DR_setting.json";
	private static final String settingFilePath = "input/" + settingFileName;

	//private MainEngine mainEngine;
	//private EventEngine eventEngine;
	private LocalDate today;
	private Map<String, String> activeSymbolDict;
	private Set<String> tickSymbolSet;
	private Map<String, BarGenerator> bgDict;
	private Map<String, Map<String, Object>> settingDict;
	private boolean active;
	private LinkedBlockingQueue<Object[]> queue;
	private Thread __thread;
	private LocalTime marketCloseTime;
	private LocalTime lastTimerTime;
	private int timerCount;

	public DrEngine(MainEngine mainEngine, EventEngine eventEngine) {
		super(mainEngine, eventEngine);
		
		//this.mainEngine = mainEngine;
		//this.eventEngine = eventEngine;

		this.setAppName("DataRecorder");
		
		// 当前日期
		this.today = LocalDate.now();

		// 主力合约代码映射字典，key为具体的合约代码（如IF1604），value为主力合约代码（如IF0000）
		this.activeSymbolDict = new HashMap<String, String>();

		// Tick对象字典
		this.tickSymbolSet = new HashSet<String>();

		// K线合成器字典
		this.bgDict = new HashMap<String, BarGenerator>();

		// 配置字典
		this.settingDict = new LinkedHashMap<String, Map<String, Object>>();

		// 负责执行数据库插入的单独线程相关
		this.active = false; // 工作状态
		this.queue = new LinkedBlockingQueue<Object[]>(); // 队列
		this.__thread = new Thread() {
			@Override
			public void run() {
				__run();
			}
		}; // 线程

		// 收盘相关
		this.marketCloseTime = null; // 收盘时间
		this.timerCount = 0; // 定时器计数
		this.lastTimerTime = null; // 上一次记录时间

		// 载入设置，订阅行情
		this.loadSetting();

		// 启动数据插入线程
		this.start();

		// 注册事件监听
		this.registerEvent();
	}

	// 运行插入线程
	private synchronized void __run() {
		while (this.active) {
			try {
				Object[] rtn = this.queue.poll(1000, TimeUnit.MILLISECONDS);
				if (rtn == null) {
					continue;
				}
				String dbName = (String) rtn[0];
				String collectionName = (String) rtn[1];
				Object d = rtn[2];
				// 使用insert模式更新数据，可能存在时间戳重复的情况，需要用户自行清洗
				this.mainEngine.dbInsert(dbName, collectionName, d);
				// self.writeDrLog(u'键值重复插入失败，报错信息：%s' %traceback.format_exc())
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	// 加载配置
	private void loadSetting() {
		try (BufferedReader br = new BufferedReader(new FileReader(settingFilePath))) {

			Gson gson = new Gson();
			DrSettingBean drSetting = gson.fromJson(br, DrSettingBean.class);

			// 如果working设为False则不启动行情记录功能
			boolean working = drSetting.isWorking();
			if (!working) {
				return;
			}

			// 加载收盘时间
			String timestamp = drSetting.getMarketCloseTime();
			if (timestamp != null && !"".equals(timestamp.trim())) {
				this.marketCloseTime = LocalTime.parse(timestamp, DateTimeFormatter.ofPattern("HH:mm:ss"));
			}

			List<List<String>> l;
			// Tick记录配置
			if (drSetting.getTick() != null) {
				l = drSetting.getTick();
				for (List<String> setting : l) {
					String symbol = setting.get(0);
					String gateway = setting.get(1);
					String vtSymbol = symbol;

					VtSubscribeReq req = new VtSubscribeReq();
					req.setSymbol(setting.get(0));

					// 针对LTS和IB接口，订阅行情需要交易所代码
					if (setting.size() >= 3) {
						req.setExchange(setting.get(2));
						vtSymbol = symbol + "." + req.getExchange();
					}

					// 针对IB接口，订阅行情需要货币和产品类型
					if (setting.size() >= 5) {
						req.setCurrency(setting.get(3));
						req.setProductClass(setting.get(4));
					}

					this.mainEngine.subscribe(req, gateway);

					this.tickSymbolSet.add(vtSymbol);

					// 保存到配置字典中
					if (!this.settingDict.containsKey(vtSymbol)) {
						Map<String, Object> d = new HashMap<String, Object>();
						d.put("symbol", symbol);
						d.put("gateway", gateway);
						d.put("tick", true);
						this.settingDict.put(vtSymbol, d);
					} else {
						Map<String, Object> d = this.settingDict.get(vtSymbol);
						d.put("tick", true);
					}
				}

				// 分钟线记录配置
				if (drSetting.getBar() != null) {
					l = drSetting.getBar();
					for (List<String> setting : l) {
						String symbol = setting.get(0);
						String gateway = setting.get(1);
						String vtSymbol = symbol;

						VtSubscribeReq req = new VtSubscribeReq();
						req.setSymbol(setting.get(0));

						// 针对LTS和IB接口，订阅行情需要交易所代码
						if (setting.size() >= 3) {
							req.setExchange(setting.get(2));
							vtSymbol = symbol + "." + req.getExchange();
						}

						// 针对IB接口，订阅行情需要货币和产品类型
						if (setting.size() >= 5) {
							req.setCurrency(setting.get(3));
							req.setProductClass(setting.get(4));
						}
						
						this.mainEngine.subscribe(req, gateway);

						// 保存到配置字典中
						if (!this.settingDict.containsKey(vtSymbol)) {
							Map<String, Object> d = new HashMap<String, Object>();
							d.put("symbol", symbol);
							d.put("gateway", gateway);
							d.put("bar", true);
							this.settingDict.put(vtSymbol, d);
						} else {
							Map<String, Object> d = this.settingDict.get(vtSymbol);
							d.put("bar", true);
						}

						// 创建BarManager对象
						this.bgDict.put(vtSymbol, new BarGenerator(new Method(this, "onBar", VtBarData.class)));
					}

				}

				// 主力合约记录配置
				if (drSetting.getActive() != null) {
					Map<String, String> d = drSetting.getActive();
					for (Entry<String, String> entry : d.entrySet()) {
						this.activeSymbolDict.put(entry.getValue(), entry.getKey());
					}
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new AppException("读取DR_setting.json失败");
		}
	}

	// 分钟线更新
	private void onBar(VtBarData bar) {
		String vtSymbol = bar.getVtSymbol();

		this.insertData(CtaBaseConstant.MINUTE_DB_NAME, vtSymbol, bar);

		if (this.activeSymbolDict.containsKey(vtSymbol)) {
			String activeSymbol = this.activeSymbolDict.get(vtSymbol);
			this.insertData(CtaBaseConstant.MINUTE_DB_NAME, activeSymbol, bar);
		}

		this.writeDrLog(String.format(Text.BAR_LOGGING_MESSAGE, bar.getVtSymbol(), bar.getTime(), bar.getOpen(),
				bar.getHigh(), bar.getLow(), bar.getClose()));
	}

	// 插入数据到数据库（这里的data可以是VtTickData或者VtBarData）
	private void insertData(String dbName, String collectionName, Object data) {
		try {
			this.queue.put(new Object[] { dbName, collectionName, data });
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	// 启动
	private void start() {
		this.active = true;
		this.__thread.start();
	}

	// 注册事件监听
	private void registerEvent() {
		this.eventEngine.register(EventType.EVENT_TICK, new Method(this, "procecssTickEvent", Event.class));
		this.eventEngine.register(EventType.EVENT_TIMER, new Method(this, "processTimerEvent", Event.class));
	}

	// 处理行情事件
	private void procecssTickEvent(Event event) {
		VtTickData tick = (VtTickData) event.getDict_().get("data");
		String vtSymbol = tick.getVtSymbol();

		// 生成datetime对象
		if (tick.getDatetime() == null) {
			if (tick.getTime().contains(".")) {
				tick.setDatetime(LocalDateTime.parse(tick.getDate() + " " + tick.getTime(),
						DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss.S")));
			} else {
				tick.setDatetime(LocalDateTime.parse(tick.getDate() + " " + tick.getTime(),
						DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss")));
			}
		}

		this.onTick(tick);

		BarGenerator bm = this.bgDict.get(vtSymbol);
		if (bm != null) {
			bm.updateTick(tick);
		}
	}

	// Tick更新
	private void onTick(VtTickData tick) {
		String vtSymbol = tick.getVtSymbol();

		if (this.tickSymbolSet.contains(vtSymbol)) {
			this.insertData(CtaBaseConstant.TICK_DB_NAME, vtSymbol, tick);

			if (this.activeSymbolDict.containsKey(vtSymbol)) {
				String activeSymbol = this.activeSymbolDict.get(vtSymbol);
				this.insertData(CtaBaseConstant.TICK_DB_NAME, activeSymbol, tick);
			}

			this.writeDrLog(String.format(Text.TICK_LOGGING_MESSAGE, tick.getVtSymbol(), tick.getTime(),
					tick.getLastPrice(), tick.getBidPrice1(), tick.getAskPrice1()));
		}
	}

	// 处理定时事件
	private void processTimerEvent(Event event) {
        // 如果没有设置收盘时间，则无需处理
    	if (this.marketCloseTime == null) {
			return;
		}
        
        // 10秒检查一次
        this.timerCount += 1;
        if (this.timerCount < 10)
            return;
        this.timerCount = 0;
        
        // 获取当前时间
        LocalTime currentTime = LocalTime.now();

        if (this.lastTimerTime == null) {
			this.lastTimerTime = currentTime;
			return;
		}
        
        // 上一个时间戳尚未到收盘时间，且当前时间戳已经到收盘时间
        if ((this.lastTimerTime.compareTo(this.marketCloseTime) <0 ) && (currentTime.compareTo(marketCloseTime)>=0)) {
        	// 强制所有的K线生成器立即完成K线
        	for (BarGenerator bg : this.bgDict.values()) {
				bg.generate();
			}
		}
        
        // 记录新的时间
        this.lastTimerTime = currentTime;
    }

	// 快速发出日志事件
	private void writeDrLog(String content) {
		VtLogData log = new VtLogData();
		log.setLogContent(content);
		Event event = new Event(EventType.EVENT_DATARECORDER_LOG);
		event.getDict_().put("data", log);
		this.eventEngine.put(event);
	}

	public static void main(String[] args) {
		System.out.println(String.format("%s = %s", 1, 2));

	}
}
