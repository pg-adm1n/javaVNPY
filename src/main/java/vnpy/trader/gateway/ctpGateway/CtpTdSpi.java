package vnpy.trader.gateway.ctpGateway;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ctp.thostapi.CThostFtdcInstrumentField;
import ctp.thostapi.CThostFtdcMdApi;
import ctp.thostapi.CThostFtdcQryInstrumentField;
import ctp.thostapi.CThostFtdcReqAuthenticateField;
import ctp.thostapi.CThostFtdcReqUserLoginField;
import ctp.thostapi.CThostFtdcRspAuthenticateField;
import ctp.thostapi.CThostFtdcRspInfoField;
import ctp.thostapi.CThostFtdcRspUserLoginField;
import ctp.thostapi.CThostFtdcSettlementInfoConfirmField;
import ctp.thostapi.CThostFtdcSettlementInfoField;
import ctp.thostapi.CThostFtdcTraderApi;
import ctp.thostapi.CThostFtdcTraderSpi;
import ctp.thostapi.THOST_TE_RESUME_TYPE;
import vnpy.trader.VtConstant;
import vnpy.trader.VtContractData;
import vnpy.trader.VtErrorData;
import vnpy.trader.VtGlobal;
import vnpy.trader.VtLogData;
import vnpy.trader.VtPositionData;

public class CtpTdSpi extends CThostFtdcTraderSpi {

	private CtpGateway gateway; // gateway对象
	private String gatewayName; // gateway对象名称

	private int reqID; // 操作请求编号
	private int orderRef; // 订单编号

	private boolean connectionStatus; // 连接状态
	private boolean loginStatus; // 登录状态
	private boolean authStatus; // 验证状态
	private boolean loginFailed; // 登录失败（账号密码错误）

	private String userID; // 账号
	private String password; // 密码
	private String brokerID; // 经纪商代码
	private String address; // 服务器地址
	private String authCode;
	private String userProductInfo;

	private int frontID; // 前置机编号
	private int sessionID; // 会话编号

	private Map<String, VtPositionData> posDict;
	private Map<String, String> symbolExchangeDict; // 保存合约代码和交易所的印射关系
	private Map<String, Integer> symbolSizeDict; // 保存合约代码和合约大小的印射关系

	private boolean requireAuthentication;

	private CThostFtdcTraderApi tdApi;

	public CtpTdSpi(CtpGateway gateway) {
		this.gateway = gateway; // gateway对象
		this.gatewayName = gateway.getGatewayName(); // gateway对象名称

		this.reqID = 0; // 操作请求编号
		this.orderRef = 0; // 订单编号

		this.connectionStatus = false; // 连接状态
		this.loginStatus = false; // 登录状态
		this.authStatus = false; // 验证状态
		this.loginFailed = false; // 登录失败（账号密码错误）

		this.userID = ""; // 账号
		this.password = ""; // 密码
		this.brokerID = ""; // 经纪商代码
		this.address = ""; // 服务器地址

		this.frontID = 0; // 前置机编号
		this.sessionID = 0; // 会话编号

		this.posDict = new ConcurrentHashMap<String, VtPositionData>();
		this.symbolExchangeDict = new ConcurrentHashMap<String, String>(); // 保存合约代码和交易所的印射关系
		this.symbolSizeDict = new ConcurrentHashMap<String, Integer>(); // 保存合约代码和合约大小的印射关系

		this.requireAuthentication = false;
	}

	// 初始化连接
	public void connect(String userID, String password, String brokerID, String address, String authCode,
			String userProductInfo) {
		this.userID = userID; // 账号
		this.password = password; // 密码
		this.brokerID = brokerID; // 经纪商代码
		this.address = address; // 服务器地址
		this.authCode = authCode; // 验证码
		this.userProductInfo = userProductInfo; // 产品信息

		// 如果尚未建立服务器连接，则进行连接
		if (!this.connectionStatus) {
			// 创建C++环境中的API对象，这里传入的参数是需要用来保存.con文件的文件夹路径
			File path = new File("temp/");
			if (!path.exists()) {
				path.mkdirs();
			}

			this.tdApi = CThostFtdcTraderApi.CreateFtdcTraderApi(path.getPath()+System.getProperty("file.separator"));
			this.tdApi.RegisterSpi(this);

			// 设置数据同步模式为推送从今日开始所有数据
			this.tdApi.SubscribePublicTopic(THOST_TE_RESUME_TYPE.THOST_TERT_RESTART);
			this.tdApi.SubscribePrivateTopic(THOST_TE_RESUME_TYPE.THOST_TERT_RESTART);

			// 注册服务器地址
			this.tdApi.RegisterFront(this.address);

			// 初始化连接，成功会调用onFrontConnected
			this.tdApi.Init();
		}
		// 若已经连接但尚未登录，则进行登录
		else {
			if (this.requireAuthentication && !this.authStatus) {
				this.authenticate();
			} else if (!this.loginStatus) {
				this.login();
			}
		}
	}

