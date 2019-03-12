package vnpy.trader;

import java.time.LocalTime;

import org.apache.log4j.Level;
import org.bson.codecs.pojo.annotations.BsonIgnore;

// 日志数据类
public class VtLogData extends VtBaseData {
	private String logTime; // 日志生成时间
	private String logContent; // 日志信息
	@BsonIgnore
	private Level logLevel; // 日志级别

	public VtLogData() {
		this.logTime = LocalTime.now().toString(); // 日志生成时间
		this.logContent = ""; // 日志信息
		this.logLevel = Level.INFO; // 日志级别
	}

	public String getLogTime() {
		return logTime;
	}

	public void setLogTime(String logTime) {
		this.logTime = logTime;
	}

	public String getLogContent() {
		return logContent;
	}

	public void setLogContent(String logContent) {
		this.logContent = logContent;
	}

	public Level getLogLevel() {
		return logLevel;
	}

	public void setLogLevel(Level logLevel) {
		this.logLevel = logLevel;
	}
}
