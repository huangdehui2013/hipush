package hipush.comet.protocol;

public class MessageDefine {

	public final static class Read {
		public final static int CMD_AUTH = 0;
		public final static int CMD_TOPIC_LIST = 1;
		public final static int CMD_SUBSCRIBE = 2;
		public final static int CMD_UNSUBSCRIBE = 3;
		public final static int CMD_MESSAGE_LIST =4;
		public final static int CMD_HEARTBEAT = 5;
		public final static int CMD_MESSAGE_ACK = 6;
		public final static int CMD_REPORT_ENVIRON = 7;
	}

	public final static class Write {
		public final static int MSG_OK = 0;
		public final static int MSG_ERROR = 1;
		public final static int MSG_TOPIC_LIST = 2;
		public final static int MSG_MESSAGE_LIST = 3;
		public final static int MSG_MESSAGE = 4;
	}
	
	public final static class Internal {
		public final static int ASYNC_FINISHED = 0;
		public final static int START_ZOOKEEPER = 1;
		public final static int PUBLISH_PRIVATE = 2;
		public final static int PUBLISH_MULTI = 3;
		public final static int PUBLISH_ITERATOR = 4;
		public final static int CLIENT_OFFLINE = 5;
		public final static int REPORT_STAT = 6;
		public final static int SAVE_JOB_STAT = 7;
		public final static int CLEAR_OVERDUE_JOBS = 8;
		public final static int SERVER_SUBSCRIBE = 9;
		public final static int SERVER_UNSUBSCRIBE = 10;
		public final static int RESEND_PENDINGS = 11;
		public final static int RESEND_MESSAGE_UNACKED = 12;
		public final static int SAVE_MAIN_HISTOGRAM = 13;
		public final static int SAVE_IO_HISTOGRAM = 14;
		public final static int SAVE_CLIENT_ENVIRON = 15;
	}

	public final static class Errors {
		public final static int TOKEN_EXPIRED_ERROR = 0;
		public final static int CLIENT_NOT_FOUND_ERROR = 1;
		public final static int CLIENT_DUP_ERROR = 2;
		public final static int SERVER_ERROR = 3;
		public final static int BADARGS_ERROR = 4;
	}

}
