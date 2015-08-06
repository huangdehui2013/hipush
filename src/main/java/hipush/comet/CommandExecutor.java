package hipush.comet;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hipush.async.AsyncManager;
import hipush.async.IAsyncTask;
import hipush.comet.protocol.Inputs.AuthCommand;
import hipush.comet.protocol.Inputs.ExchangeKeyCommand;
import hipush.comet.protocol.Inputs.MessageAckCommand;
import hipush.comet.protocol.Inputs.MessageListCommand;
import hipush.comet.protocol.Inputs.ReportEnvironCommand;
import hipush.comet.protocol.Inputs.SubscribeCommand;
import hipush.comet.protocol.Inputs.TopicListCommand;
import hipush.comet.protocol.Inputs.UnsubscribeCommand;
import hipush.comet.protocol.InternalCommand;
import hipush.comet.protocol.Internals.AsyncFinishedCommand;
import hipush.comet.protocol.Internals.ClearOverdueJobsCommand;
import hipush.comet.protocol.Internals.ClientOfflineCommand;
import hipush.comet.protocol.Internals.PublishIteratorCommand;
import hipush.comet.protocol.Internals.PublishMultiCommand;
import hipush.comet.protocol.Internals.PublishPrivateCommand;
import hipush.comet.protocol.Internals.ReportStatCommand;
import hipush.comet.protocol.Internals.ResendPendingsCommand;
import hipush.comet.protocol.Internals.ResendUnackedMessagesCommand;
import hipush.comet.protocol.Internals.SaveClientEnvironCommand;
import hipush.comet.protocol.Internals.SaveIOHistogramCommand;
import hipush.comet.protocol.Internals.SaveJobStatCommand;
import hipush.comet.protocol.Internals.SaveMainHistogramCommand;
import hipush.comet.protocol.Internals.ServerSubscribeCommand;
import hipush.comet.protocol.Internals.ServerUnSubscribeCommand;
import hipush.comet.protocol.Internals.ZkStartCommand;
import hipush.comet.protocol.MessageDefine;
import hipush.comet.protocol.Outputs.AuthSuccessResponse;
import hipush.comet.protocol.Outputs.ErrorResponse;
import hipush.comet.protocol.Outputs.MessageListResponse;
import hipush.comet.protocol.Outputs.MessageResponse;
import hipush.comet.protocol.Outputs.OkResponse;
import hipush.comet.protocol.Outputs.TopicListResponse;
import hipush.comet.protocol.ReadCommand;
import hipush.comet.protocol.WriteResponse;
import hipush.core.ClientEnvironStat;
import hipush.core.Constants;
import hipush.core.ContextUtils;
import hipush.core.ICallback;
import hipush.core.Pair;
import hipush.services.AppInfo;
import hipush.services.AppService;
import hipush.services.EncryptService;
import hipush.services.JobService;
import hipush.services.JobStat;
import hipush.services.MeasureService;
import hipush.services.MessageInfo;
import hipush.services.MessageService;
import hipush.services.ReportService;
import hipush.services.RouteService;
import hipush.services.ServerStat;
import hipush.services.TopicService;
import hipush.services.UserInfo;
import hipush.services.UserService;
import hipush.uuid.MessageId;
import hipush.zk.ZkService;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;

public class CommandExecutor {
	private final static Logger LOG = LoggerFactory.getLogger(CommandExecutor.class);

	private Map<String, JobStat> jobStats = new ConcurrentHashMap<String, JobStat>();
	private ClientEnvironStat environStat = new ClientEnvironStat();

	public void executeInternal(InternalCommand command) {
		switch (command.getType()) {
		case MessageDefine.Internal.ASYNC_FINISHED:
			onAsyncFinished((AsyncFinishedCommand) command);
			break;
		case MessageDefine.Internal.START_ZOOKEEPER:
			startZk((ZkStartCommand) command);
			break;
		case MessageDefine.Internal.PUBLISH_PRIVATE:
			publishPrivate((PublishPrivateCommand) command);
			break;
		case MessageDefine.Internal.PUBLISH_MULTI:
			publishMulti((PublishMultiCommand) command);
			break;
		case MessageDefine.Internal.PUBLISH_ITERATOR:
			publishIterator((PublishIteratorCommand) command);
			break;
		case MessageDefine.Internal.CLIENT_OFFLINE:
			offline((ClientOfflineCommand) command);
			break;
		case MessageDefine.Internal.REPORT_STAT:
			reportStat((ReportStatCommand) command);
			break;
		case MessageDefine.Internal.SAVE_JOB_STAT:
			saveJobStat((SaveJobStatCommand) command);
			break;
		case MessageDefine.Internal.CLEAR_OVERDUE_JOBS:
			clearOverdueJobs((ClearOverdueJobsCommand) command);
			break;
		case MessageDefine.Internal.SERVER_SUBSCRIBE:
			subscribeByServer((ServerSubscribeCommand) command);
			break;
		case MessageDefine.Internal.SERVER_UNSUBSCRIBE:
			unsubscribeByServer((ServerUnSubscribeCommand) command);
			break;
		case MessageDefine.Internal.RESEND_PENDINGS:
			resendPendings((ResendPendingsCommand) command);
			break;
		case MessageDefine.Internal.RESEND_MESSAGE_UNACKED:
			resendMessagesUnAcked((ResendUnackedMessagesCommand) command);
			break;
		case MessageDefine.Internal.SAVE_MAIN_HISTOGRAM:
			saveMainHistogram((SaveMainHistogramCommand) command);
			break;
		case MessageDefine.Internal.SAVE_IO_HISTOGRAM:
			saveIOHistogram((SaveIOHistogramCommand) command);
			break;
		case MessageDefine.Internal.SAVE_CLIENT_ENVIRON:
			saveClientEnvironIncrs((SaveClientEnvironCommand) command);
			break;
		}
	}

