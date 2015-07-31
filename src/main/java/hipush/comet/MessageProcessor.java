package hipush.comet;

import hipush.comet.protocol.ReadCommand;
import hipush.services.MeasureService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageProcessor {
	private final static Logger LOG = LoggerFactory
			.getLogger(MessageProcessor.class);

	public final static MessageProcessor instance = new MessageProcessor();

	public static MessageProcessor getInstance() {
		return instance;
	}

	private MessageQueue queue = new MessageQueue();
	private CommandExecutor executor = new CommandExecutor();
	private Thread thread;
	private boolean stop;

	public void putMessage(ReadCommand command) {
		queue.put(command);
	}

	public boolean isFull() {
		return queue.isFull();
	}

	public int pendings() {
		return queue.size();
	}

	public void start() {
		thread = new Thread() {

			public void run() {
				loop();
			}
		};
		thread.start();
	}

	public void loop() {
		while (true) {
			if (queue.peek() == null && stop) {
				LOG.warn("message processor stopped");
				break;
			}
			ReadCommand command = queue.take();
			if (command == null) {
				continue;
			}
			long start = System.nanoTime();
			try {
				executor.execute(command);
			} catch (Exception e) {
				LOG.error(
						String.format("execute command name=%s",
								command.getName()), e);
			}
			long end = System.nanoTime();
			MeasureService.getInstance().sampleMain(command.getName(), start,
					end - start);
			LOG.info(String.format(
					"execute command name=%s cost %s nano seconds",
					command.getName(), end - start));
		}
	}

	public void stop() {
		stop = true;
	}

}
