package hipush.core;

public class ClientEnviron {
	private int networkType;
	private int isp;
	private String phoneType;
	private String[] extras;

	public ClientEnviron(int networkType, int isp, String phoneType, String[] extras) {
		this.networkType = networkType;
		this.isp = isp;
		this.phoneType = phoneType;
		this.extras = extras;
	}

	public int getNetworkType() {
		return networkType;
	}

	public void setNetworkType(int networkType) {
		this.networkType = networkType;
	}

	public int getIsp() {
		return isp;
	}

	public void setIsp(int isp) {
		this.isp = isp;
	}

	public String getPhoneType() {
		return phoneType;
	}

	public void setPhoneType(String phoneType) {
		this.phoneType = phoneType;
	}

	public String[] getExtras() {
		return extras;
	}

	public void setExtras(String[] extras) {
		this.extras = extras;
	}

}
