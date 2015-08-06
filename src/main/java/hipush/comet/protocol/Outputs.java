package hipush.comet.protocol;

import java.util.List;

import javax.crypto.SecretKey;

import hipush.services.MessageInfo;

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
			return new ErrorResponse(MessageDefine.Errors.BADARGS_ERROR, message);
		}

		public static ErrorResponse newTokenExpiredError(String message) {
			return new ErrorResponse(MessageDefine.Errors.TOKEN_EXPIRED_ERROR, message);
		}

		public static ErrorResponse newClientNotFoundError(String message) {
			return new ErrorResponse(MessageDefine.Errors.CLIENT_NOT_FOUND_ERROR, message);
		}

		public static ErrorResponse newClientDupError(String message) {
			return new ErrorResponse(MessageDefine.Errors.CLIENT_DUP_ERROR, message);
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

		private SecretKey secretKey;
		private List<MessageInfo> messages;

		public MessageListResponse(SecretKey secretKey, List<MessageInfo> messages) {
			this.secretKey = secretKey;
			this.messages = messages;
		}

		@Override
		public void writeImpl() {
			this.writeByte((byte) messages.size());
			for (MessageInfo message : messages) {
				this.writeByte((byte) message.getType());
				this.writeStr(message.getId());
				this.writeStr(message.getJobId());
				this.writeBytes(message.encryptContent(secretKey));
				this.writeLong(message.getTs());
			}
		}

		@Override
		public String getName() {
			return "name";
		}

	}

	public static class MessageResponse extends WriteResponse {

		private SecretKey encryptKey;
		private MessageInfo message;

		public MessageResponse(SecretKey encryptKey, MessageInfo message) {
			this.encryptKey = encryptKey;
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
			writeBytes(message.encryptContent(encryptKey));
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

	public static class AuthSuccessResponse extends WriteResponse {

		private byte[] encryptKey;

		public AuthSuccessResponse(byte[] encryptKey) {
			this.encryptKey = encryptKey;
		}

		@Override
		public byte getType() {
			return MessageDefine.Write.MSG_AUTH_SUCCESS;
		}

		@Override
		public String getName() {
			return "auth_success";
		}

		@Override
		public void writeImpl() {
			this.writeBytes(encryptKey);
		}

	}
}
