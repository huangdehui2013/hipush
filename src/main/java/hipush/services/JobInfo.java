package hipush.services;

import hipush.core.Helpers;

import java.util.Calendar;

import com.alibaba.fastjson.annotation.JSONField;

public class JobInfo {

	private String id;
	private String name;
	private String type;
	private transient String typeName;
	private long ts;
	private transient JobStat stat;

	public JobInfo() {
	}

	public JobInfo(String id, String name, String type, long ts) {
		this.id = id;
		this.name = name;
		this.type = type;
		this.ts = ts;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setJobStat(JobStat stat) {
		this.stat = stat;
	}

	@JSONField(serialize = false)
	public int getSentCount() {
		if (stat == null) {
			return 0;
		}
		return stat.getSentCount();
	}

	@JSONField(serialize = false)
	public int getArrivedCount() {
		if (stat == null) {
			return 0;
		}
		return stat.getArrivedCount();
	}

	@JSONField(serialize = false)
	public int getOfflineCount() {
		if (stat == null) {
			return 0;
		}
		return stat.getOfflineCount();
	}

	@JSONField(serialize = false)
	public int getClickCount() {
		if (stat == null) {
			return 0;
		}
		return stat.getClickCount();
	}

	@JSONField(serialize = false)
	public String getArrivedRatio() {
		if (stat == null) {
			return "--";
		}
		return stat.getArrivedRatio();
	}

	@JSONField(serialize = false)
	public int getRealSentCount() {
		if (stat == null) {
			return 0;
		}
		return stat.getRealSentCount();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getTypeName() {
		return typeName;
	}

	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public long getTs() {
		return ts;
	}

	public void setTs(long ts) {
		this.ts = ts;
	}

	@JSONField(serialize = false)
	public String getCreateDate() {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(ts);
		return Helpers.formatDate(cal.getTime());
	}

}
