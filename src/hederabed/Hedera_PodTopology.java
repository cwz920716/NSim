package hederabed;

import simulator.Address;
import simulator.Link;

public class Hedera_PodTopology {
	private int k;
	private int podi;
	public Hedera_Endhost[] hosts;
	public Hedera_AggrSwitch[] aggrs;
	public Hedera_EdgeSwitch[] edges;
	public Link[] endlinks;
	public Link[] aggrlinks;
	
	public Hedera_PodTopology(int k, int podi) {
		this.k = k;
		this.podi = podi;
		this.hosts = new Hedera_Endhost[k * k / 4 + 1];
		this.aggrs = new Hedera_AggrSwitch[k / 2 + 1];
		this.edges = new Hedera_EdgeSwitch[k / 2 + 1];
		
		this.endlinks = new Link[k * k / 4 + 1];
		this.aggrlinks = new Link[k * k / 4 + 1];
		
		for (int i = 1; i <= k / 2; i++) {
			aggrs[i] = new Hedera_AggrSwitch(new Address(podi, i, 0, 0), k);
			edges[i] = new Hedera_EdgeSwitch(new Address(podi, 0, i, 0), k);
		}
		
		for (int i = 1; i <= k / 2; i++) {
			for (int j = 1; j <= k / 2; j++) {
				int tmp = (i - 1) * k / 2 + j;
				
				// set edge links
				hosts[tmp] = new Hedera_Endhost(new Address(podi, 0, i, j));
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

	public Hedera_Endhost getHost(Address addr) {
		int edgei = addr.getEdge();
		int hosti = addr.getHost();
		
		return hosts[(edgei - 1) * k / 2 + hosti];
	}

}
