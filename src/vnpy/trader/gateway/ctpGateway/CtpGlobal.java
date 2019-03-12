package vnpy.trader.gateway.ctpGateway;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import vnpy.trader.VtConstant;

public class CtpGlobal {

	// 交易所类型映射
	public static final Map<String, String> exchangeMap = new HashMap<String, String>();
	static {
		exchangeMap.put(VtConstant.EXCHANGE_CFFEX, "CFFEX");
		exchangeMap.put(VtConstant.EXCHANGE_SHFE, "SHFE");
		exchangeMap.put(VtConstant.EXCHANGE_CZCE, "CZCE");
		exchangeMap.put(VtConstant.EXCHANGE_DCE, "DCE");
		exchangeMap.put(VtConstant.EXCHANGE_SSE, "SSE");
		exchangeMap.put(VtConstant.EXCHANGE_SZSE, "SZSE");
		exchangeMap.put(VtConstant.EXCHANGE_INE, "INE");
		exchangeMap.put(VtConstant.EXCHANGE_UNKNOWN, "");
	}
	public static final Map<String, String> exchangeMapReverse = exchangeMap.entrySet().stream()
			.collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

	// 产品类型映射
	public static final Map<String, String> productClassMap = new HashMap<String, String>();
	static {
		productClassMap.put(VtConstant.PRODUCT_FUTURES, CtpDataType.defineDict.get("THOST_FTDC_PC_Futures"));
		productClassMap.put(VtConstant.PRODUCT_OPTION, CtpDataType.defineDict.get("THOST_FTDC_PC_Options"));
		productClassMap.put(VtConstant.PRODUCT_COMBINATION, CtpDataType.defineDict.get("THOST_FTDC_PC_Combination"));
	}
	public static final Map<String, String> productClassMapReverse = productClassMap.entrySet().stream()
			.collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
	static {
		productClassMapReverse.put(CtpDataType.defineDict.get("THOST_FTDC_PC_ETFOption"), VtConstant.PRODUCT_OPTION);
		productClassMapReverse.put(CtpDataType.defineDict.get("THOST_FTDC_PC_Stock"), VtConstant.PRODUCT_EQUITY);
	}

	// 全局字典, key:symbol, value:exchange
	public static final Map<String, String> symbolExchangeDict = new HashMap<String, String>();

}
