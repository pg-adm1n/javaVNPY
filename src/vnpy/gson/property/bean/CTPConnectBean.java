package vnpy.gson.property.bean;

public class CTPConnectBean {
	private String brokerID;
	private String mdAddress;
	private String tdAddress;
	private String userID;
	private String password;
	
	private String authCode;
	private String userProductInfo;
	
	public String getBrokerID() {
		return brokerID;
	}
	public void setBrokerID(String brokerID) {
		this.brokerID = brokerID;
	}
	public String getMdAddress() {
		return mdAddress;
	}
	public void setMdAddress(String mdAddress) {
		this.mdAddress = mdAddress;
	}
	public String getTdAddress() {
		return tdAddress;
	}
	public void setTdAddress(String tdAddress) {
		this.tdAddress = tdAddress;
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
