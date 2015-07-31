package hipush.http;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpServer {
	private final static Logger LOG = LoggerFactory.getLogger(HttpServer.class);

	private ServerBootstrap bootstrap = new ServerBootstrap();
	private Channel serverChannel;
	private int bossThreads;
	private int workerThreads;
	private EventLoopGroup m_bossGroup;
	private EventLoopGroup m_workerGroup;

	public HttpServer(int bossThreads, int workerThreads) {
		this.bossThreads = bossThreads;
		this.workerThreads = workerThreads;
	}

	public HttpServer build(final HttpServerHandler handler) {
		m_bossGroup = new NioEventLoopGroup(bossThreads);
		m_workerGroup = new NioEventLoopGroup(workerThreads);
		bootstrap.group(m_bossGroup, m_workerGroup)
				.channel(NioServerSocketChannel.class)
				.childHandler(new ChannelInitializer<SocketChannel>() {
					@Override
					public void initChannel(SocketChannel ch) throws Exception {
						ChannelPipeline pipeline = ch.pipeline();
						pipeline.addLast(new IdleStateHandler(0, 0, 10));
						pipeline.addLast(new TimeoutHandler());
						pipeline.addLast(new HttpServerCodec());
						pipeline.addLast(new ChunkedWriteHandler());
						pipeline.addLast(new HttpObjectAggregator(65536));
						pipeline.addLast(handler);
					}
				}).option(ChannelOption.SO_BACKLOG, 1024)
				.option(ChannelOption.SO_REUSEADDR, true)
				.option(ChannelOption.TCP_NODELAY, true);
		return this;
	}

	public void start(String ip, int port) {
		try {
			serverChannel = bootstrap.bind(ip, port).sync().channel();
			serverChannel.closeFuture().sync();
		} catch (Exception ex) {
			LOG.error("http server bind error", ex);
			System.exit(-1);
		} finally {
			m_bossGroup.shutdownGracefully();
			m_workerGroup.shutdownGracefully();
			LOG.error("http server is stopped");
		}
	}
	
	public void shutdown() {
		if(serverChannel!= null) {
			serverChannel.close();
		}
	}

}
