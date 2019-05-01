package vnpy.trader;

// 账户数据类
public class VtAccountData extends VtBaseData{
	// 账号代码相关
    private String accountID;           // 账户代码
    private String vtAccountID;         // 账户在vt中的唯一代码，通常是 Gateway名.账户代码
    
    // 数值相关
    private double preBalance;           // 昨日账户结算净值
    private double balance;              // 账户净值
    private double available;            // 可用资金
    private double commission;           // 今日手续费
    private double margin;               // 保证金占用
    private double closeProfit;          // 平仓盈亏
    private double positionProfit;       // 持仓盈亏
    
    
    
    
	public String getAccountID() {
		return accountID;
	}
	public void setAccountID(String accountID) {
		this.accountID = accountID;
	}
	public String getVtAccountID() {
		return vtAccountID;
	}
	public void setVtAccountID(String vtAccountID) {
		this.vtAccountID = vtAccountID;
	}
	public double getPreBalance() {
		return preBalance;
	}
	public void setPreBalance(double preBalance) {
		this.preBalance = preBalance;
	}
	public double getBalance() {
		return balance;
	}
	public void setBalance(double balance) {
		this.balance = balance;
	}
	public double getAvailable() {
		return available;
	}
	public void setAvailable(double available) {
		this.available = available;
	}
	public double getCommission() {
		return commission;
	}
	public void setCommission(double commission) {
		this.commission = commission;
	}
	public double getMargin() {
		return margin;
	}
	public void setMargin(double margin) {
		this.margin = margin;
	}
	public double getCloseProfit() {
		return closeProfit;
	}
	public void setCloseProfit(double closeProfit) {
		this.closeProfit = closeProfit;
	}
	public double getPositionProfit() {
		return positionProfit;
	}
	public void setPositionProfit(double positionProfit) {
		this.positionProfit = positionProfit;
	}
}
