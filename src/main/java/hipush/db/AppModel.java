package hipush.db;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "apps")
public class AppModel {

	@DatabaseField(id = true)
	private int id;
	@DatabaseField(unique = true)
	private String key;
	@DatabaseField
	private String secret;
	@DatabaseField
	private String pkg;
	@DatabaseField
	private String name;
	@DatabaseField
	private long createTs;

	public AppModel() {
	}

	public AppModel(int id, String key, String secret, String pkg, String name,
			long createTs) {
		this.id = id;
		this.key = key;
		this.secret = secret;
		this.pkg = pkg;
		this.name = name;
		this.createTs = createTs;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPkg() {
		return pkg;
	}

	public void setPkg(String pkg) {
		this.pkg = pkg;
	}

	public long getCreateTs() {
		return createTs;
	}

	public void setCreateTs(long createTs) {
		this.createTs = createTs;
	}

}
