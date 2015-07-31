package hipush.web;

import hipush.core.CommandDefine;
import hipush.core.ScheduleManager;
import hipush.db.DaoCenter;
import hipush.http.HttpServer;
import hipush.services.AppService;
import hipush.services.MessageService;
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

public class WebServer {

	private final static Logger LOG = LoggerFactory.getLogger(WebServer.class);

	public static void main(String[] args) {
		WebServer.getInstance().start(args);
	}

	private final static WebServer instance = new WebServer();

	public static WebServer getInstance() {
		return instance;
	}

	private WebConfig config = new WebConfig();
	private final HttpServer http = new HttpServer(1, 10).build(new WebHandler());

	public WebConfig getConfig() {
		return config;
	}

	public void startWeb() {
		http.start(config.getIp(), config.getPort());
	}

	private void initArgs(String[] args) {
		Map<String, String> env = System.getenv();
		int serverIdStart = Integer.parseInt(env.get("serverIdStart"));
		int portStart = Integer.parseInt(env.get("portStart"));
		CommandLine line = CommandDefine.getInstance()
				.addStringOption("runmode", "specify runmode")
				.addStringOption("seq", "server sequence number")
				.addStringOption("ip", "ip for web client")
				.addBooleanOption("debug", "run in debug mode")
				.getCommandLine(args);
		int seq = Integer.parseInt(line.getOptionValue("seq"));
		config.setRunmode(line.getOptionValue("runmode"));
		config.setServerId(serverIdStart + seq);
		config.setIp(line.getOptionValue("ip"));
		config.setPort(portStart + seq);
		config.setDebug(line.hasOption("debug"));
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
		ScheduleManager.getInstance().initWith(5);
		AppService.getInstance().setJedisPool(config.getJedisPool("meta"));
		SessionService.getInstance().setJedisPool(config.getJedisPool("meta"));
		MessageService.getInstance().setJedisPool(
				config.getJedisPool("message"));
		RouteService.getInstance().setJedisPool(config.getJedisPool("route"));
		TopicService.getInstance().setJedisPool(config.getJedisPool("topic"));
		UserService.getInstance().setJedisPool(config.getJedisPool("user"));
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
		ScheduleManager.getInstance().periodic(1, 30, new Runnable() {

			public void run() {
				AppService.getInstance().loadApps();
			}

		});
		ScheduleManager.getInstance().delay(new Runnable() {

			@Override
			public void run() {
				if (UserService.getInstance().saveClientToDB(100)) {
					ScheduleManager.getInstance().delay(this);
				} else {
					ScheduleManager.getInstance().delay(5, this);
				}
			}

		});
		Unirest.setConcurrency(2000, 2000);
		Unirest.setTimeouts(5000, 5000);
	}

	public void startZk() {
		ZkService
				.getInstance()
				.startClient(config.getCuratorClient())
				.registerWeb("" + config.getServerId(), config.getIp(),
						config.getPort());
	}

	private void hookShutdown() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				try {
					Unirest.shutdown();
				} catch (IOException e) {
					LOG.error("shutdown unirest failed", e);
				}
				ZkService.getInstance()
						.unregisterWeb("" + config.getServerId());
				ZkService.getInstance().shutdown();
				http.shutdown();
			}
		});
	}

	public void start(String[] args) {
		initArgs(args);
		initConfig();
		initialize();
		hookShutdown();
		startZk();
		LOG.warn(String.format("web server start serverId=%s ip=%s port=%s",
				config.getServerId(), config.getIp(), config.getPort()));
		startWeb();
	}
}
