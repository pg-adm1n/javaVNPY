package vnpy.trader.app.ctaStrategy;

// 本地停止单
public class StopOrder {
	private String vtSymbol;
	private String orderType;
	private String direction;
	private String offset;
	private double price;
	private int volume;

	private CtaTemplate strategy; // 下停止单的策略对象
	private String stopOrderID; // 停止单的本地编号
	private String status; // 停止单状态
	
	
	public String getVtSymbol() {
		return vtSymbol;
	}
	public void setVtSymbol(String vtSymbol) {
		this.vtSymbol = vtSymbol;
	}
	public String getOrderType() {
		return orderType;
	}
	public void setOrderType(String orderType) {
		this.orderType = orderType;
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
	public int getVolume() {
		return volume;
	}
	public void setVolume(int volume) {
		this.volume = volume;
	}
	public CtaTemplate getStrategy() {
		return strategy;
	}
	public void setStrategy(CtaTemplate strategy) {
		this.strategy = strategy;
	}
	public String getStopOrderID() {
		return stopOrderID;
	}
	public void setStopOrderID(String stopOrderID) {
		this.stopOrderID = stopOrderID;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
}
