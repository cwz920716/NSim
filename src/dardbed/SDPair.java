package dardbed;

import simulator.Address;
import simulator.FlowId;

public class SDPair {
	public long src, dest;
	
	public SDPair(long src, long dest) {
		this.src = src;
		this.dest = dest;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null)
			return false;
		
		if (o instanceof SDPair) {
			SDPair fid = (SDPair) o;
			return src == fid.src && dest == fid.dest;
		}
		
		return false;
	}
	
	@Override
	public int hashCode() {
		Long x = src + dest;
		return x.hashCode();
	}
	
	@Override
	public String toString() {
		Address src = new Address(this.src);
		String sname = "Pod " + src.getPod() + " Edge " + src.getEdge() + " Host " + src.getHost();
		Address dest = new Address(this.dest);
		String dname = "Pod " + dest.getPod() + " Edge " + dest.getEdge() + " Host " + dest.getHost();
		return sname + " -> " + dname;
	}
	
	public boolean isInterPod() {
		Address src = new Address(this.src);
		Address dest = new Address(this.dest);
		return src.getPod() != dest.getPod();
	}
	
	public boolean isSameEdge() {
		Address src = new Address(this.src);
		Address dest = new Address(this.dest);
		return src.getPod() == dest.getPod() && src.getEdge() == dest.getEdge() && src.getHost() != dest.getHost();
	}

}
