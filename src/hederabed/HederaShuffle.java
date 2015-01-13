package hederabed;

import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.Vector;

import simulator.Address;
import support.Entity;
import support.EventManager;
import support.NetworkEvent;
import support.Simusys;
import trace.Shuffle;

public class HederaShuffle {
	public static void main(String[] args) {
		int k = 4;
		Hedera.k = k;
		GFF.INIT(k);
		Hedera_Topology topo = new Hedera_Topology(k);
		Shuffle[] s = new Shuffle[k * k * k / 4];
		
		int next = 0;
		for (int i = 1; i <= k; i++)
			for (int j = 1; j <= k / 2; j++)
				for (int m = 1; m <= k / 2; m++) {
					Address addr = new Address(i, 0, j, m);
					// System.out.println(end1.getName());
					s[next] = new Shuffle(k, addr, next * k * k * k / 4, topo);
					next++;
				}
		
		Simusys.reset();
		int cnt = 0;
		while (true) {
			cnt = 0;
			for (int i = 0; i < s.length; i++) {
				if (s[i].next >= k * k * k / 4 - 1 && s[i].mapper != null && s[i].mapper.finished())
					cnt++;
			}
			if (cnt == s.length)
				break;
			
			for (int i = 0; i < s.length; i++) {
				s[i].startNewShuffle();
			}
			
			Vector<Entity> entities = EventManager.getEntities();
			
			for (Enumeration<Entity> enums = entities.elements(); enums.hasMoreElements(); ) {
				Entity e = enums.nextElement();
				e.performPendingEventsAt(Simusys.time());
			}
			
			for (Enumeration<Entity> enums = entities.elements(); enums.hasMoreElements(); ) {
				Entity e = enums.nextElement();
				e.performEventsAt(Simusys.time());
			}
			
			Simusys.iterate();

			if (Simusys.time() % (Simusys.ticePerSecond() / 10) == 0) {
				double rate = 0, r1 = 0, w2 = 0, maxr1 = 0, maxw2 = 0;
				DecimalFormat df5  = new DecimalFormat("##.00000");
				for (int i = 0; i < s.length; i++) {
					// if (tcpc[i].getRate() <= 0)
					// System.out.println(s[i].mapper.getName() + " : " + s[i].mapper.getState());
					rate += s[i].mapper.getRate(s[i].startt);
					r1 += s[i].mapper.getOIRatio();
					w2 += s[i].reducer.getOOWindow();
					if (s[i].mapper.getOIRatio() > maxr1)
						maxr1 = s[i].mapper.getOIRatio();
					if (s[i].reducer.getOOWindow() > maxw2)
						maxw2 = s[i].reducer.getOOWindow();
				}

				System.out.println(df5.format(rate) + "\t" + Simusys.time() + "\t" + r1 + "\t" + w2 + "\t" + maxr1 + "\t" + maxw2);
				// System.out.println();
			}

			// if (Simusys.time() % (Simusys.ticePerSecond() / 10) == 0) 
			//	Hedera.LOOP();
		}

		for (int i = 0; i < s.length; i++)
			System.out.println((int) (i + 1) + "\t" + s[i].completion);
		
	}
}
