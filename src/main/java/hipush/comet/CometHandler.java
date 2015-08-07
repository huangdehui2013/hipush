package hipush.comet;

import hipush.comet.protocol.Internals.ClientOfflineCommand;
import hipush.comet.protocol.Internals.ResendPendingsCommand;
import hipush.comet.protocol.Internals.ResendUnackedMessagesCommand;
import hipush.comet.protocol.MessageDefine;
import hipush.comet.protocol.Outputs.ErrorResponse;
import hipush.comet.protocol.Outputs.OkResponse;
import hipush.comet.protocol.ReadCommand;
import hipush.comet.protocol.WriteResponse;
import hipush.core.Constants;
import hipush.core.ContextUtils;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Sharable
public class CometHandler extends ChannelHandlerAdapter {

	private final static Logger LOG = LoggerFactory
			.getLogger(CometHandler.class);
	private final static AtomicInteger channels = new AtomicInteger(0);

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		if (channels.incrementAndGet() > CometServer.getInstance().getConfig()
				.getMaxConnections()) {
			ctx.close();
		}
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		channels.decrementAndGet();
		ClientInfo client = ContextUtils.getClient(ctx);
		if (client == null) {
			return;
		}
		ClientOfflineCommand command = new ClientOfflineCommand();
		command.setCtx(ctx);
		MessageProcessor.getInstance().putMessage(command);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
			throws Exception {
		if (msg instanceof ReadCommand) {
			ReadCommand command = ((ReadCommand) msg);
			if (!command.isValid()) {
				ctx.writeAndFlush(ErrorResponse
						.newBadArgsError("message format illegal"));
				return;
			}
			if (command.getType() == MessageDefine.Read.CMD_HEARTBEAT) {
				ctx.writeAndFlush(new OkResponse());
				ClientInfo client = ContextUtils.getClient(ctx);
				long now = System.currentTimeMillis();
				if (client != null
						&& !client.isEmpty()
						&& now > client.getLastResendTs() + Constants.MESSAGE_UNACKED_CHECK_PERIOD
						&& ctx.channel().isActive()
						&& ctx.channel().isWritable()) {
					MessageProcessor.getInstance().putMessage(
							new ResendUnackedMessagesCommand(ctx));
				}
				return;
			}
			command.setCtx(ctx);
			if (MessageProcessor.getInstance().isFull()) {
				LOG.warn("message queue is full!server is under ddos attacking!");
			}
			MessageProcessor.getInstance().putMessage(command);
		} else {
			LOG.error(String.format("what the fuck msg arrived from %s!", ctx
					.channel().remoteAddress()));
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		ctx.close();
		LOG.warn("exception occured", cause.getMessage());
	}

	@Override
	public void channelWritabilityChanged(ChannelHandlerContext ctx)
			throws Exception {
		if (ctx.channel().isWritable()) {
			List<WriteResponse> messages = ContextUtils
					.clearPendingMessages(ctx);
			if (!messages.isEmpty()) {
				ResendPendingsCommand command = new ResendPendingsCommand(
						messages);
				command.setCtx(ctx);
				MessageProcessor.getInstance().putMessage(command);
			}
		}
	}

}
