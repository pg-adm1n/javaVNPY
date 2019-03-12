package vnpy.trader;

public class VtConstant {
	// 方向常量
	public static final String DIRECTION_NONE = "none";
	public static final String DIRECTION_LONG = "long";
	public static final String DIRECTION_SHORT = "short";

	// 开平常量
	public static final String OFFSET_NONE = "none";
	public static final String OFFSET_OPEN = "open";
	public static final String OFFSET_CLOSE = "close";
	public static final String OFFSET_CLOSETODAY = "close today";
	public static final String OFFSET_CLOSEYESTERDAY = "close yesterday";
	public static final String OFFSET_UNKNOWN = "unknown";

	// 状态常量
	public static final String STATUS_NOTTRADED = "pending";
	public static final String STATUS_PARTTRADED = "partial filled";
	public static final String STATUS_ALLTRADED = "filled";
	public static final String STATUS_CANCELLED = "cancelled";
	public static final String STATUS_REJECTED = "rejected";
	public static final String STATUS_UNKNOWN = "unknown";

	// 合约类型常量
	public static final String PRODUCT_EQUITY = "股票";
	public static final String PRODUCT_FUTURES = "期货";
	public static final String PRODUCT_OPTION = "期权";
	public static final String PRODUCT_INDEX = "指数";
	public static final String PRODUCT_COMBINATION = "组合";
	public static final String PRODUCT_FOREX = "外汇";
	public static final String PRODUCT_UNKNOWN = "未知";
	public static final String PRODUCT_SPOT = "现货";
	public static final String PRODUCT_DEFER = "延期";
	public static final String PRODUCT_ETF = "ETF";
	public static final String PRODUCT_WARRANT = "权证";
	public static final String PRODUCT_BOND = "债券";
	public static final String PRODUCT_NONE = "";
	
	// 期权类型
	public static final String OPTION_CALL = "看涨期权";
	public static final String OPTION_PUT = "看跌期权";
	
	// 交易所类型
	public static final String EXCHANGE_SSE = "SSE"; // 上交所
	public static final String EXCHANGE_SZSE = "SZSE"; // 深交所
	public static final String EXCHANGE_CFFEX = "CFFEX"; // 中金所
	public static final String EXCHANGE_SHFE = "SHFE"; // 上期所
	public static final String EXCHANGE_CZCE = "CZCE"; // 郑商所
	public static final String EXCHANGE_DCE = "DCE"; // 大商所
	public static final String EXCHANGE_SGE = "SGE"; // 上金所
	public static final String EXCHANGE_INE = "INE"; // 国际能源交易中心
	public static final String EXCHANGE_UNKNOWN = "UNKNOWN";// 未知交易所
	public static final String EXCHANGE_NONE = ""; // 空交易所
	public static final String EXCHANGE_HKEX = "HKEX"; // 港交所
	public static final String EXCHANGE_HKFE = "HKFE"; // 香港期货交易所
	
	
	// 接口类型
	public static final String GATEWAYTYPE_EQUITY = "equity";                   // 股票、ETF、债券
	public static final String GATEWAYTYPE_FUTURES = "futures";                 // 期货、期权、贵金属
	public static final String GATEWAYTYPE_INTERNATIONAL = "international";     // 外盘
	public static final String GATEWAYTYPE_BTC = "btc";                         // 比特币
	public static final String GATEWAYTYPE_DATA = "data";                       // 数据（非交易）
}
