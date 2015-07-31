package hipush.admin;

import hipush.core.CommandDefine;
import hipush.core.ScheduleManager;
import hipush.db.DaoCenter;
import hipush.http.HttpServer;
import hipush.services.AppService;
import hipush.services.JobService;
import hipush.services.ManagerService;
import hipush.services.MeasureService;
import hipush.services.MessageService;
import hipush.services.ReportService;
import hipush.services.RouteService;
import hipush.services.SessionService;
import hipush.services.TopicService;
import hipush.services.UserService;
import hipush.zk.ZkService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.SQLException;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.cli.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mashape.unirest.http.Unirest;

public class AdminServer {

	private final static Logger LOG = LoggerFactory
			.getLogger(AdminServer.class);

	public static void main(String[] args) {
		AdminServer.getInstance().start(args);
	}

	private final static AdminServer instance = new AdminServer();

	public static AdminServer getInstance() {
		return instance;
	}

	private AdminConfig config = new AdminConfig();
	private final HttpServer http = new HttpServer(1, 10)
			.build(new AdminHandler());

	public AdminConfig getConfig() {
		return config;
	}

	public void startAdmin() {
		http.start(config.getIp(), config.getPort());
	}

	private void initArgs(String[] args) {
		Map<String, String> env = System.getenv();
		int serverIdStart = Integer.parseInt(env.get("serverIdStart"));
		int portStart = Integer.parseInt(env.get("portStart"));
		CommandLine line = CommandDefine.getInstance()
				.addStringOption("runmode", "specify runmode")
				.addStringOption("seq", "server sequencen number")
				.addStringOption("ip", "ip for Admin client")
				.addBooleanOption("debug", "run in debug mode")
				.getCommandLine(args);
		int seq = Integer.parseInt(line.getOptionValue("seq"));
		config.setRunmode(line.getOptionValue("runmode"));
		config.setServerId(serverIdStart + seq);
		config.setIp(line.getOptionValue("ip"));
		config.setPort(portStart + seq);
		config.setDebug(line.hasOption("debug"));
		config.setTemplateRoot("templates");
		config.setStaticRoot("static");
	}

	private void initConfig() {
		ScriptEngineManager factory = new ScriptEngineManager();
		ScriptEngine script = factory.getEngineByName("JavaScript");
		script.put("config", this.config);
		Reader jsReader = null;
		String jsFile = String
				.format("/config/%s.js", this.config.getRunmode());
		jsReader = new BufferedReader(new InputStreamReader(getClass()
				.getResourceAsStream(jsFile)));
		try {
			script.eval(jsReader);
		} catch (ScriptException e) {
			LOG.error("javascript config parse error", e);
			System.exit(-1);
		}
	}

	private void initialize() {
		ScheduleManager.getInstance().initWith(2);
		AppService.getInstance().setJedisPool(config.getJedisPool("meta"));
		ReportService.getInstance().setJedisPool(config.getJedisPool("meta"));
		ManagerService.getInstance().setJedisPool(config.getJedisPool("meta"));
		SessionService.getInstance().setJedisPool(config.getJedisPool("meta"));
		JobService.getInstance().setJedisPool(config.getJedisPool("message"));
		MessageService.getInstance().setJedisPool(
				config.getJedisPool("message"));
		RouteService.getInstance().setJedisPool(config.getJedisPool("route"));
		TopicService.getInstance().setJedisPool(config.getJedisPool("topic"));
		UserService.getInstance().setJedisPool(config.getJedisPool("user"));
		MeasureService.getInstance().setJedisPool(config.getJedisPool("meta"));
		try {
			DaoCenter.getInstance().initAppDao(config.getMysqlSource("meta"));
			DaoCenter.getInstance().initManagerDao(
					config.getMysqlSource("meta"));
			DaoCenter.getInstance()
					.initJobTypeDao(config.getMysqlSource("job"));
			DaoCenter.getInstance().initJobDao(config.getMysqlSource("job"));
			DaoCenter.getInstance().initUserDao(config.getMysqlSource("user"));
		} catch (SQLException e) {
			LOG.error("init dao center error", e);
		}
		AppService.getInstance().loadApps();
		AppService.getInstance().initApp();
		MessageService.getInstance().refreshCache();
		ManagerService.getInstance().initManager();
		JobService.getInstance().initJobTypes();
		ScheduleManager.getInstance().periodic(60, 3600, new Runnable() {

			public void run() {
				TopicService.getInstance().beginCollectTopicStat();
			}

		});
		ScheduleManager.getInstance().periodic(60, 60, new Runnable() {

			public void run() {
				// 定时刷新，保证服务列表同步zk
				ZkService.getInstance().refreshAllService();
			}

		});
		Unirest.setConcurrency(2000, 2000);
		Unirest.setTimeouts(5000, 5000);
	}

	public void startZk() {
		ZkService
				.getInstance()
				.startClient(config.getCuratorClient())
				.registerAdmin("" + config.getServerId(), config.getIp(),
						config.getPort());
	}

	private void hookShutdown() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				LOG.warn("server is shutting down!");
				try {
					Unirest.shutdown();
				} catch (IOException e) {
					LOG.error("shutdown unirest failed", e);
				}
				ZkService.getInstance().unregisterAdmin(
						"" + config.getServerId());
				ZkService.getInstance().shutdown();
				http.shutdown();
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
	}

	public void start(String[] args) {
		initArgs(args);
		initConfig();
		initialize();
		startZk();
		hookShutdown();
		LOG.warn(String.format("admin server start serverId=%s ip=%s port=%s",
				config.getServerId(), config.getIp(), config.getPort()));
		startAdmin();
	}
}
