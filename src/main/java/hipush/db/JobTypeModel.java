package hipush.db;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "job_types")
public class JobTypeModel {

	@DatabaseField(id = true)
	private String key;
	@DatabaseField
	private String name;
	@DatabaseField
	private long createTs;

	public JobTypeModel() {
	}

	public JobTypeModel(String key, String name, long createTs) {
		this.key = key;
		this.name = name;
		this.createTs = createTs;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getCreateTs() {
		return createTs;
	}

	public void setCreateTs(long createTs) {
		this.createTs = createTs;
	}

}
