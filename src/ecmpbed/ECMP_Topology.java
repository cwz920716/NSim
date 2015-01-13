package ecmpbed;

import simulator.Address;
import simulator.Link;
import support.TopoFace;

public class ECMP_Topology implements TopoFace {
	private int k;
	public ECMP_Core[] cores;
	public ECMP_PodTopology[] pods;
	public Link[][] corelinks;
	
	public ECMP_Topology(int k) {
		this.k = k;
		this.cores = new ECMP_Core[k * k / 4 + 1];
		this.pods = new ECMP_PodTopology[k + 1];
		
		this.corelinks = new Link[k + 1][];
		
		for (int i = 1; i <= k; i++) {
			this.pods[i] = new ECMP_PodTopology(k, i);
		}
		
		for (int i = 1; i <= k * k / 4; i++) {
			this.cores[i] = new ECMP_Core(new Address(i, 0, 0, 0), k);
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
	
	public ECMP_Endhost getHost(Address addr) {
		int podi = addr.getPod();
		
		return pods[podi].getHost(addr);
	}
	
	public static void verify(int k) {
		ECMP_Topology topo = new ECMP_Topology(k);
		
		for (int i = 1; i <= k; i++) {
			System.out.println("Pod " + i + " :");
			for (int m = 1; m <= k / 2; m++) {
				for (int n = 1; n <= k / 2; n++) {
					ECMP_Core core = (ECMP_Core) topo.corelinks[i][(m - 1) * k / 2 + n].getUpNode();
					if (core != topo.cores[(m - 1) * k / 2 + n])
						System.err.println("X");
					// System.out.println(core.getName());
					ECMP_AggrSwitch aggr = (ECMP_AggrSwitch) topo.corelinks[i][(m - 1) * k / 2 + n].getDownNode();
					// System.out.println(aggr.getName());
					ECMP_EdgeSwitch edge = (ECMP_EdgeSwitch) aggr.getDownLink(n).getDownNode();
					// System.out.println(edge.getName());
					for (int p = 1; p <= k / 2; p++) {
						ECMP_Endhost host = (ECMP_Endhost) edge.getDownLink(p).getDownNode();
						// System.out.println(host.getName());
						if (host != topo.getHost(host.getAddress()))
							System.err.println("X");
					}
				}
			}
			System.out.println();
		} 
	}

	private ECMP_Endhost getHost(long addr) {
		return getHost(new Address(addr));
	}
	
	public static void main(String[] args) {
		verify(32);
	}

	@Override
	public Iterable<Link> relatedLinks(Address addr) {
		// TODO Auto-generated method stub
		return null;
	}
}
