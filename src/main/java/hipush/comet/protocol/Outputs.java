package hipush.comet.protocol;

import hipush.services.MessageInfo;

import java.util.List;

public class Outputs {
	public static class ErrorResponse extends WriteResponse {

		@Override
		public byte getType() {
			return MessageDefine.Write.MSG_ERROR;
		}

		private int code;
		private String message;

		public ErrorResponse(int code, String message) {
			this.code = code;
			this.message = message;
		}

		public static ErrorResponse newBadArgsError(String message) {
			return new ErrorResponse(MessageDefine.Errors.BADARGS_ERROR,
					message);
		}

		public static ErrorResponse newTokenExpiredError(String message) {
			return new ErrorResponse(MessageDefine.Errors.TOKEN_EXPIRED_ERROR,
					message);
		}

		public static ErrorResponse newClientNotFoundError(String message) {
			return new ErrorResponse(
					MessageDefine.Errors.CLIENT_NOT_FOUND_ERROR, message);
		}

		public static ErrorResponse newClientDupError(String message) {
			return new ErrorResponse(MessageDefine.Errors.CLIENT_DUP_ERROR,
					message);
		}

		public static ErrorResponse newServerError(String message) {
			return new ErrorResponse(MessageDefine.Errors.SERVER_ERROR, message);
		}

		@Override
		public void writeImpl() {
			this.writeByte((byte) code);
			this.writeStr(message);
		}

		@Override
		public String getName() {
			return "name";
		}

	}

	public static class MessageListResponse extends WriteResponse {

		@Override
		public byte getType() {
			return MessageDefine.Write.MSG_MESSAGE_LIST;
		}

		private List<MessageInfo> messages;

		public MessageListResponse(List<MessageInfo> messages) {
			this.messages = messages;
		}

		@Override
		public void writeImpl() {
			this.writeByte((byte) messages.size());
			for (MessageInfo message : messages) {
				this.writeByte((byte) message.getType());
				this.writeStr(message.getId());
				this.writeStr(message.getJobId());
				this.writeStr(message.getContent());
				this.writeLong(message.getTs());
			}
		}

		@Override
		public String getName() {
			return "name";
		}

	}

	public static class MessageResponse extends WriteResponse {

		private MessageInfo message;

		public MessageResponse(MessageInfo message) {
			this.message = message;
		}

		@Override
		public byte getType() {
			return MessageDefine.Write.MSG_MESSAGE;
		}

		@Override
		public void writeImpl() {
			writeByte((byte) message.getType());
			writeStr(message.getId());
			writeStr(message.getJobId());
			writeStr(message.getContent());
			writeLong(message.getTs());
		}

		@Override
		public String getName() {
			return "message";
		}

		public String getJobId() {
			return message.getJobId();
		}

	}

	public static class OkResponse extends WriteResponse {

		@Override
		public byte getType() {
			return MessageDefine.Write.MSG_OK;
		}

		@Override
		public void writeImpl() {

		}

		@Override
		public String getName() {
			return "ok";
		}

	}

	public static class TopicListResponse extends WriteResponse {

		@Override
		public byte getType() {
			return MessageDefine.Write.MSG_TOPIC_LIST;
		}

		private List<String> topics;

		public TopicListResponse(List<String> topics) {
			this.topics = topics;
		}

		@Override
		public void writeImpl() {
			this.writeByte((byte) topics.size());
			for (String topic : topics) {
				this.writeStr(topic);
			}
		}

		@Override
		public String getName() {
			return "topic_list";
		}

	}
}
