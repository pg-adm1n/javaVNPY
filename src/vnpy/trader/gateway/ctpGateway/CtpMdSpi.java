package vnpy.trader.gateway.ctpGateway;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

import ctp.thostapi.CThostFtdcDepthMarketDataField;
import ctp.thostapi.CThostFtdcMdApi;
import ctp.thostapi.CThostFtdcMdSpi;
import ctp.thostapi.CThostFtdcReqUserLoginField;
import ctp.thostapi.CThostFtdcRspInfoField;
import ctp.thostapi.CThostFtdcRspUserLoginField;
import ctp.thostapi.CThostFtdcSpecificInstrumentField;
import vnpy.trader.VtConstant;
import vnpy.trader.VtErrorData;
import vnpy.trader.VtGateway;
import vnpy.trader.VtGlobal;
import vnpy.trader.VtLogData;
import vnpy.trader.VtSubscribeReq;
import vnpy.trader.VtTickData;

// CTP行情API实现
public class CtpMdSpi extends CThostFtdcMdSpi {

	private CtpGateway gateway; // gateway对象
	private String gatewayName; // gateway对象名称

	private int reqID; // 操作请求编号

	private boolean connectionStatus; // 连接状态
	private boolean loginStatus; // 登录状态

	private Set<VtSubscribeReq> subscribedSymbols; // 已订阅合约代码

	private String userID; // 账号
	private String password; // 密码
	private String brokerID; // 经纪商代码
	private String address; // 服务器地址

	private CThostFtdcMdApi mdApi;

	public CtpMdSpi(CtpGateway gateway) {

		this.gateway = gateway; // gateway对象
		this.gatewayName = gateway.getGatewayName(); // gateway对象名称

		this.reqID = 0; // 操作请求编号

		this.connectionStatus = false; // 连接状态
		this.loginStatus = false; // 登录状态

		this.subscribedSymbols = new HashSet<VtSubscribeReq>(); // 已订阅合约代码

		this.userID = ""; // 账号
		this.password = ""; // 密码
		this.brokerID = ""; // 经纪商代码
		this.address = ""; // 服务器地址
	}

	// 发出日志
	private void writeLog(String content) {
		VtLogData log = new VtLogData();
		log.setGatewayName(this.gatewayName);
		log.setLogContent(content);
		this.gateway.onLog(log);
	}

	// 初始化连接
	public void connect(String userID, String password, String brokerID, String address) {
		this.userID = userID; // 账号
		this.password = password; // 密码
		this.brokerID = brokerID; // 经纪商代码
		this.address = address; // 服务器地址

		// 如果尚未建立服务器连接，则进行连接
		if (!this.connectionStatus) {
			// 创建C++环境中的API对象，这里传入的参数是需要用来保存.con文件的文件夹路径
			File path = new File("temp/");
			if (!path.exists()) {
				path.mkdirs();
			}
			this.mdApi = CThostFtdcMdApi.CreateFtdcMdApi(path.getPath()+System.getProperty("file.separator"));
			this.mdApi.RegisterSpi(this);
			
			// 注册服务器地址
			this.mdApi.RegisterFront(this.address);

			// 初始化连接，成功会调用onFrontConnected
			this.mdApi.Init();
		}
		// 若已经连接但尚未登录，则进行登录
		else {
			if (!this.loginStatus) {
				this.login();
			}
		}
	}

	// 服务器连接
	@Override
	public void OnFrontConnected() {
		this.connectionStatus = true;
		this.writeLog(Text.DATA_SERVER_CONNECTED);
		this.login();
	}

	// 登录
	private void login() {
		// 如果填入了用户名密码等，则登录
		if ((this.userID != null && !"".equals(this.userID.trim()))
				&& (this.password != null && !"".equals(this.password.trim()))
				&& (this.brokerID != null && !"".equals(this.brokerID.trim()))) {
			CThostFtdcReqUserLoginField req = new CThostFtdcReqUserLoginField();
			req.setUserID(this.userID);
			req.setPassword(this.password);
			req.setBrokerID(this.brokerID);
			this.reqID += 1;
			this.mdApi.ReqUserLogin(req, this.reqID);
		}
	}

	// 登陆回报
	@Override
	public void OnRspUserLogin(CThostFtdcRspUserLoginField pRspUserLogin, CThostFtdcRspInfoField pRspInfo,
			int nRequestID, boolean bIsLast) {
		boolean isError = (pRspInfo != null) && (pRspInfo.getErrorID() != 0);
		// 如果登录成功，推送日志信息
		if (!isError) {
			this.loginStatus = true;
			this.gateway.setMdConnected(true);

			this.writeLog(Text.DATA_SERVER_LOGIN);

			// 重新订阅之前订阅的合约
			for (VtSubscribeReq subscribeReq : this.subscribedSymbols) {
				this.subscribe(subscribeReq);
			}
		}
		// 否则，推送错误信息
		else {
			VtErrorData err = new VtErrorData();
			err.setGatewayName(this.gatewayName);
			err.setErrorID(pRspInfo.getErrorID() + "");
			err.setErrorMsg(pRspInfo.getErrorMsg());
			this.gateway.onError(err);
		}
	}

