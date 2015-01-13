package simulator;

public class Measure {
	double[] portsin, portsout;
	int k, length;
	
	public Measure(int k, int length) {
		this.k = k;
		this.length = length;
		portsin = new double[length + 1];
		portsout = new double[length + 1];
	}
	
	public void clear() {
		for (int i = 1; i <= length; i++)
			if (ok(i)) {
				portsin[i] = 0;
				portsout[i] = 0;
			}
	}
	
	public double[] getMeasurementIn() {
		return portsin;
	}
	
	public double[] getMeasurementOut() {
		return portsout;
	}
	
	public double getMeasurementInOf(int i) {
		return portsin[i];
	}
	
	public double getMeasurementOutOf(int i) {
		return portsout[i];
	}
	
	public void incrementOut(int i, int size) {
		if (ok(i))
			portsout[i] += size;
	}
	
	public void incrementIn(int i, int size) {
		if (ok(i))
			portsin[i] += size;
	}
	
	public void disable(int i) {
		portsin[i] = -1;
		portsout[i] = -1;
	}
	
	public void enable(int i) {
		portsin[i] = 0;
		portsout[i] = 0;
	}
	
	public boolean ok(int i) {
		return portsin[i] >= 0 && portsout[i] >= 0;
	}
	
}
