package vnpy.trader.app.ctaStrategy;

public class CtaBaseConstant {
	public static final String LOG_DB_NAME = "VnTrader_Log_java_Db";
	
	public static final String TICK_DB_NAME = "VnTrader_Tick_java_Db";
	public static final String DAILY_DB_NAME = "VnTrader_Daily_java_Db";
	public static final String MINUTE_DB_NAME = "VnTrader_1Min_java_Db";

	// 引擎类型，用于区分当前策略的运行环境
	public static final String ENGINETYPE_BACKTESTING = "backtesting";  // 回测
	public static final String ENGINETYPE_TRADING = "trading";          // 实盘
	
	// CTA引擎中涉及到的交易方向类型
	public static final String CTAORDER_BUY = "买开";
	public static final String CTAORDER_SELL = "卖平";
	public static final String CTAORDER_SHORT = "卖开";
	public static final String CTAORDER_COVER = "买平";

	// 本地停止单前缀
	public static final String STOPORDERPREFIX = "CtaStopOrder.";

	// 本地停止单状态
	public static final String STOPORDER_WAITING = "等待中";
	public static final String STOPORDER_CANCELLED = "已撤销";
	public static final String STOPORDER_TRIGGERED = "已触发";
	
	
}
