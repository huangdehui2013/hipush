package hipush.dog;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.cli.CommandLine;
import org.apache.curator.x.discovery.ServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hipush.core.CommandDefine;
import hipush.core.ScheduleManager;
import hipush.core.TemplateEngine;
import hipush.zk.ZkService;

public class ServiceDog {

	private final static Logger LOG = LoggerFactory.getLogger(ServiceDog.class);
	private final static ServiceDog instance = new ServiceDog();
	private DogConfig config = new DogConfig();

	public static ServiceDog getInstance() {
		return instance;
	}

	public DogConfig getConfig() {
		return config;
	}

	public static void main(String[] args) {
		ServiceDog.getInstance().start(args);
	}

	public void start(String[] args) {
		initArgs(args);
		initConfig();
		initialize();
		startZk();
		checkPeriodic();
		try {
			System.in.read();
		} catch (IOException e) {
			LOG.error("main thread exit", e);
		}
	}

	private void initArgs(String[] args) {
		CommandLine line = CommandDefine.getInstance()
				.addStringOption("runmode", "specify runmode")
				.addStringOption("file", "specify nginx site config file path")
				.addStringOption("ip", "ip for nginx site")
				.addBooleanOption("debug", "run in debug mode").getCommandLine(args);
		config.setRunmode(line.getOptionValue("runmode"));
		config.setDebug(line.hasOption("debug"));
		config.setTemplateRoot("nginx");
		config.setFilePath(line.getOptionValue("file"));
		config.setIp(line.getOptionValue("ip"));
		templateEngine = new TemplateEngine(config.getTemplateRoot());
	}

	private void initConfig() {
		ScriptEngineManager factory = new ScriptEngineManager();
		ScriptEngine script = factory.getEngineByName("JavaScript");
		script.put("config", this.config);
		Reader jsReader = null;
		String jsFile = String.format("/config/%s.js", this.config.getRunmode());
		jsReader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(jsFile)));
		try {
			script.eval(jsReader);
		} catch (ScriptException e) {
			LOG.error("javascript config parse error", e);
			System.exit(-1);
		}
	}
	
	private void initialize() {
		ScheduleManager.getInstance().initWith(1);
	}

	private void startZk() {
		ZkService.getInstance().startClient(config.getCuratorClient());
	}

	private void checkPeriodic() {
		ScheduleManager.getInstance().periodic(0, 30, new Runnable() {

			public void run() {
				LOG.warn("checking zk");
				checkZkChange();
			}

		});
	}

	public void checkZkChange() {
		ZkService.getInstance().refreshAllService();
		List<ServiceInstance<String>> admins = ZkService.getInstance().getAdminList();
		List<ServiceInstance<String>> webs = ZkService.getInstance().getWebList();
		Set<String> currentAdminIds = new HashSet<String>();
		Set<String> currentWebIds = new HashSet<String>();
		for (ServiceInstance<String> admin : admins) {
			currentAdminIds.add(admin.getId());
		}
		for (ServiceInstance<String> web : webs) {
			currentWebIds.add(web.getId());
		}
		if(currentAdminIds.equals(adminIds) && currentWebIds.equals(webIds)) {
			return;
		}
		LOG.warn("zk changed");
		webIds = currentWebIds;
		adminIds = currentAdminIds;
		if(writeUpstreams()) {
			reloadNginx();
		}
	}
	
	private boolean writeUpstreams() {
		List<ServiceInstance<String>> admins = ZkService.getInstance().getAdminList();
		List<ServiceInstance<String>> webs = ZkService.getInstance().getWebList();
		Map<String, Object> context = new HashMap<String, Object>();
		context.put("admins", admins);
		context.put("webs", webs);
		context.put("ip", config.getIp());
		StringBuffer content = templateEngine.renderTemplate("hipush.mus", context);
		try {
			LOG.warn("begin write upstream");
			FileOutputStream stream = new FileOutputStream(config.getFilePath());
			stream.write(content.toString().getBytes());
			stream.close();
			LOG.warn("write upstream success");
			return true;
		} catch (IOException e) {
			LOG.error("write upstream error", e);
			return false;
		}
	}
	
	private void reloadNginx() {
		LOG.warn("begin reload nginx");
		try {
			Runtime.getRuntime().exec("bash -c 'sudo nginx -s reload'");
			LOG.warn("reload nginx success");
		} catch (IOException e) {
			LOG.error("reload nginx error", e);
		}
	}
	
	private Set<String> adminIds = new HashSet<String>();
	private Set<String> webIds = new HashSet<String>();
	private TemplateEngine templateEngine;

}
