package hipush.admin;

import hipush.core.Helpers;
import hipush.core.Helpers.Pager;
import hipush.db.JobTypeModel;
import hipush.http.BaseHandler;
import hipush.http.Branch;
import hipush.http.Form;
import hipush.http.Root;
import hipush.services.AppInfo;
import hipush.services.AppService;
import hipush.services.JobInfo;
import hipush.services.JobService;
import hipush.uuid.JobId;
import io.netty.channel.ChannelHandlerContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Root(path = "/job")
public class JobHandler extends BaseHandler {

	@Branch(path = "/genId", methods = { "GET" })
	public void nextId(ChannelHandlerContext ctx, Form form) {
		String appKey = form.getString("app_key");
		String type = form.getString("type");
		String name = form.getString("name");
		String sign = form.getString("sign");
		AppInfo app = AppService.getInstance().getApp(appKey);
		if (app == null) {
			form.raise("app_key not exists");
			return;
		}
		if (!Helpers.checkSign(app.getSecret(), sign, "app_key=" + appKey,
				"name=" + name, "type=" + type)) {
			form.raise("signature mismatch");
			return;
		}
		if (JobService.getInstance().getJobType(type) == null) {
			form.raise("job type not defined");
			return;
		}
		String jobId = JobId.nextId();
		JobInfo job = new JobInfo(jobId, name, type, System.currentTimeMillis());
		JobService.getInstance().saveJobInfo(job);
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("job_id", jobId);
		this.renderOk(ctx, result);
	}

	@Branch(path = "/list", methods = { "GET" }, isLoginRequired = true)
	public void showJobs(ChannelHandlerContext ctx, Form form) {
		int pageSize = form.getInteger("page_size", 20);
		int currentPage = form.getInteger("page", 0);
		int total = JobService.getInstance().getJobsCount();
		Pager pager = new Helpers.Pager(pageSize, total);
		pager.setCurrentPage(currentPage);
		pager.setUrlPattern("/job/list?page_size=%s&page=%s");
		List<JobInfo> jobs = JobService.getInstance().getJobs(pager.offset(),
				pager.limit());
		Map<String, String> jobTypes = JobService.getInstance().getJobTypes();
		for (JobInfo job : jobs) {
			job.setTypeName(jobTypes.get(job.getType()));
		}
		Map<String, Object> context = new HashMap<String, Object>();
		if (pager.visable()) {
			context.put("pager", pager);
		}
		context.put("jobs", jobs);
		this.renderTemplate(ctx, "jobs.mus", context);
	}

	@Branch(path = "/list_types", methods = { "GET" }, isLoginRequired = true)
	public void showJobTypes(ChannelHandlerContext ctx, Form form) {
		List<JobTypeModel> jobTypes = JobService.getInstance().getAllJobTypesFromDb();
		Map<String, Object> context = new HashMap<String, Object>();
		context.put("jobTypes", jobTypes);
		this.renderTemplate(ctx, "job_types.mus", context);
	}

	@Branch(path = "/register_type", methods = { "GET", "POST" }, isLoginRequired = true)
	public void updateJobType(ChannelHandlerContext ctx, Form form) {
		if (form.isGet()) {
			this.renderTemplate(ctx, "register_job_type.mus");
			return;
		}
		String key = form.getString("key");
		String name = form.getString("name");
		JobService.getInstance().updateJobType(key, name);
		this.redirect(ctx, "/job/list_types");
	}

	@Branch(path = "/del_type", methods = { "GET", "POST" }, isLoginRequired = true)
	public void removeJobType(ChannelHandlerContext ctx, Form form) {
		String key = form.getString("key");
		JobService.getInstance().delJobType(key);
		this.redirect(ctx, "/job/list_types");
	}

}
