package vnpy.trader;

// 撤单时传入的对象类
public class VtCancelOrderReq {
	private String symbol;              // 代码
	private String exchange;            // 交易所
	private String vtSymbol;            // VT合约代码
	        
	// 以下字段主要和CTP、LTS类接口相关
	private String orderID;             // 报单号
	private String frontID;             // 前置机号
	private String sessionID;           // 会话号
	
	
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
	public String getOrderID() {
		return orderID;
	}
	public void setOrderID(String orderID) {
		this.orderID = orderID;
	}
	public String getFrontID() {
		return frontID;
	}
	public void setFrontID(String frontID) {
		this.frontID = frontID;
	}
	public String getSessionID() {
		return sessionID;
	}
	public void setSessionID(String sessionID) {
		this.sessionID = sessionID;
	}
}
