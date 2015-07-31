package hipush.db;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "jobs")
public class JobModel {

	@DatabaseField(id = true)
	private String id;
	@DatabaseField
	private String name;
	@DatabaseField
	private String type;
	@DatabaseField
	private long createTs;
	@DatabaseField
	private int sentCount;
	@DatabaseField
	private int realSentCount;
	@DatabaseField
	private int arrivedCount;
	@DatabaseField
	private int offlineCount;
	@DatabaseField
	private int clickCount;

	public JobModel() {
	}

	public JobModel(String id, String name, String type, long createTs) {
		this.id = id;
		this.name = name;
		this.type = type;
		this.createTs = createTs;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int getSentCount() {
		return sentCount;
	}

	public void setSentCount(int sentCount) {
		this.sentCount = sentCount;
	}

	public int getRealSentCount() {
		return realSentCount;
	}

	public void setRealSentCount(int realSentCount) {
		this.realSentCount = realSentCount;
	}

	public int getArrivedCount() {
		return arrivedCount;
	}

	public void setArrivedCount(int arrivedCount) {
		this.arrivedCount = arrivedCount;
	}

	public int getOfflineCount() {
		return offlineCount;
	}

	public void setOfflineCount(int offlineCount) {
		this.offlineCount = offlineCount;
	}

	public int getClickCount() {
		return clickCount;
	}

	public void setClickCount(int clickCount) {
		this.clickCount = clickCount;
	}

	public long getCreateTs() {
		return createTs;
	}

	public void setCreateTs(long createTs) {
		this.createTs = createTs;
	}

}
