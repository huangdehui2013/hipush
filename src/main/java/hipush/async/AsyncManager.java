package hipush.async;

import hipush.comet.MessageProcessor;
import hipush.comet.protocol.Internals.AsyncFinishedCommand;
import hipush.services.MeasureService;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsyncManager {
	private final static Logger LOG = LoggerFactory
			.getLogger(AsyncManager.class);

	private final static AsyncManager instance = new AsyncManager();

	public static AsyncManager getInstance() {
		return instance;
	}

	public void initWith(int threadCount) {
		this.executors = Executors.newFixedThreadPool(threadCount);
	}

	private Executor executors;

	public void execute(final IAsyncTask runner) {
		executors.execute(new Runnable() {

			@Override
			public void run() {
				long start = System.currentTimeMillis();
				Exception error = null;
				try {
					runner.runAsync();
				} catch (Exception e) {
					LOG.error("async running error", e);
					error = e;
				}
				long end = System.currentTimeMillis();
				MeasureService.getInstance().sampleIO(runner.getName(),
						System.nanoTime(), (end - start));
				LOG.info(String.format("execute async runner %s cost %s",
						runner.getName(), end - start));
				MessageProcessor.getInstance().putMessage(
						new AsyncFinishedCommand(runner, error));
			}

		});
	}

}
