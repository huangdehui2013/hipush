package hipush.comet.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.util.List;

enum State {
	READ_LEN, READ_BODY
}

public class CommandDecoder extends ReplayingDecoder<State> {

	public CommandDecoder() {
		super(State.READ_LEN);
	}

	int length;

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in,
			List<Object> out) throws Exception {
		ByteBuf body;
		switch (state()) {
		case READ_LEN:
			length = in.readShort();
			checkpoint(State.READ_BODY);
		case READ_BODY:
			body = in.readBytes(length);
			checkpoint(State.READ_LEN);
			ReadCommand command = CommandBuilder.build(body);
			out.add(command);
			// 释放空间，避免内存泄露
			body.release();
			length = 0;
		}

	}

}
