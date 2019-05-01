package vnpy.trader.app.ctaStrategy;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import vnpy.trader.VtBarData;
import vnpy.trader.VtOrderData;
import vnpy.trader.VtTickData;
import vnpy.trader.VtTradeData;
import vnpy.utils.AppException;

//CTA策略模板
public abstract class CtaTemplate {
	private TradingEngine ctaEngine;

	// 策略类的名称和作者
	private String className = "CtaTemplate";
	private String author = null;

	// MongoDB数据库的名称，K线数据库默认为1分钟
    private String tickDbName = CtaBaseConstant.TICK_DB_NAME;
    private String barDbName = CtaBaseConstant.MINUTE_DB_NAME;
	
	// 策略的基本参数
	private String name; // 策略实例名称
	private String vtSymbol; // 交易的合约vt系统代码

	// 策略的基本变量，由引擎管理
	private boolean inited = false; // 是否进行了初始化
	private boolean trading = false; // 是否启动交易，由引擎管理
	private int pos = 0; // 持仓情况

	public CtaTemplate(TradingEngine ctaEngine, Map<String, Object> setting) {
		this.ctaEngine = ctaEngine;

		// 设置策略的参数
		if (setting != null) {
			for (String key : setting.keySet()) {
				// 获取obj类的字节文件对象
				Class<? extends CtaTemplate> c = this.getClass();
				try {
					// 获取该类的成员变量
					Field f = c.getDeclaredField(key);
					// 取消语言访问检查
					f.setAccessible(true);
					// 给变量赋值
					f.set(this, setting.get(key));
				} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
					e.printStackTrace();
					throw new AppException("设定属性失败");
				}
			}
		}
	}

	// 初始化策略（必须由用户继承实现）
	abstract public void onInit();

	// 启动策略（必须由用户继承实现）
	abstract public void onStart();

	// 停止策略（必须由用户继承实现）
	abstract public void onStop();

	// 收到行情TICK推送（必须由用户继承实现）
	abstract public void onTick(VtTickData tick);

	// 收到委托变化推送（必须由用户继承实现）
	abstract public void onOrder(VtOrderData order);

	// 收到成交推送（必须由用户继承实现）
	abstract public void onTrade(VtTradeData trade);

	// 收到Bar推送（必须由用户继承实现）
	abstract public void onBar(VtBarData bar);

	// 收到停止单推送（必须由用户继承实现）
	abstract public void onStopOrder(StopOrder so);

	// 买开
	public String[] buy(double price, int volume) {
		return buy(price, volume, false);
	}

	// 买开
	public String[] buy(double price, int volume, boolean stop) {
		return sendOrder(CtaBaseConstant.CTAORDER_BUY, price, volume, stop);
	}

	// 卖平
	public String[] sell(double price, int volume) {
		return sell(price, volume, false);
	}

	// 卖平
	public String[] sell(double price, int volume, boolean stop) {
		return sendOrder(CtaBaseConstant.CTAORDER_SELL, price, volume, stop);
	}

	// 卖开
	public String[] sshort(double price, int volume) {
		return sshort(price, volume, false);
	}

	// 卖开
	public String[] sshort(double price, int volume, boolean stop) {
		return sendOrder(CtaBaseConstant.CTAORDER_SHORT, price, volume, stop);
	}

	// 买平
	public String[] cover(double price, int volume) {
		return cover(price, volume, false);
	}

	// 买平
	public String[] cover(double price, int volume, boolean stop) {
		return sendOrder(CtaBaseConstant.CTAORDER_COVER, price, volume, stop);
	}

	// 发送委托
	public String[] sendOrder(String orderType, double price, int volume) {
		return sendOrder(orderType, price, volume, false);
	}

	// 发送委托
	public String[] sendOrder(String orderType, double price, int volume, boolean stop) {
		String[] vtOrderIDList;
		if (this.trading) {
			if (stop) {
				vtOrderIDList = this.ctaEngine.sendStopOrder(this.vtSymbol, orderType, price, volume, this);
			} else {
				vtOrderIDList = this.ctaEngine.sendOrder(this.vtSymbol, orderType, price, volume, this);
			}
		} else {
			// 交易停止时发单返回空字符串
			vtOrderIDList = new String[] {};
		}
		return vtOrderIDList;
	}

	// 撤单
	public void cancelOrder(String vtOrderID) {
		// 如果发单号为空字符串，则不进行后续操作
		if (vtOrderID == null || "".equals(vtOrderID)) {
			return;
		}
		if (vtOrderID.startsWith(CtaBaseConstant.STOPORDERPREFIX)) {
			this.ctaEngine.cancelStopOrder(vtOrderID);
		} else {
			this.ctaEngine.cancelOrder(vtOrderID);
		}
	}

	// 全部撤单
	public void cancelAll() {
		this.ctaEngine.cancelAll(this.name);
	}

	// 向数据库中插入tick数据
	public void insertTick(VtTickData tick) {
		this.ctaEngine.insertData(this.tickDbName, this.vtSymbol, tick);
	}

	// 向数据库中插入bar数据
	public void insertBar(VtBarData bar) {
		this.ctaEngine.insertData(this.barDbName, this.vtSymbol, bar);
	}

	// 读取tick数据
	public List<VtTickData> loadTick(int days) {
		return this.ctaEngine.loadTick(this.tickDbName, this.vtSymbol, days);
	}

	// 读取bar数据
	public List<VtBarData> loadBar(int days) {
		return this.ctaEngine.loadBar(this.barDbName, this.vtSymbol, days);
	}

	// 记录CTA日志
	public void writeCtaLog(String content) {
		content = this.name + ':' + content;
		this.ctaEngine.writeCtaLog(content);
	}

	// 发出策略状态变化事件
	public void putEvent() {
		this.ctaEngine.putStrategyEvent(this.name);
	}

	// 查询当前运行的环境
	public String getEngineType() {
		return this.ctaEngine.getEngineType();
	}
	
	// 保存同步数据到数据库
	public void saveSyncData() {
		if (this.trading) {
			this.ctaEngine.saveSyncData(this);
		}
	}

	// 查询最小价格变动
	public double getPriceTick() {
		return this.ctaEngine.getPriceTick(this);
	}

	/***********************************下面是getter和setter方法***********************************/

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getPos() {
		return pos;
	}

	public void setPos(int pos) {
		this.pos = pos;
	}

	public boolean isInited() {
		return inited;
	}

	public void setInited(boolean inited) {
		this.inited = inited;
	}

	public boolean isTrading() {
		return trading;
	}

	public void setTrading(boolean trading) {
		this.trading = trading;
	}
	
}
