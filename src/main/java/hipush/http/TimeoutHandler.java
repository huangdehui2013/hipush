package hipush.http;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimeoutHandler extends ChannelHandlerAdapter {
	private final static Logger LOG = LoggerFactory.getLogger(TimeoutHandler.class);

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt)
			throws Exception {
		if (evt instanceof IdleStateEvent) {
			IdleState e = ((IdleStateEvent) evt).state();
			if (e == IdleState.ALL_IDLE) {
				LOG.warn("channel is idling for too long, close it " + ctx);
				ctx.close();
			}
		}
	}
}
