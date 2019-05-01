package vnpy.trader;

import java.util.ArrayList;
import java.util.List;

// K线时间序列数据
public class VtHistoryData {
	private String vtSymbol; // vt系统代码
	private String symbol; // 代码
	private String exchange; // 交易所

	private String interval; // K线时间周期
	private String queryID; // 查询号
	private List<VtBarData> barList; // VtBarData列表
	
	public VtHistoryData() {
		barList = new ArrayList<VtBarData>();
	}

	public String getVtSymbol() {
		return vtSymbol;
	}

	public void setVtSymbol(String vtSymbol) {
		this.vtSymbol = vtSymbol;
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

	public String getInterval() {
		return interval;
	}

	public void setInterval(String interval) {
		this.interval = interval;
	}

	public String getQueryID() {
		return queryID;
	}

	public void setQueryID(String queryID) {
		this.queryID = queryID;
	}

	public List<VtBarData> getBarList() {
		return barList;
	}

	public void setBarList(List<VtBarData> barList) {
		this.barList = barList;
	}
}
