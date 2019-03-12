package examples.CtaBacktesting;

import vnpy.trader.app.ctaStrategy.CtaBaseConstant;
import vnpy.trader.app.ctaStrategy.CtaHistoryData;

public class LoadCsv {
	
	
	public static void main(String[] args) {
		CtaHistoryData.loadMcCsv("IF0000_1min.csv", CtaBaseConstant.MINUTE_DB_NAME, "IF0000");
		CtaHistoryData.loadMcCsv("rb0000_1min.csv", CtaBaseConstant.MINUTE_DB_NAME, "rb0000");
	}
}