	// 连接服务器
	private void login() {
		// 如果之前有过登录失败，则不再进行尝试
		if (this.loginFailed)
			return;

		// 如果填入了用户名密码等，则登录
		if ((this.userID != null && !"".equals(this.userID.trim()))
				&& (this.password != null && !"".equals(this.password.trim()))
				&& (this.brokerID != null && !"".equals(this.brokerID.trim()))) {
			CThostFtdcReqUserLoginField req = new CThostFtdcReqUserLoginField();
			req.setUserID(this.userID);
			req.setPassword(this.password);
			req.setBrokerID(this.brokerID);
			this.reqID += 1;
			this.tdApi.ReqUserLogin(req, this.reqID);
		}
	}

	// 申请验证
	private void authenticate() {
		if ((this.userID != null && !"".equals(this.userID.trim()))
				&& (this.brokerID != null && !"".equals(this.brokerID.trim()))
				&& (this.authCode != null && !"".equals(this.authCode.trim()))
				&& (this.userProductInfo != null && !"".equals(this.userProductInfo.trim()))) {
			CThostFtdcReqAuthenticateField req = new CThostFtdcReqAuthenticateField();
			req.setUserID(this.userID);
			req.setBrokerID(this.brokerID);
			req.setAuthCode(this.authCode);
			req.setUserProductInfo(this.userProductInfo);
			this.reqID += 1;
			this.tdApi.ReqAuthenticate(req, this.reqID);
		}
	}

	// 服务器连接
	@Override
	public void OnFrontConnected() {
		this.connectionStatus = true;

		this.writeLog(Text.TRADING_SERVER_CONNECTED);

		if (this.requireAuthentication)
			this.authenticate();
		else
			this.login();
	}

	// 验证客户端回报
	@Override
	public void OnRspAuthenticate(CThostFtdcRspAuthenticateField pRspAuthenticateField, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
		boolean isError = (pRspInfo != null) && (pRspInfo.getErrorID() != 0);
		if (!isError) {
			this.authStatus = true;
		            
		    this.writeLog(Text.TRADING_SERVER_AUTHENTICATED);
		            
		    this.login();
		} else {
			VtErrorData err = new VtErrorData();
		    err.setGatewayName(this.gatewayName);
		    err.setErrorID(pRspInfo.getErrorID()+"");
		    err.setErrorMsg(pRspInfo.getErrorMsg());
		    this.gateway.onError(err);
		}
	}
	
	// 登陆回报
	@Override
	public void OnRspUserLogin(CThostFtdcRspUserLoginField pRspUserLogin, CThostFtdcRspInfoField pRspInfo,
			int nRequestID, boolean bIsLast) {
		boolean isError = (pRspInfo != null) && (pRspInfo.getErrorID() != 0);
		// 如果登录成功，推送日志信息
		if (!isError) {
			this.frontID = pRspUserLogin.getFrontID();
		    this.sessionID = pRspUserLogin.getSessionID();
		    this.loginStatus = true;
		    this.gateway.setTdConnected(true);
		            
		    this.writeLog(Text.TRADING_SERVER_LOGIN);
		    
		    // 确认结算信息
		    CThostFtdcSettlementInfoConfirmField req = new CThostFtdcSettlementInfoConfirmField();
		    req.setBrokerID(this.brokerID);
		    req.setInvestorID(this.userID);
            this.reqID += 1;
            this.tdApi.ReqSettlementInfoConfirm(req, this.reqID);        
		}
        // 否则，推送错误信息
		else {
			VtErrorData err = new VtErrorData();
			err.setGatewayName(this.gatewayName);
			err.setErrorID(pRspInfo.getErrorID()+"");
			err.setErrorMsg(pRspInfo.getErrorMsg());
			this.gateway.onError(err);
			
			// 标识登录失败，防止用错误信息连续重复登录
            this.loginFailed = true;
		}
	}
	
	// 确认结算信息回报
	@Override
	public void OnRspSettlementInfoConfirm(CThostFtdcSettlementInfoConfirmField pSettlementInfoConfirm, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
		this.writeLog(Text.SETTLEMENT_INFO_CONFIRMED);
        
        // 查询合约代码
        this.reqID += 1;
        this.tdApi.ReqQryInstrument(new CThostFtdcQryInstrumentField(), this.reqID);
	}
	
