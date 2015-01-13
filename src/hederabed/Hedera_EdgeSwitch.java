package hederabed;

import hederabed.GFF.Route;

import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;

import protocol.TCPMessage;

import simulator.Address;
import simulator.FlowId;
import simulator.Link;
import simulator.Node;
import support.EventManager;
import support.NetworkEvent;
import support.PDU;
import support.Simusys;
import trace.Trace;

public class Hedera_EdgeSwitch extends Node {
	private int k;
	private Vector<Link> upports;
	private Vector<Link> downports;
	private Vector<Vector<NetworkEvent>> up_receive_events;
	private Vector<Vector<NetworkEvent>> down_receive_events;
	private Vector<Vector<NetworkEvent>> up_send_events;
	private Vector<Vector<NetworkEvent>> down_send_events;
	private int buffer_sz;
	
	public Hedera_EdgeSwitch(Address address, int k) {
		this.addr = address;
		this.k = k;
		this.upports = new Vector<Link>(); // index 0 is unused
		this.downports = new Vector<Link>();
		
		this.up_receive_events = new Vector<Vector<NetworkEvent>>();
		this.down_receive_events = new Vector<Vector<NetworkEvent>>();
		this.up_send_events = new Vector<Vector<NetworkEvent>>();
		this.down_send_events = new Vector<Vector<NetworkEvent>>();
		
		this.buffer_sz = k * 1000 * Link.PSIZE;
		
		for (int i = 0; i <= k / 2; i++) {
			this.upports.add(null);
			this.downports.add(null);
			this.up_receive_events.add(new Vector<NetworkEvent>());
			this.down_receive_events.add(new Vector<NetworkEvent>());
			this.up_send_events.add(new Vector<NetworkEvent>());
			this.down_send_events.add(new Vector<NetworkEvent>());
		}
		
		EventManager.register(this);
	}
	
	public void setUpLink(int i, Link l) {
		if (i > 0 && i <= k / 2) {
			this.upports.set(i, l);
		}
	}
	
	public void setDownLink(int i, Link l) {
		if (i > 0 && i <= k/2) {
			this.downports.set(i, l);
		}
	}
	
	public Link getDownLink(int i) {
		return  this.downports.get(i);
	}

	@Override
	public void addEvent(NetworkEvent e) {
		if (e.getType() == NetworkEvent.RECEIVE) {
			this.getReceiveQueueByLink(e.getRelatedLink()).add(e);
		}
	}
	
	private Vector<NetworkEvent> getSendQueueByLink(Link l) {
		if (upports.contains(l)) {
			return up_send_events.get(upports.indexOf(l));
		} else 
			return down_send_events.get(downports.indexOf(l));
	}
	
	private Vector<NetworkEvent> getReceiveQueueByLink(Link l) {
		if (upports.contains(l)) {
			return up_receive_events.get(upports.indexOf(l));
		} else 
			return down_receive_events.get(downports.indexOf(l));
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
		
		Link link = null;
		if (event.getPDU().type == PDU.TCP) {
			TCPMessage m = (TCPMessage) event.getPDU();
			FlowId fid = new FlowId(m.src, m.dest, m.getSport(), m.getDport());
			Address dest = new Address(m.dest);
			int destpod = dest.getPod();
			int destedge = dest.getEdge();
			int desthost = dest.getHost();
			
			if (destpod == addr.getPod() && destedge == addr.getEdge()) {
				link = downports.get(desthost);
			} else if (GFF.getFlow(fid) != null) {
				Route r = GFF.addNewFlow(fid);
				link = upports.get(r.aggr);
			} else {
				link = upports.get(m.hash(k / 2, this.getAddress()));
			}
			
			/* else if (GFF.getFlow(fid) != null) {
				Route r = GFF.addNewFlow(fid);
				link = upports.get(r.aggr);
			} else {
				link = upports.get(m.hash(k / 2, this.getAddress()));
			} */
			
			/*else if (Hedera.allflows.contains(fid) && fid.isInterPod()) {
				long src = m.src;
				int core = Hedera.s.get(src);
				link = upports.get((core - 1) / (k / 2) + 1);
			} else {
				link = upports.get(m.hash(k / 2, this.getAddress()));
			}*/
		}
		
		if (buffer_sz >= event.getPDU().size) {
			this.getSendQueueByLink(link).add(event);
			event.setRelatedLink(link);
			buffer_sz -= event.getPDU().size;
			// System.out.println("Forward up a packet from " + m.getSport() + "@" + m.src + " upq left: " +  up.getUpq());
		} else {
			// System.err.println(this.getName() + "has to drop packs due to buffer overflow" + buffer_sz + " @ " + Simusys.time());
		}
		
		return true;
	}

	@Override
	public String getState() {
		return "unimplemented yet";
	}
	
	@Override
	public String getName() {
		return "Pod " + addr.getPod() + " Edge " + addr.getEdge();
	}

	@Override
	public boolean performEventsAt(long tick) {
		for (int i = 1; i < up_send_events.size(); i++) {
			Vector<NetworkEvent> sb = up_send_events.get(i);
			while (!sb.isEmpty()) {
				NetworkEvent re = sb.get(0);
				
				if (!this.performEvent(re))
					break;
				sb.remove(0);
			}
		}
		
		for (int i = 1; i < down_send_events.size(); i++) {
			Vector<NetworkEvent> sb = down_send_events.get(i);
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
		for (int i = 1; i < up_receive_events.size(); i++)
			rb.add(up_receive_events.get(i));
		for (int i = 1; i < down_receive_events.size(); i++)
			rb.add(down_receive_events.get(i));
		
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