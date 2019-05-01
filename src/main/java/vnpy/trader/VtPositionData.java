package vnpy.trader;

// 持仓数据类
public class VtPositionData extends VtBaseData {
	// 代码编号相关
	private String symbol; // 合约代码
	private String exchange; // 交易所代码
	private String vtSymbol; // 合约在vt系统中的唯一代码，合约代码.交易所代码

	// 持仓相关
	private String direction; // 持仓方向
	private int position; // 持仓量
	private int frozen; // 冻结数量
	private double price; // 持仓均价
	private String vtPositionName; // 持仓在vt系统中的唯一代码，通常是vtSymbol.方向
	private int ydPosition; // 昨持仓
	private double positionProfit; // 持仓盈亏

	public static VtPositionData createFromGateway(VtGateway gateway, String exchange, String symbol, String direction,
			int position, int frozen, double price, int yestordayPosition, double profit) {
		VtPositionData vtPosition = new VtPositionData();
		vtPosition.setGatewayName(gateway.getGatewayName());
		vtPosition.symbol = symbol;
		vtPosition.exchange = exchange;
		vtPosition.vtSymbol = symbol + "." + exchange;

		vtPosition.direction = direction;
		vtPosition.position = position;
		vtPosition.frozen = frozen;
		vtPosition.price = price;
		vtPosition.vtPositionName = vtPosition.vtSymbol + "." + direction;
		vtPosition.ydPosition = yestordayPosition;
		vtPosition.positionProfit = profit;
		return vtPosition;
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

	public String getDirection() {
		return direction;
	}

	public void setDirection(String direction) {
		this.direction = direction;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public int getFrozen() {
		return frozen;
	}

	public void setFrozen(int frozen) {
		this.frozen = frozen;
	}

	public double getPrice() {
		return price;
	}

	public void setPrice(double price) {
		this.price = price;
	}

	public String getVtPositionName() {
		return vtPositionName;
	}

	public void setVtPositionName(String vtPositionName) {
		this.vtPositionName = vtPositionName;
	}

	public int getYdPosition() {
		return ydPosition;
	}

	public void setYdPosition(int ydPosition) {
		this.ydPosition = ydPosition;
	}

	public double getPositionProfit() {
		return positionProfit;
	}

	public void setPositionProfit(double positionProfit) {
		this.positionProfit = positionProfit;
	}
}
