package vnpy.trader;


import java.time.format.DateTimeFormatter;

import vnpy.utils.Method;

public class BarGenerator {
//    K线合成器，支持：
//    1. 基于Tick合成1分钟K线
//    2. 基于1分钟K线合成X分钟K线（X可以是2、3、5、10、15、30	）

    private VtBarData bar; // 1分钟K线对象
    private Method onBar; // 1分钟K线回调函数

    private VtBarData xminBar; // X分钟K线对象
    private int xmin; // X的值
    private Method onXminBar; // X分钟K线的回调函数

    private VtTickData lastTick; // 上一TICK缓存对象

    public BarGenerator(Method onBar) {
        this(onBar, 0, null);
    }

    public BarGenerator(Method onBar, int xmin, Method onXminBar) {
        this.bar = null; // 1分钟K线对象
        this.onBar = onBar; // 1分钟K线回调函数

        this.xminBar = null; // X分钟K线对象
        this.xmin = xmin; // X的值
        this.onXminBar = onXminBar; // X分钟K线的回调函数

        this.lastTick = null; // 上一TICK缓存对象
    }

    // TICK更新
    public void updateTick(VtTickData tick) {
        boolean newMinute = false; // 默认不是新的一分钟

        // 尚未创建对象
        if (this.bar == null) {
            this.bar = new VtBarData();
            newMinute = true;
        }
        // 新的一分钟
        else if (this.bar.getDatetime().getMinute() != tick.getDatetime().getMinute()) {
            // 生成上一分钟K线的时间戳
            this.bar.setDatetime(this.bar.getDatetime().withSecond(0).withNano(0)); // 将秒和微秒设为0
            this.bar.setDate(this.bar.getDatetime().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
            this.bar.setTime(this.bar.getDatetime().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS")));

            // 推送已经结束的上一分钟K线
            this.onBar.invoke(this.bar);

            // 创建新的K线对象
            this.bar = new VtBarData();
            newMinute = true;
        }
        // 初始化新一分钟的K线数据
        if (newMinute) {
            this.bar.setVtSymbol(tick.getVtSymbol());
            this.bar.setSymbol(tick.getSymbol());
            this.bar.setExchange(tick.getExchange());

            this.bar.setOpen(tick.getLastPrice());
            this.bar.setHigh(tick.getLastPrice());
            this.bar.setLow(tick.getLastPrice());
        }
        // 累加更新老一分钟的K线数据
        else {
            this.bar.setHigh(Math.max(this.bar.getHigh(), tick.getLastPrice()));
            this.bar.setLow(Math.min(this.bar.getLow(), tick.getLastPrice()));
        }
        // 通用更新部分
        this.bar.setClose(tick.getLastPrice());
        this.bar.setDatetime(tick.getDatetime());
        this.bar.setOpenInterest(tick.getOpenInterest());

        if (this.lastTick != null) {
            int volumeChange = tick.getVolume() - this.lastTick.getVolume(); // 当前K线内的成交量
            this.bar.setVolume(this.bar.getVolume() + Math.max(volumeChange, 0)); // 避免夜盘开盘lastTick.volume为昨日收盘数据，导致成交量变化为负的情况
        }
        // 缓存Tick
        this.lastTick = tick;
    }

    // 1分钟K线更新
    public void updateBar(VtBarData bar) {
        // 尚未创建对象
        if (this.xminBar == null) {
            this.xminBar = new VtBarData();

            this.xminBar.setVtSymbol(bar.getVtSymbol());
            this.xminBar.setSymbol(bar.getSymbol());
            this.xminBar.setExchange(bar.getExchange());

            this.xminBar.setOpen(bar.getOpen());
            this.xminBar.setHigh(bar.getHigh());
            this.xminBar.setLow(bar.getLow());

            this.xminBar.setDatetime(bar.getDatetime()); // 以第一根分钟K线的开始时间戳作为X分钟线的时间戳
        }
        // 累加老K线
        else {
            this.xminBar.setHigh(Math.max(this.xminBar.getHigh(), bar.getHigh()));
            this.xminBar.setLow(Math.min(this.xminBar.getLow(), bar.getLow()));
        }
        // 通用部分
        this.xminBar.setClose(bar.getClose());
        this.xminBar.setOpenInterest(bar.getOpenInterest());
        this.xminBar.setVolume(this.xminBar.getVolume() + bar.getVolume());

        // X分钟已经走完
        if ((bar.getDatetime().getMinute() + 1) % this.xmin == 0) { // 可以用X整除
            // 生成上一X分钟K线的时间戳
            this.xminBar.setDatetime(this.xminBar.getDatetime().withSecond(0).withNano(0)); // 将秒和微秒设为0
            this.xminBar.setDate(this.xminBar.getDatetime().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
            this.xminBar.setTime(this.xminBar.getDatetime().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS")));

            // 推送
            this.onXminBar.invoke(this.xminBar);

            // 清空老K线缓存对象
            this.xminBar = null;
        }
    }

    // 手动强制立即完成K线合成
    public void generate() {
        this.onBar.invoke(this.bar);
        this.bar = null;
    }
}
