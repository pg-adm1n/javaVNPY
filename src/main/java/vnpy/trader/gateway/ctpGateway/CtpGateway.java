package vnpy.trader.gateway.ctpGateway;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.Gson;

import vnpy.event.Event;
import vnpy.event.EventEngine;
import vnpy.event.EventType;
import vnpy.gson.property.bean.CTPConnectBean;
import vnpy.gson.property.bean.DrSettingBean;
import vnpy.trader.BarGenerator;
import vnpy.trader.VtBarData;
import vnpy.trader.VtCancelOrderReq;
import vnpy.trader.VtConstant;
import vnpy.trader.VtGateway;
import vnpy.trader.VtHistoryReq;
import vnpy.trader.VtLogData;
import vnpy.trader.VtOrderReq;
import vnpy.trader.VtSubscribeReq;
import vnpy.utils.AppException;
import vnpy.utils.Method;

// CTP接口
public class CtpGateway extends VtGateway {

	private CtpMdSpi mdSpi;// 行情SPI
	private CtpTdSpi tdSpi;// 交易API

	private boolean mdConnected;// 行情API连接状态，登录完成后为True
	private boolean tdConnected; // 交易API连接状态

	private String fileName;
	private String filePath;
	
	private ArrayList<Method> qryFunctionList;
	private int qryCount;// 查询触发倒计时
	private int qryTrigger;// 查询触发点
	private int qryNextFunction;// 上次运行的查询函数索引
	
	static{
		System.loadLibrary("thostmduserapi");
		System.loadLibrary("thosttraderapi");
		System.loadLibrary("thostapi_wrap");
	}
	
	public CtpGateway(EventEngine eventEngine) {
		this(eventEngine, "CTP");
	}

	public CtpGateway(EventEngine eventEngine, String gatewayName) {
		super(eventEngine, gatewayName);
		this.mdSpi = new CtpMdSpi(this); // 行情API
		this.tdSpi = new CtpTdSpi(this); // 交易API

		this.mdConnected = false; // 行情API连接状态，登录完成后为True
		this.tdConnected = false; // 交易API连接状态

		this.setQryEnabled(true); // 循环查询
		this.setGatewayType(VtConstant.GATEWAYTYPE_FUTURES);
		
		this.fileName = this.getGatewayName() + "_connect.json";
		this.filePath = "input/" + fileName;
	}

	// 连接
	@Override
	public void connect() {
		String userID;
	    String password;
	    String brokerID;
	    String tdAddress;
	    String mdAddress;
	    
	    String authCode;
        String userProductInfo;
	    
		try (BufferedReader br = new BufferedReader(new FileReader(this.filePath))) {

			Gson gson = new Gson();
			CTPConnectBean setting = gson.fromJson(br, CTPConnectBean.class);

			userID = setting.getUserID();
		    password = setting.getPassword();
		    brokerID = setting.getBrokerID();
		    tdAddress = setting.getTdAddress();
		    mdAddress = setting.getMdAddress();
		    
		    // 如果json文件提供了验证码
            if (setting.getAuthCode()!=null){ 
                authCode = setting.getAuthCode();
                userProductInfo = setting.getUserProductInfo();
                this.tdSpi.setRequireAuthentication(true);
            }
            else {
            	authCode = null;
                userProductInfo = null;
            }
		} catch (IOException e) {
			e.printStackTrace();
			VtLogData log = new VtLogData();
		    log.setGatewayName(this.getGatewayName());
		    log.setLogContent(Text.LOADING_ERROR);
		    this.onLog(log);
			throw new AppException("读取CTP_connect.json失败");
		}
		
        // 创建行情和交易接口对象
		this.mdSpi.connect(userID, password, brokerID, mdAddress);
        this.tdSpi.connect(userID, password, brokerID, tdAddress, authCode, userProductInfo);
        
        // 初始化并启动查询
        this.initQuery();
	}

	// 初始化连续查询
    private void initQuery() {
        if (this.isQryEnabled()) {
        	// 需要循环的查询函数列表
        	this.qryFunctionList = new ArrayList<Method>();
        	this.qryFunctionList.add(new Method(this, "qryAccount"));
        	this.qryFunctionList.add(new Method(this, "qryPosition"));
            
            this.qryCount = 0;           // 查询触发倒计时
            this.qryTrigger = 2;         // 查询触发点
            this.qryNextFunction = 0;    // 上次运行的查询函数索引
            
            this.startQuery();
		}
    }
    
    // 启动连续查询
    private void startQuery() {
        this.getEventEngine().register(EventType.EVENT_TIMER, new Method(this, "query", Event.class));
    }
    
    // 注册到事件处理引擎上的查询函数
    private void query(Event event) {
        this.qryCount += 1;
        
        if (this.qryCount > this.qryTrigger) {
        	// 清空倒计时
            this.qryCount = 0;
            
            // 执行查询函数
            Method function = this.qryFunctionList.get(this.qryNextFunction);
            function.invoke();
            
            // 计算下次查询函数的索引，如果超过了列表长度，则重新设为0
            this.qryNextFunction += 1;
            if (this.qryNextFunction == this.qryFunctionList.size()) {
            	this.qryNextFunction = 0;
			}
		}
    }
    
    // 订阅行情
	@Override
	public void subscribe(VtSubscribeReq subscribeReq) {
	    this.mdSpi.subscribe(subscribeReq);
	}

	@Override
	public void sendOrder(VtOrderReq orderReq) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void cancelOrder(VtCancelOrderReq cancelOrderReq) {
		// TODO Auto-generated method stub
		
	}

	// 查询账户资金
	@Override
	public void qryAccount() {
		this.tdSpi.qryAccount();
	}

	// 查询持仓
	@Override
	public void qryPosition() {
		this.tdSpi.qryPosition();
	}

	@Override
	public void qryHistory(VtHistoryReq historyReq) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
		
	}
	
	////////////////////Getter Setter//////////////////////////
	public CtpMdSpi getMdSpi() {
		return mdSpi;
	}

	public void setMdSpi(CtpMdSpi mdSpi) {
		this.mdSpi = mdSpi;
	}

	public CtpTdSpi getTdSpi() {
		return tdSpi;
	}

	public void setTdSpi(CtpTdSpi tdSpi) {
		this.tdSpi = tdSpi;
	}

	public boolean isMdConnected() {
		return mdConnected;
	}

	public void setMdConnected(boolean mdConnected) {
		this.mdConnected = mdConnected;
	}

	public boolean isTdConnected() {
		return tdConnected;
	}

	public void setTdConnected(boolean tdConnected) {
		this.tdConnected = tdConnected;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

}
