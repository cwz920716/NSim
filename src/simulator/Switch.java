package simulator;

public interface Switch {
	public double[] portmeasurmentIn(boolean up);
	public double[] portmeasurmentOut(boolean up);
	public Address[] peers(boolean up);
	public void disableLink(Link l);
}
