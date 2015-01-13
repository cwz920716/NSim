package support;

import java.util.Vector;

import detour.DetourMessage;
import algorithm.Bypass;
import algorithm.Bypass.Detour;
import simulator.Address;
import simulator.Link;
import simulator.Node;
import simulator.Switch;
import simulator.Topology;

public class Fails {
	public static Fails F;
	
	private Vector<Link> failedLinks = new Vector<Link>();
	private Vector<Node> failedNodes = new Vector<Node>();
	
	private int k;
	private int[][] corelinks;
	private int[][] aggrlinks;
	private int[][] edgelinks;
	private int[] hostlinks;
	
	private Topology topo;
	
	public void setTopology(Topology topo) {
		this.topo = topo;
	}
	
	public Fails(int k) {
		this.k = k;
		corelinks = new int[k * k / 4 + 1][];
		for (int i = 1; i < corelinks.length; i++)
			corelinks[i] = new int[k + 1];
		
		aggrlinks = new int[k * k / 2 + 1][];
		for (int i = 1; i < aggrlinks.length; i++)
			aggrlinks[i] = new int[k + 1];
		
		edgelinks = new int[k * k / 2 + 1][];
		for (int i = 1; i < edgelinks.length; i++)
			edgelinks[i] = new int[k + 1];
		
		hostlinks = new int[k * k * k / 4 + 1];
	}

	public boolean isfailedlink(Link l) {
		return l != null && failedLinks.contains(l);
	}
	
	public boolean inFault() {
		return !(failedLinks.isEmpty() && failedNodes.isEmpty());
	}
	
	public void addFailedNode(Node n, Iterable<Link> ls) {
		if (!failedNodes.contains(n)) {
			failedNodes.add(n);
			for (Link l : ls)
				this.addFailedLink(l);
		}
	}
	
	public void addFailedLink(Link l) {
		if (!failedLinks.contains(l)) {
			failedLinks.add(l);
			Address addr1 = new Address(l.getDownNode().getAddress());
			Address addr2 = new Address(l.getUpNode().getAddress());
			
			if (topo != null) {
				Switch s1 = topo.getSwitchByAddress(addr1);
				s1.disableLink(l);
				Switch s2 = topo.getSwitchByAddress(addr2);
				s2.disableLink(l);
			}
			
			if (addr1.isCore()) {
				System.err.println("core won't be down nodes!");
				return;
			}
			
			if (addr1.isAggr() && addr2.isCore()) {
				int ap = addr1.getPod();
				int c = addr2.getPod();
				corelinks[c][ap]--;
				int ag = addr1.getAggr();
				int i = c % (k / 2);
				if (i == 0)
					i = k / 2;
				aggrlinks[(ap - 1) * k / 2 + ag][i]--;
			}
			
			if (addr1.isEdge() && addr2.isAggr() && addr1.getPod() == addr2.getPod()) {
				int p = addr1.getPod();
				int e = addr1.getEdge();
				int a = addr2.getAggr();
				edgelinks[(p - 1) * k / 2 + e][a]--;
				aggrlinks[(p - 1) * k / 2 + a][e + k / 2]--;
			}
			
			if (addr1.isHost() && addr2.isEdge() && addr1.getPod() == addr2.getPod() && addr1.getEdge() == addr2.getEdge()) {
				int p = addr1.getPod();
				int e = addr1.getEdge();
				int h = addr1.getHost();
				hostlinks[(p - 1) * k * k / 4 + (e - 1) * k / 2 + h]--;
				edgelinks[(p - 1) * k / 2 + e][k / 2 + h]--;
			}
				
		}
	}
	
	public boolean okpath(Address cur, long src, long dst, int curport) {
		return okpath(cur, new Address(src), new Address(dst), curport);
	}
	
	public boolean okpath(Address cur, Address src, Address dst, int curport) {
		int up1, up2, up3, down3, down2, down1;
		
		if (cur.isCore()) {
			int c = cur.getPod();
			int p = dst.getPod(), e = dst.getEdge(), h = dst.getHost();
			int a = (c - 1) / (k / 2) + 1;
			
			down3 = corelinks[cur.getPod()][curport];
			down2 = aggrlinks[(p - 1) * k / 2 + a][e + k / 2];
			down1 = hostlinks[(p - 1) * k * k / 4 + (e - 1) * k / 2 + h];
			return (down1 == 0) && (down2 == 0) && (down3 == 0);
		}
		
		if (cur.isAggr()) {
			int a = cur.getAggr();
			int dp = dst.getPod(), de = dst.getEdge(), dh = dst.getHost();
			
			if (curport > k / 2) {
				down2 = aggrlinks[(dp - 1) * k / 2 + a][de + k / 2];
				down1 = hostlinks[(dp - 1) * k * k / 4 + (de - 1) * k / 2 + dh];
				return (down1 == 0) && (down2 == 0);
			} else {
				int cp = cur.getPod();
				up3 = aggrlinks[(cp - 1) * k / 2 + a][curport];
				down3 = aggrlinks[(dp - 1) * k / 2 + a][curport];
				down2 = aggrlinks[(dp - 1) * k / 2 + a][de + k / 2];
				down1 = hostlinks[(dp - 1) * k * k / 4 + (de - 1) * k / 2 + dh];
				return (up3 == 0) && (down1 == 0) && (down2 == 0) && (down3 == 0);
			}
		}
		
		if (cur.isEdge()) {
			int dp = dst.getPod(), de = dst.getEdge(), dh = dst.getHost();
			int ce = cur.getEdge();
			
			if (curport > k / 2) {
				down1 = hostlinks[(dp - 1) * k * k / 4 + (de - 1) * k / 2 + dh];
				return down1 == 0;
			} else {
				int cp = cur.getPod();
				if (cp == dp) {
					up2 = edgelinks[(cp - 1) * k / 2 + ce][curport];
					down2 = edgelinks[(dp - 1) * k / 2 + de][curport];
					down1 = hostlinks[(dp - 1) * k * k / 4 + (de - 1) * k / 2 + dh];
					return (up2 == 0) && (down2 == 0) && (down1 == 0);
				} else {
					up2 = edgelinks[(cp - 1) * k / 2 + ce][curport];
					up3 = -1;
					down3 = -1;
					for (int i = 1; i <= k / 2; i++) {
						up3 = aggrlinks[(cp - 1) * k / 2 + curport][i];
						down3 = aggrlinks[(dp - 1) * k / 2 + curport][i];
						if (up3 == 0 && down3 == 0)
							break;
					}
					down2 = edgelinks[(dp - 1) * k / 2 + de][curport];
					down1 = hostlinks[(dp - 1) * k * k / 4 + (de - 1) * k / 2 + dh];
					return (up2 == 0) && (up3 == 0) && (down3 == 0) && (down2 == 0) && (down1 == 0);
				}
			}
		}
		
		if (cur.isHost()) {
			int p = dst.getPod(), e = dst.getEdge(), h = dst.getHost();
			up1 = hostlinks[(p - 1) * k * k / 4 + (e - 1) * k / 2 + h];
			return up1 == 0;
		}
		
		return true;
	}
	
