package simulator;

import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.Vector;

import protocol.TCPBony;
import support.Entity;
import support.EventManager;
import support.Fails;
import support.Simusys;
import trace.Trace;

public class Simulator {
	public static void main(String[] args) {
		int k = 4;
		Topology topo = new Topology(k);
		Fails.F = new Fails(k);
		Fails.F.setTopology(topo);
		int xmax = 1;
		TCPBony[] tcpc = new TCPBony[k * k * k / 4 * xmax];
		TCPBony[] tcps = new TCPBony[k * k * k / 4 * xmax];
		Trace.reset(k);
		int sum_ear = 0;
		
		int next = 0;
		int cnt = 0;
		for (int i = 1; i <= k; i++)
			for (int j = 1; j <= k / 2; j++)
				for (int m = 1; m <= k / 2; m++) {
					Address addr = new Address(i, 0, j, m);
					Endhost end1 = topo.getHost(addr);
					// System.out.println(end1.getName());
					for (int x = 0; x < xmax; x++) {
					Endhost end2 = topo.getHost(Trace.Randbij(addr));
					// System.out.println(end2.getName());
					if (addr.getPod() != new Address(end2.getAddress()).getPod()) {
						cnt++;
					}
					// System.out.println(end1.getName() + " -> " + end2.getName());
					tcpc[next] = new TCPBony(end1, (short) (next + 1));
					tcps[next] = new TCPBony(end2, (short) (next + 1));
					tcpc[next].setPeer(tcps[next], true);
					tcps[next].setPeer(tcpc[next], false);
					// long x = end1.getAddress() + end2.getAddress() + next + 1 + next + 1;
					// System.out.println(next + " " + (int) (Math.abs(x) % (k / 2) + 1));
					next++;
					}
				}
		
		Simusys.reset();
		Address a = new Address(Trace.rand.nextInt(4) + 1, Trace.rand.nextInt(2) + 1, 0, 0);
		Link l = topo.corelinks[Trace.rand.nextInt(k) + 1][Trace.rand.nextInt(k * k / 4) + 1];
		long end = 1 * 20 * Simusys.ticePerSecond();
		for (int i = 0; i < tcpc.length; i++)
			tcpc[i].send();
		
		while (Simusys.time() <= end) {
			Vector<Entity> entities = EventManager.getEntities();
			
			for (Enumeration<Entity> enums = entities.elements(); enums.hasMoreElements(); ) {
				Entity e = enums.nextElement();
				e.performPendingEventsAt(Simusys.time());
			}
			
			for (Enumeration<Entity> enums = entities.elements(); enums.hasMoreElements(); ) {
				Entity e = enums.nextElement();
				e.performEventsAt(Simusys.time());
			}

			if (Simusys.time() % (Simusys.ticePerSecond() / 10) == 0) {
				sum_ear = 0;
				double rate = 0, amount = 0;
				DecimalFormat df5  = new DecimalFormat("##.00000");
				for (int i = 0; i < tcpc.length; i++) {
					// if (tcpc[i].getRate() <= 0)
					double r = tcpc[i].getInstantRate() * 10;
					// System.out.println(tcpc[i].getName() + " : " + r);
					rate += r;
					// System.out.println(tcps[i].getName() + " : " + tcps[i].getState());
					// amount += tcpc[i].getAmount();
				}
				
				for (int i = 1; i < topo.cores.length; i++) {
					// System.out.println(topo.cores[i].getName() + " : " + topo.cores[i].getState());
				}
				
				for (int i = 1; i < topo.pods.length; i++) {
					// System.out.println(topo.cores[i].getName() + " : " + topo.cores[i].getState());
					PodTopology pt = topo.pods[i];
					for (int j = 1; j < pt.aggrs.length; j++) {
						// System.out.println(pt.aggrs[j].getState());
						sum_ear += pt.aggrs[j].num_ear;
					}
					for (int j = 1; j < pt.edges.length; j++) {
						// System.out.println(pt.edges[j].getState());
						sum_ear += pt.edges[j].num_ear;
					}
					// System.out.println();
				}
				// System.out.println(rate);
				System.out.println(df5.format(rate) + "\t" + Simusys.time() + "\t" + df5.format(sum_ear) + "\t" +Trace.seed);
				// System.out.println();
			}
			
			Simusys.iterate();
			if (Simusys.time() == 5 * Simusys.ticePerSecond()) {
				// Fails.F.addFailedLink(l);
				genIndependentLinkFailure(topo);
				// Fails.F.addFailedNode((Node) topo.getSwitchByAddress(a), topo.relatedLinks(a));
				// System.out.println(Fails.F.isolatedLinkFailure());
			}
			if (Simusys.time() == (20-5) * Simusys.ticePerSecond()) {
				// Fails.F.removeFailedLink(l);
				fixIndependentLinkFailure(topo);
				// Fails.F.removeFailedNode((Node) topo.getSwitchByAddress(a), topo.relatedLinks(a));
				// System.out.println(Fails.F.isolatedLinkFailure());
			}
		}
		
	}
	
	private static void genIndependentLinkFailure(Topology topo) {
		for (int i = 1; i <= 4; i++) {
			Fails.F.addFailedLink(topo.corelinks[i][i]);
		}
	}
	
	private static void fixIndependentLinkFailure(Topology topo) {
		for (int i = 1; i <= 4; i++) {
			Fails.F.removeFailedLink(topo.corelinks[i][i]);
		}
	}
}
