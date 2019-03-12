package vnpy.trader.app.ctaStrategy.strategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import vnpy.trader.ArrayManager;
import vnpy.trader.BarGenerator;
import vnpy.trader.VtBarData;
import vnpy.trader.VtFunction;
import vnpy.trader.VtOrderData;
import vnpy.trader.VtTickData;
import vnpy.trader.VtTradeData;
import vnpy.trader.app.ctaStrategy.BacktestingEngine;
import vnpy.trader.app.ctaStrategy.CtaTemplate;
import vnpy.trader.app.ctaStrategy.StopOrder;
import vnpy.utils.Method;

public class KkStrategy extends CtaTemplate {

	// 策略参数
	private int kkLength = 11; // 计算通道中值的窗口数
	private double kkDev = 1.6; // 计算通道宽度的偏差
	private double trailingPrcnt = 0.8; // 移动止损
	private int initDays = 10; // 初始化数据所用的天数
	private int fixedSize = 1; // 每次交易的数量

	// 策略变量
	private double kkUp = 0; // KK通道上轨
	private double kkDown = 0; // KK通道下轨
	private double intraTradeHigh = 0; // 持仓期内的最高点
	private double intraTradeLow = 0; // 持仓期内的最低点

	private BarGenerator bg;
	private ArrayManager am;

	private String[] buyOrderIDList;
	private String[] shortOrderIDList;
	private List<String> orderList;

	public KkStrategy(BacktestingEngine ctaEngine, Map<String, Object> setting) {
		super(ctaEngine, setting);

		Method onBar = new Method(this, "onBar", VtBarData.class);
		Method onFiveBar = new Method(this, "onFiveBar", VtBarData.class);
		this.bg = new BarGenerator(onBar, 5, onFiveBar); // 创建K线合成器对象
		this.am = new ArrayManager();

		this.orderList = new ArrayList<String>();
	}

	@Override
	public void onInit() {
		this.writeCtaLog(this.getName() + "策略初始化");

		// 载入历史数据，并采用回放计算的方式初始化策略数值
		List<VtBarData> initData = this.loadBar(this.initDays);
		for (VtBarData bar : initData) {
			this.onBar(bar);
		}

		this.putEvent();
	}

	@Override
	public void onStart() {
		this.writeCtaLog(this.getName() + "策略启动");
		this.putEvent();
	}

	@Override
	public void onStop() {
		this.writeCtaLog(this.getName() + "策略停止");
		this.putEvent();
	}

	@Override
	public void onTick(VtTickData tick) {
		this.bg.updateTick(tick);
	}

	@Override
	public void onOrder(VtOrderData order) {
		return;
	}

	@Override
	public void onTrade(VtTradeData trade) {
		if (this.getPos() != 0) {
			// 多头开仓成交后，撤消空头委托
			if (this.getPos() > 0) {
				for (String shortOrderID : this.shortOrderIDList) {
					this.cancelOrder(shortOrderID);
				}
			}
			// 反之同样
			else if (this.getPos() < 0) {
				for (String buyOrderID : this.buyOrderIDList) {
					this.cancelOrder(buyOrderID);
				}
			}

			// 移除委托号
			for (String orderID : VtFunction.arrayAppend(this.buyOrderIDList, this.shortOrderIDList)) {
				if (this.orderList.contains(orderID)) {
					this.orderList.remove(orderID);
				}
			}
		}
		
		// 发出状态更新事件
		this.putEvent();
	}

	@Override
	public void onBar(VtBarData bar) {
		this.bg.updateBar(bar);
	}

	public void onFiveBar(VtBarData bar) {
		// 撤销之前发出的尚未成交的委托（包括限价单和停止单）
		for (String orderID : this.orderList) {
			this.cancelOrder(orderID);
		}
		this.orderList = new ArrayList<String>();

		// 保存K线数据
		ArrayManager am = this.am;
		am.updateBar(bar);
		if (!am.isInited()) {
			return;
		}

		// 计算指标数值
		double[] kk = am.keltner(this.kkLength, this.kkDev);
		this.kkUp = kk[0];
		this.kkDown = kk[1];
		
		// 判断是否要进行交易

		// 当前无仓位，发送OCO开仓委托
		String[] l;
		if (this.getPos() == 0) {
			this.intraTradeHigh = bar.getHigh();
			this.intraTradeLow = bar.getLow();
			this.sendOcoOrder(this.kkUp, this.kkDown, this.fixedSize);
		}
		// 持有多头仓位
		else if (this.getPos() > 0) {
			this.intraTradeHigh = Math.max(this.intraTradeHigh, bar.getHigh());
			this.intraTradeLow = bar.getLow();

			l = this.sell(this.intraTradeHigh * (1 - this.trailingPrcnt / 100), Math.abs(this.getPos()), true);
			for (int i = 0; i < l.length; i++) {
				this.orderList.add(l[i]);
			}
		}
		// 持有空头仓位
		else if (this.getPos() < 0) {
			this.intraTradeHigh = bar.getHigh();
			this.intraTradeLow = Math.min(this.intraTradeLow, bar.getLow());

			l = this.cover(this.intraTradeLow * (1 + this.trailingPrcnt / 100), Math.abs(this.getPos()), true);
			for (int i = 0; i < l.length; i++) {
				this.orderList.add(l[i]);
			}
		}

		// 同步数据到数据库
		this.saveSyncData();

		// 发出状态更新事件
		this.putEvent();
	}

	@Override
	public void onStopOrder(StopOrder so) {
		return;
	}
	
	// 发送OCO委托
    // OCO(One Cancel Other)委托：
    // 1. 主要用于实现区间突破入场
    // 2. 包含两个方向相反的停止单
    // 3. 一个方向的停止单成交后会立即撤消另一个方向的
    private void sendOcoOrder(double buyPrice, double shortPrice, int volume) {
        // 发送双边的停止单委托，并记录委托号
        this.buyOrderIDList = this.buy(buyPrice, volume, true);
        this.shortOrderIDList = this.sshort(shortPrice, volume, true);
        
        // 将委托号记录到列表中
        for (int i = 0; i < this.buyOrderIDList.length; i++) {
			this.orderList.add(this.buyOrderIDList[i]);
		}
        for (int i = 0; i < this.shortOrderIDList.length; i++) {
			this.orderList.add(this.shortOrderIDList[i]);
		}
    }
}