	public void execute(ReadCommand command) {
		if (command.isInternal()) {
			executeInternal((InternalCommand) command);
			return;
		}
		switch (command.getType()) {
		case MessageDefine.Read.CMD_AUTH:
			auth((AuthCommand) command);
			break;
		case MessageDefine.Read.CMD_TOPIC_LIST:
			getTopicList((TopicListCommand) command);
			break;
		case MessageDefine.Read.CMD_SUBSCRIBE:
			subscribe((SubscribeCommand) command);
			break;
		case MessageDefine.Read.CMD_UNSUBSCRIBE:
			unsubscribe((UnsubscribeCommand) command);
			break;
		case MessageDefine.Read.CMD_MESSAGE_LIST:
			getMessageList((MessageListCommand) command);
			break;
		case MessageDefine.Read.CMD_MESSAGE_ACK:
			ackMessages((MessageAckCommand) command);
			break;
		case MessageDefine.Read.CMD_REPORT_ENVIRON:
			reportEnviron((ReportEnvironCommand) command);
			break;
		case MessageDefine.Read.CMD_EXCHANGE_KEY:
			exchangeKey((ExchangeKeyCommand) command);
			break;
		default:
			LOG.error("no method defined for command type=%s", command.getType());
		}
	}

	public void onAsyncFinished(AsyncFinishedCommand command) {
		Exception error = command.getError();
		if (error == null) {
			command.getRunner().afterOk();
		} else {
			command.getRunner().afterError(error);
		}
	}

