package vnpy.trader.app.ctaStrategy;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import vnpy.trader.VtConstant;
import vnpy.trader.VtTradeData;

public class DailyResult {
	private LocalDate date; // 日期
	private double closePrice; // 当日收盘价
	private double previousClose; // 昨日收盘价

	private List<VtTradeData> tradeList; // 成交列表
	private int tradeCount; // 成交数量

	private int openPosition; // 开盘时的持仓
	private int closePosition; // 收盘时的持仓

	private double tradingPnl; // 交易盈亏
	private double positionPnl; // 持仓盈亏
	private double totalPnl; // 总盈亏

	private double turnover; // 成交量
	private double commission; // 手续费
	private double slippage; // 滑点
	private double netPnl; // 净盈亏

	public DailyResult(LocalDate date, double closePrice) {
		this.date = date; // 日期
		this.closePrice = closePrice; // 当日收盘价
		this.previousClose = 0; // 昨日收盘价

		this.tradeList = new ArrayList<VtTradeData>(); // 成交列表
		this.tradeCount = 0; // 成交数量

		this.openPosition = 0; // 开盘时的持仓
		this.closePosition = 0; // 收盘时的持仓

		this.tradingPnl = 0; // 交易盈亏
		this.positionPnl = 0; // 持仓盈亏
		this.totalPnl = 0; // 总盈亏

		this.turnover = 0; // 成交量
		this.commission = 0; // 手续费
		this.slippage = 0; // 滑点
		this.netPnl = 0; // 净盈亏
	}
	
	// 添加交易
    public void addTrade(VtTradeData trade) {
        this.tradeList.add(trade);
    }
    
    public void calculatePnl() {
    	calculatePnl(0, 1, 0, 0);
	}
    
    public void calculatePnl(int openPosition) {
    	calculatePnl(openPosition, 1, 0, 0);
	}
    
    public void calculatePnl(int openPosition, int size) {
    	calculatePnl(openPosition, size, 0, 0);
	}
    
    public void calculatePnl(int openPosition, int size, double rate) {
    	calculatePnl(openPosition, size, rate, 0);
	}
    
    public void calculatePnl(int openPosition, int size, double rate, double slippage) {
//        计算盈亏
//        size: 合约乘数
//        rate：手续费率
//        slippage：滑点点数

        // 持仓部分
        this.openPosition = openPosition;
        this.positionPnl = this.openPosition * (this.closePrice - this.previousClose) * size;
        this.closePosition = this.openPosition;
        
        // 交易部分
        this.tradeCount = this.tradeList.size();
        
        int posChange;
        for (VtTradeData trade : tradeList) {
			if (VtConstant.DIRECTION_LONG.equals(trade.getDirection())) {
				posChange = trade.getVolume();
			} else {
				posChange = -trade.getVolume();
			}
			
			this.tradingPnl += posChange * (this.closePrice - trade.getPrice()) * size;
			this.closePosition += posChange;
		    this.turnover += trade.getPrice() * trade.getVolume() * size;
		    this.commission += trade.getPrice() * trade.getVolume() * size * rate;
		    this.slippage += trade.getVolume() * size * slippage;
		}
        
        // 汇总
        this.totalPnl = this.tradingPnl + this.positionPnl;
        this.netPnl = this.totalPnl - this.commission - this.slippage;
    }

    
    /////////////////Getter Setter////////////////////////
	public LocalDate getDate() {
		return date;
	}

	public void setDate(LocalDate date) {
		this.date = date;
	}

	public double getClosePrice() {
		return closePrice;
	}

	public void setClosePrice(double closePrice) {
		this.closePrice = closePrice;
	}

	public double getPreviousClose() {
		return previousClose;
	}

	public void setPreviousClose(double previousClose) {
		this.previousClose = previousClose;
	}

	public List<VtTradeData> getTradeList() {
		return tradeList;
	}

	public void setTradeList(List<VtTradeData> tradeList) {
		this.tradeList = tradeList;
	}

	public int getTradeCount() {
		return tradeCount;
	}

	public void setTradeCount(int tradeCount) {
		this.tradeCount = tradeCount;
	}

	public int getOpenPosition() {
		return openPosition;
	}

	public void setOpenPosition(int openPosition) {
		this.openPosition = openPosition;
	}

	public int getClosePosition() {
		return closePosition;
	}

	public void setClosePosition(int closePosition) {
		this.closePosition = closePosition;
	}

	public double getTradingPnl() {
		return tradingPnl;
	}

	public void setTradingPnl(double tradingPnl) {
		this.tradingPnl = tradingPnl;
	}

	public double getPositionPnl() {
		return positionPnl;
	}

	public void setPositionPnl(double positionPnl) {
		this.positionPnl = positionPnl;
	}

	public double getTotalPnl() {
		return totalPnl;
	}

	public void setTotalPnl(double totalPnl) {
		this.totalPnl = totalPnl;
	}

	public double getTurnover() {
		return turnover;
	}

	public void setTurnover(double turnover) {
		this.turnover = turnover;
	}

	public double getCommission() {
		return commission;
	}

	public void setCommission(double commission) {
		this.commission = commission;
	}

	public double getSlippage() {
		return slippage;
	}

	public void setSlippage(double slippage) {
		this.slippage = slippage;
	}

	public double getNetPnl() {
		return netPnl;
	}

	public void setNetPnl(double netPnl) {
		this.netPnl = netPnl;
	}
    
}
