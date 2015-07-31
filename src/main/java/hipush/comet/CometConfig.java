package hipush.comet;

import hipush.core.Config;

public class CometConfig extends Config {

	private String cometIp;
	private String rpcIp;
	private int port;
	private int maxConnections;

	public String getCometIp() {
		return cometIp;
	}

	public void setCometIp(String cometIp) {
		this.cometIp = cometIp;
	}

	public String getRpcIp() {
		return rpcIp;
	}

	public void setRpcIp(String rpcIp) {
		this.rpcIp = rpcIp;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getMaxConnections() {
		return maxConnections;
	}

	public void setMaxConnections(int maxConnections) {
		this.maxConnections = maxConnections;
	}

}
