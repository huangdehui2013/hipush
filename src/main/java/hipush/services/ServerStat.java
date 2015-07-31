package hipush.services;

import hipush.core.Helpers;

import java.util.Date;

import com.alibaba.fastjson.annotation.JSONField;

public class ServerStat {

	private int id;
	private int users;
	private int pendingMessages;
	private int threadsCount;
	private int totalMemory;
	private int usedMemory;
	@JSONField(format = "yyyy-MM-dd HH:mm:ss")
	private Date reportDate;
	private transient String reportDateStr;

	public ServerStat() {
	}

	public ServerStat(int id, int users, int pendingMessages, int threadsCount) {
		this.id = id;
		this.users = users;
		this.pendingMessages = pendingMessages;
		this.threadsCount = threadsCount;
		this.reportDate = new Date();
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getPendingMessages() {
		return pendingMessages;
	}

	public void setPendingMessages(int pendingMessages) {
		this.pendingMessages = pendingMessages;
	}

	public int getUsers() {
		return users;
	}

	public void setUsers(int users) {
		this.users = users;
	}

	public int getThreadsCount() {
		return threadsCount;
	}

	public void setThreadsCount(int threadsCount) {
		this.threadsCount = threadsCount;
	}

	public Date getReportDate() {
		return reportDate;
	}

	public void setReportDate(Date reportDate) {
		this.reportDate = reportDate;
	}

	public String getReportDateStr() {
		if (reportDateStr == null) {
			reportDateStr = Helpers.formatDate(reportDate);
		}
		return reportDateStr;
	}

	public int getTotalMemory() {
		return totalMemory;
	}

	public void setTotalMemory(int totalMemory) {
		this.totalMemory = totalMemory;
	}

	public int getUsedMemory() {
		return usedMemory;
	}

	public void setUsedMemory(int usedMemory) {
		this.usedMemory = usedMemory;
	}

}
