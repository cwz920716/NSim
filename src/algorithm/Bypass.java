package algorithm;

import trace.Trace;

public class Bypass {
	public static class Detour {
		public int hop1, hop2;
		
		public Detour(int hop1, int hop2) {
			this.hop1 = hop1;
			this.hop2 = hop2;
		}
	}
	
	public static Detour computeBypass(int k1, int k2, double[] links0, double[][] links1, double[] links2) {
		double[][] detours = new double[k1 + 1][k2 + 1];
		int mini = -1, minj = -1;
		double mindetour = -1;
		for (int i = 1; i <= k1; i++) {
			for (int j = 1; j <= k2; j++) {
				double link0 = links0[i];
				double link1 = links1[i][j];
				double link2 = links2[j];
				// System.out.print("[" + link0 + "," + link1 + "," + link2 + "] ");
				double min = Math.min(Math.min(link0, link1), link2);
				if (min < 0) {
					detours[i][j] = min;
					continue;
				}
				detours[i][j] = Math.max(Math.max(link0, link1), link2);
				if (mindetour < 0 || (detours[i][j] < mindetour)) {
					mini = i;
					minj = j;
					mindetour = detours[i][j];
				}
			}
			// System.out.println();
		}
		
		while (true) {
			int randi = Trace.rand.nextInt(k1) + 1;
			int randj = Trace.rand.nextInt(k2) + 1;
			if (detours[randi][randj] >= 0) {
				mini = randi;
				minj = randj;
				break;
			}
		}
		
		return new Detour(mini, minj);
	}
	
	public static void main(String[] args) {
		int k = 6;
		double[] links0 = {0, -1, 0.5, 1.2};
		double[][] links1 = {{0, 0, 0, 0}, {0, -1, 1.2, 1.6}, {0, 2.4, -1, 1.5}, {0, 2.2, 1.4, 6.7}};
		double[] links2 = {0, -1, 0.9, 1.3};
		Detour d = Bypass.computeBypass(3, 3, links0, links1, links2);
		System.out.println(d.hop1 + "->" + d.hop2);
	}
}
