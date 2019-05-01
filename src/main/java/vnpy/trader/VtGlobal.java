package vnpy.trader;

import java.util.Map;

public class VtGlobal {
	public static Map<String, String> globalSetting;
	
	static {
		globalSetting = VtFunction.loadJsonSetting("input/VT_setting.txt");
	}
}
