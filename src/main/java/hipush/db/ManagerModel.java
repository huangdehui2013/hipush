package hipush.db;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "managers")
public class ManagerModel {

	@DatabaseField(id = true)
	private String username;
	@DatabaseField
	private String passwordHash;
	@DatabaseField
	private String displayName;
	@DatabaseField
	private long createTs;

	public ManagerModel() {
	}

	public ManagerModel(String username, String passwordHash,
			String displayName, long createTs) {
		this.username = username;
		this.passwordHash = passwordHash;
		this.displayName = displayName;
		this.createTs = createTs;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public long getCreateTs() {
		return createTs;
	}

	public void setCreateTs(long createTs) {
		this.createTs = createTs;
	}

}
