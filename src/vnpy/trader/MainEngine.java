package vnpy.trader;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Level;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import vnpy.event.Event;
import vnpy.event.EventEngine;
import vnpy.event.EventType;
import vnpy.trader.app.ctaStrategy.BacktestingEngine;
import vnpy.trader.app.ctaStrategy.CtaBaseConstant;
import vnpy.trader.app.ctaStrategy.CtaTemplate;
import vnpy.trader.riskManager.RmEngine;
import vnpy.utils.AppException;
import vnpy.utils.Method;

// 主引擎
public class MainEngine {
	private String todayDate;// 记录今日日期
	private EventEngine eventEngine;// 事件引擎
	private DataEngine dataEngine;// 数据引擎
	private MongoClient dbClient; // MongoDB客户端对象
	private Map<String, VtGateway> gatewayDict; // 接口实例
	private List<Map<String, String>> gatewayDetailList;
	private Map<String, VtAppModule> appDict; // 应用模块实例
	private List<Map<String, String>> appDetailList;
	private RmEngine rmEngine;// 风控引擎实例
	private LogEngine logEngine; // 日志引擎实例

	private final CodecRegistry pojoCodecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
			fromProviders(PojoCodecProvider.builder().automatic(true).build()));

	// Constructor
	public MainEngine(EventEngine eventEngine) {
		// 记录今日日期
		this.todayDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

		// 绑定事件引擎
		this.eventEngine = eventEngine;
		this.eventEngine.start();

		// 创建数据引擎
		this.dataEngine = new DataEngine(this.eventEngine);

		// MongoDB数据库相关
		this.dbClient = null; // MongoDB客户端对象

		// 接口实例
		this.gatewayDict = new LinkedHashMap<String, VtGateway>();
		this.gatewayDetailList = new ArrayList<Map<String, String>>();

		// 应用模块实例
		this.appDict = new LinkedHashMap<String, VtAppModule>();
		this.appDetailList = new ArrayList<Map<String, String>>();

		// 风控引擎实例（特殊独立对象）
		this.rmEngine = null;

		// 日志引擎实例
		this.logEngine = null;
		this.initLogEngine();

	}

	// 初始化日志引擎
	private void initLogEngine() {
		if (!Boolean.parseBoolean(VtGlobal.globalSetting.get("logActive"))) {
			return;
		}

		// 创建引擎
		this.logEngine = LogEngine.getInstance();

		// 设置日志级别
		Map<String, Level> levelDict = new HashMap<String, Level>() {
			private static final long serialVersionUID = 1L;
			{
				put("debug", LogEngine.LEVEL_DEBUG);
				put("info", LogEngine.LEVEL_INFO);
				put("warn", LogEngine.LEVEL_WARN);
				put("error", LogEngine.LEVEL_ERROR);
				put("critical", LogEngine.LEVEL_CRITICAL);
			}
		};
		Level level = levelDict.getOrDefault(VtGlobal.globalSetting.get("logLevel"), LogEngine.LEVEL_CRITICAL);
		this.logEngine.setLogLevel(level);

		// 设置输出
		if (Boolean.parseBoolean(VtGlobal.globalSetting.get("logConsole"))) {
			this.logEngine.addConsoleHandler();
		}

		if (Boolean.parseBoolean(VtGlobal.globalSetting.get("logFile"))) {
			this.logEngine.addFileHandler();
		}

		// 注册事件监听
		this.registerLogEvent(EventType.EVENT_LOG);
	}

	// 注册日志事件监听
	private void registerLogEvent(String eventType) {
		if (this.logEngine != null) {
			this.eventEngine.register(eventType, new Method(this.logEngine, "processLogEvent", Event.class));
		}
	}

	// 添加底层接口
	public void addGateway(Class<?> gatewayModule) {
		String gatewayName;

		// 创建接口实例
		Constructor<?> con;
		VtGateway gateway;
		try {
			con = gatewayModule.getConstructor(EventEngine.class);
			gateway = (VtGateway) con.newInstance(this.eventEngine);
		} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
			throw new AppException("实例化出错");
		}
		gatewayName = gateway.getGatewayName();
		this.gatewayDict.put(gatewayName, gateway);

		// 保存接口详细信息
		Map<String, String> d = new HashMap<String, String>();
		d.put("gatewayName", gateway.getGatewayName());
		d.put("gatewayDisplayName", gateway.getGatewayName());
		d.put("gatewayType", gateway.getGatewayType());

		this.gatewayDetailList.add(d);
	}

	// 向MongoDB中插入数据，d是具体数据
	public void dbInsert(String dbName, String collectionName, Object d) {
		if (this.dbClient != null) {

//			CodecRegistry pojoCodecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
//					fromProviders(PojoCodecProvider.builder().automatic(true).build()));
			MongoDatabase db = this.dbClient.getDatabase(dbName).withCodecRegistry(this.pojoCodecRegistry);
			MongoCollection collection = db.getCollection(collectionName, d.getClass());
			collection.insertOne(d);
		} else {
			this.writeLog(Text.DATA_INSERT_FAILED);
		}
	}

	// 添加上层应用
	public void addApp(Class<?> appModule) {
		// 创建应用实例
		Constructor<?> con;
		VtAppModule app;
		try {
			con = appModule.getConstructor(MainEngine.class, EventEngine.class);
			app = (VtAppModule) con.newInstance(this, this.eventEngine);
		} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
			throw new AppException("实例化出错");
		}
		String appName = app.getAppName();
		this.appDict.put(appName, app);

		// 保存应用信息
		Map<String, String> d = new HashMap<String, String>();
		d.put("appName", app.getAppName());
		d.put("appDisplayName", app.getAppName());

		this.appDetailList.add(d);
	}

	// 订阅特定接口的行情
	public void subscribe(VtSubscribeReq subscribeReq, String gatewayName) {
		VtGateway gateway = this.gatewayDict.get(gatewayName);

		if (gateway != null) {
			gateway.subscribe(subscribeReq);
		}
	}

	// 连接特定名称的接口
	public void connect(String gatewayName) {
		VtGateway gateway = this.gatewayDict.get(gatewayName);

		if (gateway != null) {
			gateway.connect();
		}

		this.dbConnect();
	}

	// 连接MongoDB数据库
	private void dbConnect() {
		if (this.dbClient == null) {
			// 读取MongoDB的设置
			try {
				// 设置MongoDB操作的超时时间为0.5秒
				this.dbClient = MongoClients.create("mongodb://" + VtGlobal.globalSetting.get("mongoHost") + ":"
						+ VtGlobal.globalSetting.get("mongoPort"));

				// 调用server_info查询服务器状态，防止服务器异常并未连接成功
				// self.dbClient.server_info()
				this.writeLog(Text.DATABASE_CONNECTING_COMPLETED);

				// 如果启动日志记录，则注册日志事件监听函数
				if (VtGlobal.globalSetting.get("mongoLogging") != null
						&& !"".equals(VtGlobal.globalSetting.get("mongoLogging").trim())) {
					this.eventEngine.register(EventType.EVENT_LOG, new Method(this, "dbLogging", Event.class));
				}
			} catch (Exception e) {
				this.dbClient = null;
				this.writeLog(Text.DATABASE_CONNECTING_FAILED);
			}
		}
	}

	// 快速发出日志事件
	private void writeLog(String content) {
		VtLogData log = new VtLogData();
		log.setLogContent(content);
		log.setGatewayName("MAIN_ENGINE");
		Event event = new Event(EventType.EVENT_LOG);
		event.getDict_().put("data", log);
		this.eventEngine.put(event);
	}

	// 向MongoDB中插入日志
	private void dbLogging(Event event) {
		VtLogData log = (VtLogData) event.getDict_().get("data");

		this.dbInsert(CtaBaseConstant.LOG_DB_NAME, this.todayDate.toString(), log);
	}

	public static void main(String[] args) {
		Object d;
		d = new VtTickData();
		System.out.println(d.getClass());
	}
}
