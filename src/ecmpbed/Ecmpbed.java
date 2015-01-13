package ecmpbed;

import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.Vector;

import protocol.TCPBony;
import simulator.Address;
import support.Entity;
import support.EventManager;
import support.Fails;
import support.Simusys;
import trace.Trace;

public class Ecmpbed {
	public static void main(String[] args) {
		int k = 16;
		ECMP_Topology topo = new ECMP_Topology(k);
		int xmax = 1;
		TCPBony[] tcpc = new TCPBony[k * k * k / 4 * xmax];
		TCPBony[] tcps = new TCPBony[k * k * k / 4 * xmax];
		Trace.reset(k);
		
		int next = 0;
		int cnt = 0;
		for (int i = 1; i <= k; i++)
			for (int j = 1; j <= k / 2; j++)
				for (int m = 1; m <= k / 2; m++) {
					Address addr = new Address(i, 0, j, m);
					ECMP_Endhost end1 = topo.getHost(addr);
					// System.out.println(end1.getName());
					for (int x = 0; x < xmax; x++) {
					ECMP_Endhost end2 = topo.getHost(Trace.Stride(k, 64, addr));
					// System.out.println(end1.getName() + "->" + end2.getName());
					if (addr.getPod() != new Address(end2.getAddress()).getPod())
						cnt++;
					tcpc[next] = new TCPBony(end1, (short) (next + 1));
					tcps[next] = new TCPBony(end2, (short) (next + 1));
					tcpc[next].setPeer(tcps[next], true);
					tcps[next].setPeer(tcpc[next], false);
					// long x = end1.getAddress() + end2.getAddress() + next + 1 + next + 1;
					// System.out.println(next + " " + (int) (Math.abs(x) % (k / 2) + 1));
					next++;
					// System.out.println(new TCPMessage(0, end1.getAddress(), end2.getAddress(), (short) next, (short) next, 0).hash(k / 2));
					}
				}
		
		Simusys.reset();
		long end = 1 * 60 * Simusys.ticePerSecond();
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
				double rate = 0, amount = 0;
				DecimalFormat df5  = new DecimalFormat("##.00000");
				for (int i = 0; i < tcpc.length; i++) {
					// if (tcpc[i].getRate() <= 0)
					// System.out.println(tcpc[i].getName() + " : " + tcpc[i].getState());
					rate += tcpc[i].getInstantRate();
					amount += tcpc[i].getAmount();
				}
				for (int i = 1; i < topo.cores.length; i++) {
					// System.out.println(topo.cores[i].getName() + " : " + topo.cores[i].getState());
				}
				// System.out.println(rate);
				System.out.println(df5.format(rate) + "\t" + Simusys.time() + "\t" + df5.format(amount) + "\t" + df5.format(cnt));
				// System.out.println();
			}
			
			Simusys.iterate();
		}
		
	}
}
