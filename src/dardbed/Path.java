package dardbed;

import java.util.ArrayList;
import java.util.Vector;

import simulator.Address;
import simulator.FlowId;

public class Path {
	private SDPair p;
	private int pathID;
	private int k;
	public ArrayList<Address> nodes = new ArrayList<Address>();
	public int aggr;
	public int core;
	
	public Path(SDPair fid, int pid) {
		this.p = fid;
		this.k = Dard.k;
		this.setID(pid);
	}

	public void setID(int i) {
		this.pathID = i;
		
		if (i == 0) {
			aggr = core = 0;
		}
		
		if (this.isSameEdge()) {
			aggr = core = 0;
			
			Address src = new Address(p.src);
			nodes.add(src);
			Address edge = new Address(src.getPod(), 0, src.getEdge(), 0);
			nodes.add(edge);
			Address dest = new Address(p.dest);
			nodes.add(dest);
		} else if (this.isInterPod()) {
			aggr = (i - 1) / (k / 2) + 1;
			core = (i - 1) % (k / 2) + 1;

			Address src = new Address(p.src);
			Address dest = new Address(p.dest);
			nodes.add(src);
			Address sedge = new Address(src.getPod(), 0, src.getEdge(), 0);
			nodes.add(sedge);
			Address saggr = new Address(src.getPod(), aggr, 0, 0);
			nodes.add(saggr);
			Address corea = new Address(i, 0, 0, 0);
			nodes.add(corea);
			Address daggr = new Address(dest.getPod(), aggr, 0, 0);
			nodes.add(daggr);
			Address dedge = new Address(dest.getPod(), 0, dest.getEdge(), 0);
			nodes.add(dedge);
			nodes.add(dest);
		} else {
			aggr = i;
			core = 0;

			Address src = new Address(p.src);
			Address dest = new Address(p.dest);
			nodes.add(src);
			Address sedge = new Address(src.getPod(), 0, src.getEdge(), 0);
			nodes.add(sedge);
			Address aggra = new Address(src.getPod(), i, 0, 0);
			nodes.add(aggra);
			Address dedge = new Address(dest.getPod(), 0, dest.getEdge(), 0);
			nodes.add(dedge);
			nodes.add(dest);
		}
	}
	
	public int getID() {
		return this.pathID;
	}
	
	public boolean isInterPod() {
		return p.isInterPod();
	}
	
	public boolean isSameEdge() {
		return p.isSameEdge();
	}
	
	public int length() {
		if (this.isInterPod())
			return 7;
		else if (this.isSameEdge())
			return 3;
		else
			return 5;
		
	}
}
