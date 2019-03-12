package vnpy.trader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import vnpy.event.Event;
import vnpy.event.EventEngine;
import vnpy.event.EventType;
import vnpy.utils.Method;

// 数据引擎
public class DataEngine {
	private EventEngine eventEngine;

	// 保存数据的字典和列表
	private Map<String, VtTickData> tickDict;
	private Map<String, VtContractData> contractDict;
	private Map<String, VtOrderData> orderDict;
	private Map<String, VtOrderData> workingOrderDict;// 可撤销委托
	private Map<String, VtTradeData> tradeDict;
	private Map<String, VtAccountData> accountDict;
	private Map<String, VtPositionData> positionDict;
	private List<VtLogData> logList;
	private List<VtErrorData> errorList;

	// 持仓细节相关
	private Map<String, PositionDetail> detailDict; // vtSymbol:PositionDetail
	private List<String> tdPenaltyList; // 平今手续费惩罚的产品代码列表

	private static final Set<String> FINISHED_STATUS = new HashSet<String>() {
		private static final long serialVersionUID = 1L;
		{
			add(VtConstant.STATUS_ALLTRADED);
			add(VtConstant.STATUS_REJECTED);
			add(VtConstant.STATUS_CANCELLED);
		}
	};

	// Constructor
	public DataEngine(EventEngine eventEngine) {
		this.eventEngine = eventEngine;

		// 保存数据的字典和列表
		this.tickDict = new HashMap<String, VtTickData>();
		this.contractDict = new HashMap<String, VtContractData>();
		this.orderDict = new HashMap<String, VtOrderData>();
		this.workingOrderDict = new HashMap<String, VtOrderData>(); // 可撤销委托
		this.tradeDict = new HashMap<String, VtTradeData>();
		this.accountDict = new HashMap<String, VtAccountData>();
		this.positionDict = new HashMap<String, VtPositionData>();
		this.logList = new ArrayList<VtLogData>();
		this.errorList = new ArrayList<VtErrorData>();

		// 持仓细节相关
		this.detailDict = new HashMap<String, PositionDetail>(); // vtSymbol:PositionDetail
		this.tdPenaltyList = Arrays.asList(VtGlobal.globalSetting.get("tdPenalty").split(";")); // 平今手续费惩罚的产品代码列表

		// 读取保存在硬盘的合约数据
		// this.loadContracts();

		// 注册事件监听
		this.registerEvent();
	}

	// 从硬盘读取合约对象
//    private void loadContracts() {
//        f = shelve.open(self.contractFilePath)
//        if 'data' in f:
//            d = f['data']
//            for key, value in d.items():
//                self.contractDict[key] = value
//        f.close()
//    }

	// 注册事件监听
	private void registerEvent() {
        this.eventEngine.register(EventType.EVENT_TICK, new Method(this, "processTickEvent", Event.class));
        this.eventEngine.register(EventType.EVENT_CONTRACT, new Method(this, "processContractEvent", Event.class));
        this.eventEngine.register(EventType.EVENT_ORDER, new Method(this, "processOrderEvent", Event.class));
        this.eventEngine.register(EventType.EVENT_TRADE, new Method(this, "processTradeEvent", Event.class));
        this.eventEngine.register(EventType.EVENT_POSITION, new Method(this, "processPositionEvent", Event.class));
        this.eventEngine.register(EventType.EVENT_ACCOUNT, new Method(this, "processAccountEvent", Event.class));
        this.eventEngine.register(EventType.EVENT_LOG, new Method(this, "processLogEvent", Event.class));
        this.eventEngine.register(EventType.EVENT_ERROR, new Method(this, "processErrorEvent", Event.class));
    }

	// 处理成交事件
	private void processTickEvent(Event event) {
		VtTickData tick = (VtTickData) event.getDict_().get("data");
		this.tickDict.put(tick.getVtSymbol(), tick);
	}

	// 处理合约事件
	private void processContractEvent(Event event) {
		VtContractData contract = (VtContractData) event.getDict_().get("data");
		this.contractDict.put(contract.getVtSymbol(), contract);
		this.contractDict.put(contract.getSymbol(), contract); // 使用常规代码（不包括交易所）可能导致重复
	}

	// 处理委托事件
	private void processOrderEvent(Event event) {
        VtOrderData order = (VtOrderData)event.getDict_().get("data");  
        this.orderDict.put(order.getVtOrderID(), order);
        
        // 如果订单的状态是全部成交或者撤销，则需要从workingOrderDict中移除
        if (DataEngine.FINISHED_STATUS.contains(order.getStatus())) {
			if (this.workingOrderDict.containsKey(order.getVtOrderID())) {
				this.workingOrderDict.remove(order.getVtOrderID());
			}
		}
        // 否则则更新字典中的数据        
        else {
        	this.workingOrderDict.put(order.getVtOrderID(), order);
        }
        
        // 更新到持仓细节中
        PositionDetail detail = this.getPositionDetail(order.getVtSymbol());
        detail.updateOrder(order);  
    }
	
	// 查询持仓细节
    private PositionDetail getPositionDetail(String vtSymbol) {
    	PositionDetail detail;
    	if (this.detailDict.containsKey(vtSymbol)) {
    		detail = this.detailDict.get(vtSymbol);
		} else {
			VtContractData contract = this.getContract(vtSymbol);
			detail = new PositionDetail(vtSymbol, contract);
			this.detailDict.put(vtSymbol, detail);
			
			// 设置持仓细节的委托转换模式
            //contract = this.getContract(vtSymbol)
            
            if (contract!=null) {
            	detail.setExchange(contract.getExchange()); 
                        
            	// 上期所合约
            	if (VtConstant.EXCHANGE_SHFE.equals(contract.getExchange())) {
					detail.setMode(PositionDetail.MODE_SHFE);
				}
            	
            	// 检查是否有平今惩罚
            	for (String productID : this.tdPenaltyList) {
					if (contract!=null && contract.getSymbol().startsWith(productID)) {
						detail.setMode(PositionDetail.MODE_TDPENALTY);
					}
				}
			}
		}
                
        return detail;
    }
    
    // 查询合约对象
    private VtContractData getContract(String vtSymbol) {
        return this.contractDict.get(vtSymbol);
    }
    
    // 处理成交事件
    private void processTradeEvent(Event event) {
        VtTradeData trade = (VtTradeData) event.getDict_().get("data");
        
        this.tradeDict.put(trade.getVtTradeID(), trade);
    
        // 更新到持仓细节中
        PositionDetail detail = this.getPositionDetail(trade.getVtSymbol());
        detail.updateTrade(trade);
    }
    
    // 处理持仓事件
    private void processPositionEvent(Event event) {
        VtPositionData pos = (VtPositionData) event.getDict_().get("data");
        
        this.positionDict.put(pos.getVtPositionName(), pos);
    
        // 更新到持仓细节中
        PositionDetail detail = this.getPositionDetail(pos.getVtSymbol());
        detail.updatePosition(pos);
    }
      
    // 处理账户事件
    private void processAccountEvent(Event event) {
    	VtAccountData account = (VtAccountData) event.getDict_().get("data");
        this.accountDict.put(account.getVtAccountID(), account);
    }
    
    // 处理日志事件
    private void processLogEvent(Event event) {
    	VtLogData log = (VtLogData) event.getDict_().get("data");
        this.logList.add(log);
    }
    
    // 处理错误事件
    private void processErrorEvent(Event event) {
        VtErrorData error = (VtErrorData) event.getDict_().get("data");
        this.errorList.add(error);
    }
}
