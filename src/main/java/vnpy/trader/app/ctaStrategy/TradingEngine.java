package vnpy.trader.app.ctaStrategy;

import java.util.List;

import vnpy.trader.VtBarData;
import vnpy.trader.VtBaseData;
import vnpy.trader.VtTickData;

public interface TradingEngine {

	// 获取引擎类型
	public String getEngineType();
	
	// 发停止单（本地实现）
	public String[] sendStopOrder(String vtSymbol, String orderType, double price, int volume, CtaTemplate ctaTemplate);

	// 发单
	public String[] sendOrder(String vtSymbol, String orderType, double price, int volume, CtaTemplate ctaTemplate);

	// 撤销停止单
	public void cancelStopOrder(String stopOrderID);

	// 撤单
	public void cancelOrder(String vtOrderID);

	// 全部撤单
	public void cancelAll(String name);

	// 插入数据到MongoDB
	public void insertData(String dbName, String collectionName, VtBaseData data);

	// 读取tick数据
	public List<VtTickData> loadTick(String dbName, String collectionName, int days);

	// 读取bar数据
	public List<VtBarData> loadBar(String dbName, String collectionName, int days);
	
	// 记录日志
	public void writeCtaLog(String content);

	// 发送策略更新事件
	public void putStrategyEvent(String name);

	// 保存同步数据
	public void saveSyncData(CtaTemplate strategy);

	// 查询最小价格变动
	public double getPriceTick(CtaTemplate ctaTemplate);
}
