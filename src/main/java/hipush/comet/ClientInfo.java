package hipush.comet;

import hipush.core.Helpers;
import hipush.services.MessageInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClientInfo {

	private String clientId;
	private int appId;
	private List<String> topics;
	private List<MessageInfo> messages;
	private boolean ready;  // ［拉完离线消息后］准备好接受推送消息
	private long lastResendTs; // 上一次重发没有ack的消息的时间戳

	private List<MessageInfo> DUMMY = Collections.emptyList();

	public ClientInfo(String clientId, int appId, List<String> topics,
			List<MessageInfo> messages) {
		this.clientId = clientId;
		this.appId = appId;
		this.topics = topics;
		if (messages.isEmpty()) {
			this.messages = DUMMY;
		} else {
			this.messages = messages;
		}
		this.lastResendTs = System.currentTimeMillis();
	}
	
	public boolean ready() {
		return ready;
	}
	
	public void setReady() {
		this.ready = true;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public List<String> getTopics() {
		return topics;
	}

	public List<String> getClientTopics() {
		List<String> ctopics = new ArrayList<String>(4);
		for (String topic : topics) {
			if (Helpers.isClientTopic(topic)) {
				ctopics.add(topic);
			}
		}
		return ctopics;
	}

	public void setTopics(List<String> topics) {
		this.topics = topics;
	}

	public boolean isEmpty() {
		return messages == DUMMY;
	}

	public List<MessageInfo> getMessages() {
		return messages;
	}

	public void setMessages(List<MessageInfo> messages) {
		this.messages = messages;
	}

	public void addMessage(MessageInfo message) {
		if (this.isEmpty()) {
			messages = new ArrayList<MessageInfo>(1);
		}
		messages.add(message);
	}

	public MessageInfo removeMessage(String messageId) {
		if (this.isEmpty()) {
			return null;
		}
		for (int i = 0; i < messages.size(); i++) {
			MessageInfo message = messages.get(i);
			if (message.getId().equals(messageId)) {
				messages.remove(i);
				if (messages.isEmpty()) {
					messages = DUMMY;
				}
				return message;
			}
		}
		return null;
	}

	public int getAppId() {
		return appId;
	}

	public void setAppId(int appId) {
		this.appId = appId;
	}

	public long getLastResendTs() {
		return lastResendTs;
	}

	public void setLastResendTs(long lastResendTs) {
		this.lastResendTs = lastResendTs;
	}

}
