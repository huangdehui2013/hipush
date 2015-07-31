package hipush.core;

public class Errors {

	@SuppressWarnings("serial")
	public static class HiError extends RuntimeException {
		public HiError(String message) {
			super(message);
		}
	}

	@SuppressWarnings("serial")
	public static class MessageUndefinedError extends HiError {
		public MessageUndefinedError(String message) {
			super(message);
		}
	}

	@SuppressWarnings("serial")
	public static class MessageFormatError extends HiError {

		public MessageFormatError(String message) {
			super(message);
		}

	}

}
