package vnpy.trader;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class VtTickData extends VtBaseData {
	
	// 代码相关
	private String symbol; // 合约代码
	private String exchange; // 交易所代码
	private String vtSymbol; // 合约在vt系统中的唯一代码，通常是 合约代码.交易所代码

	// 成交数据
	private double lastPrice; // 最新成交价
	private int lastVolume; // 最新成交量
	private int volume; // 今天总成交量
	private int openInterest; // 持仓量
	private String time; // 时间 11:20:56.5
	private String date; // 日期 20151009
	private LocalDateTime datetime; // python的datetime时间对象

	private double turnover; //成交额
	
	// 常规行情
	private double openPrice; // 今日开盘价
	private double highPrice; // 今日最高价
	private double lowPrice; // 今日最低价
	private double preClosePrice;

	private double upperLimit; // 涨停价
	private double lowerLimit; // 跌停价

	// 五档行情
	private double bidPrice1;
	private double bidPrice2;
	private double bidPrice3;
	private double bidPrice4;
	private double bidPrice5;

	private double askPrice1;
	private double askPrice2;
	private double askPrice3;
	private double askPrice4;
	private double askPrice5;

	private int bidVolume1;
	private int bidVolume2;
	private int bidVolume3;
	private int bidVolume4;
	private int bidVolume5;

	private int askVolume1;
	private int askVolume2;
	private int askVolume3;
	private int askVolume4;
	private int askVolume5;

	private double bt1;
	private double st1;
	private double bt2;
	private double st2;
	private double wpr;
	private double ret;

	public static VtTickData createFromGateway(VtGateway gateway, String symbol, String exchange,
            double lastPrice, int lastVolume,
            double highPrice, double lowPrice,
            double openPrice,
            int openInterest,
            double upperLimit,
            double lowerLimit) {
		VtTickData tick = new VtTickData();
		tick.setGatewayName( gateway.getGatewayName());
		tick.symbol = symbol;
		tick.exchange = exchange;
		tick.vtSymbol = symbol + '.' + exchange;

		tick.lastPrice = lastPrice;
		tick.lastVolume = lastVolume;
		tick.openInterest = openInterest;
		tick.datetime = LocalDateTime.now();
		tick.date = tick.datetime.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
		tick.time = tick.datetime.format(DateTimeFormatter.ofPattern("HH:mm:ss"));

		tick.openPrice = openPrice;
		tick.highPrice = highPrice;
		tick.lowPrice = lowPrice;
		tick.upperLimit = upperLimit;
		tick.lowerLimit = lowerLimit;
		return tick;
    }

	public double getBt1() {
		return bt1;
	}

	public void setBt1(double bt1) {
		this.bt1 = bt1;
	}

	public double getSt1() {
		return st1;
	}

	public void setSt1(double st1) {
		this.st1 = st1;
	}

	public double getBt2() {
		return bt2;
	}

	public void setBt2(double bt2) {
		this.bt2 = bt2;
	}

	public double getSt2() {
		return st2;
	}

	public void setSt2(double st2) {
		this.st2 = st2;
	}

	public double getWpr() {
		return wpr;
	}

	public void setWpr(double wpr) {
		this.wpr = wpr;
	}

	public double getRet() {
		return ret;
	}

	public void setRet(double ret) {
		this.ret = ret;
	}

	
	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public String getExchange() {
		return exchange;
	}

	public void setExchange(String exchange) {
		this.exchange = exchange;
	}

	public String getVtSymbol() {
		return vtSymbol;
	}

	public void setVtSymbol(String vtSymbol) {
		this.vtSymbol = vtSymbol;
	}

	public double getLastPrice() {
		return lastPrice;
	}

	public void setLastPrice(double lastPrice) {
		this.lastPrice = lastPrice;
	}

	public int getLastVolume() {
		return lastVolume;
	}

	public void setLastVolume(int lastVolume) {
		this.lastVolume = lastVolume;
	}

	public int getVolume() {
		return volume;
	}

	public void setVolume(int volume) {
		this.volume = volume;
	}

	public int getOpenInterest() {
		return openInterest;
	}

	public void setOpenInterest(int openInterest) {
		this.openInterest = openInterest;
	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public LocalDateTime getDatetime() {
		return datetime;
	}

	public void setDatetime(LocalDateTime datetime) {
		this.datetime = datetime;
	}

	public double getOpenPrice() {
		return openPrice;
	}

	public void setOpenPrice(double openPrice) {
		this.openPrice = openPrice;
	}

	public double getHighPrice() {
		return highPrice;
	}

	public void setHighPrice(double highPrice) {
		this.highPrice = highPrice;
	}

	public double getLowPrice() {
		return lowPrice;
	}

	public void setLowPrice(double lowPrice) {
		this.lowPrice = lowPrice;
	}

	public double getPreClosePrice() {
		return preClosePrice;
	}

	public void setPreClosePrice(double preClosePrice) {
		this.preClosePrice = preClosePrice;
	}

	public double getUpperLimit() {
		return upperLimit;
	}

	public void setUpperLimit(double upperLimit) {
		this.upperLimit = upperLimit;
	}

	public double getLowerLimit() {
		return lowerLimit;
	}

	public void setLowerLimit(double lowerLimit) {
		this.lowerLimit = lowerLimit;
	}

	public double getBidPrice1() {
		return bidPrice1;
	}

	public void setBidPrice1(double bidPrice1) {
		this.bidPrice1 = bidPrice1;
	}

	public double getBidPrice2() {
		return bidPrice2;
	}

	public void setBidPrice2(double bidPrice2) {
		this.bidPrice2 = bidPrice2;
	}

	public double getBidPrice3() {
		return bidPrice3;
	}

	public void setBidPrice3(double bidPrice3) {
		this.bidPrice3 = bidPrice3;
	}

	public double getBidPrice4() {
		return bidPrice4;
	}

	public void setBidPrice4(double bidPrice4) {
		this.bidPrice4 = bidPrice4;
	}

	public double getBidPrice5() {
		return bidPrice5;
	}

	public void setBidPrice5(double bidPrice5) {
		this.bidPrice5 = bidPrice5;
	}

	public double getAskPrice1() {
		return askPrice1;
	}

	public void setAskPrice1(double askPrice1) {
		this.askPrice1 = askPrice1;
	}

	public double getAskPrice2() {
		return askPrice2;
	}

	public void setAskPrice2(double askPrice2) {
		this.askPrice2 = askPrice2;
	}

	public double getAskPrice3() {
		return askPrice3;
	}

	public void setAskPrice3(double askPrice3) {
		this.askPrice3 = askPrice3;
	}

	public double getAskPrice4() {
		return askPrice4;
	}

	public void setAskPrice4(double askPrice4) {
		this.askPrice4 = askPrice4;
	}

	public double getAskPrice5() {
		return askPrice5;
	}

	public void setAskPrice5(double askPrice5) {
		this.askPrice5 = askPrice5;
	}

	public int getBidVolume1() {
		return bidVolume1;
	}

	public void setBidVolume1(int bidVolume1) {
		this.bidVolume1 = bidVolume1;
	}

	public int getBidVolume2() {
		return bidVolume2;
	}

	public void setBidVolume2(int bidVolume2) {
		this.bidVolume2 = bidVolume2;
	}

	public int getBidVolume3() {
		return bidVolume3;
	}

	public void setBidVolume3(int bidVolume3) {
		this.bidVolume3 = bidVolume3;
	}

	public int getBidVolume4() {
		return bidVolume4;
	}

	public void setBidVolume4(int bidVolume4) {
		this.bidVolume4 = bidVolume4;
	}

	public int getBidVolume5() {
		return bidVolume5;
	}

	public void setBidVolume5(int bidVolume5) {
		this.bidVolume5 = bidVolume5;
	}

	public int getAskVolume1() {
		return askVolume1;
	}

	public void setAskVolume1(int askVolume1) {
		this.askVolume1 = askVolume1;
	}

	public int getAskVolume2() {
		return askVolume2;
	}

	public void setAskVolume2(int askVolume2) {
		this.askVolume2 = askVolume2;
	}

	public int getAskVolume3() {
		return askVolume3;
	}

	public void setAskVolume3(int askVolume3) {
		this.askVolume3 = askVolume3;
	}

	public int getAskVolume4() {
		return askVolume4;
	}

	public void setAskVolume4(int askVolume4) {
		this.askVolume4 = askVolume4;
	}

	public int getAskVolume5() {
		return askVolume5;
	}

	public void setAskVolume5(int askVolume5) {
		this.askVolume5 = askVolume5;
	}

	public double getTurnover() {
		return turnover;
	}


	public void setTurnover(double turnover) {
		this.turnover = turnover;
	}
	
}
