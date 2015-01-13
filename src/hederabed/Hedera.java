package hederabed;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Random;
import java.util.Vector;

import simulator.Address;
import simulator.FlowId;
import support.Simusys;

public class Hedera {
	public static HashMap<Long, Integer> s = new HashMap<Long, Integer>();
	public static Hedera_Topology topo;
	public static int k;
	public static int T0 = 1000;
	public static Vector<FlowId> allflows = new Vector<FlowId>();
	public static Estimator es;
	private static Random rand = new Random(0);
	private static final int GRAN = 99;
	
	public static class LS {
		double up = 0;
		double down = 0;
		
		public boolean ok() {
			return up <= 1 && down <= 1;
		}
	}
	
	private static class Pair {
		long a;
		long b;
		
		public Pair(long a, long b) {
			this.a = a;
			this.b = b;
		}
	}
	
	public static void INIT_STATE() {
		es  = new Estimator(k * k * k / 4, k);
		
		int[] perm = new int[k * k / 4];
		for (int i = 0; i < k * k / 4; i++)
			perm[i] = i + 1;
		
		for (int pod = 1; pod <= k; pod++) {
			shuffle(perm);
			int next = 0;
			for (int edge = 1; edge <= k / 2; edge++) {
				for (int host = 1; host <= k / 2; host++) {
					s.put(new Address(pod,  0, edge, host).getAddress(), perm[next++]);
				}
			}
		}
		// System.out.println("xxx" + s.get(new Address(1,  0, 2, 1).getAddress()));
	}
	
	public static void LOOP() {
		es.ESTIMATE(allflows);
		// System.out.println(es);
		
		double eB = E(s);
		for (int T = T0; T >= 1; T--) {
			Pair p = NEIGHBOUR();
			SWAP(p);
			double eN = E(s);
			if (eN <= eB) {
				eB = eN;
			} else {
				double r = rand.nextDouble();
				// System.out.println(eB + " " + eN + " " + P(eB, eN, T) + " " + r);
				if (P(eB, eN, T) > r)
					eB = eN;
				else
					SWAP(p);
			}
		}
	}
	
	private static double P(double eB, double eN, int T) {
		// System.out.println(Math.pow(Math.E, 0.5 * (eB - eN) * T0 / T));
		return Math.pow(Math.E, 0.5 * (eB - eN) * T0 / T);
	}

	private static void SWAP(Pair p) {
		int corea = s.get(p.a);
		int coreb = s.get(p.b);
		if (corea != coreb) {
			s.put(p.a, coreb);
			s.put(p.b, corea);
		}
	}

	public static Pair NEIGHBOUR() {
		int i = rand.nextInt(GRAN) + 1;
		
		if (i < 33) {
			int pod = rand.nextInt(k) + 1;
			int edge = rand.nextInt(k / 2) + 1;
			int hostx = rand.nextInt(k / 2) + 1;
			int hosty = rand.nextInt(k / 2) + 1;
			Address x = new Address(pod, 0, edge, hostx);
			Address y = new Address(pod, 0, edge, hosty);
			
			return new Pair(x.getAddress(), y.getAddress());
		} else if (i < 66) {
			int pod = rand.nextInt(k) + 1;
			int edgex = rand.nextInt(k / 2) + 1;
			int hostx = rand.nextInt(k / 2) + 1;
			int edgey = rand.nextInt(k / 2) + 1;
			int hosty = rand.nextInt(k / 2) + 1;
			Address x = new Address(pod, 0, edgex, hostx);
			Address y = new Address(pod, 0, edgey, hosty);
			
			return new Pair(x.getAddress(), y.getAddress());
		} else {
			int podx = rand.nextInt(k) + 1;
			int pody = rand.nextInt(k) + 1;
			int edgex = rand.nextInt(k / 2) + 1;
			int hostx = rand.nextInt(k / 2) + 1;
			int edgey = rand.nextInt(k / 2) + 1;
			int hosty = rand.nextInt(k / 2) + 1;
			Address x = new Address(podx, 0, edgex, hostx);
			Address y = new Address(pody, 0, edgey, hosty);
			
			return new Pair(x.getAddress(), y.getAddress());
		}
	}
	
	private static void shuffle(int[] perm) {
		Random random = new Random(Simusys.time());
		
		int size = perm.length;
		while (size > 1) {
			int i = Math.abs(random.nextInt()) % size;
			int j = size - 1;
			if (i != j) {
				int x = perm[i];
				int y = perm[j];
				perm[i] = y;
				perm[j] = x;
			}
			size--;
		}
	}

	public static double E(HashMap<Long, Integer> State) {
		double energy = 0;
		LS[] hostlinks = new LS[k * k * k / 4 + 1];
		LS[] corelinks = new LS[k * k * k / 4 + 1];
		LS[] aggrlinks = new LS[k * k * k / 4 + 1];
		for (int i = 1; i <= k * k * k / 4; i++) {
			hostlinks[i] = new LS();
			corelinks[i] = new LS();
			aggrlinks[i] = new LS();
		}
		
		for (Enumeration<FlowId> enums = allflows.elements(); enums.hasMoreElements(); ) {
			FlowId fid = enums.nextElement();
			Address src = new Address(fid.src);
			int srcIndex = map(src.getAddress());
			Address dest = new Address(fid.dest);
			int destIndex = map(dest.getAddress());
			int core = State.get(src.getAddress());
			int srcedge = src.getEdge();
			int destedge = dest.getEdge();
			int srcpod = src.getPod();
			int destpod = dest.getPod();
			long srcEdgeAddress = new Address(srcpod, 0, srcedge, 0).getAddress();
			int aggr = (fid.isInterPod()) ? ((core - 1) / (k / 2) + 1) : (fid.hash(k / 2, srcEdgeAddress));
			
			hostlinks[srcIndex].up += es.getDemand(fid);
			hostlinks[destIndex].down += es.getDemand(fid);
			if (srcpod == destpod && srcedge == destedge)
				continue;

			if (srcpod != destpod) {
				int baseline = (core - 1) * k;
				// System.out.println(core + " " + baseline + " " + srcpod);
				corelinks[baseline + srcpod].up += es.getDemand(fid);
				corelinks[baseline + destpod].down += es.getDemand(fid);
			}
			
			int srcbase = ((srcpod - 1) * (k / 2) + srcedge - 1) * k / 2;
			aggrlinks[srcbase + aggr].up += es.getDemand(fid);
			
			int destbase = ((destpod - 1) * (k / 2) + destedge - 1) * k / 2;
			aggrlinks[destbase + aggr].down += es.getDemand(fid);
		}
		
		for (int i = 1; i <= k * k * k / 4; i++) {
			if (hostlinks[i].up > 1)
				energy += hostlinks[i].up - 1;
			if (hostlinks[i].down > 1)
				energy += hostlinks[i].down - 1;
			if (corelinks[i].up > 1)
				energy += corelinks[i].up - 1;
			if (corelinks[i].down > 1)
				energy += corelinks[i].down - 1;
			if (aggrlinks[i].up > 1)
				energy += aggrlinks[i].up - 1;
			if (aggrlinks[i].down > 1)
				energy += aggrlinks[i].down - 1;
		}
		
		return energy;
	}
	
	public static int map(long src) {
		Address addr = new Address(src);
		return (addr.getPod() - 1) * (k * k) / 4 + (addr.getEdge() - 1) * k / 2 + addr.getHost();
	}
}
