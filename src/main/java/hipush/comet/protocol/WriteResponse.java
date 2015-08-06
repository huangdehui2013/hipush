package hipush.comet.protocol;

import hipush.core.Charsets;
import io.netty.buffer.ByteBuf;

public abstract class WriteResponse {

	private ByteBuf out;

	public ByteBuf getOut() {
		return out;
	}

	public void setOut(ByteBuf out) {
		this.out = out;
	}

	public abstract byte getType();
	
	public abstract String getName();

	public abstract void writeImpl();

	public void writeStr(String s) {
		byte[] bytes = s.getBytes(Charsets.utf8);
		this.writeBytes(bytes);
	}
	
	public void writeBytes(byte[] bs) {
		out.writeShort(bs.length);
		out.writeBytes(bs);
	}

	public void writeByte(byte b) {
		out.writeByte(b);
	}

	public void writeShort(short c) {
		out.writeShort(c);
	}

	public void writeInt(int c) {
		out.writeInt(c);
	}
	
	public void writeLong(long c) {
		out.writeLong(c);
	}
}
