package hipush.async;

public interface IAsyncTask {

	public void runAsync();

	public void afterOk();
	
	public void afterError(Exception e);
	
	public String getName();

}
