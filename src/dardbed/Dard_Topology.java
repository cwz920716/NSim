package dardbed;

import simulator.Address;
import simulator.Link;
import support.TopoFace;

public class Dard_Topology implements TopoFace {
	private int k;
	public Dard_Core[] cores;
	public Dard_PodTopology[] pods;
	public Link[][] corelinks;
	
	public Dard_Topology(int k) {
		this.k = k;
		this.cores = new Dard_Core[k * k / 4 + 1];
		this.pods = new Dard_PodTopology[k + 1];
		
		this.corelinks = new Link[k + 1][];
		
		for (int i = 1; i <= k; i++) {
			this.pods[i] = new Dard_PodTopology(k, i);
		}
		
		for (int i = 1; i <= k * k / 4; i++) {
			this.cores[i] = new Dard_Core(new Address(i, 0, 0, 0), k);
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
	
	public Dard_Endhost getHost(Address addr) {
		int podi = addr.getPod();
		
		return pods[podi].getHost(addr);
	}

	public Dard_Endhost getHost(long addr) {
		return getHost(new Address(addr));
	}

	@Override
	public Iterable<Link> relatedLinks(Address addr) {
		// TODO Auto-generated method stub
		return null;
	}
}
