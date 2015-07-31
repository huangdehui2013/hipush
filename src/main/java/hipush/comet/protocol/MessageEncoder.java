package hipush.comet.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Sharable
public class MessageEncoder extends MessageToMessageEncoder<WriteResponse> {
	private final static Logger LOG = LoggerFactory
			.getLogger(MessageEncoder.class);

	@Override
	protected void encode(ChannelHandlerContext ctx, WriteResponse msg,
			List<Object> out) throws Exception {
		ByteBuf buf = PooledByteBufAllocator.DEFAULT.directBuffer();
		msg.setOut(buf);
		buf.writeByte(msg.getType());
		msg.writeImpl();
		ByteBuf lenBuf = ctx.alloc().buffer(2).writeShort(buf.readableBytes());
		out.add(lenBuf);
		out.add(buf);
		if (msg.getType() != MessageDefine.Write.MSG_OK) {
			LOG.info(String.format("message name=%s is sent", msg.getName()));
		}
	}

}
