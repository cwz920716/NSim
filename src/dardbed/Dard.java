package dardbed;

import java.util.Vector;


import simulator.FlowId;

public class Dard {
	static int k;
	public Vector<FlowId> elephants = new Vector<FlowId>();
	public Vector<FlowId> mice = new Vector<FlowId>();
	public Vector<SDPair> pairs = new Vector<SDPair>();
	
	public void addMiceFlow(FlowId fid) {
		if (!mice.contains(fid))
			mice.add(fid);
		else
			return;
	}
	
	public void addElephantFlow(FlowId fid) {
		if (!elephants.contains(fid))
			elephants.add(fid);
		else
			return;
		
		SDPair fp = new SDPair(fid.src, fid.dest);
		if (fid.isSameEdge()) {
			Path p = new Path(fp, 0);
			SourceRouting.addElephantFlowPath(fid, p);
			// System.out.println("install " + fid.toString() + ": at " + 0);
		} else if (fid.isInterPod()) {
			int pid = fid.hash(k * k / 4);
			Path p = new Path(fp, pid);
			SourceRouting.addElephantFlowPath(fid, p);
			// System.out.println("install " + fid.toString() + ": at " + pid);
		} else {
			int pid = fid.hash(k / 2);
			Path p = new Path(fp, pid);
			SourceRouting.addElephantFlowPath(fid, p);
			// System.out.println("install " + fid.toString() + ": at " + pid);
		}
		
		if (!pairs.contains(fp))
			pairs.add(fp);

		
		if (mice.contains(fid))
			mice.remove(fid);
	}
	
	public void SelfishPathSelector() {
		for (SDPair p : pairs) {
			if (p.isSameEdge())
				continue;
			
			Vector<FlowId> flows = new Vector<FlowId>();
			Vector<Integer> pIDs = new Vector<Integer>();
			for (FlowId fid : elephants) {
				if (fid.match(p)) {
					flows.add(fid);
					pIDs.add(SourceRouting.getPath(fid).getID());
				}
			}
			
			int nPath = (p.isInterPod()) ? (k * k / 4) : (k / 2);
			int maxI = -1, minI = -1, minN = -1, maxN = -1;
			for (int i = 1; i <= nPath; i++) {
				Path path = new Path(p, i);
				int N = SourceRouting.getPathN(path);
				if (maxI == -1 || N < minN) {
					maxI = i;
					minN = N;
				}
				
				if (pIDs.contains(i)) {
					if (minI == -1 || N > maxN) {
						maxN = N;
						minI = i;
					}
				}
			}

			double minS = 1.0 / maxN;
			if (maxN != minN) {
				// System.out.println("XX" + maxN + "XX" + minN);
				double est = 1.0 / (minN + 1);
				if (est - minS > 0.01) {
					for (FlowId f : flows) {
						if (SourceRouting.getPath(f).getID() == minI) {
							Path path = new Path(p, maxI);
							SourceRouting.addElephantFlowPath(f, path);
							// System.out.println("move" + f.toString() + ": from " + minI + " to " + maxI);
							break;
						}
					}
				}
			}
		}
	}

	public static void INIT(int k2) {
		// TODO Auto-generated method stub
		Dard.k = k2;
	}

}