	public void reportStat(ReportStatCommand command) {
		int serverId = CometServer.getInstance().getConfig().getServerId();
		ServerStat stat = new ServerStat(serverId, OnlineManager.getInstance().getCount(),
				MessageProcessor.getInstance().pendings(), Thread.getAllStackTraces().size());
		stat.setTotalMemory((int) (Runtime.getRuntime().totalMemory() >> 20));
		stat.setUsedMemory((int) ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) >> 20));
		ReportService.getInstance().saveServerStat("" + serverId, stat);
	}

	public void startZk(ZkStartCommand command) {
		CometConfig config = CometServer.getInstance().getConfig();
		ZkService.getInstance().startClient(config.getCuratorClient())
				.registerComet("" + config.getServerId(), config.getCometIp(), config.getPort())
				.registerRpc("" + config.getServerId(), config.getRpcIp(), config.getPort());
	}

	private static class SavePrivateTask implements IAsyncTask {

		private volatile String clientId;
		private volatile MessageInfo message;

		public SavePrivateTask(String clientId, MessageInfo message) {
			this.clientId = clientId;
			this.message = message;
		}

		@Override
		public void runAsync() {
			MessageService.getInstance().savePrivateMessage(message);
			MessageService.getInstance().saveUserMessage(clientId, message.getId());
		}

		@Override
		public void afterOk() {

		}

		@Override
		public void afterError(Exception e) {

		}

		@Override
		public String getName() {
			return "save_private";
		}

	}

	public void publishPrivate(PublishPrivateCommand command) {
		final ChannelHandlerContext ctx = OnlineManager.getInstance().getClient(command.getClientId());
		String mid = MessageId.nextPrivateId();
		boolean online = command.isOnline();
		final MessageInfo msg = new MessageInfo(command.getMessageType(), command.getJobId(), mid, command.getContent(),
				System.currentTimeMillis());
		final String jobId = command.getJobId();
		JobStat stat = getJobStat(jobId);
		if (ctx == null) {
			// 离线的存起来再说
			if (!online) {
				AsyncManager.getInstance().execute(new SavePrivateTask(command.getClientId(), msg));
				stat.incrSentCount();
				stat.incrOfflineCount();
			}
			return;
		}
		stat.incrSentCount();
		// 用户在线就直接发过去
		ClientInfo client = ContextUtils.getClient(ctx);
		final MessageResponse response = new MessageResponse(client.getSecretKey(), msg);
		client.addMessage(msg);
		if (client.ready()) {
			ContextUtils.writeAndFlush(ctx, response, new ICallback<Future<Void>>() {

				@Override
				public void invoke(Future<Void> future) {
					if (future.isSuccess()) {
						JobStat stat = getJobStat(jobId);
						stat.incrRealSentCount();
					}
				}

			});
		}
	}

	private class LoadTopicClientsTask implements IAsyncTask {

		private volatile int appId;
		private volatile String topic;
		private volatile int serverId;
		private volatile MessageInfo message;
		private volatile Set<String> clients;

		public LoadTopicClientsTask(int appId, String topic, int serverId, MessageInfo message) {
			this.appId = appId;
			this.topic = topic;
			this.serverId = serverId;
			this.message = message;
		}

		@Override
		public void runAsync() {
			clients = TopicService.getInstance().getClients(appId, topic, serverId);
			LOG.warn("load topic clients size=%s", clients.size());
		}

		@Override
		public void afterOk() {
			if (clients.size() > 0) {
				afterTopicClientsLoad(clients, topic, message);
			}
		}

		@Override
		public void afterError(Exception e) {

		}

		@Override
		public String getName() {
			return "load_topic_clients";
		}

	}

	public void publishMulti(PublishMultiCommand command) {
		MessageInfo message = MessageService.getInstance().getCachedMessage(command.getMsgId());
		if (message == null) {
			LOG.error(String.format("message id=%s not exists in cache", command.getMsgId()));
			return;
		}
		AppInfo app = AppService.getInstance().getApp(command.getAppKey());
		if (app == null) {
			LOG.error(String.format("appkey %s not exists", command.getAppKey()));
			return;
		}
		if (!command.isOnline()) {
			AsyncManager.getInstance().execute(new LoadTopicClientsTask(app.getId(), command.getTopic(),
					CometServer.getInstance().getConfig().getServerId(), message));
		} else {
			Iterator<String> iter = OnlineManager.getInstance().getAllClientIds().iterator();
			PublishIteratorCommand cmd = new PublishIteratorCommand(iter, command.getTopic(), message, true);
			MessageProcessor.getInstance().putMessage(cmd);
		}
	}

	public void afterTopicClientsLoad(Set<String> clientIds, String topic, MessageInfo message) {
		Iterator<String> iter = clientIds.iterator();
		PublishIteratorCommand command = new PublishIteratorCommand(iter, topic, message, false);
		MessageProcessor.getInstance().putMessage(command);
	}

	private class SaveOfflineMessageTask implements IAsyncTask {

		private volatile Iterator<String> clientsIter;
		private volatile List<String> offlines;
		private volatile String topic;
		private volatile MessageInfo message;

		public SaveOfflineMessageTask(Iterator<String> clientsIter, String topic, List<String> offlines,
				MessageInfo message) {
			this.clientsIter = clientsIter;
			this.topic = topic;
			this.offlines = offlines;
			this.message = message;
		}

		@Override
		public void runAsync() {
			for (String clientId : offlines) {
				MessageService.getInstance().saveUserMessage(clientId, message.getId());
			}
		}

		@Override
		public void afterOk() {
			if (clientsIter.hasNext()) {
				MessageProcessor.getInstance()
						.putMessage(new PublishIteratorCommand(clientsIter, topic, message, false));
			}
		}

		@Override
		public void afterError(Exception e) {
			LOG.error("save offline messages error", e);
			if (clientsIter.hasNext()) {
				MessageProcessor.getInstance()
						.putMessage(new PublishIteratorCommand(clientsIter, topic, message, false));
			}
		}

		@Override
		public String getName() {
			return "save_offline_messages";
		}

	}

	private JobStat getJobStat(String jobId) {
		JobStat stat = jobStats.get(jobId);
		if (stat == null) {
			synchronized (this) {
				stat = jobStats.get(jobId);
				if (stat == null) {
					stat = new JobStat(jobId);
					jobStats.put(jobId, stat);
				}
			}
		}
		return stat;
	}

	public void publishIterator(final PublishIteratorCommand command) {
		Iterator<String> iter = command.getClientsIter();
		boolean online = command.isOnline();
		int total = 0;
		List<String> offlines = new ArrayList<String>();
		final String jobId = command.getMessage().getJobId();
		JobStat stat = getJobStat(jobId);
		while (iter.hasNext()) {
			String clientId = iter.next();
			final ChannelHandlerContext ctx = OnlineManager.getInstance().getClient(clientId);
			if (ctx == null) {
				offlines.add(clientId);
				continue;
			}
			Channel channel = ctx.channel();
			if (!channel.isActive()) {
				offlines.add(clientId);
				continue;
			}
			ClientInfo clientInfo = ContextUtils.getClient(ctx);
			stat.incrSentCount(); // 增加发送量
			final MessageResponse message = new MessageResponse(clientInfo.getSecretKey(), command.getMessage());
			clientInfo.addMessage(command.getMessage());
			if (clientInfo.ready()) {
				ContextUtils.writeAndFlush(ctx, message, new ICallback<Future<Void>>() {

					@Override
					public void invoke(Future<Void> future) {
						if (future.isSuccess()) {
							JobStat stat = getJobStat(jobId);
							stat.incrRealSentCount(); // 增加实际发送量
						}
					}

				});
			}
			total++;
			// 批次进行，一次1000个
			if (total >= 1000) {
				break;
			}
		}
		if (offlines.size() > 0) {
			if (!online) {
				stat.incrSentCount(offlines.size());
				stat.incrOfflineCount(offlines.size());
				AsyncManager.getInstance().execute(new SaveOfflineMessageTask(command.getClientsIter(),
						command.getTopic(), offlines, command.getMessage()));
			}
		} else {
			if (command.getClientsIter().hasNext()) {
				MessageProcessor.getInstance().putMessage(command);
			}
		}
	}

	private class AuthTask implements IAsyncTask {

		private volatile String token;
		private volatile String clientId;
		private volatile UserInfo client;
		private volatile boolean success;
		private ChannelHandlerContext ctx;

		public AuthTask(ChannelHandlerContext ctx, String clientId, String token) {
			this.ctx = ctx;
			this.clientId = clientId;
			this.token = token;
		}

		@Override
		public void runAsync() {
			String savedClientId = UserService.getInstance().getClientId(token);
			if (clientId.equals(savedClientId)) {
				success = true;
			}
			client = UserService.getInstance().getClient(clientId);
		}

		@Override
		public void afterOk() {
			afterAuth(ctx, clientId, client, success);
		}

		@Override
		public void afterError(Exception e) {
			ctx.writeAndFlush(ErrorResponse.newServerError("unknown server error for auth command"));
			return;
		}

		@Override
		public String getName() {
			return "auth_client";
		}

	}

	public void auth(AuthCommand command) {
		ChannelHandlerContext ctx = OnlineManager.getInstance().getClient(command.getClientId());
		if (ctx != null) {
			command.getCtx().writeAndFlush(ErrorResponse.newClientDupError("clientId already online"))
					.addListener(ChannelFutureListener.CLOSE);
			return;
		}
		AsyncManager.getInstance().execute(new AuthTask(command.getCtx(), command.getClientId(), command.getToken()));
	}

	public void afterAuth(ChannelHandlerContext ctx, String clientId, UserInfo client, boolean success) {
		if (!success) {
			ErrorResponse err = null;
			if (client != null) {
				err = ErrorResponse.newTokenExpiredError("token expired or illegal");
			} else {
				err = ErrorResponse.newClientNotFoundError("client id not exists");
			}
			ctx.writeAndFlush(err);
			return;
		}
		AsyncManager.getInstance().execute(new LoadClientTask(ctx, clientId));
	}

	private class LoadClientTask implements IAsyncTask {

		private volatile String clientId;
		private volatile ClientInfo clientInfo;
		private ChannelHandlerContext ctx;
		private PublicKey encryptKey;

		public LoadClientTask(ChannelHandlerContext ctx, String clientId) {
			this.ctx = ctx;
			this.clientId = clientId;
		}

		@Override
		public void runAsync() {
			UserInfo pair = UserService.getInstance().getClient(clientId);
			List<MessageInfo> messages = MessageService.getInstance().getUserMessages(clientId);
			MessageService.getInstance().removeUserMessages(clientId);
			List<String> topics = TopicService.getInstance().getClientTopics(clientId);
			String lastServerId = RouteService.getInstance().getRoute(clientId);
			int currentServerId = CometServer.getInstance().getConfig().getServerId();
			if (lastServerId == null && topics.size() > 0) {
				LOG.error("impossible with empty lastServerId but have topics");
			}
			if (lastServerId != null && !("" + currentServerId).equals(lastServerId)) {
				// 搬迁topic，从lastServerId搬到currentServerId
				TopicService.getInstance().unsubscribeTopics(pair.getAppId(), Integer.parseInt(lastServerId), clientId,
						topics);
				TopicService.getInstance().subscribeTopics(pair.getAppId(), currentServerId, clientId, topics);
			}
			KeyPair keyPair = EncryptService.getInstance().randomKeyPair();
			encryptKey = keyPair.getPublic();
			clientInfo = new ClientInfo(clientId, pair.getAppId(), topics, messages);
			ContextUtils.setDecryptKey(ctx, keyPair.getPrivate());
			if (lastServerId == null || !("" + currentServerId).equals(lastServerId)) {
				// 更新路由
				RouteService.getInstance().saveRoute(clientId, "" + currentServerId);
			}
		}

		@Override
		public void afterOk() {
			afterClientLoaded(ctx, encryptKey, clientInfo);
		}

		@Override
		public void afterError(Exception e) {
			ctx.writeAndFlush(ErrorResponse.newServerError("unknown error when loading client info"));
		}

		@Override
		public String getName() {
			return "load_client";
		}

	}

	public void afterClientLoaded(ChannelHandlerContext ctx, PublicKey encryptKey, ClientInfo clientInfo) {
		ContextUtils.attachClient(ctx, clientInfo);
		OnlineManager.getInstance().addClient(clientInfo.getClientId(), ctx);
		ctx.writeAndFlush(new AuthSuccessResponse(encryptKey.getEncoded()));
	}

	public void getTopicList(TopicListCommand command) {
		ClientInfo client = ContextUtils.getClient(command.getCtx());
		ChannelHandlerContext ctx = OnlineManager.getInstance().getClient(client.getClientId());
		ctx.writeAndFlush(new TopicListResponse(client.getClientTopics()));
	}

	private static class SubscribeTopicsTask implements IAsyncTask {

		private volatile int appId;
		private volatile String clientId;
		private volatile List<String> allTopics;
		private volatile List<String> newTopics;
		private volatile ChannelHandlerContext ctx;
		private volatile boolean serverSide;

		public SubscribeTopicsTask(int appId, String clientId, List<String> allTopics, List<String> newTopics,
				ChannelHandlerContext ctx) {
			this.appId = appId;
			this.clientId = clientId;
			this.allTopics = allTopics;
			this.newTopics = newTopics;
			this.ctx = ctx;
		}

		public boolean isServerSide() {
			return serverSide;
		}

		public void setServerSide(boolean serverSide) {
			this.serverSide = serverSide;
		}

		@Override
		public void runAsync() {
			int serverId = CometServer.getInstance().getConfig().getServerId();
			TopicService.getInstance().subscribeTopics(appId, serverId, clientId, newTopics);
			TopicService.getInstance().saveClientTopics(clientId, allTopics);
			TopicService.getInstance().saveTopicsMeta(appId, newTopics);
		}

		@Override
		public void afterOk() {
			if (!isServerSide()) {
				ctx.writeAndFlush(new OkResponse());
			}
		}

		@Override
		public void afterError(Exception e) {
			if (!isServerSide()) {
				ctx.writeAndFlush(ErrorResponse.newServerError("subscribe failed"));
			}
		}

		@Override
		public String getName() {
			return "subscribe_topics";
		}

	}

	public void subscribe(SubscribeCommand command) {
		ChannelHandlerContext ctx = command.getCtx();
		ClientInfo client = ContextUtils.getClient(ctx);
		List<String> allTopics = client.getTopics();
		for (String topic : command.getTopics()) {
			if (!allTopics.contains(topic)) {
				allTopics.add(topic);
			}
		}
		AsyncManager.getInstance().execute(new SubscribeTopicsTask(client.getAppId(), client.getClientId(), allTopics,
				command.getTopics(), command.getCtx()));
	}

	public void subscribeByServer(ServerSubscribeCommand command) {
		for (String clientId : command.getClientIds()) {
			ChannelHandlerContext ctx = OnlineManager.getInstance().getClient(clientId);
			if (ctx != null) {
				// 用户在线
				ClientInfo client = ContextUtils.getClient(ctx);
				List<String> allTopics = client.getTopics();
				String topic = command.getTopic();
				List<String> newTopics = new ArrayList<String>(1);
				if (!allTopics.contains(topic)) {
					allTopics.add(topic);
					newTopics.add(topic);
					SubscribeTopicsTask task = new SubscribeTopicsTask(client.getAppId(), client.getClientId(),
							allTopics, newTopics, command.getCtx());
					task.setServerSide(true);
					AsyncManager.getInstance().execute(task);
				}
			} else {
				AsyncManager.getInstance().execute(new SubscribeOfflineTask(clientId, command.getTopic()));
			}
		}
	}

	private final static class SubscribeOfflineTask implements IAsyncTask {

		private volatile String clientId;
		private volatile String topic;

		public SubscribeOfflineTask(String clientId, String topic) {
			this.clientId = clientId;
			this.topic = topic;
		}

		@Override
		public void runAsync() {
			UserInfo client = UserService.getInstance().getClient(clientId);
			String serverId = RouteService.getInstance().getRoute(clientId);
			if (client == null || serverId == null) {
				LOG.error("clientId=%s not exists", clientId);
				return;
			}
			List<String> allTopics = TopicService.getInstance().getClientTopics(clientId);
			List<String> newTopics = new ArrayList<String>(1);
			if (!allTopics.contains(topic)) {
				allTopics.add(topic);
				newTopics.add(topic);
				TopicService.getInstance().saveClientTopics(clientId, allTopics);

				TopicService.getInstance().subscribeTopics(client.getAppId(), Integer.parseInt(serverId), clientId,
						newTopics);
			}
		}

		@Override
		public void afterOk() {

		}

		@Override
		public void afterError(Exception e) {

		}

		@Override
		public String getName() {
			return "subscribe_offline";
		}

	}

	private final static class UnsubscribeTopicsTask implements IAsyncTask {

		private volatile int appId;
		private volatile String clientId;
		private volatile List<String> allTopics;
		private volatile List<String> oldTopics;
		private volatile ChannelHandlerContext ctx;
		private volatile boolean serverSide;

		public UnsubscribeTopicsTask(int appId, String clientId, List<String> allTopics, List<String> oldTopics,
				ChannelHandlerContext ctx) {
			this.appId = appId;
			this.clientId = clientId;
			this.allTopics = allTopics;
			this.oldTopics = oldTopics;
			this.ctx = ctx;
		}

		public boolean isServerSide() {
			return serverSide;
		}

		public void setServerSide(boolean serverSide) {
			this.serverSide = serverSide;
		}

		@Override
		public void runAsync() {
			int serverId = CometServer.getInstance().getConfig().getServerId();
			TopicService.getInstance().unsubscribeTopics(appId, serverId, clientId, oldTopics);
			TopicService.getInstance().saveClientTopics(clientId, allTopics);
		}

		@Override
		public void afterOk() {
			if (!isServerSide()) {
				ctx.writeAndFlush(new OkResponse());
			}
		}

		@Override
		public void afterError(Exception e) {
			if (!isServerSide()) {
				ctx.writeAndFlush(ErrorResponse.newServerError("unsubscribe error"));
			}
		}

		@Override
		public String getName() {
			return "unsubscribe_topics";
		}

	}

	public void unsubscribe(UnsubscribeCommand command) {
		ChannelHandlerContext ctx = command.getCtx();
		ClientInfo client = ContextUtils.getClient(ctx);
		List<String> allTopics = client.getTopics();
		LOG.info(allTopics.getClass().getCanonicalName());
		for (String topic : command.getTopics()) {
			if (allTopics.contains(topic)) {
				allTopics.remove(topic);
			}
		}
		AsyncManager.getInstance().execute(new UnsubscribeTopicsTask(client.getAppId(), client.getClientId(), allTopics,
				command.getTopics(), command.getCtx()));
	}

	public void unsubscribeByServer(ServerUnSubscribeCommand command) {
		for (String clientId : command.getClientIds()) {
			ChannelHandlerContext ctx = OnlineManager.getInstance().getClient(clientId);
			if (ctx != null) {
				// 用户在线
				ClientInfo client = ContextUtils.getClient(ctx);
				List<String> allTopics = client.getTopics();
				String topic = command.getTopic();
				List<String> oldTopics = new ArrayList<String>(1);
				if (allTopics.contains(topic)) {
					allTopics.remove(topic);
					oldTopics.add(topic);
					UnsubscribeTopicsTask task = new UnsubscribeTopicsTask(client.getAppId(), client.getClientId(),
							allTopics, oldTopics, command.getCtx());
					task.setServerSide(true);
					AsyncManager.getInstance().execute(task);
				}
			} else {
				AsyncManager.getInstance().execute(new UnsubscribeOfflineTask(clientId, command.getTopic()));
			}
		}
	}

	private final static class UnsubscribeOfflineTask implements IAsyncTask {

		private volatile String clientId;
		private volatile String topic;

		public UnsubscribeOfflineTask(String clientId, String topic) {
			this.clientId = clientId;
			this.topic = topic;
		}

		@Override
		public void runAsync() {
			UserInfo client = UserService.getInstance().getClient(clientId);
			String serverId = RouteService.getInstance().getRoute(clientId);
			if (client == null || serverId == null) {
				LOG.error("clientId=%s not exists", clientId);
				return;
			}
			List<String> allTopics = TopicService.getInstance().getClientTopics(clientId);
			List<String> oldTopics = new ArrayList<String>(1);
			if (allTopics.contains(topic)) {
				allTopics.remove(topic);
				oldTopics.add(topic);
				TopicService.getInstance().saveClientTopics(clientId, allTopics);

				TopicService.getInstance().unsubscribeTopics(client.getAppId(), Integer.parseInt(serverId), clientId,
						oldTopics);
			}
		}

		@Override
		public void afterOk() {

		}

		@Override
		public void afterError(Exception e) {

		}

		@Override
		public String getName() {
			return "unsubscribe_offline";
		}

	}

	public void getMessageList(MessageListCommand command) {
		ChannelHandlerContext ctx = command.getCtx();
		final ClientInfo client = ContextUtils.getClient(ctx);
		MessageListResponse message = new MessageListResponse(client.getSecretKey(), client.getMessages());
		ctx.writeAndFlush(message).addListener(new FutureListener<Void>() {

			@Override
			public void operationComplete(Future<Void> future) throws Exception {
				if (future.isSuccess()) {
					for (MessageInfo msgInfo : client.getMessages()) {
						String jobId = msgInfo.getJobId();
						JobStat stat = jobStats.get(jobId);
						if (stat == null) {
							stat = new JobStat(jobId);
							jobStats.put(jobId, stat);
						}
						stat.incrRealSentCount();
					}
				}
			}

		});
		client.setReady();
	}

	public void ackMessages(MessageAckCommand command) {
		List<String> messageIds = command.getMessageIds();
		ChannelHandlerContext ctx = command.getCtx();
		final ClientInfo client = ContextUtils.getClient(ctx);
		for (String messageId : messageIds) {
			MessageInfo message = client.removeMessage(messageId);
			if (message != null) {
				String jobId = message.getJobId();
				JobStat stat = jobStats.get(jobId);
				if (stat == null) {
					stat = new JobStat(jobId);
					jobStats.put(jobId, stat);
				}
				stat.incrArrivedCount(); // 增加到达量
			}
		}
		ctx.writeAndFlush(new OkResponse());
	}

	public void offline(ClientOfflineCommand command) {
		ClientInfo client = ContextUtils.getClient(command.getCtx());
		OnlineManager.getInstance().removeClient(client.getClientId());
		if (!client.isEmpty()) {
			AsyncManager.getInstance().execute(new SaveClientMessagesTask(client.getClientId(), client.getMessages()));
		}
	}

	private static class SaveClientMessagesTask implements IAsyncTask {

		private String clientId;

		public List<MessageInfo> messages;

		public SaveClientMessagesTask(String clientId, List<MessageInfo> messages) {
			this.clientId = clientId;
			this.messages = messages;
		}

		@Override
		public void runAsync() {
			for (MessageInfo message : messages) {
				if (MessageId.isPrivate(message.getId())) {
					MessageService.getInstance().savePrivateMessage(message);
				}
			}
			if (messages.size() > 0) {
				MessageService.getInstance().saveUserMessages(clientId, messages);
			}
		}

		@Override
		public void afterOk() {

		}

		@Override
		public void afterError(Exception e) {

		}

		@Override
		public String getName() {
			return "save_messages";
		}

	}

	public void saveJobStat(SaveJobStatCommand command) {
		Map<String, JobStat> hitStats = new HashMap<String, JobStat>();
		for (Entry<String, JobStat> entry : jobStats.entrySet()) {
			String jobId = entry.getKey();
			JobStat stat = entry.getValue();
			JobStat tempStat = new JobStat(stat.getId());
			if (stat.getSentCount() > 100 || stat.getArrivedCount() > 100 || stat.getRealSentCount() > 100
					|| stat.getOfflineCount() > 100 || command.isSaveAll()) {
				tempStat.setSentCount(stat.getSentCount());
				stat.setSentCount(0);
				tempStat.setOfflineCount(stat.getOfflineCount());
				stat.setOfflineCount(0);
				tempStat.setArrivedCount(stat.getArrivedCount());
				stat.setArrivedCount(0);
				// 实际发送量是多线程并发更新的，比较特殊
				int realSentCount = stat.getRealSentCount();
				tempStat.setRealSentCount(realSentCount);
				stat.decrRealSentCount(realSentCount);
				hitStats.put(jobId, tempStat);
			}
		}
		if (command.isSaveAll()) {
			synchronized (this) {
				// 和getJobStat公用一把锁
				for (String jobId : hitStats.keySet()) {
					JobStat stat = jobStats.remove(jobId);
					if (stat != null && !stat.isEmpty()) {
						// 再放回去
						jobStats.put(jobId, stat);
					}
				}
			}
		}
		if (!hitStats.isEmpty()) {
			AsyncManager.getInstance().execute(new SaveJobStatTask(hitStats));
		}
	}

	private static class SaveJobStatTask implements IAsyncTask {

		private Map<String, JobStat> hitStats;

		public SaveJobStatTask(Map<String, JobStat> hitStats) {
			this.hitStats = hitStats;
		}

		@Override
		public void runAsync() {
			JobService.getInstance().incrJobStats(hitStats);
		}

		@Override
		public void afterOk() {

		}

		@Override
		public void afterError(Exception e) {
			LOG.error("save job stat error", e);
		}

		@Override
		public String getName() {
			return "save_job_stat";
		}

	}

	public void resendMessagesUnAcked(ResendUnackedMessagesCommand command) {
		final ChannelHandlerContext ctx = command.getCtx();
		ClientInfo client = ContextUtils.getClient(ctx);
		long now = System.currentTimeMillis();
		if (client == null || now - client.getLastResendTs() < Constants.MESSAGE_UNACKED_CHECK_PERIOD
				|| client.isEmpty()) {
			return;
		}
		if (!(ctx.channel().isActive() && ctx.channel().isWritable())) {
			return;
		}
		client.setLastResendTs(now);
		for (final MessageInfo message : client.getMessages()) {
			if (now - message.getTs() > Constants.MESSAGE_UNACKED_CHECK_PERIOD) {
				ContextUtils.writeAndFlush(ctx, new MessageResponse(client.getSecretKey(), message),
						new ICallback<Future<Void>>() {

							@Override
							public void invoke(Future<Void> f) {
								if (f.isSuccess()) {
									JobStat stat = getJobStat(message.getJobId());
									stat.incrRealSentCount();
								}
							}
						});
			}
		}
	}

	public void resendPendings(ResendPendingsCommand command) {
		List<WriteResponse> messages = command.getMessages();
		ChannelHandlerContext ctx = command.getCtx();
		for (final WriteResponse message : messages) {
			ContextUtils.writeAndFlush(ctx, message, new ICallback<Future<Void>>() {

				@Override
				public void invoke(Future<Void> future) {
					if (!future.isSuccess()) {
						return;
					}
					if (message.getType() == MessageDefine.Write.MSG_MESSAGE) {
						JobStat stat = getJobStat(((MessageResponse) message).getJobId());
						stat.incrRealSentCount();
					}
				}

			});
		}
	}

	public static class CleanOverdueJobsTask implements IAsyncTask {

		@Override
		public void runAsync() {
			JobService.getInstance().clearOverdueJobs();
		}

		@Override
		public void afterOk() {

		}

		@Override
		public void afterError(Exception e) {

		}

		@Override
		public String getName() {
			return "clear_overdue_jobs";
		}

	}

	public void clearOverdueJobs(ClearOverdueJobsCommand command) {
		AsyncManager.getInstance().execute(new CleanOverdueJobsTask());
	}

	private static class SaveMainHistTask implements IAsyncTask {

		private Map<String, Pair<Double, Long>> savings;

		public SaveMainHistTask(Map<String, Pair<Double, Long>> savings) {
			this.savings = savings;
		}

		@Override
		public void runAsync() {
			MeasureService.getInstance().saveMainHistogram(savings);
		}

		@Override
		public void afterOk() {

		}

		@Override
		public void afterError(Exception e) {

		}

		@Override
		public String getName() {
			return "save_main_hist";
		}

	}

	private static class SaveIOHistTask implements IAsyncTask {

		private Map<String, Pair<Double, Long>> savings;

		public SaveIOHistTask(Map<String, Pair<Double, Long>> savings) {
			this.savings = savings;
		}

		@Override
		public void runAsync() {
			MeasureService.getInstance().saveIOHistogram(savings);
		}

		@Override
		public void afterOk() {

		}

		@Override
		public void afterError(Exception e) {

		}

		@Override
		public String getName() {
			return "save_io_hist";
		}

	}

	public void saveMainHistogram(SaveMainHistogramCommand command) {
		Map<String, Pair<Double, Long>> savings = MeasureService.getInstance().clearMainHistogram();
		AsyncManager.getInstance().execute(new SaveMainHistTask(savings));
	}

	public void saveIOHistogram(SaveIOHistogramCommand command) {
		Map<String, Pair<Double, Long>> savings = MeasureService.getInstance().clearIOHistogram();
		AsyncManager.getInstance().execute(new SaveIOHistTask(savings));
	}

	public void reportEnviron(ReportEnvironCommand command) {
		this.environStat.incrNetwork(command.getNetworkType());
		this.environStat.incrIsp(command.getIsp());
		this.environStat.incrPhone(command.getPhoneType());
		command.getCtx().writeAndFlush(new OkResponse());
	}

	private static class SaveClientEnvironTask implements IAsyncTask {

		private ClientEnvironStat environIncrs;

		public SaveClientEnvironTask(ClientEnvironStat environIncrs) {
			this.environIncrs = environIncrs;
		}

		@Override
		public void runAsync() {
			ReportService.getInstance().saveClientEnviron(environIncrs);
		}

		@Override
		public void afterOk() {

		}

		@Override
		public void afterError(Exception e) {

		}

		@Override
		public String getName() {
			return "save_client_environ";
		}

	}

	private void saveClientEnvironIncrs(SaveClientEnvironCommand command) {
		AsyncManager.getInstance().execute(new SaveClientEnvironTask(environStat));
		this.environStat = new ClientEnvironStat();
	}

	public void exchangeKey(ExchangeKeyCommand command) {
		ChannelHandlerContext ctx = command.getCtx();
		ClientInfo client = ContextUtils.getClient(ctx);
		PrivateKey decryptKey = ContextUtils.getDecryptKey(ctx);
		byte[] secretKey = EncryptService.getInstance().decryptWithPrivateKey(decryptKey,
				command.getEncryptedSecretKey());
		client.setSecretKey(new SecretKeySpec(secretKey, 0, secretKey.length, "des"));
		ContextUtils.removeDecryptKey(ctx);
		ctx.writeAndFlush(new OkResponse());
	}

}
