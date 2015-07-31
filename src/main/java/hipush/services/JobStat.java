package hipush.services;

import hipush.core.Annotations.Concurrent;

import java.util.concurrent.atomic.AtomicInteger;

public class JobStat {

	private String id;
	private int sentCount;
	private AtomicInteger realSentCount = new AtomicInteger(0);
	private int arrivedCount;
	private int offlineCount;
	private int clickCount;

	public JobStat(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public int getSentCount() {
		return sentCount;
	}

	public void setSentCount(int sentCount) {
		this.sentCount = sentCount;
	}

	public void incrSentCount() {
		incrSentCount(1);
	}

	public void incrSentCount(int value) {
		this.sentCount += value;
	}

	public void decrSendCount(int value) {
		incrSentCount(-value);
	}

	public int getRealSentCount() {
		return realSentCount.get();
	}

	public void setRealSentCount(int realSentCount) {
		this.realSentCount.set(realSentCount);
	}

	public void incrRealSentCount() {
		this.realSentCount.incrementAndGet();
	}

	public void decrRealSentCount(int value) {
		this.realSentCount.addAndGet(-value);
	}

	public int getArrivedCount() {
		return arrivedCount;
	}

	public void setArrivedCount(int arrivedCount) {
		this.arrivedCount = arrivedCount;
	}

	public void decrArrivedCount(int value) {
		this.arrivedCount -= value;
	}

	public void incrArrivedCount() {
		incrArrivedCount(1);
	}

	@Concurrent
	public void incrArrivedCount(int value) {
		this.arrivedCount += value;
	}

	public void incrOfflineCount() {
		incrOfflineCount(1);
	}

	public void incrOfflineCount(int value) {
		this.offlineCount += value;
	}

	public int getOfflineCount() {
		return this.offlineCount;
	}

	public void setOfflineCount(int value) {
		this.offlineCount = value;
	}

	public int getClickCount() {
		return clickCount;
	}

	public void setClickCount(int clickCount) {
		this.clickCount = clickCount;
	}

	public void incrClickCount(int value) {
		this.clickCount += value;
	}

	public void incrClickCount() {
		this.incrClickCount(1);
	}

	public boolean isEmpty() {
		return this.getSentCount() == 0 && this.getOfflineCount() == 0
				&& this.getArrivedCount() == 0;
	}

	public String getArrivedRatio() {
		if (this.getSentCount() == 0) {
			return "--";
		}
		return String.format("%.2f",
				this.getArrivedCount() * 100.0f / this.getSentCount());
	}

}
