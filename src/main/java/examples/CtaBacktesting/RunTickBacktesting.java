package examples.CtaBacktesting;

import vnpy.trader.app.ctaStrategy.BacktestingEngine;

public class RunTickBacktesting {
	public static void main(String[] args) {
		// 创建回测引擎
		BacktestingEngine engine = new BacktestingEngine();

		// 设置引擎的回测模式为K线
		engine.setBacktestingMode(BacktestingEngine.CSV_TICK_MODE);
		
		// 设置回测用的数据起始日期
	    engine.setStartDate("20160101");
	    engine.setEndDate("20181231");
	    
	    // 设置产品相关参数
	    engine.setSlippage(0);     // 假设没有滑点
	    engine.setRate(0.3/10000);  	// 万0.3
	    engine.setSize(300);         // 股指合约大小 
	    engine.setPriceTick(0.2);    // 股指最小价格变动
	}
}
