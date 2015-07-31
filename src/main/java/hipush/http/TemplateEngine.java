package hipush.http;

import java.io.StringWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

public class TemplateEngine {

	private final static Logger LOG = LoggerFactory
			.getLogger(TemplateEngine.class);

	private final static String DEFAULT_NAME = "error.mus";
	private MustacheFactory factory;

	public TemplateEngine(String root) {
		factory = new DefaultMustacheFactory(root);
	}
	
	private Mustache getTemplate0(String name) {
		return factory.compile(name);
	}

	private Mustache getTemplate(String name) {
		Mustache template = getTemplate0(name);
		if (template == null) {
			LOG.error(String.format("template %s not exists"), name);
			template = getTemplate0(DEFAULT_NAME);
		}
		return template;
	}

	public StringBuffer renderTemplate(String name, Object context) {
		Mustache template = getTemplate(name);
		StringWriter writer = new StringWriter();
		template.execute(writer, context);
		return writer.getBuffer();
	}

}