	public DetourMessage bypass(Address dsrc, Address ddst, Address[] hops1) {
		int k1 = hops1.length - 1;
		double[][] links1 = new double[k1 + 1][];
		double[] links0, links2;
		boolean up = false;
		
		if ((dsrc.isAggr() && ddst.isCore()) || (dsrc.isEdge() && ddst.isAggr())) {
			up = true;
		}
		
		Address[] hops2 = topo.getSwitchByAddress(ddst).peers(!up);
		int k2 = hops2.length - 1;
		
		links0 = topo.getSwitchByAddress(dsrc).portmeasurmentOut(up);
		for (int i = 1; i <= k1; i++)
			links1[i] = topo.getSwitchByAddress(hops1[i]).portmeasurmentOut(!up);
		links2 = topo.getSwitchByAddress(ddst).portmeasurmentIn(!up);
		// System.out.println(k2 + " " + dsrc + " " + links2[1]);
		Detour d = Bypass.computeBypass(k1, k2, links0, links1, links2);
		DetourMessage dm = new DetourMessage(PDU.DETOUR, 0, PDU.LBDAR, null, dsrc, hops1[d.hop1], hops2[d.hop2], ddst);
		// System.out.println(dm.dsrc + " " + dm.hop1 + " " + dm.hop2 + " " + dm.ddest);
		
		return dm;
	}
	
	public int getUsefulUphillPortNum(Address cur, long src, long dst) {
		return getUsefulUphillPortNum(cur, new Address(src), new Address(dst));
	}
	
	public int getUsefulUphillPortNum(Address cur, Address src, Address dst) {
		int sum = 0;
		
		for (int i = 1; i <= k / 2; i++)
			if (this.okpath(cur, src, dst, i))
				sum++;
		
		return sum;
	}
	
	public boolean singleLinkFailure() {
		return failedLinks.size() == 1 && failedNodes.isEmpty();
	}
	
	public boolean isolatedLinkFailure() {
		if (failedLinks.size() > 5 || !failedNodes.isEmpty()) {
			return false;
		}
		
		Vector<Long> va = new Vector<Long>();
		for (int i = 0; i < failedLinks.size(); i++) {
			Link l = failedLinks.get(i);
			if (va.contains(l.getDownNode().getAddress()) || va.contains(l.getUpNode().getAddress()))
				return false;
			
			va.add(l.getUpNode().getAddress());
			va.add(l.getDownNode().getAddress());
		}
		
		return true;
	}
	
	public void removeFailedLink(Link l) {
		if (failedLinks.contains(l)) {
			failedLinks.remove(l);
			Address addr1 = new Address(l.getDownNode().getAddress());
			Address addr2 = new Address(l.getUpNode().getAddress());
			
			if (addr1.isCore()) {
				System.err.println("core won't be down nodes!");
				return;
			}
			
			if (addr1.isAggr() && addr2.isCore()) {
				int ap = addr1.getPod();
				int c = addr2.getPod();
				corelinks[c][ap]++;
				int ag = addr1.getAggr();
				int i = c % (k / 2);
				if (i == 0)
					i = k / 2;
				aggrlinks[(ap - 1) * k / 2 + ag][i]++;
			}
			
			if (addr1.isEdge() && addr2.isAggr() && addr1.getPod() == addr2.getPod()) {
				int p = addr1.getPod();
				int e = addr1.getEdge();
				int a = addr2.getAggr();
				edgelinks[(p - 1) * k / 2 + e][a]++;
				aggrlinks[(p - 1) * k / 2 + a][e + k / 2]++;
			}
			
			if (addr1.isHost() && addr2.isEdge() && addr1.getPod() == addr2.getPod() && addr1.getEdge() == addr2.getEdge()) {
				int p = addr1.getPod();
				int e = addr1.getEdge();
				int h = addr1.getHost();
				hostlinks[(p - 1) * k * k / 4 + (e - 1) * k / 2 + h]++;
				edgelinks[(p - 1) * k / 2 + e][k / 2 + h]++;
			}
				
		}
	}

	public void removeFailedNode(Node n, Iterable<Link> ls) {
		if (failedNodes.contains(n)) {
			failedNodes.remove(n);
			for (Link l : ls)
				this.removeFailedLink(l);;
		}
		
	}
}
