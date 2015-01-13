package dardbed;

import java.util.ArrayList;
import java.util.Hashtable;


import simulator.Address;
import simulator.FlowId;

public class SourceRouting {
	private static Hashtable<FlowId, Path> f2p = new Hashtable<FlowId, Path>();
	private static Hashtable<SDPair, Integer> state = new Hashtable<SDPair, Integer>();
	
	public static void addLinkN(SDPair p) {
		if (state.containsKey(p)) {
			int i = state.get(p);
			state.put(p, i + 1);
		} else {
			state.put(p, 1);
		}
	}


	public static void removeLinkN(SDPair p) {
		if (state.containsKey(p)) {
			int i = state.get(p);
			if (i > 0)
				state.put(p, i - 1);
		} else {
			
		}
	}
	
	public static int getLinkN(SDPair p) {
		if (!state.containsKey(p)) {
			state.put(p, 0);
			return 0;
		}
		
		return state.get(p);
	}
	
	public static void addElephantFlowPath(FlowId fid, Path path) {
		if (f2p.containsKey(fid))
			removeFlowPath(fid);
		
		f2p.put(fid, path);
		ArrayList<Address> nodes = path.nodes;
		
		for (int i = 0; i < nodes.size() - 1; i++) {
			Address cur = nodes.get(i);
			Address next = nodes.get(i + 1);
			
			SourceRouting.addLinkN(new SDPair(cur.getAddress(), next.getAddress())); 
		}
	}
	
	public static void removeFlowPath(FlowId fid) {
		Path path = f2p.get(fid);
		if (path == null)
			return;
		
		ArrayList<Address> nodes = path.nodes;
		
		for (int i = 0; i < nodes.size() - 1; i++) {
			Address cur = nodes.get(i);
			Address next = nodes.get(i + 1);
			
			SourceRouting.removeLinkN(new SDPair(cur.getAddress(), next.getAddress())); 
		}
	}


	public static Path getPath(FlowId fid) {
		return f2p.get(fid);
	}


	public static int getPathN(Path path) {
		ArrayList<Address> nodes = path.nodes;
		int maxN = -1;
		
		for (int i = 1; i < nodes.size() - 2; i++) {
			Address cur = nodes.get(i);
			Address next = nodes.get(i + 1);
			
			int N = SourceRouting.getLinkN(new SDPair(cur.getAddress(), next.getAddress())); 
			if (maxN == -1 || N > maxN)
				maxN = N;
		}
		
		return maxN;
	}
	
	public static double getPathS(Path path) {
		double S = 0;
		int maxN = getPathN(path);
		
		if (maxN > 0) {
			S = 1.0 / (double) maxN;
		}
		
		return S;
	}

}
