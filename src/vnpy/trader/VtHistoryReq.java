package vnpy.trader;

import java.time.LocalDateTime;

// 查询历史数据时传入的对象类
public class VtHistoryReq {
	private String symbol;              // 代码
	private String exchange;            // 交易所
	private String vtSymbol;            // VT合约代码
	        
	private String interval;           // K线周期
	private LocalDateTime start;                       // 起始时间datetime对象
	private LocalDateTime end;                         // 结束时间datetime对象
	
	
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
	public String getInterval() {
		return interval;
	}
	public void setInterval(String interval) {
		this.interval = interval;
	}
	public LocalDateTime getStart() {
		return start;
	}
	public void setStart(LocalDateTime start) {
		this.start = start;
	}
	public LocalDateTime getEnd() {
		return end;
	}
	public void setEnd(LocalDateTime end) {
		this.end = end;
	}
}
