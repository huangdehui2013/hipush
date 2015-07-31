package hipush.services;

import java.util.HashMap;

@SuppressWarnings("serial")
public class Session extends HashMap<String, Object> {

	private transient boolean clean = true;
	private transient String sessionId;

	public Session() {
	}

	public Session(String sessionId) {
		this.sessionId = sessionId;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public boolean isClean() {
		return clean;
	}

	public void setClean(boolean clean) {
		this.clean = clean;
	}

	@Override
	public Object put(String key, Object value) {
		this.setClean(false);
		return super.put(key, value);
	}

	@Override
	public Object remove(Object key) {
		this.setClean(false);
		return super.remove(key);
	}

	@Override
	public void clear() {
		this.setClean(false);
		super.clear();
	}

	public void setUsername(String username) {
		if (username == null) {
			this.remove("username");
		} else {
			this.put("username", username);
		}
	}

	public String getUsername() {
		return (String) this.get("username");
	}

}
