package p2pbed;

import java.util.Vector;

import protocol.TCPMessage;

import simulator.Address;
import simulator.Link;
import simulator.Node;
import support.EventManager;
import support.NetworkEvent;
import support.PDU;
import support.Simusys;
import trace.Trace;

public class Hub2 extends Node {
	
	private Link down, up;
	private Vector<NetworkEvent> down_receive_events = new Vector<NetworkEvent>();
	private Vector<NetworkEvent> up_receive_events = new Vector<NetworkEvent>();
	private Vector<NetworkEvent> down_send_events = new Vector<NetworkEvent>();
	private Vector<NetworkEvent> up_send_events = new Vector<NetworkEvent>();
	private int buffer_sz = 1000 * 1280;
	
	public Hub2(long address) {
		this.addr = new Address(address);
		EventManager.register(this);
	}

	@Override
	public String getState() {
		return "unimplemented yet";
	}

	@Override
	public long getAddress() {
		return addr.getAddress();
	}

	@Override
	public boolean performEvent(NetworkEvent event) {
		if (event.getTarget() == this) {
			if (event.getTime() == Simusys.time() && event.getType() == NetworkEvent.RECEIVE) {
				event.getRelatedLink().increaseQueueSize(this, event.getPDU().size);
				return receive(event);
			} else if (event.getType() == NetworkEvent.SEND)
				return send(event);
			else
				return false;
		} else
			System.err.println("Scheduler fail: Unmatched event " + event.getTarget().getName()
					+ " expected at " + event.getTime()
					+ ", at " + Simusys.time());
		return false;
	}

	private boolean send(NetworkEvent event) {
		if (!event.getRelatedLink().hasMoreSpace(this, event.getPDU().size)) {
			// link busy: stay in buffer
			return false;
		}
		
		// System.out.println(this.getName() + "send");
		event.setTarget(this);
		buffer_sz += event.getPDU().size;
		event.getRelatedLink().transmit(event);
		return true;
	}

	private boolean receive(NetworkEvent event) {
		event.setType(NetworkEvent.SEND);
		event.setTarget(this);
		
		if (event.getPDU().type == PDU.TCP) {
			TCPMessage m = (TCPMessage) event.getPDU();
			if (m.dest > addr.getAddress() && buffer_sz > m.size) { // from down to up
				up_send_events.add(event);
				buffer_sz -= m.size;
				event.setRelatedLink(up);
				return true;
			} else if (m.dest < addr.getAddress()  && buffer_sz > m.size) { // from up to down
				down_send_events.add(event);
				buffer_sz -= m.size;
				event.setRelatedLink(down);
				return true;
			}

			// hub has to drop
		}
		
		return true;
	}

	@Override
	public void addEvent(NetworkEvent e) {
		if (e.getType() == NetworkEvent.RECEIVE) {
			if (e.getRelatedLink() == up)
				up_receive_events.add(e);
			else if (e.getRelatedLink() == down)
				down_receive_events.add(e);
		} else {
			System.out.println(this.getName() + " Drop packet due to buffer overflow: " + buffer_sz + 
				" from " + ((TCPMessage) e.getPDU()).getSport() + 
				" Seq: " + ((TCPMessage) e.getPDU()).getSeq());
		}
	}

	public void setDown(Link down) {
		this.down = down;
	}

	public Link getDown() {
		return down;
	}

	public void setUp(Link up) {
		this.up = up;
	}

	public Link getUp() {
		return up;
	}

	@Override
	public boolean performEventsAt(long tick) {
		while (!up_send_events.isEmpty()) {
			// System.out.println(this.getName() + " receive At" + tick);
			NetworkEvent re = up_send_events.get(0);
			if (re.getType() != NetworkEvent.SEND) {
				System.err.println("NON-RECEIVE event in rb!");
				System.exit(0);
			}
			
			if (!this.performEvent(re))
				break;
			up_send_events.remove(0);
		}
		
		while (!down_send_events.isEmpty()) {
			// System.out.println(this.getName() + " receive At" + tick);
			NetworkEvent re = down_send_events.get(0);
			if (re.getType() != NetworkEvent.SEND) {
				System.err.println("NON-RECEIVE event in rb!");
				System.exit(0);
			}
			
			if (!this.performEvent(re))
				break;
			down_send_events.remove(0);
		}
		
		return true;
	}

	@Override
	public boolean performPendingEventsAt(long tick) {
		Vector<Vector<NetworkEvent>> rb = new Vector<Vector<NetworkEvent>>();
		rb.add(down_receive_events);
		rb.add(up_receive_events);
		
		while (!rb.isEmpty()) {
			int i = Trace.rand.nextInt(rb.size());
			if (rb.get(i).isEmpty()) {
				rb.remove(i);
				continue;
			}
			
			NetworkEvent se = rb.get(i).get(0);
			if (!this.performEvent(se)) {
				rb.remove(i);
				continue;
			}
			rb.get(i).remove(0);
		}
		
		return true;
	}

}
