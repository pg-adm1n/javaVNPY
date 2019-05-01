package vnpy.trader;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.varia.NullAppender;

import vnpy.event.Event;
import vnpy.utils.AppException;
import vnpy.utils.Method;

// 日志引擎
public class LogEngine {
	// 日志级别
	public static final Level LEVEL_DEBUG = Level.DEBUG;
	public static final Level LEVEL_INFO = Level.INFO;
	public static final Level LEVEL_WARN = Level.WARN;
	public static final Level LEVEL_ERROR = Level.ERROR;
	public static final Level LEVEL_CRITICAL = Level.FATAL;
	 
	// 单例模式
	private static LogEngine INSTANCE = new LogEngine();

	public static LogEngine getInstance() {
		return INSTANCE;
	}

	private Logger logger;
	private Level level;
	private PatternLayout formatter;

	private ConsoleAppender consoleHandler;
	private FileAppender fileHandler;
	private Map<Level, Method> levelFunctionDict;

	// Constructor
	private LogEngine() {
		this.logger = Logger.getLogger(LogEngine.class);
		;
		// self.formatter = logging.Formatter('%(asctime)s %(levelname)s: %(message)s')
		this.formatter = new PatternLayout("%d [%p]: %m%n");
		this.level = LogEngine.LEVEL_CRITICAL;

		this.consoleHandler = null;
		this.fileHandler = null;

		// 添加NullHandler防止无handler的错误输出
		NullAppender nullHandler = new NullAppender();
		this.logger.addAppender(nullHandler);

		// 日志级别函数映射
		this.levelFunctionDict = new HashMap<Level, Method>();
        this.levelFunctionDict.put(LogEngine.LEVEL_DEBUG, new Method(this, "debug", String.class));
        this.levelFunctionDict.put(LogEngine.LEVEL_INFO, new Method(this, "info", String.class));
        this.levelFunctionDict.put(LogEngine.LEVEL_WARN, new Method(this, "warn", String.class));
        this.levelFunctionDict.put(LogEngine.LEVEL_ERROR, new Method(this, "error", String.class));
        this.levelFunctionDict.put(LogEngine.LEVEL_CRITICAL, new Method(this, "critical", String.class));
	}

	// 设置日志级别
	public void setLogLevel(Level level) {
		this.logger.setLevel(level);
		this.level = level;
	}

	// 添加终端输出
	public void addConsoleHandler() {
		if (this.consoleHandler == null) {
			this.consoleHandler = new ConsoleAppender();
			this.consoleHandler.setThreshold(this.level);
			this.consoleHandler.setLayout(this.formatter);
			this.consoleHandler.activateOptions();
			this.logger.addAppender(this.consoleHandler);
		}
	}

	// 添加文件输出
	public void addFileHandler() {
		addFileHandler("");
	}

	// 添加文件输出
	public void addFileHandler(String filename) {
		if (this.fileHandler == null) {
			if (filename == null || "".equals(filename.trim())) {
				filename = "vt_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".log";
				String filepath = "log/" + filename;
				File logPath = new File("log/");
				if (!logPath.exists()) {
					logPath.mkdir();
				}
				File logFile = new File("log/" + filename);
				if (!logPath.exists()) {
					try {
						logFile.createNewFile();
					} catch (IOException e) {
						e.printStackTrace();
						throw new AppException("创建日志失败!");
					}
				}
				this.fileHandler = new FileAppender();
				this.fileHandler.setFile(filepath);
				this.fileHandler.setAppend(true);
				this.fileHandler.setEncoding("utf-8");
				this.fileHandler.setThreshold(this.level);
				this.fileHandler.setLayout(this.formatter);
				this.fileHandler.activateOptions();
				this.logger.addAppender(this.fileHandler);
			}
		}
	}

	// 开发时用
	public void debug(String msg) {
		this.logger.debug(msg);
	}

	// 正常输出
	public void info(String msg) {
		this.logger.info(msg);
	}

	// 警告信息
	public void warn(String msg) {
		this.logger.warn(msg);
	}

	// 报错输出
	public void error(String msg) {
		this.logger.error(msg);
	}

	// 报错输出+记录异常信息
	public void exception(String msg, Throwable e) {
		this.logger.error(msg, e);
	}

	// 影响程序运行的严重错误
	public void critical(String msg) {
		this.logger.fatal(msg);
	}

	// 处理日志事件
	public void processLogEvent(Event event) {
		VtLogData log = (VtLogData) event.getDict_().get("data");
		// 获取日志级别对应的处理函数
		Method function = this.levelFunctionDict.get(log.getLogLevel());
		String msg = log.getGatewayName() + "\t" + log.getLogContent();
		function.invoke(msg);
	}

	public static void main(String[] args) {
		LogEngine le = LogEngine.getInstance();
		// LogEngine le = new LogEngine();
		le.addConsoleHandler();
		// le.addFileHandler();
		le.debug("debug");
		le.info("info");
		le.warn("warn");
		le.error("error");
		le.critical("critical");

//		Event event = new Event(EventType.EVENT_LOG);
//		VtLogData log = new VtLogData();
//		log.setLogContent("Test!!!");
//		log.setLogLevel(Level.INFO);
//		event.getDict_().put("data", log);
//		le.processLogEvent(event);
	}
}
