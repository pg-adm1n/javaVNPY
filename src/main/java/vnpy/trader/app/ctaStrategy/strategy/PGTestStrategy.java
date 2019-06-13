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

public class PGTestStrategy extends CtaTemplate {


    private String[] buyOrderIDList;
    private String[] shortOrderIDList;
    private List<String> orderList;

    public PGTestStrategy(BacktestingEngine ctaEngine, Map<String, Object> setting) {
        super(ctaEngine, setting);

        this.orderList = new ArrayList<String>();
    }

    @Override
    public void onInit() {
        this.writeCtaLog(this.getName() + "策略初始化");

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
        // TODO:just focus on onTick

        // 发出状态更新事件
        this.putEvent();
    }

    @Override
    public void onOrder(VtOrderData order) {
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

    }

    @Override
    public void onStopOrder(StopOrder so) {
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
