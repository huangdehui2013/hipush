package hipush.db;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "users")
public class UserModel {
	@DatabaseField(id = true)
	private String clientId;
	@DatabaseField(uniqueCombo = true)
	private String deviceId;
	@DatabaseField(uniqueCombo = true)
	private int appId;
	@DatabaseField
	private long createTs;

	public UserModel() {
	}

	public UserModel(String deviceId, int appId, String clientId, long createTs) {
		this.deviceId = deviceId;
		this.appId = appId;
		this.clientId = clientId;
		this.createTs = createTs;
	}

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public int getAppId() {
		return appId;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public long getCreateTs() {
		return createTs;
	}

	public void setCreateTs(long createTs) {
		this.createTs = createTs;
	}

}
