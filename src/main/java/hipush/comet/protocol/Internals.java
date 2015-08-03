package hipush.comet.protocol;

import java.util.Iterator;
import java.util.List;

import hipush.async.IAsyncTask;
import hipush.services.MessageInfo;
import io.netty.channel.ChannelHandlerContext;

public class Internals {

	public static class ResendUnackedMessagesCommand extends InternalCommand {

		public ResendUnackedMessagesCommand(ChannelHandlerContext ctx) {
			this.setCtx(ctx);
		}

		@Override
		public byte getType() {
			return MessageDefine.Internal.RESEND_MESSAGE_UNACKED;
		}

		@Override
		public String getName() {
			return "resend_message_unacked";
		}

	}

	public static class ResendPendingsCommand extends InternalCommand {

		private List<WriteResponse> messages;

		public ResendPendingsCommand(List<WriteResponse> messages) {
			this.messages = messages;
		}

		public List<WriteResponse> getMessages() {
			return this.messages;
		}

		@Override
		public byte getType() {
			return MessageDefine.Internal.RESEND_PENDINGS;
		}

		@Override
		public String getName() {
			return "resend_pendings";
		}

	}

	public static class AsyncFinishedCommand extends InternalCommand {

		private IAsyncTask runner;
		private Exception error;

		public AsyncFinishedCommand(IAsyncTask runner) {
			this(runner, null);
		}

		public AsyncFinishedCommand(IAsyncTask runner, Exception error) {
			this.runner = runner;
			this.error = error;
		}

		public IAsyncTask getRunner() {
			return runner;
		}

		public Exception getError() {
			return error;
		}

		@Override
		public byte getType() {
			return MessageDefine.Internal.ASYNC_FINISHED;
		}

		@Override
		public String getName() {
			return "async_finished";
		}

	}

	public static class ClientOfflineCommand extends InternalCommand {

		@Override
		public byte getType() {
			return MessageDefine.Internal.CLIENT_OFFLINE;
		}

		@Override
		public String getName() {
			return "client_offline";
		}

	}

	public static class PublishIteratorCommand extends InternalCommand {

		private Iterator<String> clientsIter;
		private String topic;
		private MessageInfo message;
		private boolean online;

		public PublishIteratorCommand(Iterator<String> clientsIter, String topic, MessageInfo message, boolean online) {
			this.topic = topic;
			this.clientsIter = clientsIter;
			this.message = message;
			this.online = online;
		}

		public Iterator<String> getClientsIter() {
			return clientsIter;
		}

		public void setClientsIter(Iterator<String> clientsIter) {
			this.clientsIter = clientsIter;
		}

		public String getTopic() {
			return topic;
		}

		public boolean isOnline() {
			return online;
		}

		public MessageInfo getMessage() {
			return message;
		}

		public void setMessage(MessageInfo message) {
			this.message = message;
		}

		@Override
		public byte getType() {
			return MessageDefine.Internal.PUBLISH_ITERATOR;
		}

		@Override
		public String getName() {
			return "publish_iterator";
		}

	}

	public static class PublishMultiCommand extends InternalCommand {

		private String appKey;
		private String topic;
		private String msgId;
		private boolean online;

		public PublishMultiCommand(String appKey, String topic, String msgId, boolean online) {
			this.appKey = appKey;
			this.topic = topic;
			this.msgId = msgId;
			this.online = online;
		}

		public String getAppKey() {
			return appKey;
		}

		public String getTopic() {
			return topic;
		}

		public String getMsgId() {
			return msgId;
		}

		public boolean isOnline() {
			return online;
		}

		public void setOnline(boolean online) {
			this.online = online;
		}

		@Override
		public byte getType() {
			return MessageDefine.Internal.PUBLISH_MULTI;
		}

		@Override
		public String getName() {
			return "publish_multi";
		}

	}

	public static class PublishPrivateCommand extends InternalCommand {

		private int messageType;
		private String jobId;
		private String clientId;
		private String content;
		private boolean online;

		public PublishPrivateCommand(int messageType, String jobId, String clientId, String content, boolean online) {
			this.jobId = jobId;
			this.messageType = messageType;
			this.clientId = clientId;
			this.content = content;
			this.online = online;
		}

