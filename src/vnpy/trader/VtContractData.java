package vnpy.trader;

// 合约详细信息类
public class VtContractData extends VtBaseData {

	private String symbol; // 代码
	private String exchange; // 交易所代码
	private String vtSymbol; // 合约在vt系统中的唯一代码，通常是 合约代码.交易所代码
	private String name; // 合约中文名

	private String productClass; // 合约类型
	private int size; // 合约大小
	private double priceTick; // 合约最小价格TICK

	// 期权相关
	private double strikePrice; // 期权行权价
	private String underlyingSymbol; // 标的物合约代码
	private String optionType; // 期权类型
	private String expiryDate; // 到期日

	public static VtContractData createFromGateway(VtGateway gateway, String exchange, String symbol,
			String productClass, int size, double priceTick, String name, double strikePrice, String underlyingSymbol,
			String optionType, String expiryDate) {
		VtContractData d = new VtContractData();
		d.setGatewayName(gateway.getGatewayName());
		d.setSymbol(symbol);
		d.setExchange(exchange);
		d.setVtSymbol(symbol + "." + exchange);
		d.setProductClass(productClass);
		d.setSize(size);
		d.setPriceTick(priceTick);
		if (name == null || "".equals((name + "".trim()))) {
			d.setName(d.getSymbol());
		}
		d.setStrikePrice(strikePrice);
		d.setUnderlyingSymbol(underlyingSymbol);
		d.setOptionType(optionType);
		d.setExpiryDate(expiryDate);
		return d;
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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getProductClass() {
		return productClass;
	}

	public void setProductClass(String productClass) {
		this.productClass = productClass;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public double getPriceTick() {
		return priceTick;
	}

	public void setPriceTick(double priceTick) {
		this.priceTick = priceTick;
	}

	public double getStrikePrice() {
		return strikePrice;
	}

	public void setStrikePrice(double strikePrice) {
		this.strikePrice = strikePrice;
	}

	public String getUnderlyingSymbol() {
		return underlyingSymbol;
	}

	public void setUnderlyingSymbol(String underlyingSymbol) {
		this.underlyingSymbol = underlyingSymbol;
	}

	public String getOptionType() {
		return optionType;
	}

	public void setOptionType(String optionType) {
		this.optionType = optionType;
	}

	public String getExpiryDate() {
		return expiryDate;
	}

	public void setExpiryDate(String expiryDate) {
		this.expiryDate = expiryDate;
	}

}
