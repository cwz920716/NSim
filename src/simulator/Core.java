package simulator;

import java.util.Vector;

import detour.DetourMessage;

import protocol.TCPMessage;

import support.EventManager;
import support.Fails;
import support.NetworkEvent;
import support.PDU;
import support.Simusys;
import trace.Trace;

public class Core extends Node implements Switch {
	private int k;
	private Vector<Link> podports;
	private Vector<Vector<NetworkEvent>> pod_send_events;
	private Vector<Vector<NetworkEvent>> pod_receive_events;
	private int buffer_sz;
	private FlowTable table;
	private Measure medule;
	
	public Core(Address address, int k) {
		this.addr = address;
		this.k = k;
		this.table = new FlowTable(k, address);
		this.podports = new Vector<Link> ();; // index 0 is unused
		buffer_sz = k * 1000 * Link.PSIZE;
		pod_send_events = new Vector<Vector<NetworkEvent>> ();
		pod_receive_events = new Vector<Vector<NetworkEvent>> ();
		for (int i = 0; i <= k; i++) {
			// podbuf_sz[i] = Link.PSIZE * 500;
			pod_send_events.add(new Vector<NetworkEvent>());
			pod_receive_events.add(new Vector<NetworkEvent>());
			podports.add(null);
		}
		medule = new Measure(k, k);
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

	public boolean preprocess(NetworkEvent e) {
		if (e.getType() == NetworkEvent.RECEIVE) {
			// System.out.println("receive");
			PDU pdu = e.getPDU();
			if (pdu.type == PDU.LBDAR) {
				FlowMessage fm = (FlowMessage) pdu;
				if (!table.contains(fm.fid) || fm.timestamp > table.stamps.get(fm.fid))
					table.addInFlow(fm.fid, (Link) e.getRelatedLink(), fm.timestamp);
			}
		}
		
		return true;
	}
	
	@Override
	public boolean performEvent(NetworkEvent event) {
		if (event.getTarget() == this) {
			if (event.getType() == NetworkEvent.SEND)
				return send(event);
			else if (event.getTime() == Simusys.time() && event.getType() == NetworkEvent.RECEIVE) {
				medule.incrementIn(podports.indexOf(event.getRelatedLink()), event.getPDU().size);
				event.getRelatedLink().increaseQueueSize(this, event.getPDU().size);
				
				if (!preprocess(event))
					return true;
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
		medule.incrementOut(podports.indexOf(event.getRelatedLink()), event.getPDU().size);
		event.getRelatedLink().transmit(event);
		return true;
	}

	private boolean receive(NetworkEvent event) {
		// System.out.println(this.getName() + "recv @" + Simusys.time());
		event.setType(NetworkEvent.SEND);
		event.setTarget(this);
		
		PDU pdu = event.getPDU();
		Link link = null;
		
		if (pdu.type == PDU.DETOUR) {
			DetourMessage dm = (DetourMessage) pdu;
			// System.out.println("d: " + this.getName() + " " + dm.hop1);
			
			if (addr.getAddress() == dm.ddest.getAddress())
				event.setPDU(pdu.sdu);
			else if (addr.getAddress() == dm.hop1.getAddress())
				link = podports.get(Address.A2V(peers(false)).indexOf(dm.hop2));
			else if (addr.getAddress() == dm.hop2.getAddress())
				link = podports.get(Address.A2V(peers(false)).indexOf(dm.ddest));
			else 
				return true;
			// System.out.println(podports.indexOf(link));
			
			if (link != null && Fails.F.isfailedlink(link)) {
				return true;
			}
		}
		pdu = event.getPDU();
		
		if (pdu.type == PDU.LBDAR) {
			pdu = pdu.sdu;
		}
		
		if (pdu.type == PDU.TCP) {
			TCPMessage m = (TCPMessage) pdu;
			int destpod = new Address(m.dest).getPod();
			link = podports.get(destpod);
			if (Fails.F!= null && Fails.F.isfailedlink(link) && !Fails.F.isolatedLinkFailure()) {
				return true;
			}
			if (Fails.F!= null && Fails.F.isfailedlink(link) && Fails.F.isolatedLinkFailure()) {
				// System.out.println(this.getName() + ": Detour");
				DetourMessage dm = Fails.F.bypass(addr, link.getPeerAddress(addr), peers(false));
				dm.sdu = event.getPDU();
				dm.size = dm.sdu.size + 8 * 4;
				event.setPDU(dm);
				link = podports.get(Address.A2V(peers(false)).indexOf(dm.hop1));
				if (link == null || Fails.F.isfailedlink(link)) {
					return true;
				}
			} else if (Fails.F!= null && !Fails.F.okpath(addr, m.src, m.dest, destpod) && !Fails.F.isolatedLinkFailure()) {
				// System.out.println(this.getName() + "has to drop this packet due to network failure" + new Address(m.dest).getEdge());
				return true;
			}
		} else if (pdu.type == PDU.FCTRL) {
			FlowId fid = (( FlowControlMessage) event.getPDU() ).fid;
			if (!table.contains(fid)) {
				// System.out.println(this.getName() + "X");
				return true;
			}
			link = table.getFlowInPort(fid);
			if (Fails.F!= null && Fails.F.isfailedlink(link)) {
				// System.out.println(this.getName() + "has to drop this packet due to network failure" + new Address(m.dest).getEdge());
				return true;
			}
		}
		
		if (buffer_sz >= event.getPDU().size) {
			this.getSendQueueByLink(link).add(event);
			event.setRelatedLink(link);
			buffer_sz -= event.getPDU().size;
		}else
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
		res += buffer_sz;
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

	@Override
	public double[] portmeasurmentIn(boolean up) {
		return medule.getMeasurementIn();
	}

	@Override
	public double[] portmeasurmentOut(boolean up) {
		return medule.getMeasurementOut();
	}

	@Override
	public Address[] peers(boolean up) {
		Address[] pa = new Address[k + 1];
		for (int i = 1; i <= k; i++)
			pa[i] = podports.get(i).getPeerAddress(addr);
		return pa;
	}

	@Override
	public void disableLink(Link l) {
		medule.disable(podports.indexOf(l));
	}

}