	// 合约查询回报
	@Override
	public void OnRspQryInstrument(CThostFtdcInstrumentField pInstrument, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
		VtContractData contract = new VtContractData();
        contract.setGatewayName(this.gatewayName);

        contract.setSymbol(pInstrument.getInstrumentID());
        contract.setExchange(CtpGlobal.exchangeMapReverse.get(pInstrument.getExchangeID()));
        contract.setVtSymbol(contract.getSymbol());//'.'.join([contract.symbol, contract.exchange])
        contract.setName(pInstrument.getInstrumentName());

        // 合约数值
        contract.setSize(pInstrument.getVolumeMultiple());
        contract.setPriceTick(pInstrument.getPriceTick());
        contract.setStrikePrice(pInstrument.getStrikePrice());
        contract.setProductClass(CtpGlobal.productClassMapReverse.getOrDefault(pInstrument.getProductClass(), VtConstant.PRODUCT_UNKNOWN));
        contract.setExpiryDate(pInstrument.getExpireDate());
        
        // ETF期权的标的命名方式需要调整（ETF代码 + 到期月份）
        if (VtConstant.EXCHANGE_SSE.equals(contract.getExchange()) || VtConstant.EXCHANGE_SZSE.equals(contract.getExchange())) {
			contract.setUnderlyingSymbol(pInstrument.getUnderlyingInstrID()+"-"+pInstrument.getExpireDate().substring(2, pInstrument.getExpireDate().length()-2));
		}
        // 商品期权无需调整
        else {
			contract.setUnderlyingSymbol(pInstrument.getUnderlyingInstrID());
		}

        // 期权类型
        if (VtConstant.PRODUCT_OPTION.equals(contract.getProductClass())) {
			if (pInstrument.getOptionsType() == '1') {
				contract.setOptionType(VtConstant.OPTION_CALL);
			}else if (pInstrument.getOptionsType() == '2') {
				contract.setOptionType(VtConstant.OPTION_PUT);
			}
		}
        
        // 缓存代码和交易所的印射关系
        this.symbolExchangeDict.put(contract.getSymbol(), contract.getExchange());
        this.symbolSizeDict.put(contract.getSymbol(), contract.getSize());

        // 推送
        this.gateway.onContract(contract);
        
        // 缓存合约代码和交易所映射
        CtpGlobal.symbolExchangeDict.put(contract.getSymbol(), contract.getExchange());
        
        if(bIsLast) {
        	this.writeLog(Text.CONTRACT_DATA_RECEIVED);
        }
	}
	
	
	public void qryAccount() {
		// TODO Auto-generated method stub

	}

	public void qryPosition() {
		// TODO Auto-generated method stub

	}

	// 发出日志
	private void writeLog(String content) {
		VtLogData log = new VtLogData();
		log.setGatewayName(this.getGatewayName());
		log.setLogContent(content);
		this.gateway.onLog(log);
	}

	/////////////////////// Getter Setter////////////////////////////
	public CtpGateway getGateway() {
		return gateway;
	}

	public void setGateway(CtpGateway gateway) {
		this.gateway = gateway;
	}

	public String getGatewayName() {
		return gatewayName;
	}

	public void setGatewayName(String gatewayName) {
		this.gatewayName = gatewayName;
	}

	public int getReqID() {
		return reqID;
	}

	public void setReqID(int reqID) {
		this.reqID = reqID;
	}

	public int getOrderRef() {
		return orderRef;
	}

	public void setOrderRef(int orderRef) {
		this.orderRef = orderRef;
	}

	public boolean isConnectionStatus() {
		return connectionStatus;
	}

	public void setConnectionStatus(boolean connectionStatus) {
		this.connectionStatus = connectionStatus;
	}

	public boolean isLoginStatus() {
		return loginStatus;
	}

	public void setLoginStatus(boolean loginStatus) {
		this.loginStatus = loginStatus;
	}

	public boolean isAuthStatus() {
		return authStatus;
	}

	public void setAuthStatus(boolean authStatus) {
		this.authStatus = authStatus;
	}

	public boolean isLoginFailed() {
		return loginFailed;
	}

	public void setLoginFailed(boolean loginFailed) {
		this.loginFailed = loginFailed;
	}

	public String getUserID() {
		return userID;
	}

	public void setUserID(String userID) {
		this.userID = userID;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getBrokerID() {
		return brokerID;
	}

	public void setBrokerID(String brokerID) {
		this.brokerID = brokerID;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public int getFrontID() {
		return frontID;
	}

	public void setFrontID(int frontID) {
		this.frontID = frontID;
	}

	public int getSessionID() {
		return sessionID;
	}

	public void setSessionID(int sessionID) {
		this.sessionID = sessionID;
	}

	public Map<String, VtPositionData> getPosDict() {
		return posDict;
	}

	public void setPosDict(Map<String, VtPositionData> posDict) {
		this.posDict = posDict;
	}

	public Map<String, String> getSymbolExchangeDict() {
		return symbolExchangeDict;
	}

	public void setSymbolExchangeDict(Map<String, String> symbolExchangeDict) {
		this.symbolExchangeDict = symbolExchangeDict;
	}

	public Map<String, Integer> getSymbolSizeDict() {
		return symbolSizeDict;
	}

	public void setSymbolSizeDict(Map<String, Integer> symbolSizeDict) {
		this.symbolSizeDict = symbolSizeDict;
	}

	public boolean isRequireAuthentication() {
		return requireAuthentication;
	}

	public void setRequireAuthentication(boolean requireAuthentication) {
		this.requireAuthentication = requireAuthentication;
	}

	public String getAuthCode() {
		return authCode;
	}

	public void setAuthCode(String authCode) {
		this.authCode = authCode;
	}

	public String getUserProductInfo() {
		return userProductInfo;
	}

	public void setUserProductInfo(String userProductInfo) {
		this.userProductInfo = userProductInfo;
	}

}
