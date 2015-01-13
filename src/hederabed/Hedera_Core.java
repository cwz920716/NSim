package hederabed;

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

public class Hedera_Core extends Node {
	private int k;
	private Vector<Link> podports;
	private Vector<Vector<NetworkEvent>> pod_send_events;
	private Vector<Vector<NetworkEvent>> pod_receive_events;
	private int buffer_sz;
	
	public Hedera_Core(Address address, int k) {
		this.addr = address;
		this.k = k;
		this.podports = new Vector<Link> ();; // index 0 is unused
		// podbuf_sz = new int[k + 1];
		buffer_sz = 1000 * k * Link.PSIZE;
		pod_send_events = new Vector<Vector<NetworkEvent>> ();
		pod_receive_events = new Vector<Vector<NetworkEvent>> ();
		for (int i = 0; i <= k; i++) {
			// podbuf_sz[i] = Link.PSIZE * 5000;
			pod_send_events.add(new Vector<NetworkEvent>());
			pod_receive_events.add(new Vector<NetworkEvent>());
			podports.add(null);
		}
		EventManager.register(this);
	}
	
	public void setPodLink(int i, Link l) {
		if (i > 0 && i <= k) {
			podports.set(i, l);
		}
	}
	
	public Link getPodLink(int i) {
		return podports.get(i);
	}

	@Override
	public void addEvent(NetworkEvent e) {
		if (e.getType() == NetworkEvent.RECEIVE) {
			this.getReceiveQueueByLink(e.getRelatedLink()).add(e);
		}
	}
	
	private Vector<NetworkEvent> getReceiveQueueByLink(Link relatedLink) {
		return pod_receive_events.get(podports.indexOf(relatedLink));
	}

	@Override
	public boolean performEvent(NetworkEvent event) {
		if (event.getTarget() == this) {
			if (event.getType() == NetworkEvent.SEND)
				return send(event);
			else if (event.getTime() == Simusys.time() && event.getType() == NetworkEvent.RECEIVE) {
				event.getRelatedLink().increaseQueueSize(this, event.getPDU().size);
				return receive(event);
			}
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
		
		// System.out.println(this.getName() + "send @" + Simusys.time());
		event.setTarget(this);
		buffer_sz += event.getPDU().size;
		event.getRelatedLink().transmit(event);
		return true;
	}
	
	private boolean receive(NetworkEvent event) {
		// System.out.println(this.getName() + "recv @" + Simusys.time());
		event.setType(NetworkEvent.SEND);
		event.setTarget(this);
		
		PDU pdu = event.getPDU();
		Link link = null;
		
		if (pdu.type == PDU.TCP) {
			TCPMessage m = (TCPMessage) pdu;
			int destpod = new Address(m.dest).getPod();
			link = podports.get(destpod);
		}
		
		if (buffer_sz >= event.getPDU().size) {
			this.getSendQueueByLink(link).add(event);
			event.setRelatedLink(link);
			buffer_sz -= event.getPDU().size;
			// System.out.println("Forward up a packet from ");
		} else
			System.err.println(this.getName() + "has to drop packs due to buffer overflow" + buffer_sz + " @ " + Simusys.time());
		
		return true;
	}

	private Vector<NetworkEvent> getSendQueueByLink(Link link) {
		return pod_send_events.get(podports.indexOf(link));
	}

	@Override
	public String getState() {
		String res = "[";
		// for (int i = 0; i <= k; i ++)
		//	res += podbuf_sz[i] + ", ";
		res += "]";
		return res;
	}
	
	@Override
	public String getName() {
		return "Core " + addr.getPod();
	}

	@Override
	public boolean performEventsAt(long tick) {
		for (int i = 1; i < pod_send_events.size(); i++) {
			Vector<NetworkEvent> sb = pod_send_events.get(i);
			while (!sb.isEmpty()) {
				NetworkEvent re = sb.get(0);
				
				if (!this.performEvent(re))
					break;
				sb.remove(0);
			}
		}
		
		return true;
	}

	@Override
	public boolean performPendingEventsAt(long tick) {
		
		Vector<Vector<NetworkEvent>> rb = new Vector<Vector<NetworkEvent>>();
		for (int i = 1; i < pod_receive_events.size(); i++)
			rb.add(pod_receive_events.get(i));
		
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
