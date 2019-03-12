package examples.CtaBacktesting;

import java.util.HashMap;
import java.util.Map;

import vnpy.trader.app.ctaStrategy.BacktestingEngine;
import vnpy.trader.app.ctaStrategy.CtaBaseConstant;
import vnpy.trader.app.ctaStrategy.strategy.KkStrategy;

public class RunBacktesting {
	public static void main(String[] args) {
		// 创建回测引擎
		BacktestingEngine engine = new BacktestingEngine();
		
		// 设置引擎的回测模式为K线
	    engine.setBacktestingMode(BacktestingEngine.BAR_MODE);
	
	    // 设置回测用的数据起始日期
	    engine.setStartDate("20120101");
	    //engine.setEndDate("20120113");
	    // 设置产品相关参数
	    engine.setSlippage(0.2);     // 股指1跳
	    engine.setRate(0.3/10000);  	// 万0.3
	    engine.setSize(300);         // 股指合约大小 
	    engine.setPriceTick(0.2);    // 股指最小价格变动
	    
	    // 设置使用的历史数据库
	    engine.setDatabase(CtaBaseConstant.MINUTE_DB_NAME, "IF0000");
	    
	    // 在引擎中创建策略对象
	    Map<String, Object> d = new HashMap<String, Object>();
	    engine.initStrategy(KkStrategy.class, d);
	    
	    // 开始跑回测
	    engine.runBacktesting();
	    
	    // 显示回测结果
	    engine.showBacktestingResult();
	    
	}
}
