package hipush.core;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandDefine {
	private final static Logger LOG = LoggerFactory.getLogger(CommandDefine.class);

	private final static CommandDefine instance = new CommandDefine();
	
	public static CommandDefine getInstance() {
		return instance;
	}
	
	private Options options = new Options();

	@SuppressWarnings("static-access")
	public CommandDefine addStringOption(String name, String desc) {
		Option option = OptionBuilder.withArgName(name).hasArg(true)
				.withDescription(desc).create(name);
		options.addOption(option);
		return this;
	}

	public CommandDefine addBooleanOption(String name, String desc) {
		Option option = new Option(name, desc);
		options.addOption(option);
		return this;
	}

	public CommandLine getCommandLine(String[] args) {
		CommandLineParser parser = new BasicParser();
		try {
			CommandLine line = parser.parse(options, args);
			return line;
		} catch (ParseException exp) {
			LOG.error("illegal arguments for boostrap server");
			System.exit(-1);
		}
		return null;
	}

}