	// 订阅合约
	public void subscribe(VtSubscribeReq subscribeReq) {
		// 这里的设计是，如果尚未登录就调用了订阅方法
		// 则先保存订阅请求，登录完成后会自动订阅
		if (this.loginStatus) {
			this.mdApi.SubscribeMarketData(new String[] { subscribeReq.getSymbol() }, 1);
		}
		this.subscribedSymbols.add(subscribeReq);
	}

	// 订阅合约回报
	@Override
	public void OnRspSubMarketData(CThostFtdcSpecificInstrumentField pSpecificInstrument,
			CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
		boolean isError = (pRspInfo != null) && (pRspInfo.getErrorID() != 0);
		if (isError) {
			VtErrorData err = new VtErrorData();
			err.setGatewayName(this.gatewayName);
			err.setErrorID(pRspInfo.getErrorID() + "");
			err.setErrorMsg(pRspInfo.getErrorMsg());
			this.gateway.onError(err);		
		}
	}

	// 行情推送
	@Override
	public void OnRtnDepthMarketData(CThostFtdcDepthMarketDataField pDepthMarketData) {
		// 过滤尚未获取合约交易所时的行情推送
		String symbol = pDepthMarketData.getInstrumentID();
		if (!CtpGlobal.symbolExchangeDict.containsKey(symbol)) {
			return;
		}
		// 创建对象
		VtTickData tick = new VtTickData();
		tick.setGatewayName(this.gatewayName);

		tick.setSymbol(symbol);
		tick.setExchange(CtpGlobal.symbolExchangeDict.get(tick.getSymbol()));
		tick.setVtSymbol(tick.getSymbol()); // '.'.join([tick.symbol, tick.exchange])

		tick.setLastPrice(pDepthMarketData.getLastPrice());
		tick.setVolume(pDepthMarketData.getVolume());
		tick.setOpenInterest((int) pDepthMarketData.getOpenInterest());
		tick.setTime(pDepthMarketData.getUpdateTime() + "." + (pDepthMarketData.getUpdateMillisec() / 100));

		tick.setTurnover(pDepthMarketData.getTurnover());

		// 上期所和郑商所可以直接使用，大商所需要转换
		tick.setDate(pDepthMarketData.getActionDay());

		tick.setOpenPrice(pDepthMarketData.getOpenPrice());
		tick.setHighPrice(pDepthMarketData.getHighestPrice());
		tick.setLowPrice(pDepthMarketData.getLowestPrice());
		tick.setPreClosePrice(pDepthMarketData.getPreClosePrice());

		tick.setUpperLimit(pDepthMarketData.getUpperLimitPrice());
		tick.setLowerLimit(pDepthMarketData.getLowerLimitPrice());

		// CTP只有一档行情
		tick.setBidPrice1(pDepthMarketData.getBidPrice1());
		tick.setBidVolume1(pDepthMarketData.getBidVolume1());
		tick.setAskPrice1(pDepthMarketData.getAskPrice1());
		tick.setAskVolume1(pDepthMarketData.getAskVolume1());

		// 大商所日期转换
		if (VtConstant.EXCHANGE_DCE.equals(tick.getExchange())) {
			tick.setDate(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
		}

		// 上交所，SSE，股票期权相关
		if (VtConstant.EXCHANGE_SSE.equals(tick.getExchange())) {
			tick.setBidPrice2(pDepthMarketData.getBidPrice2());
			tick.setBidVolume2(pDepthMarketData.getBidVolume2());
			tick.setAskPrice2(pDepthMarketData.getAskPrice2());
			tick.setAskVolume2(pDepthMarketData.getAskVolume2());

			tick.setBidPrice3(pDepthMarketData.getBidPrice3());
			tick.setBidVolume3(pDepthMarketData.getBidVolume3());
			tick.setAskPrice3(pDepthMarketData.getAskPrice3());
			tick.setAskVolume3(pDepthMarketData.getAskVolume3());

			tick.setBidPrice4(pDepthMarketData.getBidPrice4());
			tick.setBidVolume4(pDepthMarketData.getBidVolume4());
			tick.setAskPrice4(pDepthMarketData.getAskPrice4());
			tick.setAskVolume4(pDepthMarketData.getAskVolume4());

			tick.setBidPrice5(pDepthMarketData.getBidPrice5());
			tick.setBidVolume5(pDepthMarketData.getBidVolume5());
			tick.setAskPrice5(pDepthMarketData.getAskPrice5());
			tick.setAskVolume5(pDepthMarketData.getAskVolume5());

			tick.setDate(pDepthMarketData.getTradingDay());
		}
		this.gateway.onTick(tick);
	}

	public static void main(String[] args) {
		File path = new File("temp/"+"CTP"+"/");
    	if (!path.exists()) {
			path.mkdirs();
		}
	}
}
