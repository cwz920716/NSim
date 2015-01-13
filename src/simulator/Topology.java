package simulator;

import java.util.ArrayList;

import support.TopoFace;

public class Topology implements TopoFace {
	private int k;
	public Core[] cores;
	public PodTopology[] pods;
	public Link[][] corelinks;
	
	public Topology(int k) {
		this.k = k;
		this.cores = new Core[k * k / 4 + 1];
		this.pods = new PodTopology[k + 1];
		
		this.corelinks = new Link[k + 1][];
		
		for (int i = 1; i <= k; i++) {
			this.pods[i] = new PodTopology(k, i);
		}
		
		for (int i = 1; i <= k * k / 4; i++) {
			this.cores[i] = new Core(new Address(i, 0, 0, 0), k);
		}
		
		for (int i = 1; i <= k; i++) {
			this.corelinks[i] = new Link[k * k / 4 + 1];
			for (int m = 1; m <= k / 2; m++) {
				for (int n = 1; n <= k / 2; n++) {
					this.corelinks[i][(m - 1) * k / 2 + n] = new Link(pods[i].aggrs[m], cores[(m - 1) * k / 2 + n]);
					this.cores[(m - 1) * k / 2 + n].setPodLink(i, corelinks[i][(m - 1) * k / 2 + n]);
				}
			}
			this.pods[i].setCoreLinks(corelinks[i]);
		}
	}

	public int getK() {
		return k;
	}
	
	public Endhost getHost(Address addr) {
		int podi = addr.getPod();
		
		return pods[podi].getHost(addr);
	}
	
	public Switch getSwitchByAddress(Address addr) {
		if (addr.isCore())
			return cores[addr.getPod()];
		else if (addr.isAggr())
			return pods[addr.getPod()].getSwitchByAddress(addr);
		else if (addr.isEdge())
			return pods[addr.getPod()].getSwitchByAddress(addr);
		else
			return pods[addr.getPod()].getSwitchByAddress(addr);
	}
	
	public static void verify(int k) {
		Topology topo = new Topology(k);
		
		for (int i = 1; i <= k; i++) {
			System.out.println("Pod " + i + " :");
			for (int m = 1; m <= k / 2; m++) {
				for (int n = 1; n <= k / 2; n++) {
					Core core = (Core) topo.corelinks[i][(m - 1) * k / 2 + n].getUpNode();
					if (core != topo.cores[(m - 1) * k / 2 + n])
						System.err.println("X");
					// System.out.println(core.getName());
					AggrSwitch aggr = (AggrSwitch) topo.corelinks[i][(m - 1) * k / 2 + n].getDownNode();
					// System.out.println(aggr.getName());
					EdgeSwitch edge = (EdgeSwitch) aggr.getDownLink(n).getDownNode();
					// System.out.println(edge.getName());
					for (int p = 1; p <= k / 2; p++) {
						Endhost host = (Endhost) edge.getDownLink(p).getDownNode();
						// System.out.println(host.getName());
						if (host != topo.getHost(host.getAddress()))
							System.err.println("X");
					}
				}
			}
			System.out.println();
		} 
	}

	private Endhost getHost(long addr) {
		return getHost(new Address(addr));
	}
	
	public static void main(String[] args) {
		verify(32);
	}

	@Override
	public Iterable<Link> relatedLinks(Address addr) {
		// TODO Auto-generated method stub
		for (int i = 1; i <= k; i++) {
			Core c = cores[i];
			if (c.getAddress() == addr.getAddress()) {
				ArrayList<Link> a = new ArrayList<Link>();
				for (int j = 1; j <= k; j++)
					a.add(c.getPodLink(j));
				return a;
			}
		}
		int pod = addr.getPod();
		return pods[pod].relatedlinks(addr);
	}

}
