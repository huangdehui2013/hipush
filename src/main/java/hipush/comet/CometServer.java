package hipush.comet;

import hipush.async.AsyncManager;
import hipush.comet.protocol.CommandDecoder;
import hipush.comet.protocol.Internals.ClearOverdueJobsCommand;
import hipush.comet.protocol.Internals.ReportStatCommand;
import hipush.comet.protocol.Internals.SaveIOHistogramCommand;
import hipush.comet.protocol.Internals.SaveJobStatCommand;
import hipush.comet.protocol.Internals.SaveMainHistogramCommand;
import hipush.comet.protocol.Internals.ZkStartCommand;
import hipush.comet.protocol.MessageEncoder;
import hipush.comet.rpc.RpcServer;
import hipush.core.CommandDefine;
import hipush.core.ScheduleManager;
import hipush.db.DaoCenter;
import hipush.services.AppService;
import hipush.services.JobService;
import hipush.services.MeasureService;
import hipush.services.MessageInfo;
import hipush.services.MessageService;
import hipush.services.ReportService;
import hipush.services.RouteService;
import hipush.services.SessionService;
import hipush.services.TopicService;
import hipush.services.UserService;
import hipush.uuid.MessageId;
import hipush.zk.ZkService;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.cli.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CometServer {
	private final static Logger LOG = LoggerFactory
			.getLogger(CometServer.class);

	private final static CometServer instance = new CometServer();
	private CometConfig config = new CometConfig();

	public static CometServer getInstance() {
		return instance;
	}

	public CometConfig getConfig() {
		return config;
	}

	public static void main(String[] args) {
		CometServer.getInstance().start(args);
	}

	public void startFront() {
		ServerBootstrap bootstrap = new ServerBootstrap();
		EventLoopGroup m_bossGroup = new NioEventLoopGroup(2);
		EventLoopGroup m_workerGroup = new NioEventLoopGroup(4);
		final CometHandler handler = new CometHandler();
		final MessageEncoder msgEncoder = new MessageEncoder();
		bootstrap
				.group(m_bossGroup, m_workerGroup)
				.channel(NioServerSocketChannel.class)
				.childHandler(new ChannelInitializer<SocketChannel>() {
					@Override
					public void initChannel(SocketChannel ch) throws Exception {
						ChannelPipeline pipe = ch.pipeline();
						// 控制读速度上线，防止流量攻击
						// 控制写速度上线，避免消息推送风暴
						pipe.addLast(new ChannelTrafficShapingHandler(10240,
								1024, 5000));
						pipe.addLast(new ReadTimeoutHandler(120));
						pipe.addLast(new CommandDecoder());
						pipe.addLast(msgEncoder);
						pipe.addLast(handler);
					}
				})
				.option(ChannelOption.SO_BACKLOG, 10000)
				.option(ChannelOption.SO_REUSEADDR, true)
				.option(ChannelOption.TCP_NODELAY, true)
				.childOption(ChannelOption.SO_KEEPALIVE, true)
				.childOption(ChannelOption.SO_RCVBUF, 1024)
				// 接受的数据比较少
				.childOption(ChannelOption.SO_SNDBUF, 65536)
				// 发送的数据可能较大
				.childOption(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK,
						8 * 1024)
				.childOption(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK,
						32 * 1024);
		try {
			Channel future = bootstrap
					.bind(config.getCometIp(), config.getPort()).sync()
					.channel();
			future.closeFuture().sync();
		} catch (Exception e) {
			LOG.error("listen comet address error", e);
			System.exit(-1);
		}
	}

	public void startRpc() {
		RpcServer.getInstance().start(config.getRpcIp(), config.getPort());
	}

	private void initArgs(String[] args) {
		Map<String, String> env = System.getenv();
		int serverIdStart = Integer.parseInt(env.get("serverIdStart"));
		int portStart = Integer.parseInt(env.get("portStart"));
		CommandLine line = CommandDefine.getInstance()
				.addStringOption("runmode", "specify runmode")
				.addStringOption("seq", "server sequence per service")
				.addStringOption("cometip", "ip front for comet client")
				.addStringOption("rpcip", "rpc ip backend for admin and web")
				.addStringOption("maxconn", "max connections allowed")
				.addBooleanOption("debug", "run in debug mode")
				.getCommandLine(args);
		int seq = Integer.parseInt(line.getOptionValue("seq"));
		config.setRunmode(line.getOptionValue("runmode"));
		config.setServerId(serverIdStart + seq);
		config.setCometIp(line.getOptionValue("cometip"));
		config.setRpcIp(line.getOptionValue("rpcip"));
		config.setPort(portStart + seq);
		config.setMaxConnections(Integer.parseInt(line
				.getOptionValue("maxconn")));
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
		AsyncManager.getInstance().initWith(10);
		ScheduleManager.getInstance().initWith(1);
		AppService.getInstance().setJedisPool(config.getJedisPool("meta"));
		ReportService.getInstance().setJedisPool(config.getJedisPool("meta"));
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
		MessageService.getInstance().refreshCache();
	}

	private void hookShutdown() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				ZkService.getInstance().unregisterComet(
						"" + config.getServerId());
				ZkService.getInstance()
						.unregisterRpc("" + config.getServerId());
				// 最后一次把任务统计数字刷到库里去
				MessageProcessor.getInstance().putMessage(
						new SaveJobStatCommand(true));
				MessageProcessor.getInstance().stop();
				RpcServer.getInstance().stop();
				// 同步保存用户没有ack的消息
				List<ClientInfo> clients = OnlineManager.getInstance()
						.getDirtyClients();
				for (ClientInfo client : clients) {
					List<MessageInfo> messages = client.getMessages();
					for (MessageInfo message : messages) {
						if (MessageId.isPrivate(message.getId())) {
							MessageService.getInstance().savePrivateMessage(
									message);
						}
					}
					MessageService.getInstance().saveUserMessages(
							client.getClientId(), messages);
				}
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e1) {
				}
				LOG.warn("server shutdown finished!");
			}
		});
	}

	public void delayZk() {
		ScheduleUtils.delay(5, new ZkStartCommand());
	}

	public void startLoop() {
		MessageProcessor.getInstance().start();
	}

	public void reportStat() {
		ScheduleUtils.periodic(10, 5, new ReportStatCommand());
	}

	public void saveJobStat() {
		ScheduleUtils.periodic(10, 10, new SaveJobStatCommand(false));
		ScheduleUtils.periodic(30, 30, new SaveJobStatCommand(true));
		ScheduleUtils.periodic(3600, 7200, new ClearOverdueJobsCommand());
	}

	public void savePerfHistogram() {
		ScheduleUtils.periodic(60, 60, new SaveMainHistogramCommand());
		ScheduleUtils.periodic(60, 60, new SaveIOHistogramCommand());
	}

	public void syncZk() {
		ScheduleManager.getInstance().periodic(60, 60, new Runnable() {

			public void run() {
				// 定时刷新，保证服务列表同步zk
				ZkService.getInstance().refreshAllService();
			}

		});
	}

	public void start(String[] args) {
		initArgs(args);
		initConfig();
		initialize();
		LOG.warn(String.format("start rpc server serverId=%s ip=%s port=%s",
				config.getServerId(), config.getRpcIp(), config.getPort()));
		startRpc();
		hookShutdown();
		delayZk();
		reportStat();
		saveJobStat();
		savePerfHistogram();
		startLoop();
		syncZk();
		LOG.warn(String.format("start comet server serverId=%s ip=%s port=%s",
				config.getServerId(), config.getCometIp(), config.getPort()));
		startFront();
	}
}
