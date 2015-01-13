package standalone;

import java.util.Random;

public class ControlLoop {
	public static void main(String[] args) {
		int num_flows = 250000;
		int[] flows = new int[num_flows];
		int k = 48;
		int[] nlinks = new int[k / 2];
		for (int i = 0; i < num_flows; i++) {
			int x = new Random().nextInt(nlinks.length);
			nlinks[x]++;
			flows[i] = x;
		}

		long sys_start = System.currentTimeMillis();
		int min = -1, mini = -1, sum = 0;
		for (int i = 0; i < nlinks.length; i++) {
			if (min == -1 || nlinks[i] < min) {
				mini = i;
				min = nlinks[i];
			}
			sum += nlinks[i];
		}
		double avg = sum / (double) k;
		
		for (int i = 0; i < num_flows; i++) {
			int l = flows[i];
			if (nlinks[l] - avg >= 1 || (min == 0 && nlinks[l] > 1))
				break;
		}
		long sys_end = System.currentTimeMillis();
		System.out.println(sys_end - sys_start);
	}
}
