package simulator;

import support.Entity;

public abstract class Node implements Entity {
	protected Address addr;
	protected boolean failed = false;
	
	@Override
	public String getName() {
		return addr.toString();
	}
	
	@Override
	public long getAddress() {
		return addr.getAddress();
	}
	
	public void fail() {
		failed = true;
	}
	
	public boolean failed() {
		return failed;
	}
}
