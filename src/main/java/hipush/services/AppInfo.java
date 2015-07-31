package hipush.services;

import hipush.core.Helpers;

import java.util.Calendar;

import com.alibaba.fastjson.annotation.JSONField;

public class AppInfo {

	private int id;
	private String key;
	private String secret;
	private String pkg;
	private String name;
	private long ts;

	public AppInfo() {
	}

	public AppInfo(int id, String key, String secret, String pkg, String name,
			long ts) {
		this.id = id;
		this.key = key;
		this.secret = secret;
		this.pkg = pkg;
		this.name = name;
		this.ts = ts;
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

	public long getTs() {
		return ts;
	}

	public void setTs(long ts) {
		this.ts = ts;
	}

	public String getPkg() {
		return pkg;
	}

	public void setPkg(String pkg) {
		this.pkg = pkg;
	}

	@JSONField(serialize = false)
	public String getCreateDate() {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(ts);
		return Helpers.formatDate(cal.getTime());
	}

}
