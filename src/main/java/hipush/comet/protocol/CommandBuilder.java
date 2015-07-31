package hipush.comet.protocol;

import hipush.comet.protocol.Inputs.AuthCommand;
import hipush.comet.protocol.Inputs.HeartBeatCommand;
import hipush.comet.protocol.Inputs.MessageAckCommand;
import hipush.comet.protocol.Inputs.MessageListCommand;
import hipush.comet.protocol.Inputs.SubscribeCommand;
import hipush.comet.protocol.Inputs.TopicListCommand;
import hipush.comet.protocol.Inputs.UnsubscribeCommand;
import hipush.core.Errors.MessageFormatError;
import hipush.core.Errors.MessageUndefinedError;
import io.netty.buffer.ByteBuf;

public class CommandBuilder {

	public static ReadCommand build(ByteBuf body) {
		int cmdType = body.readByte();
		ReadCommand result = null;
		switch (cmdType) {
		case MessageDefine.Read.CMD_AUTH:
			result = new AuthCommand(body);
			break;
		case MessageDefine.Read.CMD_TOPIC_LIST:
			result = new TopicListCommand(body);
			break;
		case MessageDefine.Read.CMD_SUBSCRIBE:
			result = new SubscribeCommand(body);
			break;
		case MessageDefine.Read.CMD_UNSUBSCRIBE:
			result = new UnsubscribeCommand(body);
			break;
		case MessageDefine.Read.CMD_MESSAGE_LIST:
			result = new MessageListCommand(body);
			break;
		case MessageDefine.Read.CMD_HEARTBEAT:
			result = new HeartBeatCommand(body);
			break;
		case MessageDefine.Read.CMD_MESSAGE_ACK:
			result = new MessageAckCommand(body);
			break;
		default:
			throw new MessageUndefinedError("message type not defined");
		}
		try {
			result.readImpl();
		} catch (IndexOutOfBoundsException e) {
			throw new MessageFormatError("message too short");
		}
		if(body.readableBytes() > 0) {
			throw new MessageFormatError("message too long");
		}
		return result;
	}

}
