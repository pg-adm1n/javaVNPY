package vnpy.trader;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PositionDetail {

	private static final Set<String> WORKING_STATUS = new HashSet<String>() {
		private static final long serialVersionUID = 1L;
		{
			add(VtConstant.STATUS_UNKNOWN);
			add(VtConstant.STATUS_NOTTRADED);
			add(VtConstant.STATUS_PARTTRADED);
		}
	};

	public static final String MODE_NORMAL = "normal"; // 普通模式
	public static final String MODE_SHFE = "shfe"; // 上期所今昨分别平仓
	public static final String MODE_TDPENALTY = "tdpenalty"; // 平今惩罚

	private String vtSymbol;
	private String symbol;
	private String exchange;
	private String name;
	private int size;

	private int longPos;
	private int longYd;
	private int longTd;
	private int longPosFrozen;
	private int longYdFrozen;
	private int longTdFrozen;
	private double longPnl;
	private double longPrice;

	private int shortPos;
	private int shortYd;
	private int shortTd;
	private int shortPosFrozen;
	private int shortYdFrozen;
	private int shortTdFrozen;
	private double shortPnl;
	private double shortPrice;

	private double lastPrice;

	private String mode;

	private Map<String, VtOrderData> workingOrderDict;

	// Constructor
	public PositionDetail(String vtSymbol, VtContractData contract) {
		this.vtSymbol = vtSymbol;
		this.size = 1;

		if (contract != null) {
			this.symbol = contract.getSymbol();
			this.exchange = contract.getExchange();
			this.name = contract.getName();
			this.size = contract.getSize();
		}

		this.mode = PositionDetail.MODE_NORMAL;

		this.workingOrderDict = new HashMap<String, VtOrderData>();
	}

	// 委托更新
	public void updateOrder(VtOrderData order) {
		// 将活动委托缓存下来
		if (PositionDetail.WORKING_STATUS.contains(order.getStatus())) {
			this.workingOrderDict.put(order.getVtOrderID(), order);
		}
		// 移除缓存中已经完成的委托
		else {
			if (this.workingOrderDict.containsKey(order.getVtOrderID())) {
				this.workingOrderDict.remove(order.getVtOrderID());
			}
		}

		// 计算冻结
		this.calculateFrozen();
	}

	// 计算冻结情况
	private void calculateFrozen() {
		// 清空冻结数据
		this.longPosFrozen = 0;
		this.longYdFrozen = 0;
		this.longTdFrozen = 0;
		this.shortPosFrozen = 0;
		this.shortYdFrozen = 0;
		this.shortTdFrozen = 0;

		// 遍历统计
		for (VtOrderData order : this.workingOrderDict.values()) {
			// 计算剩余冻结量
			int frozenVolume = order.getTotalVolume() - order.getTradedVolume();

			// 多头委托
			if (VtConstant.DIRECTION_LONG.equals(order.getDirection())) {
				// 平今
				if (VtConstant.OFFSET_CLOSETODAY.equals(order.getOffset())) {
					this.shortTdFrozen += frozenVolume;
				}
				// 平昨
				else if (VtConstant.OFFSET_CLOSEYESTERDAY.equals(order.getOffset())) {
					this.shortYdFrozen += frozenVolume;
				}
				// 平仓
				else if (VtConstant.OFFSET_CLOSE.equals(order.getOffset())) {
					this.shortTdFrozen += frozenVolume;

					if (this.shortTdFrozen > this.shortTd) {
						this.shortYdFrozen += (this.shortTdFrozen - this.shortTd);
						this.shortTdFrozen = this.shortTd;
					}
				}
			}
			// 空头委托
			else if (VtConstant.DIRECTION_SHORT.equals(order.getDirection())) {
				// 平今
				if (VtConstant.OFFSET_CLOSETODAY.equals(order.getOffset())) {
					this.longTdFrozen += frozenVolume;
				}
				// 平昨
				else if (VtConstant.OFFSET_CLOSEYESTERDAY.equals(order.getOffset())) {
					this.longYdFrozen += frozenVolume;
				}
				// 平仓
				else if (VtConstant.OFFSET_CLOSE.equals(order.getOffset())) {
					this.longTdFrozen += frozenVolume;

					if (this.longTdFrozen > this.longTd) {
						this.longYdFrozen += (this.longTdFrozen - this.longTd);
						this.longTdFrozen = this.longTd;
					}
				}
			}

			// 汇总今昨冻结
			this.longPosFrozen = this.longYdFrozen + this.longTdFrozen;
			this.shortPosFrozen = this.shortYdFrozen + this.shortTdFrozen;
		}
	}

	// 成交更新
	public void updateTrade(VtTradeData trade) {
		// 多头
		if (VtConstant.DIRECTION_LONG.equals(trade.getDirection())) {
			// 开仓
			if (VtConstant.OFFSET_OPEN.equals(trade.getOffset())) {
				this.longTd += trade.getVolume();
			}
			// 平今
			else if (VtConstant.OFFSET_CLOSETODAY.equals(trade.getOffset())) {
				this.shortTd -= trade.getVolume();
			}
			// 平昨
			else if (VtConstant.OFFSET_CLOSEYESTERDAY.equals(trade.getOffset())) {
				this.shortYd -= trade.getVolume();
			}
			// 平仓
			else if (VtConstant.OFFSET_CLOSE.equals(trade.getOffset())) {
				// 上期所等同于平昨
				if (VtConstant.EXCHANGE_SHFE.equals(this.exchange)) {
					this.shortYd -= trade.getVolume();
				}
				// 非上期所，优先平今
				else {
					this.shortTd -= trade.getVolume();

					if (this.shortTd < 0) {
						this.shortYd += this.shortTd;
						this.shortTd = 0;
					}
				}
			}
		}
		// 空头
		else if (VtConstant.DIRECTION_SHORT.equals(trade.getDirection())) {
			// 开仓
			if (VtConstant.OFFSET_OPEN.equals(trade.getOffset())) {
				this.shortTd += trade.getVolume();
			}
			// 平今
			else if (VtConstant.OFFSET_CLOSETODAY.equals(trade.getOffset())) {
				this.longTd -= trade.getVolume();
			}
			// 平昨
			else if (VtConstant.OFFSET_CLOSEYESTERDAY.equals(trade.getOffset())) {
				this.longYd -= trade.getVolume();
			}
			// 平仓
			else if (VtConstant.OFFSET_CLOSE.equals(trade.getOffset())) {
				// 上期所等同于平昨
				if (VtConstant.EXCHANGE_SHFE.equals(this.exchange)) {
					this.longYd -= trade.getVolume();
				}
				// 非上期所，优先平今
				else {
					this.longTd -= trade.getVolume();
					if (this.longTd < 0) {
						this.longYd += this.longTd;
						this.longTd = 0;
					}
				}
			}
		}

		// 汇总
		this.calculatePrice(trade);
		this.calculatePosition();
		this.calculatePnl();
	}

	// 计算持仓均价（基于成交数据）
	private void calculatePrice(VtTradeData trade) {
		// 只有开仓会影响持仓均价
		if (VtConstant.OFFSET_OPEN.equals(trade.getOffset())) {
			if (VtConstant.DIRECTION_LONG.equals(trade.getDirection())) {
				double cost = this.longPrice * this.longPos;
				cost += trade.getVolume() * trade.getPrice();
				int newPos = this.longPos + trade.getVolume();
				if (newPos != 0) {
					this.longPrice = cost / newPos;
				} else {
					this.longPrice = 0;
				}
			} else {
				double cost = this.shortPrice * this.shortPos;
				cost += trade.getVolume() * trade.getPrice();
				int newPos = this.shortPos + trade.getVolume();
				if (newPos != 0) {
					this.shortPrice = cost / newPos;
				} else {
					this.shortPrice = 0;
				}
			}
		}
	}

	// 计算持仓情况
	private void calculatePosition() {
		this.longPos = this.longTd + this.longYd;
		this.shortPos = this.shortTd + this.shortYd;
	}

	// 计算持仓盈亏
	private void calculatePnl() {
		this.longPnl = this.longPos * (this.lastPrice - this.longPrice) * this.size;
		this.shortPnl = this.shortPos * (this.shortPrice - this.lastPrice) * this.size;
	}
	
	// 持仓更新
    public void updatePosition(VtPositionData pos) {
        if (VtConstant.DIRECTION_LONG.equals(pos.getDirection())) {
        	this.longPos = pos.getPosition();
        	this.longYd = pos.getYdPosition();
        	this.longTd = this.longPos - this.longYd;
        	this.longPnl = pos.getPositionProfit();
        	this.longPrice = pos.getPrice();
		} else if (VtConstant.DIRECTION_SHORT.equals(pos.getDirection())) {
			this.shortPos = pos.getPosition();
			this.shortYd = pos.getYdPosition();
			this.shortTd = this.shortPos - this.shortYd;
			this.shortPnl = pos.getPositionProfit();
			this.shortPrice = pos.getPrice();
		}
    } 
            
	//////////////////////// Getter Setter///////////////////////

	public String getExchange() {
		return exchange;
	}

	public void setExchange(String exchange) {
		this.exchange = exchange;
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

}
