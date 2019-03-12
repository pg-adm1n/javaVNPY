package vnpy.trader.app.ctaStrategy;

import java.time.LocalDateTime;

// 每笔交易的结果
public class TradingResult {
	private double entryPrice; // 开仓价格
	private double exitPrice; // 平仓价格

	private LocalDateTime entryDt; // 开仓时间datetime
	private LocalDateTime exitDt; // 平仓时间

	private int volume; // 交易数量（+/-代表方向）

	private double turnover; // 成交金额
	private double commission; // 手续费成本
	private double slippage; // 滑点成本
	private double pnl; // 净盈亏

	public TradingResult(double entryPrice, LocalDateTime entryDt, double exitPrice, LocalDateTime exitDt, int volume,
			double rate, double slippage, double size) {
		this.entryPrice = entryPrice; // 开仓价格
		this.exitPrice = exitPrice; // 平仓价格

		this.entryDt = entryDt; // 开仓时间datetime
		this.exitDt = exitDt; // 平仓时间

		this.volume = volume; // 交易数量（+/-代表方向）

		this.turnover = (this.entryPrice + this.exitPrice) * size * Math.abs(volume); // 成交金额
		this.commission = this.turnover * rate; // 手续费成本
		this.slippage = slippage * 2 * size * Math.abs(volume); // 滑点成本
		this.pnl = ((this.exitPrice - this.entryPrice) * volume * size - this.commission - this.slippage); // 净盈亏
	}

	public double getEntryPrice() {
		return entryPrice;
	}

	public void setEntryPrice(double entryPrice) {
		this.entryPrice = entryPrice;
	}

	public double getExitPrice() {
		return exitPrice;
	}

	public void setExitPrice(double exitPrice) {
		this.exitPrice = exitPrice;
	}

	public LocalDateTime getEntryDt() {
		return entryDt;
	}

	public void setEntryDt(LocalDateTime entryDt) {
		this.entryDt = entryDt;
	}

	public LocalDateTime getExitDt() {
		return exitDt;
	}

	public void setExitDt(LocalDateTime exitDt) {
		this.exitDt = exitDt;
	}

	public int getVolume() {
		return volume;
	}

	public void setVolume(int volume) {
		this.volume = volume;
	}

	public double getTurnover() {
		return turnover;
	}

	public void setTurnover(double turnover) {
		this.turnover = turnover;
	}

	public double getCommission() {
		return commission;
	}

	public void setCommission(double commission) {
		this.commission = commission;
	}

	public double getSlippage() {
		return slippage;
	}

	public void setSlippage(double slippage) {
		this.slippage = slippage;
	}

	public double getPnl() {
		return pnl;
	}

	public void setPnl(double pnl) {
		this.pnl = pnl;
	}
	
}
