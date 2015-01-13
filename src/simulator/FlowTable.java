package simulator;

import java.util.Hashtable;

import support.Simusys;

public class FlowTable {
	public Hashtable<FlowId, Link> out = new Hashtable<FlowId, Link>();
	public Hashtable<FlowId, Link> in = new Hashtable<FlowId, Link>();
	public Hashtable<FlowId, Long> states = new Hashtable<FlowId, Long>();
	public Hashtable<FlowId, Long> stamps = new Hashtable<FlowId, Long>();
	public int[][] inter_out; // [1, k/2] refer to up ports, [k/2, k] refer to down ports| (k/2 * k)
	public int[][] intra_out; // [1, k/2] refer to up ports, [k/2, k] refer to down ports| (k/2 * k/2)| only useful for 
	public int[] ins;
	public int[] hosts;
	public int k;
	public Address addr;
	public static final long TIMEOUT = (long) (0.05 * Simusys.ticePerSecond());
	
	public FlowTable(int k, Address addr) {
		this.k = k;
		this.addr = addr;
		
		inter_out = new int[k / 2 + 1][];
		ins = new int[k / 2 + 1];
		hosts = new int[k / 2 + 1];
		intra_out = new int[k / 2 + 1][];
		for (int i = 1; i <= k / 2; i++) {
			inter_out[i] = new int[k + 1];
			intra_out[i] = new int[k / 2 + 1];
		}
	}
	
	public void addOutFlow(FlowId fid, Link o) {
		out.put(fid, o);
		// states.put(fid, Simusys.time());
	}
	
	public void addInFlow(FlowId fid, Link i, long timestamp) {
		stamps.put(fid, (Long) timestamp);
		in.put(fid, i);
		// states.put(fid, Simusys.time());
	}
	
	public Link getFlowOutPort(FlowId fid) {
		// states.put(fid, Simusys.time());
		return out.get(fid);
	}
	
	public Link getFlowInPort(FlowId fid) {
		// states.put(fid, Simusys.time());
		return in.get(fid);
	}
	
	public void touch(FlowId fid) {
		states.put(fid, Simusys.time());
	}
	
	public void deleteFlow(FlowId fid) {
		if (in.contains(fid) && out.containsKey(fid)) {
			in.remove(fid);
			out.remove(fid);
			states.remove(fid);
			return;
		}
		
		System.err.println("delete a flow from non-containned id");
	}

	public boolean contains(FlowId fid) {
		return in.containsKey(fid);
	}

	public void increaseOutFlowIntra(int i, int edge) {
		intra_out[i][edge]++;
		intra_out[i][0]++;
	}
	
	public int size() {
		return in.size();
	}

}
