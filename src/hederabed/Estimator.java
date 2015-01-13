package hederabed;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Vector;

import simulator.Address;
import simulator.FlowId;

public class Estimator {
	public double[][] Mdemand;
	public boolean[][] Mconverged;
	public int k;
	public int h;
	private boolean changed = false;
	
	public Estimator(int h, int k) {
		Mdemand = new double[h + 1][];
		Mconverged = new boolean[h + 1][];
		this.k = k;
		this.h = h;
		
		for (int i = 1; i <= h; i++) {
			Mdemand[i] = new double[h + 1];
			Mconverged[i] = new boolean[h + 1];
		}
	}
	
	public void ESTIMATE(Vector<FlowId> allflows) {
		do {
			changed = false;
			for (int i = 1; i <= h; i++)
				EST_SRC(i, allflows);
			for (int i = 1; i <= h; i++)
				EST_DST(i, allflows);
		} while(changed);
	}
	
	public void EST_SRC(int src, Vector<FlowId> allflows) {
		double df = 0;
		int nu = 0;
		Vector<FlowId> inflows = new Vector<FlowId>();
		
		for (Enumeration<FlowId> enums = allflows.elements(); enums.hasMoreElements(); ) {
			FlowId fid = enums.nextElement();
			if (map(fid.src) == src) {
				int dst = map(fid.dest);
				inflows.add(fid);
				if (Mconverged[src][dst]) {
					df += Mdemand[src][dst];
				} else {
					nu++;
				}
			}
		}
		
		double es = (1.0 - df) / nu;
		for (Enumeration<FlowId> enums = inflows.elements(); enums.hasMoreElements(); ) {
			FlowId fid = enums.nextElement();
			int dst = map(fid.dest);
			if (!Mconverged[src][dst]) {
				if (es != Mdemand[src][dst])
					changed = true;
				Mdemand[src][dst] = es;
			}
		}
	}
	
	public void EST_DST(int dst, Vector<FlowId> allflows) {
		HashMap<FlowId, Boolean> rl = new HashMap<FlowId, Boolean>();
		Vector<FlowId> inflows = new Vector<FlowId>();
		double dt = 0, ds = 0;
		int nr = 0;
		
		for (Enumeration<FlowId> enums = allflows.elements(); enums.hasMoreElements(); ) {
			FlowId fid = enums.nextElement();
			if (map(fid.dest) == dst) {
				int src = map(fid.src);
				inflows.add(fid);
				rl.put(fid, true);
				dt += Mdemand[src][dst];
				nr++;
			}
		}
		if (dt <= 1.0) {
			return;
		}
		
		double es = 1.0 / nr;
		boolean setFalse = false;
		do {
			nr = 0;
			setFalse = false;
			for (Enumeration<FlowId> enums = inflows.elements(); enums.hasMoreElements(); ) {
				FlowId fid = enums.nextElement();
				if (rl.get(fid)) {
					int src = map(fid.src);
					if (Mdemand[src][dst] < es) {
						ds += Mdemand[src][dst];
						rl.put(fid, false);
						setFalse = true;
					} else {
						nr++;
					}
				}
			}
			es = (1.0 - ds) / nr;
		} while (setFalse);
		
		for (Enumeration<FlowId> enums = inflows.elements(); enums.hasMoreElements(); ) {
			FlowId fid = enums.nextElement();
			if (rl.get(fid)) {
				int src = map(fid.src);
				if (es != Mdemand[src][dst])
					changed = true;
				Mdemand[src][dst] = es;
				Mconverged[src][dst] = true;
			}
		}
	}
	
	public double getDemand(FlowId fid) {
		return Mdemand[map(fid.src)][map(fid.dest)];
	}

	private int map(long src) {
		Address addr = new Address(src);
		return (addr.getPod() - 1) * (k * k) / 4 + (addr.getEdge() - 1) * k / 2 + addr.getHost();
	}
	
	@Override
	public String toString() {
		String res = "";
		
		for (int i = 1; i <= h; i++) {
			for (int j = 1; j <= h; j++)
				res += Mdemand[i][j] + " ";
			res += System.lineSeparator();
		}
		
		return res;
	}
	
	public static void main(String[] args) {
		Vector<FlowId> flows = new Vector<FlowId>();
		flows.add(new FlowId(1, 2, (short) 1, (short) 1));
		flows.add(new FlowId(1, 3, (short) 1, (short) 1));
		flows.add(new FlowId(1, 4, (short) 1, (short) 1));
		flows.add(new FlowId(2, 1, (short) 1, (short) 1));
		flows.add(new FlowId(2, 1, (short) 1, (short) 1));
		flows.add(new FlowId(2, 3, (short) 1, (short) 1));
		flows.add(new FlowId(3, 1, (short) 1, (short) 1));
		flows.add(new FlowId(3, 4, (short) 1, (short) 1));
		flows.add(new FlowId(4, 2, (short) 1, (short) 1));
		flows.add(new FlowId(4, 2, (short) 1, (short) 1));
		
		Estimator e = new Estimator(4, 4);
		e.ESTIMATE(flows);
		System.out.println(e);
	}
}
