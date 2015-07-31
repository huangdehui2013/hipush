package hipush.http;

public class Errors {
	
	private final static int BADARGUMENT_ERROR_CODE = 1024;
	private final static int SERVER_ERROR_CODE = 1025;
	private final static int NOTFOUND_ERROR_CODE = 1026;
	private final static int METHOD_NOTALLOWED_ERROR_CODE = 1027;
	private final static int AUTH_ERROR_CODE = 1028;

	@SuppressWarnings("serial")
	public static class APIError extends RuntimeException {
		private int errorCode;
		private int httpErrorCode;
		
		public APIError(int errorCode, String reason, int httpErrorCode) {
			super(reason);
			this.errorCode = errorCode;
			this.httpErrorCode = httpErrorCode;
		}
		
		public String getReason() {
			return this.getMessage();
		}
		
		public int getErrorCode() {
			return this.errorCode;
		}
		
		public int getHttpErrorCode() {
			return this.httpErrorCode;
		}
	}
	
	@SuppressWarnings("serial")
	public static class BadArgumentError extends APIError {

		public BadArgumentError(String reason) {
			super(BADARGUMENT_ERROR_CODE, reason, 400);
		}
		
	}
	
	@SuppressWarnings("serial")
	public static class ServerError extends APIError {

		public ServerError(String reason) {
			super(SERVER_ERROR_CODE, reason, 500);
		}
		
	}
	
	@SuppressWarnings("serial")
	public static class NotFoundError extends APIError {

		public NotFoundError(String reason) {
			super(NOTFOUND_ERROR_CODE, reason, 404);
		}
		
	}
	
	@SuppressWarnings("serial")
	public static class AuthError extends APIError {
		public AuthError(String reason) {
			super(AUTH_ERROR_CODE, reason, 401);
		}
	}
	
	@SuppressWarnings("serial")
	public static class MethodNotAllowedError extends APIError {
		
		public MethodNotAllowedError(String reason) {
			super(METHOD_NOTALLOWED_ERROR_CODE, reason, 405);
		}
		
	}
	
}
