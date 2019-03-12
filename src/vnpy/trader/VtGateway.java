package vnpy.trader;

import vnpy.event.Event;
import vnpy.event.EventEngine;
import vnpy.event.EventType;

// 交易接口
public abstract class VtGateway {
	private EventEngine eventEngine;
	private String gatewayName;

	private boolean qryEnabled;// 循环查询
	private String gatewayType;
	
	public VtGateway(EventEngine eventEngine, String gatewayName) {
		this.eventEngine = eventEngine;
		this.gatewayName = gatewayName;
	}

	// 市场行情推送
    public void onTick(VtTickData tick) {
        // 通用事件
    	Event event1 = new Event(EventType.EVENT_TICK);
        event1.getDict_().put("data", tick);
        this.eventEngine.put(event1);
        
        // 特定合约代码的事件
        Event event2 = new Event(EventType.EVENT_TICK+tick.getVtSymbol());
        event2.getDict_().put("data", tick);
        this.eventEngine.put(event2);
    }
    
    // 成交信息推送
    public void onTrade(VtTradeData trade) {
        // 通用事件
    	Event event1 = new Event(EventType.EVENT_TRADE);
        event1.getDict_().put("data", trade);
        this.eventEngine.put(event1);
        
        // 特定合约的成交事件
        Event event2 = new Event(EventType.EVENT_TRADE+trade.getVtSymbol());
        event2.getDict_().put("data", trade);
        this.eventEngine.put(event2);
    }
    
    // 订单变化推送
    public void onOrder(VtOrderData order) {
        // 通用事件
    	Event event1 = new Event(EventType.EVENT_ORDER);
        event1.getDict_().put("data", order);
        this.eventEngine.put(event1);
        
        // 特定订单编号的事件
        Event event2 = new Event(EventType.EVENT_ORDER+order.getVtOrderID());
        event2.getDict_().put("data", order);
        this.eventEngine.put(event2);
    }
    
    // 持仓信息推送
    public void onPosition(VtPositionData position) {
        // 通用事件
    	Event event1 = new Event(EventType.EVENT_POSITION);
        event1.getDict_().put("data", position);
        this.eventEngine.put(event1);
        
        // 特定合约代码的事件
        Event event2 = new Event(EventType.EVENT_POSITION+position.getVtSymbol());
        event2.getDict_().put("data", position);
        this.eventEngine.put(event2);
    }
    
    // 账户信息推送
    public void onAccount(VtAccountData account) {
        // 通用事件
    	Event event1 = new Event(EventType.EVENT_ACCOUNT);
        event1.getDict_().put("data", account);
        this.eventEngine.put(event1);
        
        // 特定合约代码的事件
        Event event2 = new Event(EventType.EVENT_ACCOUNT+account.getVtAccountID());
        event2.getDict_().put("data", account);
        this.eventEngine.put(event2);
    }
    
    // 错误信息推送
    public void onError(VtErrorData error) {
        // 通用事件
    	Event event1 = new Event(EventType.EVENT_ERROR);
        event1.getDict_().put("data", error);
        this.eventEngine.put(event1);
    }
    
    // 日志推送
    public void onLog(VtLogData log) {
        // 通用事件
    	Event event1 = new Event(EventType.EVENT_LOG);
        event1.getDict_().put("data", log);
        this.eventEngine.put(event1);
    }
    
    // 合约基础信息推送
    public void onContract(VtContractData contract) {
        // 通用事件
    	Event event1 = new Event(EventType.EVENT_CONTRACT);
        event1.getDict_().put("data", contract);
        this.eventEngine.put(event1);     
    }
    
    // 历史数据推送
    public void onHistory(VtHistoryData history) {
        Event event = new Event(EventType.EVENT_HISTORY);
    	event.getDict_().put("data", history);
        this.eventEngine.put(event);  
    }
    
    // 连接
    abstract public void connect();
    
    // 订阅行情
    abstract public void subscribe(VtSubscribeReq subscribeReq);
    
    // 发单
    abstract public void sendOrder(VtOrderReq orderReq);
    
    // 撤单
    abstract public void cancelOrder(VtCancelOrderReq cancelOrderReq);

    // 查询账户资金
    abstract public void qryAccount();
    
    // 查询持仓
    abstract public void qryPosition();
    
    // 查询历史
    abstract public void qryHistory(VtHistoryReq historyReq);
    
    // 关闭
    abstract public void close();
    
    
	//////////////////Getter Setter////////////////////////////
	
	public EventEngine getEventEngine() {
		return eventEngine;
	}

	public void setEventEngine(EventEngine eventEngine) {
		this.eventEngine = eventEngine;
	}

	public String getGatewayName() {
		return gatewayName;
	}

	public void setGatewayName(String gatewayName) {
		this.gatewayName = gatewayName;
	}

	public boolean isQryEnabled() {
		return qryEnabled;
	}

	public void setQryEnabled(boolean qryEnabled) {
		this.qryEnabled = qryEnabled;
	}

	public String getGatewayType() {
		return gatewayType;
	}

	public void setGatewayType(String gatewayType) {
		this.gatewayType = gatewayType;
	}
	
	
}
