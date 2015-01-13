package simulator;

import java.util.Vector;

public class Address {
	private long addr;
	private static long MASK = 0x000000000000ffffL;
	
	public Address(long addr) {
		this.addr = addr;
		// check();
	}
	
	private void check() {
		if (!isValid()) {
			System.err.println("You create an invalid address!");
			System.exit(0);
		}
	}

	public Address(short pod, short aggr, short edge, short host) {
		this.addr |= (pod & MASK);
		addr <<= 16;
		this.addr |= (aggr & MASK);
		addr <<= 16;
		this.addr |= (edge & MASK);
		addr <<= 16;
		this.addr |= (host & MASK);
		check();
	}
	
	public Address(int pod, int aggr, int edge, int host) {
		this((short) pod, (short) aggr, (short) edge, (short) host);
	}

	public long getAddress() {
		return addr;
	}
	
	public boolean isHost() {
		return getHost() != 0 && getEdge() != 0 && getAggr() == 0 && getPod() != 0; // host != 0, aggr == 0, edge != 0, pod != 0
	}

	public boolean isEdge() {
		return getHost() == 0 && getEdge() != 0 && getAggr() == 0 && getPod() != 0; // host == 0, aggr == 0, edge != 0, pod != 0
	}
	
	public boolean isAggr() {
		return getHost() == 0 && getEdge() == 0 && getAggr() != 0 && getPod() != 0; // host == 0, aggr != 0, edge == 0, pod != 0
	}
	
	public boolean isCore() {
		return getHost() == 0 && getEdge() == 0 && getAggr() == 0 && getPod() != 0; // host == 0, aggr == 0, edge == 0, pod != 0
	}
	
	public short getPod() {
		return (short) ((addr >> 48) & MASK);
	}
	
	public short getAggr() {
		return (short) ((addr >> 32) & MASK);
	}
	
	public short getEdge() {
		return (short) ((addr >> 16) & MASK);
	}
	
	public short getHost() {
		return (short) ((addr) & MASK);
	}
	
	public String toString() {
		return "P" + this.getPod() + "A" + this.getAggr() + "E" + this.getEdge() + "H" + this.getHost();
	}
	
	public boolean isValid() {
		return this.isCore() || this.isAggr() || this.isEdge() || this.isHost();
	}
	

	public static Vector<Address> A2V(Address[] a) {
		Vector<Address> v  = new Vector<Address>();
		for (int i = 0; i < a.length; i++)
			v.add(a[i]);
		return v;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null || ! (o instanceof Address))
			return false;
		
		Address another = (Address) o;
		return addr == another.addr;
	}

}
