package vnpy.trader;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.SortedMap;

// 成交数据类
// 一般来说，一个VtOrderData可能对应多个VtTradeData：一个订单可能多次部分成交
public class VtTradeData extends VtBaseData {
	// 代码编号相关
	private String symbol; // 合约代码
	private String exchange; // 交易所代码
	private String vtSymbol; // 合约在vt系统中的唯一代码，通常是 合约代码.交易所代码
	
	private String tradeID; // 成交编号 gateway内部自己生成的编号
	private String vtTradeID; // 成交在vt系统中的唯一编号，通常是 Gateway名.成交编号

	private String orderID; // 订单编号
	private String vtOrderID; // 订单在vt系统中的唯一编号，通常是 Gateway名.订单编号

	// 成交相关
	private String direction; // 成交方向
	private String offset; // 成交开平仓
	private double price; // 成交价格
	private int volume; // 成交数量
	private String tradeTime; // 成交时间
	private LocalDateTime dt;

	public static VtTradeData createFromGateway(VtGateway gateway, String symbol, String exchange, String tradeID,
			String orderID, String direction, double tradePrice, int tradeVolume) {
		VtTradeData trade = new VtTradeData();
		trade.setGatewayName(gateway.getGatewayName());
		trade.setSymbol(symbol);
		trade.setExchange(exchange);
		trade.setVtSymbol(symbol + "." + exchange);

		trade.setOrderID(orderID);
		trade.setVtOrderID(trade.getGatewayName() + "." + trade.getTradeID());

		trade.setTradeID(tradeID);
		trade.setVtTradeID(trade.getGatewayName() + "." + tradeID);

		trade.setDirection(direction);
		trade.setPrice(tradePrice);
		trade.setVolume(tradeVolume);
		trade.setTradeTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
		return trade;
	}

	public static VtTradeData createFromOrderData(VtOrderData order, String tradeID, double tradePrice,
			int tradeVolume) {
		VtTradeData trade = new VtTradeData();
		trade.setGatewayName(order.getGatewayName());
		trade.setSymbol(order.getSymbol());
		trade.setVtSymbol(order.getVtSymbol());

		trade.setOrderID(order.getOrderID());
		trade.setVtOrderID(order.getVtOrderID());
		trade.setTradeID(tradeID);
		trade.setVtTradeID(trade.getGatewayName() + "." + tradeID);
		trade.setDirection(order.getDirection());
		trade.setPrice(tradePrice);
		trade.setVolume(tradeVolume);
		trade.setTradeTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));

		return trade;
	}

	///////////////////////// Getter Setter/////////////////////////////////

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

	public String getTradeID() {
		return tradeID;
	}

	public void setTradeID(String tradeID) {
		this.tradeID = tradeID;
	}

	public String getVtTradeID() {
		return vtTradeID;
	}

	public void setVtTradeID(String vtTradeID) {
		this.vtTradeID = vtTradeID;
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

	public int getVolume() {
		return volume;
	}

	public void setVolume(int volume) {
		this.volume = volume;
	}

	public String getTradeTime() {
		return tradeTime;
	}

	public void setTradeTime(String tradeTime) {
		this.tradeTime = tradeTime;
	}

	public LocalDateTime getDt() {
		return dt;
	}

	public void setDt(LocalDateTime dt) {
		this.dt = dt;
	}

}
