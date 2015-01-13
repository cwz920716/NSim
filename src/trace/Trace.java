package trace;

import java.util.Random;
import java.util.Vector;

import simulator.Address;
import support.EventManager;

public class Trace {
	public static long seed = 1420870401736L; //System.currentTimeMillis();
	public static Random rand = new Random(seed);
	private static final int GRAN = 100;
	
	private static int bijnext = -1;
	private static Vector<Address> bij = new Vector<Address>();
	
	public static Address Stride(int k, int i, Address addr) {
		long num_host = k * k * k / 4;
		long x = (addr.getPod() - 1) * k * k / 4 + (addr.getEdge() - 1) * k / 2 + (int) addr.getHost() - 1;
		long y = (x + i) % num_host;
		long ypod = y / (k * k / 4) + 1;
		long yedge = (y - (ypod - 1) * k * k / 4) / (k / 2) + 1;
		long yhost = y - (ypod - 1) * k * k / 4 - (yedge - 1) * k / 2 + 1;
		
		return new Address((int) ypod, 0, (int) yedge, (int) yhost);
	}
	
	public static Address Stag(int k, double edgeP, double podP, Address ref) {
		int decision = StagDecide(edgeP, podP);
		int podi = 0, edgei = 0, hosti = 0;
		
		// System.out.println(decision);
		if (decision == 1) {
			podi = ref.getPod();
			edgei = ref.getEdge();
			do {
				hosti = rand.nextInt(k / 2) + 1;
			} while (hosti == ref.getHost());
		} else if (decision == 2) {
			podi = ref.getPod();
			do {
				edgei = rand.nextInt(k / 2) + 1;
			} while (edgei == ref.getEdge());
			hosti = rand.nextInt(k / 2) + 1;
		} else {
			Address addr = null;
			do {
				addr = Rand(k, ref);
				podi = addr.getPod();
			} while (podi == ref.getPod());
			edgei = addr.getEdge();
			hosti = addr.getHost();
		}
		
		Address addr = new Address(podi, 0, edgei, hosti);
		return addr;
	}
	
	private static int StagDecide(double edgeP, double podP) {
		
		int i = rand.nextInt(GRAN) + 1;
		
		if (i <= GRAN * edgeP)
			return 1;
		else if (i <= GRAN * (edgeP + podP))
			return 2;
		else
			return 3;
	}

	public static Address Rand(int k, Address ref) {
		int i = rand.nextInt(k * k * k / 4);
		int podi = i / (k * k / 4) + 1;
		int edgei = (i % (k * k / 4)) / (k / 2) + 1;
		int hosti = i % (k / 2) + 1;
		
		Address addr = new Address(podi, 0, edgei, hosti);
		if (addr.getAddress() == ref.getAddress())
			return Rand(k, ref);
		else
			return addr;
	}
	
	public static void reset(int k) {
		bijnext = 0;
		bij = new Vector<Address>();
		for (int pod = 1; pod <= k; pod++)
			for (int edge = 1; edge <= k / 2; edge++)
				for (int host = 1; host <= k / 2; host++)
					bij.add(new Address(pod, 0, edge, host));
		
		EventManager.shuffle(bij, rand);
	}
	
	public static Address Randbij(Address addr) {
		Address next = bij.get(bijnext);
		if (next.getAddress() == addr.getAddress()) {
			int x = rand.nextInt(bij.size() - (bijnext + 1)) + (bijnext + 1);
			Address a = bij.get(x);
			bij.set(bijnext, a);
			bij.set(x, next);
		}
		
		bijnext++;
		return bij.get(bijnext - 1);
	}
}
