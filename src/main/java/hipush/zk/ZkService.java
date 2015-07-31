package hipush.zk;

import hipush.core.ConsistentHash;
import hipush.core.LocalObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.x.discovery.ServiceCache;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.ServiceType;
import org.apache.curator.x.discovery.details.ServiceCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZkService {
	private final static Logger LOG = LoggerFactory.getLogger(ZkService.class);
	private final static ZkService instance = new ZkService();
	private final static String ROOT = "/hipush";
	private CuratorFramework client;
	private ServiceDiscovery<String> discovery;
	private Map<String, ServiceCache<String>> caches = new ConcurrentHashMap<String, ServiceCache<String>>();
	private Map<String, Map<String, ServiceInstance<String>>> servicesMap = new ConcurrentHashMap<String, Map<String, ServiceInstance<String>>>();
	private Map<String, List<ServiceInstance<String>>> servicesList = new ConcurrentHashMap<String, List<ServiceInstance<String>>>();
	private Map<String, ConsistentHash<ServiceInstance<String>>> ringsMap = new ConcurrentHashMap<String, ConsistentHash<ServiceInstance<String>>>();
	private final static Comparator<ServiceInstance<String>> comparator = new Comparator<ServiceInstance<String>>() {

		@Override
		public int compare(ServiceInstance<String> o1,
				ServiceInstance<String> o2) {
			return Integer.parseInt(o1.getId()) - Integer.parseInt(o2.getId());
		}

	};

	public final static ZkService getInstance() {
		return instance;
	}

	public ZkService startClient(CuratorFramework client) {
		client.start();
		this.client = client;
		discovery = ServiceDiscoveryBuilder.builder(String.class)
				.client(client).basePath(ROOT).build();
		try {
			discovery.start();
		} catch (Exception e) {
			LOG.error("curator discovery start failed", e);
			System.exit(-1);
		}
		startAllCache();
		return this;
	}

	public ZkService startCache(final String serviceName) {
		final ServiceCache<String> cache = discovery.serviceCacheBuilder()
				.name(serviceName).build();
		caches.put(serviceName, cache);
		try {
			cache.start();
			cache.addListener(new ServiceCacheListener() {

				@Override
				public void stateChanged(CuratorFramework client,
						ConnectionState newState) {
					LOG.info(String.format("zookeeper state changed state=%s",
							newState.name()));
				}

				@Override
				public void cacheChanged() {
					refreshServiceFromCache(serviceName);
				}

			});
		} catch (Exception e) {
			LOG.error("curator cache start failed", e);
			System.exit(-1);
		}
		return this;
	}

	public void startAllCache() {
		refreshService("comet");
		refreshService("rpc");
		refreshService("web");
		refreshService("admin");
		startCache("comet");
		startCache("rpc");
		startCache("web");
		startCache("admin");
	}

	public synchronized void refreshServiceFromCache(String serviceName) {
		Map<String, ServiceInstance<String>> instances = new HashMap<String, ServiceInstance<String>>();
		List<ServiceInstance<String>> items = new ArrayList<ServiceInstance<String>>();
		ConsistentHash<ServiceInstance<String>> ring = new ConsistentHash<ServiceInstance<String>>();
		for (ServiceInstance<String> instance : caches.get(serviceName)
				.getInstances()) {
			instances.put(instance.getId(), instance);
			items.add(instance);
			ring.addNode(serviceName + instance.getId(), instance);
		}
		Collections.sort(items, comparator);
		servicesMap.put(serviceName, instances);
		servicesList.put(serviceName, items);
		ringsMap.put(serviceName, ring);
	}

	public synchronized void refreshService(String serviceName) {
		Map<String, ServiceInstance<String>> instances = new HashMap<String, ServiceInstance<String>>();
		List<ServiceInstance<String>> items = new ArrayList<ServiceInstance<String>>();
		ConsistentHash<ServiceInstance<String>> ring = new ConsistentHash<ServiceInstance<String>>();
		try {
			for (ServiceInstance<String> instance : discovery
					.queryForInstances(serviceName)) {
				instances.put(instance.getId(), instance);
				items.add(instance);
				ring.addNode(serviceName + instance.getId(), instance);
			}
		} catch (Exception e) {
			LOG.error("discovery query for instances error", e);
		}
		Collections.sort(items, comparator);
		servicesMap.put(serviceName, instances);
		servicesList.put(serviceName, items);
		ringsMap.put(serviceName, ring);
	}

	public ZkService registerService(String name, String sid, String ip,
			int port) {
		ServiceInstance<String> service;
		service = new ServiceInstance<String>(name, sid, ip, port, null, "",
				System.currentTimeMillis(), ServiceType.DYNAMIC, null);
		try {
			discovery.registerService(service);
		} catch (Exception e) {
			LOG.error("curator register service failed", e);
			System.exit(-1);
		}
		return this;
	}

	public void unregisterService(String name, String sid) {
		ServiceInstance<String> instance = getService(name, sid);
		if (instance != null) {
			try {
				LOG.warn(String.format("unregister service %s=%s", name, sid));
				discovery.unregisterService(instance);
			} catch (Exception e) {
				LOG.error(
						String.format("unregister service %s id=%s", name, sid),
						e);
			}
		}
	}

	public ServiceInstance<String> getService(String name, String sid) {
		Map<String, ServiceInstance<String>> instances = servicesMap.get(name);
		if (instances == null) {
			LOG.error(String.format("service %s not exists", name));
			return null;
		}
		ServiceInstance<String> instance = instances.get(sid);
		if (instance == null) {
			LOG.error(String.format("service %s=%s not exists", name, sid));
			return null;
		}
		return instance;
	}

	public ServiceInstance<String> getRandomService(String name) {
		List<ServiceInstance<String>> instances = servicesList.get(name);
		if (instances == null || instances.isEmpty()) {
			LOG.error(String.format("service %s not exists", name));
			return null;
		}
		int index = LocalObject.random.get().nextInt(instances.size());
		return instances.get(index);
	}

	public ZkService registerComet(String cometId, String ip, int port) {
		return registerService("comet", cometId, ip, port);
	}

	public void unregisterComet(String cometId) {
		unregisterService("comet", cometId);
	}

	public void registerRpc(String rpcId, String ip, int port) {
		registerService("rpc", rpcId, ip, port);
	}

	public void unregisterRpc(String rpcId) {
		unregisterService("rpc", rpcId);
	}

	public void registerWeb(String webId, String ip, int port) {
		registerService("web", webId, ip, port);
	}

	public void unregisterWeb(String webId) {
		unregisterService("web", webId);
	}

	public void registerAdmin(String adminId, String ip, int port) {
		registerService("admin", adminId, ip, port);
	}

	public void unregisterAdmin(String adminId) {
		unregisterService("admin", adminId);
	}

	public ServiceInstance<String> getComet(String cometId) {
		return getService("comet", cometId);
	}

	public ServiceInstance<String> getRpc(String rpcId) {
		return getService("rpc", rpcId);
	}

	public ServiceInstance<String> getRandomRpc() {
		return getRandomService("rpc");
	}

	public List<ServiceInstance<String>> getServiceList(String name) {
		List<ServiceInstance<String>> services = servicesList.get(name);
		if (services == null) {
			services = Collections.emptyList();
		}
		return services;
	}

	public List<ServiceInstance<String>> getRpcList() {
		return getServiceList("rpc");
	}

	public int getRpcSize() {
		List<ServiceInstance<String>> services = getRpcList();
		if (services == null || services.isEmpty()) {
			return 0;
		}
		return services.size();
	}

	public List<ServiceInstance<String>> getCometList() {
		return getServiceList("comet");
	}

	public ServiceInstance<String> getCometByHash(String key) {
		return this.getServiceByHash("comet", key);
	}

	public ServiceInstance<String> getServiceByHash(String serviceName,
			String key) {
		return this.getServiceRing(serviceName).getNode(key);
	}

	public ConsistentHash<ServiceInstance<String>> getServiceRing(
			String serviceName) {
		return this.ringsMap.get(serviceName);
	}

	public int getCometSize() {
		List<ServiceInstance<String>> services = getCometList();
		if (services == null || services.isEmpty()) {
			return 0;
		}
		return services.size();
	}

	public List<ServiceInstance<String>> getWebList() {
		return getServiceList("web");
	}

	public int getWebSize() {
		List<ServiceInstance<String>> services = getWebList();
		if (services == null || services.isEmpty()) {
			return 0;
		}
		return services.size();
	}

	public List<ServiceInstance<String>> getAdminList() {
		return getServiceList("admin");
	}

	public ServiceInstance<String> getAdmin(String adminId) {
		return this.getService("admin", adminId);
	}

	public int getAdminSize() {
		List<ServiceInstance<String>> services = getAdminList();
		if (services == null || services.isEmpty()) {
			return 0;
		}
		return services.size();
	}

	public void shutdown() {
		try {
			for (ServiceCache<String> cache : caches.values()) {
				cache.close();
			}
			if (discovery != null) {
				discovery.close();
			}
			if (client != null) {
				client.close();
			}
		} catch (IOException e) {
			LOG.error("shutdown zkservice error", e);
		}
	}
}
