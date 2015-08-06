package hipush.services;

import javax.crypto.SecretKey;

public class MessageInfo {

	private int type;
	private String id;
	private String jobId;
	private long ts;
	private String content;

	public MessageInfo() {
	}

	public MessageInfo(int type, String jobId, String id, String content, long ts) {
		this.jobId = jobId;
		this.type = type;
		this.id = id;
		this.content = content;
		this.ts = ts;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public long getTs() {
		return ts;
	}

	public void setTs(long ts) {
		this.ts = ts;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public String getJobId() {
		return jobId;
	}

	public void setJobId(String jobId) {
		this.jobId = jobId;
	}

	public byte[] encryptContent(SecretKey encryptKey) {
		return EncryptService.getInstance().encryptByDes(encryptKey, content);
	}

}
