package hipush.comet.protocol;


public abstract class InternalCommand extends ReadCommand {

	public InternalCommand() {
		super(null);
	}

	@Override
	public void readImpl() {

	}

	@Override
	public boolean isInternal() {
		return true;
	}
	
	@Override
	public boolean isValid() {
		return true;
	}
}
