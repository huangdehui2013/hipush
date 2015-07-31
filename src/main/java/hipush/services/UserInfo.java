package hipush.services;

public class UserInfo {
	private int appId;
	private String deviceId;
	private transient String clientId;

	public UserInfo() {

	}

	public UserInfo(int appId, String deviceId) {
		this.appId = appId;
		this.deviceId = deviceId;
	}

	public int getAppId() {
		return appId;
	}

	public void setAppId(int appId) {
		this.appId = appId;
	}

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

}
