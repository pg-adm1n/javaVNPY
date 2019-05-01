package vnpy.trader;

// 订单数据类
public class VtOrderData extends VtBaseData{
	// 代码编号相关
    private String symbol;              // 合约代码
    private String exchange; // 交易所代码
    private String vtSymbol;  // 索引，统一格式：f"{symbol}.{exchange}"
    
    private String orderID;    // 订单编号 gateway内部自己生成的编号
    private String vtOrderID;  // 索引，统一格式：f"{gatewayName}.{orderId}"
    
    // 报单相关
    private String direction;          // 报单方向
    private String offset;             // 报单开平仓
    private double price;                // 报单价格
    private int totalVolume;            // 报单总数量
    private int tradedVolume;           // 报单成交数量
    private String status;             // 报单状态
    
    private String orderTime;           // 发单时间
    private String cancelTime;          // 撤单时间
    
    // CTP/LTS相关
    private int frontID;                // 前置机编号
    private int sessionID;              // 连接编号
    
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
	public String getVtOrderID() {
		return vtOrderID;
	}
	public void setVtOrderID(String vtOrderID) {
		this.vtOrderID = vtOrderID;
	}
	public String getDirection() {
		return direction;
	}
	public void setDirection(String direction) {
		this.direction = direction;
	}
	public String getOffset() {
		return offset;
	}
	public void setOffset(String offset) {
		this.offset = offset;
	}
	public double getPrice() {
		return price;
	}
	public void setPrice(double price) {
		this.price = price;
	}
	public int getTotalVolume() {
		return totalVolume;
	}
	public void setTotalVolume(int totalVolume) {
		this.totalVolume = totalVolume;
	}
	public int getTradedVolume() {
		return tradedVolume;
	}
	public void setTradedVolume(int tradedVolume) {
		this.tradedVolume = tradedVolume;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getOrderTime() {
		return orderTime;
	}
	public void setOrderTime(String orderTime) {
		this.orderTime = orderTime;
	}
	public String getCancelTime() {
		return cancelTime;
	}
	public void setCancelTime(String cancelTime) {
		this.cancelTime = cancelTime;
	}
	public int getFrontID() {
		return frontID;
	}
	public void setFrontID(int frontID) {
		this.frontID = frontID;
	}
	public int getSessionID() {
		return sessionID;
	}
	public void setSessionID(int sessionID) {
		this.sessionID = sessionID;
	}
}
