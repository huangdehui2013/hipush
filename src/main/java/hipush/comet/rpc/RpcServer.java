package hipush.comet.rpc;

import hipush.http.HttpServer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcServer {
	private final static Logger LOG = LoggerFactory.getLogger(RpcServer.class);

	private final static RpcServer instance = new RpcServer();
	private final HttpServer http = new HttpServer(1, 1)
			.build(new RpcHandler());
	private Thread thread;

	public static RpcServer getInstance() {
		return instance;
	}

	public void start(final String ip, final int port) {
		thread = new Thread() {
			public void run() {
				http.start(ip, port);
			}
		};
		thread.start();
	}
	
	public void stop() {
		LOG.warn("rpc server stopped");
		if(thread != null) {
			thread.interrupt();
		}
	}

}
