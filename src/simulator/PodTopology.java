package simulator;

import java.util.ArrayList;

public class PodTopology {
	private int k;
	private int podi;
	public Endhost[] hosts;
	public AggrSwitch[] aggrs;
	public EdgeSwitch[] edges;
	public Link[] endlinks;
	public Link[] aggrlinks;
	
	public PodTopology(int k, int podi) {
		this.k = k;
		this.podi = podi;
		this.hosts = new Endhost[k * k / 4 + 1];
		this.aggrs = new AggrSwitch[k / 2 + 1];
		this.edges = new EdgeSwitch[k / 2 + 1];
		
		this.endlinks = new Link[k * k / 4 + 1];
		this.aggrlinks = new Link[k * k / 4 + 1];
		
		for (int i = 1; i <= k / 2; i++) {
			aggrs[i] = new AggrSwitch(new Address(podi, i, 0, 0), k);
			edges[i] = new EdgeSwitch(new Address(podi, 0, i, 0), k);
		}
		
		for (int i = 1; i <= k / 2; i++) {
			for (int j = 1; j <= k / 2; j++) {
				int tmp = (i - 1) * k / 2 + j;
				
				// set edge links
				hosts[tmp] = new Endhost(new Address(podi, 0, i, j));
				endlinks[tmp] = new Link(hosts[tmp], edges[i]);
				hosts[tmp].setEdgeLink(endlinks[tmp]);
				edges[i].setDownLink(j, endlinks[tmp]);
				
				// set aggr links
				aggrlinks[tmp] = new Link(edges[i], aggrs[j]);
				edges[i].setUpLink(j, aggrlinks[tmp]);
				aggrs[j].setDownLink(i, aggrlinks[tmp]);
			}
		}
	}

	public int getK() {
		return k;
	}
	
	public int getPodi() {
		return podi;
	}
	
	public void setCoreLinks(Link[] corelinks) {
		if (corelinks.length != k * k / 4 + 1)
			System.err.println("Pod " + this.podi + " with uncompliable core links");
		
		for (int i = 1; i <= k / 2; i++) {
			for (int j = 1; j <= k / 2; j++)
				this.aggrs[i].setUpLink(j, corelinks[(i - 1) * k / 2 + j]);
		}
	}

	public Endhost getHost(Address addr) {
		int edgei = addr.getEdge();
		int hosti = addr.getHost();
		
		return hosts[(edgei - 1) * k / 2 + hosti];
	}

	public Switch getSwitchByAddress(Address addr) {
		if (addr.isAggr())
			return aggrs[addr.getAggr()];
		else if (addr.isEdge())
			return edges[addr.getEdge()];
		else
			return hosts[(addr.getEdge() - 1) * k / 2 + addr.getHost()];
	}

	public Iterable<Link> relatedlinks(Address addr) {
		// TODO Auto-generated method stub
		ArrayList<Link> al = new ArrayList<Link>();
		if (addr.isHost()) {
			for (int i = 1; i <= k / 2 * k / 2; i++)
				if (hosts[i].getAddress() == addr.getAddress())
					al.add(endlinks[i]);
		} else if (addr.isAggr()) {
			for (int i = 1; i <= k / 2; i++)
				if (aggrs[i].getAddress() == addr.getAddress()) {
					for (int j = 1; j <= k/2; j++) {
						al.add(aggrs[i].getDownLink(j));
						al.add(aggrs[i].getUpLink(j));
					}
				}
		} else {
			for (int i = 1; i <= k / 2; i++)
				if (edges[i].getAddress() == addr.getAddress()) {
					for (int j = 1; j <= k/2; j++) {
						al.add(edges[i].getDownLink(j));
						al.add(edges[i].getUpLink(j));
					}
				}
		}
		return al;
	}
}
