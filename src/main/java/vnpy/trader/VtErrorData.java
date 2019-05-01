package vnpy.trader;

import java.time.LocalTime;

// 错误数据类
public class VtErrorData extends VtBaseData {
	private String errorID; // 错误代码
	private String errorMsg; // 错误信息
	private String additionalInfo; // 补充信息

	private String errorTime; // 错误生成时间

	public VtErrorData() {
		this.errorTime = LocalTime.now().toString();    // 错误生成时间
	}

	
	public String getErrorID() {
		return errorID;
	}

	public void setErrorID(String errorID) {
		this.errorID = errorID;
	}

	public String getErrorMsg() {
		return errorMsg;
	}

	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}

	public String getAdditionalInfo() {
		return additionalInfo;
	}

	public void setAdditionalInfo(String additionalInfo) {
		this.additionalInfo = additionalInfo;
	}

	public String getErrorTime() {
		return errorTime;
	}

	public void setErrorTime(String errorTime) {
		this.errorTime = errorTime;
	}
}
