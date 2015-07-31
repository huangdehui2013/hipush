package hipush.http;

import io.netty.channel.ChannelHandlerContext;

@Root(path = "")
public class DefaultHandler extends BaseHandler {

	@Branch(path = "", methods = { "GET", "POST", "DELETE", "PUT" })
	public void raise404(ChannelHandlerContext ctx, Form form) {
		throw new Errors.NotFoundError(String.format(
				"API not found for path=%s", form.getPath()));
	}

}
