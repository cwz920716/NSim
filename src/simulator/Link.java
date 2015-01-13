package simulator;

import support.NetworkEvent;
import support.Simusys;


public class Link {
	public static final int PSIZE = (int) (1280 * 1);
	protected Node down;
	protected Node up;
	
	protected double bandwidth; // in Gbps
	protected int delay; // in ticks
	protected int upq, downq; // queue size, in byte
	protected boolean failed = false;
	
	public Link(Node down, Node up) {
		this.down = down;
		this.up = up;
		
		this.bandwidth = 1;
		this.delay = 1;
		this.upq = this.downq = (int) (PSIZE * bandwidth * delay); // 10 packets in queue
	}
	
	public Link(Node down, Node up, int bandwidth, int delay) {
		this.down = down;
		this.up = up;
		
		this.bandwidth = bandwidth;
		this.delay = delay;
		this.upq = this.downq = (int) (PSIZE * bandwidth * delay); // 10 packets in queue
	}
	
	public Node getUpNode() {
		return up;
	}
	
	public Node getDownNode() {
		return down;
	}
	
	public Address getPeerAddress(Address addr) {
		if (addr.getAddress() == up.getAddress()) {
			return new Address(down.getAddress());
		} else if (addr.getAddress() == down.getAddress()) {
			return new Address(up.getAddress());
		}
		
		return null;
	}
	
	public long getDelay() {
		return delay;
	}
	
	public double getBandwidth() {
		return bandwidth;
	}

	public String getName() {
		return "<down=" + down.getName() + ", up=" + up.getName() + ">";
	}

	protected void reduceUpQueueSize(int size) {
		upq -= size;
		if(upq < 0)
			System.err.println(this.getName() + " Link Error: Up Queue Overflow!");
	}
	
	protected void increaseUpQueueSize(int size) {
		upq += size;
		if(upq > PSIZE * bandwidth * delay) {
			System.err.println(this.getName() + " Link Error: Up Queue Underflow!" + upq);
			throw new IndexOutOfBoundsException();
			// System.exit(0);
		}
	}

	protected void reduceDownQueueSize(int size) {
		downq -= size;
		if(downq < 0)
			System.err.println(this.getName() + " Link Error: Down Queue Overflow!");
	}
	
	protected void increaseDownQueueSize(int size) {
		downq += size;
		if(downq > PSIZE * bandwidth * delay)
			System.err.println(this.getName() + " Link Error: Down Queue Underflow!" + downq);
	}

	public boolean transmit(NetworkEvent event) {
		if (failed) {
			System.out.println("Transmit data to failed link!");
			return false;
		}
		
		event.setType(NetworkEvent.RECEIVE);
		event.setRelatedLink(this);
		event.setTime(Simusys.time() + delay);
		
		if (event.getTarget() == up) {
			event.setTarget(down);
			// System.out.println(event);
			reduceDownQueueSize(event.getPDU().size);
		} else if (event.getTarget() == down) {
			event.setTarget(up);
			// System.out.println(event);
			reduceUpQueueSize(event.getPDU().size);
		} else {
			System.out.println("Invalid addEvent: this link has no queue can carry this event");
			return false;
		}
		
		event.getTarget().addEvent(event);
		return true;
	}
	
	public boolean hasMoreSpace(Node src, int size) {
		if (failed)
			return false;
		
		if (src == down)
			return upq >= size;
		else if (src == up)
			return downq >= size;
		return false;
	}

	public String getState() {
		String res = "";
		
		res += "down queue size: [";
		res += downq;
		res += "] up queue size: [";
		res += upq;
		res += "]";
		
		return res;
	}

	public void increaseQueueSize(Node node, int size) {
		// System.out.println("increase qs");
		if (node == up) {
			increaseUpQueueSize(size);
		} else if (node == down) {
			increaseDownQueueSize(size);
		}
	}
	
	@Override
	public String toString() {
		return "up: " + up.getName() + "down: " + down.getName();
	}

	
	public void fail() {
		failed = true;
	}
	
	public boolean failed() {
		return failed;
	}
}
