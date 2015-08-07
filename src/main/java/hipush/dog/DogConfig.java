package hipush.dog;

import hipush.core.Config;

public class DogConfig extends Config {

	private String templateRoot;
	private String upstreamPath;

	public String getTemplateRoot() {
		return templateRoot;
	}

	public void setTemplateRoot(String templateRoot) {
		this.templateRoot = templateRoot;
	}

	public String getUpstreamPath() {
		return upstreamPath;
	}

	public void setUpstreamPath(String upstreamPath) {
		this.upstreamPath = upstreamPath;
	}

}