		public String getClientId() {
			return clientId;
		}

		public String getContent() {
			return content;
		}

		public int getMessageType() {
			return messageType;
		}

		public String getJobId() {
			return jobId;
		}

		public void setJobId(String jobId) {
			this.jobId = jobId;
		}

		public boolean isOnline() {
			return online;
		}

		public void setOnline(boolean online) {
			this.online = online;
		}

		@Override
		public byte getType() {
			return MessageDefine.Internal.PUBLISH_PRIVATE;
		}

		@Override
		public String getName() {
			return "publish_private";
		}

	}

	public static class ZkStartCommand extends ScheduleCommand {

		@Override
		public byte getType() {
			return MessageDefine.Internal.START_ZOOKEEPER;
		}

		@Override
		public String getName() {
			return "start_zk";
		}

	}

	public static class ReportStatCommand extends ScheduleCommand {

		@Override
		public byte getType() {
			return MessageDefine.Internal.REPORT_STAT;
		}

		@Override
		public String getName() {
			return "report_stat";
		}

	}

	public static class SaveJobStatCommand extends ScheduleCommand {

		private boolean saveAll;

		public SaveJobStatCommand(boolean saveAll) {
			this.saveAll = saveAll;
		}

		@Override
		public byte getType() {
			return MessageDefine.Internal.SAVE_JOB_STAT;
		}

		@Override
		public String getName() {
			return "save_job_stat";
		}

		public boolean isSaveAll() {
			return saveAll;
		}

	}

	public static class ClearOverdueJobsCommand extends ScheduleCommand {

		@Override
		public byte getType() {
			return MessageDefine.Internal.CLEAR_OVERDUE_JOBS;
		}

		@Override
		public String getName() {
			return "clear_overdue_jobs";
		}

	}

	public static class ServerSubscribeCommand extends ScheduleCommand {

		private List<String> clientIds;
		private String topic;

		public ServerSubscribeCommand(List<String> clientIds, String topic) {
			this.clientIds = clientIds;
			this.topic = topic;
		}

		public List<String> getClientIds() {
			return clientIds;
		}

		public void setClientIds(List<String> clientIds) {
			this.clientIds = clientIds;
		}

		public String getTopic() {
			return topic;
		}

		public void setTopic(String topic) {
			this.topic = topic;
		}

		@Override
		public byte getType() {
			return MessageDefine.Internal.SERVER_SUBSCRIBE;
		}

		@Override
		public String getName() {
			return "server_subscribe";
		}
	}

	public static class ServerUnSubscribeCommand extends ScheduleCommand {

		private List<String> clientIds;
		private String topic;

		public ServerUnSubscribeCommand(List<String> clientIds, String topic) {
			this.clientIds = clientIds;
			this.topic = topic;
		}

		public List<String> getClientIds() {
			return clientIds;
		}

		public void setClientIds(List<String> clientIds) {
			this.clientIds = clientIds;
		}

		public String getTopic() {
			return topic;
		}

		public void setTopic(String topic) {
			this.topic = topic;
		}

		@Override
		public byte getType() {
			return MessageDefine.Internal.SERVER_UNSUBSCRIBE;
		}

		@Override
		public String getName() {
			return "server_unsubscribe";
		}
	}

	public static class SaveMainHistogramCommand extends ScheduleCommand {

		@Override
		public byte getType() {
			return MessageDefine.Internal.SAVE_MAIN_HISTOGRAM;
		}

		@Override
		public String getName() {
			return "save_main_histogram";
		}

	}

	public static class SaveIOHistogramCommand extends ScheduleCommand {

		@Override
		public byte getType() {
			return MessageDefine.Internal.SAVE_IO_HISTOGRAM;
		}

		@Override
		public String getName() {
			return "save_io_histogram";
		}

	}

	public static class SaveClientEnvironCommand extends ScheduleCommand {

		@Override
		public byte getType() {
			return MessageDefine.Internal.SAVE_CLIENT_ENVIRON;
		}

		@Override
		public String getName() {
			return "save_client_environ";
		}

	}
}
