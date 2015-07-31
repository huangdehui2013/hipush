package hipush.comet;

import hipush.comet.protocol.ScheduleCommand;
import hipush.core.ScheduleManager;

public class ScheduleUtils {

	public static void periodic(int delay, int period, final ScheduleCommand command) {
		ScheduleManager.getInstance().periodic(delay, period, new Runnable() {

			@Override
			public void run() {
				MessageProcessor.getInstance().putMessage(command);
			}
			
		});
	}
	
	public static void delay(int delay, final ScheduleCommand command) {
		ScheduleManager.getInstance().delay(delay, new Runnable() {

			@Override
			public void run() {
				MessageProcessor.getInstance().putMessage(command);
			}
			
		});
	}
	
}
