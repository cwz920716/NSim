package hederabed;

import hederabed.Hedera.LS;

import java.util.HashMap;
import java.util.Vector;

import simulator.Address;
import simulator.FlowId;

public class GFF {
	public static Vector<FlowId> allflows = new Vector<FlowId>();
	public static HashMap<FlowId, Route> flowRoutes = new HashMap<FlowId, Route>();
	public static int k;
	public static Estimator es;
	
	public static class Route {
		public Route(int i, int j) {
			aggr = i;
			core = j;
		}
		
		public int aggr;
		public int core;
		
		public String toString() {
			return "a" + aggr + " -> " + "c" + core;
		}
	}
	
	public static void INIT(int k) {
		GFF.k = k;
		es  = new Estimator(k * k * k / 4, k);
	}
	
	public static Route getFlow(FlowId fid) {
		if (flowRoutes.containsKey(fid)) {
			return flowRoutes.get(fid);
		}
		
		return null;
	}
	
	public static Route addNewFlow(FlowId fid) {
		if (flowRoutes.containsKey(fid)) {
			return flowRoutes.get(fid);
		}
		
		if (!allflows.contains(fid)) {
			allflows.add(fid);
		}
		es.ESTIMATE(allflows);
		// System.out.println(es);
		
		LS[] hostlinks = new LS[k * k * k / 4 + 1];
		LS[] corelinks = new LS[k * k * k / 4 + 1];
		LS[] aggrlinks = new LS[k * k * k / 4 + 1];
		for (int i = 1; i <= k * k * k / 4; i++) {
			hostlinks[i] = new LS();
			corelinks[i] = new LS();
			aggrlinks[i] = new LS();
		}
		
		for (FlowId f : allflows) {
			if (flowRoutes.containsKey(f) && !f.equals(fid)) {
				Route r = flowRoutes.get(f);
				Address src = new Address(f.src);
				int srcIndex = Hedera.map(src.getAddress());
				Address dest = new Address(f.dest);
				int destIndex = Hedera.map(dest.getAddress());
				hostlinks[srcIndex].up += es.getDemand(f);
				hostlinks[destIndex].down += es.getDemand(f);
				if (f.isSameEdge())
					continue;
				
				int srcedge = src.getEdge();
				int destedge = dest.getEdge();
				int srcpod = src.getPod();
				int destpod = dest.getPod();
				int aggr = r.aggr;
				
				int srcbase = ((srcpod - 1) * (k / 2) + srcedge - 1) * k / 2;
				aggrlinks[srcbase + aggr].up += es.getDemand(f);
				
				int destbase = ((destpod - 1) * (k / 2) + destedge - 1) * k / 2;
				aggrlinks[destbase + aggr].down += es.getDemand(f);
				
				
				if (f.isInterPod()) {
					int core = (aggr - 1) * k / 2 + r.core;
					int baseline = (core - 1) * k;
					// System.out.println(core + " " + baseline + " " + srcpod);
					corelinks[baseline + srcpod].up += es.getDemand(f);
					corelinks[baseline + destpod].down += es.getDemand(f);
				}
			}
		}
		
		if (fid.isSameEdge()) {
			Route r = new Route(0, 0);
			flowRoutes.put(fid, r);
			return r;
		}
		
		for (int i = 1; i <= k / 2; i++)
			for (int j = 1; j <= k / 2; j++) {
				Route r = new Route(i, j);
				Address src = new Address(fid.src);
				Address dest = new Address(fid.dest);
				int srcedge = src.getEdge();
				int destedge = dest.getEdge();
				int srcpod = src.getPod();
				int destpod = dest.getPod();
				int aggr = r.aggr;
				
				int srcbase = ((srcpod - 1) * (k / 2) + srcedge - 1) * k / 2;
				aggrlinks[srcbase + aggr].up += es.getDemand(fid);
				
				int destbase = ((destpod - 1) * (k / 2) + destedge - 1) * k / 2;
				aggrlinks[destbase + aggr].down += es.getDemand(fid);
				
				if (aggrlinks[srcbase + aggr].up > 1 || aggrlinks[destbase + aggr].down > 1) {
					aggrlinks[srcbase + aggr].up -= es.getDemand(fid);
					aggrlinks[destbase + aggr].down -= es.getDemand(fid);
					continue;
				}
				
				
				if (fid.isInterPod()) {
					int core = (aggr - 1) * k / 2 + r.core;
					int baseline = (core - 1) * k;
					corelinks[baseline + srcpod].up += es.getDemand(fid);
					corelinks[baseline + destpod].down += es.getDemand(fid);
					if (corelinks[baseline + srcpod].up > 1 || corelinks[baseline + destpod].down > 1){
						aggrlinks[srcbase + aggr].up -= es.getDemand(fid);
						aggrlinks[destbase + aggr].down -= es.getDemand(fid);
						corelinks[baseline + srcpod].up -= es.getDemand(fid);
						corelinks[baseline + destpod].down -= es.getDemand(fid);
						continue;
					}
				}
				
				// System.out.println("ping" + fid + " " + es.getDemand(fid) + " " + r);
				flowRoutes.put(fid, r);
				return r;
				
			}
		
		return null;
	}
	
	public static void removeFlow(FlowId fid) {
		if (flowRoutes.containsKey(fid)) {
			flowRoutes.remove(fid);
		}
		
		if (allflows.contains(fid)) {
			allflows.remove(fid);
		}
	}

}
