package hipush.admin;

import hipush.core.Config;

public class AdminConfig extends Config {

	private String ip;
	private int port;
	private String templateRoot;
	private String staticRoot;

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getTemplateRoot() {
		return templateRoot;
	}

	public void setTemplateRoot(String templateRoot) {
		this.templateRoot = templateRoot;
	}

	public String getStaticRoot() {
		return staticRoot;
	}

	public void setStaticRoot(String staticRoot) {
		this.staticRoot = staticRoot;
	}

}
