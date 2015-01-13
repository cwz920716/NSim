package simulator;

import java.util.Random;

import dardbed.SDPair;

public class FlowId {
	public long src, dest;
	public short sport, dport;
	
	public FlowId(long src, long dest, short sport, short dport) {
		this.src = src;
		this.dest = dest;
		this.sport = sport;
		this.dport = dport;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null)
			return false;
		
		if (o instanceof FlowId) {
			FlowId fid = (FlowId) o;
			return src == fid.src && dest == fid.dest
			       && sport == fid.sport && dport == fid.dport;
		}
		
		return false;
	}
	
	@Override
	public int hashCode() {
		Long x = src + dest + sport + dport;
		return x.hashCode();
	}
	
	@Override
	public String toString() {
		Address src = new Address(this.src);
		String sname = "Pod " + src.getPod() + " Edge " + src.getEdge() + " Host " + src.getHost();
		Address dest = new Address(this.dest);
		String dname = "Pod " + dest.getPod() + " Edge " + dest.getEdge() + " Host " + dest.getHost();
		return sport + "@" + sname + " -> " + dport + "@" + dname;
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
	
	public int hash(int k) {
		long i = src * dest * sport * dport;
		Random rand = new Random(i);
		return (int) (Math.abs(((Integer) rand.nextInt()).hashCode() % k) + 1);
	}
	
	public int hash(int k, long addr) {
		long i = src * dest * sport * dport;
		Random rand = new Random(i);
		return (int) (Math.abs(((Integer) rand.nextInt()).hashCode() % k) + 1);
	}
	
	public boolean match(SDPair p) {
		return src == p.src && dest == p.dest;
	}

}
