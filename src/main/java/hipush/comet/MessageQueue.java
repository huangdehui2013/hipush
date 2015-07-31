package hipush.comet;

import hipush.comet.protocol.ReadCommand;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageQueue {
	private final static Logger LOG = LoggerFactory.getLogger(MessageQueue.class);
	private final int QUEUE_MAX_SIZE = 100000;
	private BlockingQueue<ReadCommand> queue = new LinkedBlockingQueue<ReadCommand>(QUEUE_MAX_SIZE);
	
	public void put(ReadCommand message) {
		try {
			queue.put(message);
		} catch (InterruptedException e) {
			LOG.error("put message to queue is interrupted", e);
		}
	}
	
	public ReadCommand peek() {
		return queue.peek();
	}
	
	public ReadCommand take() {
		try {
			return queue.take();
		} catch (InterruptedException e) {
			LOG.error("take message from queue is interrupted", e);
		}
		return null;
	}
	
	public int size() {
		return queue.size();
	}
	
	public boolean isFull() {
		return queue.size() > QUEUE_MAX_SIZE;
	}
	
}
