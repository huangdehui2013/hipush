package hipush.core;

import java.util.HashMap;
import java.util.Map;

public class ClientEnvironStat {
	public final static int NETWORK_TYPE_WIFI = 0;
	public final static int NETWORK_TYPE_2G = 1;
	public final static int NETWORK_TYPE_3G = 2;
	public final static int NETWORK_TYPE_4G = 3;
	public final static int ISP_EMPTY = 0;
	public final static int ISP_MOBILE = 1;
	public final static int ISP_UNICOM = 2;
	public final static int ISP_TELECOM = 3;

	private int[] networkIncrs = new int[4];
	private int[] ispIncrs = new int[4];
	private Map<String, Integer> phoneIncrs = new HashMap<String, Integer>();
	private int totalPhoneIncrs;

	public void incrNetwork(int networkType) {
		this.networkIncrs[networkType]++;
	}

	public void setNetwork(int networkType, int value) {
		this.networkIncrs[networkType] = value;
	}

	public void incrIsp(int isp) {
		this.ispIncrs[isp]++;
	}

	public void setIsp(int isp, int value) {
		this.ispIncrs[isp] = value;
	}

	public void incrPhone(String phoneType) {
		Integer incr = phoneIncrs.get(phoneType);
		if (incr == null) {
			incr = Integer.valueOf(0);
		}
		incr++;
		phoneIncrs.put(phoneType, incr);
		totalPhoneIncrs++;
	}

	public void setPhone(String phoneType, int value) {
		phoneIncrs.put(phoneType, Integer.valueOf(value));
		totalPhoneIncrs += value;
	}

	public int[] getNetworkIncrs() {
		return networkIncrs;
	}

	public int[] getIspIncrs() {
		return ispIncrs;
	}

	public Map<String, Integer> getPhoneIncrs() {
		return phoneIncrs;
	}

	public float getNetworkRatio(int networkType) {
		int sum = networkIncrs[0] + networkIncrs[1] + networkIncrs[2] + networkIncrs[3];
		if (sum == 0) {
			return 0.0f;
		}
		return networkIncrs[networkType] * 100.0f / sum;
	}

	public float getIspRatio(int isp) {
		int sum = ispIncrs[0] + ispIncrs[1] + ispIncrs[2] + ispIncrs[3];
		if (sum == 0) {
			return 0.0f;
		}
		return ispIncrs[isp] * 100.0f / sum;
	}

	public float getPhoneRatio(String phone) {
		if (totalPhoneIncrs == 0) {
			return 0.0f;
		}
		return this.phoneIncrs.get(phone) * 100.0f / totalPhoneIncrs;
	}

}
