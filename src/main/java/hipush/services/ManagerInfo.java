package hipush.services;

import hipush.core.Helpers;

import java.util.Date;

import com.alibaba.fastjson.annotation.JSONField;

public class ManagerInfo {
	
	public final static int PUSH = 0;
	public final static int CHAT = 1;
	public final static int TEST = 127;

	private String username;
	private String passwordHash;
	private String displayName;
	@JSONField(format = "yyyy-MM-dd HH:mm:ss")
	private Date lastLoginDate;

	public ManagerInfo() {
	}

	public ManagerInfo(String username, String passwordHash, String displayName) {
		this.username = username;
		this.passwordHash = passwordHash;
		this.displayName = displayName;
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

	public Date getLastLoginDate() {
		return lastLoginDate;
	}

	public void setLastLoginDate(Date lastLoginDate) {
		this.lastLoginDate = lastLoginDate;
	}
	
	public String getLastLoginDateStr() {
		if(this.lastLoginDate == null) {
			return "--";
		}
		return Helpers.formatDate(this.lastLoginDate);
	}

}
