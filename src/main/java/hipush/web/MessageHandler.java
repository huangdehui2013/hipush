package hipush.web;

import hipush.http.BaseHandler;
import hipush.http.Branch;
import hipush.http.Form;
import hipush.http.Root;
import hipush.services.JobService;
import hipush.services.JobStat;
import hipush.uuid.JobId;
import hipush.uuid.MessageId;
import io.netty.channel.ChannelHandlerContext;

@Root(path="/message")
public class MessageHandler extends BaseHandler {

	@Branch(path="/click", methods={"GET", "POST"})
	public void click(ChannelHandlerContext ctx, Form form) {
		String jobId = form.getString("job_id");
		String messageId = form.getString("message_id");
		if(!JobId.isValid(jobId)) {
			form.raise("illegal job_id");
			return;
		}
		if(!MessageId.isValid(messageId)) {
			form.raise("illegal message_id");
			return;
		}
		// 检验messageId和jobId的关系需要额外存储空间，所以暂不提供
		JobStat stat = new JobStat(jobId);
		stat.incrClickCount();
		JobService.getInstance().incrJobStat(stat);
		this.renderOk(ctx);
	}
	
}
