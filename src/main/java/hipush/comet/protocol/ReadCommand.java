package hipush.comet.protocol;

import hipush.core.Charsets;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public abstract class ReadCommand {

	private ByteBuf in;
	private ChannelHandlerContext ctx;

	public ReadCommand(ByteBuf in) {
		this.in = in;
	}

	public boolean isInternal() {
		return false;
	}

	public ChannelHandlerContext getCtx() {
		return ctx;
	}

	public void setCtx(ChannelHandlerContext ctx) {
		this.ctx = ctx;
	}

	public abstract byte getType();

	public abstract void readImpl();

	public abstract String getName();

	public abstract boolean isValid();

	public byte readByte() {
		return this.in.readByte();
	}

	public short readShort() {
		return this.in.readShort();
	}

	public int readInt() {
		return this.in.readInt();
	}

	public long readLong() {
		return this.in.readLong();
	}

	public ByteBuf readBytes(byte[] bytes) {
		return this.in.readBytes(bytes);
	}

	public String readStr() {
		short len = readShort();
		byte[] bytes = new byte[len];
		this.in.readBytes(bytes);
		return new String(bytes, Charsets.utf8);
	}

}
