package vnpy.event;

public class EventType {
	// 系统相关
	public static final String EVENT_TIMER = "eTimer"; // 计时器事件，每隔1秒发送一次
	public static final String EVENT_LOG = "eLog"; // 日志事件，全局通用

	// Gateway相关
	public static final String EVENT_TICK = "eTick."; // TICK行情事件，可后接具体的vtSymbol
	public static final String EVENT_TRADE = "eTrade."; // 成交回报事件
	public static final String EVENT_ORDER = "eOrder."; // 报单回报事件
	public static final String EVENT_POSITION = "ePosition."; // 持仓回报事件
	public static final String EVENT_ACCOUNT = "eAccount."; // 账户回报事件
	public static final String EVENT_CONTRACT = "eContract."; // 合约基础信息回报事件
	public static final String EVENT_ERROR = "eError."; // 错误回报事件
	public static final String EVENT_HISTORY = "eHistory."; // K线数据查询回报事件

	// 行情记录模块事件
	public static final String EVENT_DATARECORDER_LOG = "eDataRecorderLog"; // 行情记录日志更新事件
}
