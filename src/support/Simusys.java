package support;

public class Simusys {
	private static long tick = 0; // in 0.01 ms
	private static long overflow = 0;
	
	public static void iterate() {
		if (tick == Long.MAX_VALUE)
			overflow++;
		tick++;
	}
	
	public static long time() {
		return tick;
	}
	
	public static long overflow() {
		return overflow;
	}
	
	public static void reset() {
		tick = 0;
		overflow = 0;
	}
	
	public static long ticePerSecond() {
		return 100000;
	}

}
