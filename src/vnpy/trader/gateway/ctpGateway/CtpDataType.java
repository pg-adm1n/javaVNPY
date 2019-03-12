package vnpy.trader.gateway.ctpGateway;

import java.util.HashMap;
import java.util.Map;



public class CtpDataType {
	public static final Map<String, String> defineDict = new HashMap<String, String>();
	public static final Map<String, String> typedefDict = new HashMap<String, String>();
	
	static {
		////////////////////////////////////////////////////////////////////////
		//TFtdcProductClassType是一个产品类型类型
		////////////////////////////////////////////////////////////////////////
		//期货
		defineDict.put("THOST_FTDC_PC_Futures", "1");
		//期货期权
		defineDict.put("THOST_FTDC_PC_Options", "2");
		//组合
		defineDict.put("THOST_FTDC_PC_Combination", "3");
		//即期
		defineDict.put("THOST_FTDC_PC_Spot", "4");
		//期转现
		defineDict.put("THOST_FTDC_PC_EFP", "5");
		//现货期权
		defineDict.put("THOST_FTDC_PC_SpotOption", "6");
		//个股期权
		defineDict.put("THOST_FTDC_PC_ETFOption", "7");
		//证券
		defineDict.put("THOST_FTDC_PC_Stock", "8");

		typedefDict.put("TThostFtdcProductClassType", "char");
	}
	
	public static void main(String[] args) {

	}
}
