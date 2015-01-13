package simulator;

import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;

import detour.DetourMessage;
import protocol.TCPMessage;
import support.EventManager;
import support.Fails;
import support.NetworkEvent;
import support.PDU;
import support.Simusys;
import trace.Trace;

public class EdgeSwitch extends Node implements Switch {
	private int k;
	private Vector<Link> upports;
	private Vector<Link> downports;
	private Vector<Vector<NetworkEvent>> up_receive_events;
	private Vector<Vector<NetworkEvent>> down_receive_events;
	private Vector<Vector<NetworkEvent>> up_send_events;
	private Vector<Vector<NetworkEvent>> down_send_events;
	private FlowTable table;
	public int num_ear = 0;
	private int buffer_sz;
	private long lastDifsTick = 0;
	private long preDifsShift = 0;
	private static final long PERIOD = 1000;
	private static final double RANGE = 0.05;

	private Measure upm, downm;
	
	public EdgeSwitch(Address address, int k) {
		this.addr = address;
		this.k = k;
		this.table = new FlowTable(k, address);
		this.upports = new Vector<Link>(); // index 0 is unused
		this.downports = new Vector<Link>(); // index 0 is unused
		
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
		upm = new Measure(k, k / 2);
		downm = new Measure(k, k / 2);
		EventManager.register(this);
	}
	
	public void setUpLink(int i, Link l) {
		if (i > 0 && i <= k / 2) {
			this.upports.set(i, l);
		}
	}
	
	public Link getUpLink(int i) {
		return  this.upports.get(i);
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
	
	private boolean preprocess(NetworkEvent e) {
		if (e.getType() == NetworkEvent.RECEIVE) {
			if (e.getPDU().type == PDU.TCP || e.getPDU().type == PDU.LBDAR || e.getPDU().type == PDU.FCTRL) {
				Link src = e.getRelatedLink();
				
				// accept a flow control message
				if (e.getPDU().type == PDU.FCTRL) {
					// execute this hint
					FlowControlMessage fcm = (FlowControlMessage) e.getPDU();
					FlowId fid = fcm.fid;
					Address fsrc = new Address(fid.src);
					Address fdest = new Address(fid.dest);
					
					// an inter pod flow have to change its output port
					if (fsrc.getPod() != fdest.getPod() && fsrc.getPod() == addr.getPod() && fcm.core == 0) {
						// execute this message
						int iout = upports.indexOf(table.getFlowOutPort(fid));
						int iout2 = fcm.aggr;
						// System.out.println(this.getName() + " " + iout + "->" + iout2);
						if (iout == iout2 || iout == 0)
							return false;
						short dpod = fdest.getPod();
						Link outport = upports.get(iout2);
						if (table.inter_out[iout][0] + table.intra_out[iout][0] <= table.inter_out[iout2][0] + table.intra_out[iout2][0]) {
							// do swap
							for (Enumeration<FlowId> enums = table.states.keys(); enums.hasMoreElements(); ) {
								FlowId fid2 = enums.nextElement();
								Address src2 = new Address(fid2.src);
								Address dest2 = new Address(fid2.dest);
								if (table.getFlowOutPort(fid2) == outport && src2.getPod() != dest2.getPod()/* && new Address(fid2.dest).getPod() == dpod*/) {
									table.addOutFlow(fid2, upports.get(iout));
									int dpod2 = dest2.getPod();
									table.inter_out[iout][dpod2]++;
									table.inter_out[iout][0]++;
									table.inter_out[iout2][dpod2]--;
									table.inter_out[iout2][0]--;
									break;
								}
								
								if (table.getFlowOutPort(fid2) == outport && src2.getPod() == dest2.getPod()/* && new Address(fid2.dest).getEdge() == dedge*/) {
									table.addOutFlow(fid2, upports.get(iout));
									int dedge2 = dest2.getEdge();
									table.intra_out[iout][dedge2]++;
									table.intra_out[iout][0]++;
									table.intra_out[iout2][dedge2]--;
									table.intra_out[iout2][0]--;
									break;
								}
							}
						}
						table.inter_out[iout][dpod]--;
						table.inter_out[iout][0]--;
						table.inter_out[iout2][dpod]++;
						table.inter_out[iout2][0]++;
						table.addOutFlow(fid, outport);
						return false;
					} else if (fsrc.getPod() == fdest.getPod() && fsrc.getEdge() == addr.getEdge() && fcm.core == 0) {
						int iout = upports.indexOf(table.getFlowOutPort(fid));
						int iout2 = fcm.aggr;
						if (iout == iout2 || iout == 0)
							return false;
						// System.out.println(this.getName() + " " + iout + "->" + iout2);
						short dedge = fdest.getEdge();
						Link outport = upports.get(iout2);
						if (table.inter_out[iout][0] + table.intra_out[iout][0] <= table.inter_out[iout2][0] + table.intra_out[iout2][0]) {
							// do swap
							for (Enumeration<FlowId> enums = table.states.keys(); enums.hasMoreElements(); ) {
								FlowId fid2 = enums.nextElement();
								Address src2 = new Address(fid2.src);
								Address dest2 = new Address(fid2.dest);
								if (table.getFlowOutPort(fid2) == outport && src2.getPod() != dest2.getPod()/* && new Address(fid2.dest).getPod() == dpod*/) {
									table.addOutFlow(fid2, upports.get(iout));
									int dpod2 = dest2.getPod();
									table.inter_out[iout][dpod2]++;
									table.inter_out[iout][0]++;
									table.inter_out[iout2][dpod2]--;
									table.inter_out[iout2][0]--;
									break;
								}
								
								if (table.getFlowOutPort(fid2) == outport && src2.getPod() == dest2.getPod()/* && new Address(fid2.dest).getEdge() == dedge*/) {
									table.addOutFlow(fid2, upports.get(iout));
									int dedge2 = dest2.getEdge();
									table.intra_out[iout][dedge2]++;
									table.intra_out[iout][0]++;
									table.intra_out[iout2][dedge2]--;
									table.intra_out[iout2][0]--;
									break;
								}
							}
						}
						
						table.intra_out[iout][dedge]--;
						table.intra_out[iout][0]--;
						table.intra_out[iout2][dedge]++;
						table.intra_out[iout2][0]++;
						table.addOutFlow(fid, outport);
						return false;
					}
					
					System.err.println("FCTRL X");
					return false;
				} 
				
				if (e.getPDU().type == PDU.LBDAR) {
					FlowMessage fm = ((FlowMessage) e.getPDU()); 
					FlowId fid = fm.fid;
					Address fsrc = new Address(fid.src);
					Address fdest = new Address(fid.dest);
					
					if (table.contains(fid) && table.getFlowInPort(fid) != src && fm.timestamp > table.stamps.get(fid)) { // a flow has changed its path
						
						if ( (fsrc.getPod() != fdest.getPod() && fdest.getPod() == addr.getPod()) ||
								(fsrc.getPod() == fdest.getPod() && fdest.getPod() == addr.getPod() && fsrc.getEdge() != addr.getEdge()) ) { // has multi port to come in
							Link inport = table.in.get(fid);
							int ip = upports.indexOf(inport);
							table.ins[ip]--;
							ip = upports.indexOf(src);
							table.ins[ip]++;
						}
						table.addInFlow(fid, src, fm.timestamp);
						table.touch(fid);
					} if (!table.contains(fid)) { // find a new flow
						// System.out.println(this.getName() + " find a new flow " + table.size());
						table.addInFlow(fid, src, fm.timestamp);
						int ip = upports.indexOf(src);
						if (fsrc.getPod() == fdest.getPod() && fdest.getPod() == addr.getPod() && fsrc.getEdge() != addr.getEdge()) {
							table.ins[ip]++;
						} else if (fsrc.getPod() != fdest.getPod() && fdest.getPod() == addr.getPod()) {
							table.ins[ip]++;
						}
						// register the flow into the incoming matrix
						
						if (fsrc.getPod() == fdest.getPod() && fdest.getPod() == addr.getPod()) { // intra pod flow
							if (fdest.getEdge() == addr.getEdge()) { // has only one port to forward
								// System.out.println("intra only one");
								table.hosts[fdest.getHost()]++;
								table.addOutFlow(fid, this.downports.get(fdest.getHost()));
								table.touch(fid);
							} else { // has multi ports to forward
								short edge = fdest.getEdge();
								Vector<Link> available = upports; // get available links to forward this flow
								// search available links for a link with smallest number to that pod
								int min = -1;
								for (int i = 1; i < available.size(); i++) {
									if (min == -1 || table.intra_out[i][0] < min) // fetch available link to find the smallest flow to edge dest
										min = table.intra_out[i][0];
								}
								
								Vector<Integer> least = new Vector<Integer>();
								for (int i = 1; i < available.size(); i++) {
									if (true || table.intra_out[i][0] == min) // fetch all links with the smallest flow to edge dest
										least.add(i);
								}
								
								int id = -1;
								if (least.size() == 1) { // only one smallest link
									id = least.get(0);
								} else { // more than one smallest link
									int minall = -1;
									for (int i = 0; i < least.size(); i++) {
										if (minall == -1 || table.inter_out[least.get(i)][0] + table.intra_out[least.get(i)][0] < minall) // fetch selected link to find the smallest flow in all
											minall = table.inter_out[least.get(i)][0] + table.intra_out[least.get(i)][0];
									}
									
									Vector<Integer> least2 = new Vector<Integer>();
									for (int i = 0; i < least.size(); i++) {
										if (table.inter_out[least.get(i)][0] + table.intra_out[least.get(i)][0] == minall) // fetch all links with smallest flow in all
											least2.add(least.get(i));
									}
									
									if (least2.size() == 1) {
										id = least2.get(0);
									} else {
										id = least2.get( Math.abs( Trace.rand.nextInt( least2.size() ) ) );
									}
								}
								// System.out.println(id);
								table.intra_out[id][edge]++;
								table.intra_out[id][0]++;
								table.addOutFlow(fid, upports.get(id));
								table.touch(fid);
							}
						} else if (fsrc.getPod() != fdest.getPod() && fsrc.getPod() == addr.getPod()) { // has multi ports to forward
							short pod = fdest.getPod();
							Vector<Link> available = upports; // get available links to forward this flow
							// search available links for a link with smallest number to that pod
							int min = -1;
							for (int i = 1; i < available.size(); i++) {
								if (min == -1 || table.inter_out[i][pod] < min) // fetch available link to find the smallest flow to pod dest
									min = table.inter_out[i][pod];
							}
							
							Vector<Integer> least = new Vector<Integer>();
							for (int i = 1; i < available.size(); i++) {
								if (true || table.inter_out[i][pod] == min) // fetch all links with the smallest flow to pod dest
									least.add(i);
							}
							
							int id = -1;
							if (least.size() == 1) { // only one smallest link
								id = least.get(0);
							} else { // more than one smallest link
								int minall = -1;
								for (int i = 0; i < least.size(); i++) {
									if (minall == -1 || table.inter_out[least.get(i)][0] + table.intra_out[least.get(i)][0] < minall) // fetch selected link to find the smallest flow in all
										minall = table.inter_out[least.get(i)][0] + table.intra_out[least.get(i)][0];
								}
								
								Vector<Integer> least2 = new Vector<Integer>();
								for (int i = 0; i < least.size(); i++) {
									if (table.inter_out[least.get(i)][0] + table.intra_out[least.get(i)][0] == minall) // fetch all links with smallest flow in all
										least2.add(least.get(i));
								}
								
								if (least2.size() == 1) {
									id = least2.get(0);
								} else {
									
									id = least2.get( Math.abs( Trace.rand.nextInt( least2.size() ) ) );
								}
							}
							// System.out.println(id);
							table.inter_out[id][pod]++;
							table.inter_out[id][0]++;
							table.addOutFlow(fid, upports.get(id));
							table.touch(fid);
						} else if (fsrc.getPod() != fdest.getPod() && fdest.getPod() == addr.getPod()) { // has only one ports to forward
							short edge = fdest.getEdge();
							short host = fdest.getHost();
							if (edge == addr.getEdge()) {
								table.addOutFlow(fid, this.downports.get(host));
								table.touch(fid);
								table.hosts[host]++;
								// table.increaseOutFlowIntra(host + k / 2);
							} else 
								System.err.println("a flow is routed to uncompliable edge");
						} else
							System.err.println("a flow is routed to uncompliable pod");
					}
				}
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
				if (upports.contains(event.getRelatedLink())) {
					upm.incrementIn(upports.indexOf(event.getRelatedLink()), event.getPDU().size);
				} else
					downm.incrementIn(downports.indexOf(event.getRelatedLink()), event.getPDU().size);
				event.getRelatedLink().increaseQueueSize(this, event.getPDU().size);
				
				if (!preprocess(event))
					return true;
				return receive(event);
			} else if (event.getType() == NetworkEvent.REFLECT)
				return reflect(event);
		} else
			System.err.println("Scheduler fail: Unmatched event " + event.getTarget().getName()
					+ " expected at " + event.getTime()
					+ ", at " + Simusys.time());
		return false;
	}

	private boolean receive(NetworkEvent event) {
		// System.out.println(this.getName() + "recv @" + Simusys.time());
		event.setType(NetworkEvent.SEND);
		event.setTarget(this);
		
		Link link = null;
		if (event.getPDU().type == PDU.DETOUR) {
			DetourMessage dm = (DetourMessage) event.getPDU();
			
			if (addr.getAddress() == dm.ddest.getAddress())
				event.setPDU(event.getPDU().sdu);
			else if (addr.getAddress() == dm.hop1.getAddress()) {
				if (Address.A2V(peers(true)).contains(dm.hop2))
					link = upports.get(Address.A2V(peers(true)).indexOf(dm.hop2));
				else
					link = downports.get(Address.A2V(peers(false)).indexOf(dm.hop2));
			} else if (addr.getAddress() == dm.hop2.getAddress()) {
				if (Address.A2V(peers(true)).contains(dm.ddest))
					link = upports.get(Address.A2V(peers(true)).indexOf(dm.ddest));
				else
					link = downports.get(Address.A2V(peers(false)).indexOf(dm.ddest));
			}
			else
				return true;
			
			if (link != null && Fails.F.isfailedlink(link)) {
				return true;
			}
		}
		
		if (event.getPDU().type == PDU.FCTRL) {
			// System.out.println(this.getName() + " receive fctl @" + Simusys.time());
			FlowId fid = (( FlowControlMessage) event.getPDU() ).fid;
			if (!table.contains(fid)) {
				// System.exit(0);
				return true;
			}
			link = table.getFlowInPort(fid);
			if (Fails.F!= null && Fails.F.isfailedlink(link)) {
				return true;
			}
		} if (event.getPDU().type == PDU.LBDAR) {
			FlowId fid = ((FlowMessage) event.getPDU()).fid;
			if (table.contains(fid)) {
				link = table.getFlowOutPort(fid);
				table.touch(fid);
			} else
				System.err.println(this.getName() + " : An unregistered flow " + fid);
			
			int index = -1;
			if (upports.contains(link))
				index = upports.indexOf(link);
			else
				index = downports.indexOf(link) + k / 2;
			if (Fails.F!= null && !Fails.F.okpath(addr, fid.src, fid.dest, index) && index > k/2) {
				// System.out.println(this.getName() + " : Drop downhill packets...");
				return true;
			}
			if (Fails.F!= null && Fails.F.isfailedlink(link) && Fails.F.isolatedLinkFailure()) {
				DetourMessage dm = Fails.F.bypass(addr, link.getPeerAddress(addr), peers(link.getPeerAddress(addr).isAggr()));
				dm.sdu = event.getPDU();
				dm.size = dm.sdu.size + 8 * 4;
				event.setPDU(dm);
				if (Address.A2V(peers(true)).contains(dm.hop1))
					link = upports.get(Address.A2V(peers(true)).indexOf(dm.hop1));
				else
					link = downports.get(Address.A2V(peers(false)).indexOf(dm.hop1));
				if (link != null && Fails.F.isfailedlink(link)) {
					return true;
				}
			} else if (Fails.F!= null && !Fails.F.okpath(addr, fid.src, fid.dest, index) && !Fails.F.isolatedLinkFailure()) {
				TCPMessage m = (TCPMessage) event.getPDU().sdu;
				int num = Fails.F.getUsefulUphillPortNum(addr, fid.src, fid.dest);
				// System.out.println(this.getName() + " has failed with num = " + num);
				if (Trace.rand.nextInt(10) > 3) {
					// return true;
				}
				if (num == 0) {
					// System.out.println(this.getName() + "has to drop uphill packet due to network failure and num == 0");
					return true;
				} else {
					int subsititude = Trace.rand.nextInt(num) + 1, next = 0;
					for (int i = 1; i <= k / 2; i++) {
						if (Fails.F.okpath(addr, fid.src, fid.dest, i))
							next++;
						if (next == subsititude) {
							link = upports.get(i);
							break;
						}
					}
				}
				// return true;
			}
		} else if (event.getPDU().type == PDU.TCP) {
			TCPMessage m = (TCPMessage) event.getPDU();
			Address dest = new Address(m.dest);
			int destpod = dest.getPod();
			int destedge = dest.getEdge();
			int desthost = dest.getHost();
			
			if (destpod == addr.getPod() && destedge == addr.getEdge()) {
				link = downports.get(desthost);
				if (Fails.F!= null && !Fails.F.okpath(addr, m.src, m.dest, destedge + k / 2)) {
					System.out.println(this.getName() + "has to drop downhill packet due to network failure");
					return true;
				}
			} else {
				link = upports.get(m.hash(k / 2, this.getAddress()));
				if (Fails.F!= null && !Fails.F.okpath(addr, m.src, m.dest, m.hash(k / 2, this.getAddress()))) {
					int num = Fails.F.getUsefulUphillPortNum(addr, m.src, m.dest);
					// if (m.dest == new Address(3, 0, 1, 2).getAddress())
					//    System.out.println(this.getName() + "X" + m.hash(k / 2));
					if (num == 0) {
						// System.out.println(this.getName() + "has to drop this packet due to network failure" + new Address(m.dest));
						return true;
					} else {
						int substitude = m.hash(num), next = 0;
						for (int i = 1; i <= k / 2; i++) {
							if (Fails.F.okpath(addr, m.src, m.dest, i))
								next++;
							if (next == substitude) {
								link = upports.get(i);
								break;
							}
						}
					}
				}
			}
		}
		
		if (buffer_sz >= event.getPDU().size) {
			this.getSendQueueByLink(link).add(event);
			event.setRelatedLink(link);
			buffer_sz -= event.getPDU().size;
			// System.out.println("Forward up a packet from " + m.getSport() + "@" + m.src + " upq left: " +  up.getUpq());
		} else
			System.err.println(this.getName() + "has to drop packs due to buffer overflow" + buffer_sz + " @ " + Simusys.time());
		
		return true;
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

	private boolean send(NetworkEvent event) {
		if (!event.getRelatedLink().hasMoreSpace(this, event.getPDU().size)) {
			// link busy: stay in buffer
			return false;
		}
		
		// System.out.println(this.getName() + "send @" + Simusys.time());
		event.setTarget(this);
		buffer_sz += event.getPDU().size;
		if (upports.contains(event.getRelatedLink())) {
			upm.incrementOut(upports.indexOf(event.getRelatedLink()), event.getPDU().size);
		} else
			downm.incrementOut(downports.indexOf(event.getRelatedLink()), event.getPDU().size);
		event.getRelatedLink().transmit(event);
		return true;
	}

	private boolean reflect(NetworkEvent event) {
		long cur = Simusys.time();
		Vector<FlowId> disappeared = new Vector<FlowId>();
		for (Enumeration<FlowId> enums = table.states.keys(); enums.hasMoreElements(); ) {
			FlowId fid = enums.nextElement();
			long last = table.states.get(fid);
			if (cur - last > FlowTable.TIMEOUT) {
				disappeared.add(fid);
			}
		}
		
		for (Enumeration<FlowId> enums = disappeared.elements(); enums.hasMoreElements(); ) {
			FlowId fid = enums.nextElement();
			table.states.remove(fid);
			Link inport = table.in.get(fid);
			Address fsrc = new Address(fid.src);
			Address fdest = new Address(fid.dest);
			if (fsrc.getPod() != fdest.getPod() && fdest.getPod() == addr.getPod()) { // has multi port to come in
				int ip = upports.indexOf(inport);
				table.ins[ip]--;
			} else if(fsrc.getPod() == fdest.getPod() && fdest.getPod() == addr.getPod() && fsrc.getEdge() != addr.getEdge()) {
				int ip = upports.indexOf(inport);
				table.ins[ip]--;
			}
			Link outport = table.out.get(fid);
			int oi = upports.indexOf(outport);
			
			if (fsrc.getPod() != fdest.getPod() && fsrc.getPod() == addr.getPod()) {
				table.inter_out[oi][fdest.getPod()]--;
				table.inter_out[oi][0]--;
			} else if (fsrc.getPod() == fdest.getPod() && fdest.getEdge() != addr.getEdge()) {
				table.intra_out[oi][fdest.getEdge()]--;
				table.intra_out[oi][0]--;
			}
			
			table.in.remove(fid);
			table.out.remove(fid);
		}
		
		// if some port now has no flow but some port now has more than one flow, then transfer the flow to the output port
		
		// for (int pod = 1; pod <= k; pod++) {
		//	if (pod == addr.getPod())
		//		continue;
			
		int min = -1, max = -1, maxi = -1, sum = 0, mini = -1;
		for (int i = 1; i <= k / 2; i++) {
			if (min == -1 || table.inter_out[i][0] + table.intra_out[i][0] < min) {
				min = table.inter_out[i][0] + table.intra_out[i][0];
				mini = i;
			}
			if (max == -1 || table.inter_out[i][0] + table.intra_out[i][0] > max) {
				maxi = i;
				max = table.inter_out[i][0] + table.intra_out[i][0];
			}
			sum += table.inter_out[i][0] + table.intra_out[i][0];
		}
		double avg = sum / (double) (k / 2);
		if (max - avg >= 1 || (max > 1 && min == 0)) {
			for (Enumeration<FlowId> enums = table.states.keys(); enums.hasMoreElements(); ) {
				FlowId fid2 = enums.nextElement();
				Address fsrc2 = new Address(fid2.src);
				Address fdest2 = new Address(fid2.dest);
				if (table.getFlowOutPort(fid2) == upports.get(maxi) && fsrc2.getPod() != fdest2.getPod()) {
					int pod = fdest2.getPod();
					table.addOutFlow(fid2, upports.get(mini));
					table.inter_out[maxi][pod]--;
					table.inter_out[maxi][0]--;
					table.inter_out[mini][pod]++;
					table.inter_out[mini][0]++;
					break;
				}
				
				if (table.getFlowOutPort(fid2) == upports.get(maxi) && fsrc2.getPod() == fdest2.getPod()) {
					table.addOutFlow(fid2, upports.get(mini));
					int edge = fdest2.getEdge();
					table.intra_out[maxi][edge]--;
					table.inter_out[maxi][0]--;
					table.intra_out[mini][edge]++;
					table.intra_out[mini][0]++;
					break;
				}
			}
		}
		
		// is there are some unbalanced incoming flow
		min = -1; max = -1; sum = 0; mini = -1;
		for (int i = 1; i <= k / 2; i++) {
			if (min == -1 || table.ins[i] < min) {
				min = table.ins[i];
				mini = i;
			}
			if (max == -1 || table.ins[i] > max) {
				max = table.ins[i];
			}
			sum += table.ins[i];
		}
		avg = sum / (double) (k / 2);

		Vector<FlowId> fids = new Vector<FlowId>(); 
		for (Enumeration<FlowId> enums = table.states.keys(); enums.hasMoreElements(); ) {
			FlowId fid = enums.nextElement();
			Address src = new Address(fid.src);
			Address dest = new Address(fid.dest);
			if (src.getPod() != dest.getPod() && src.getPod() != addr.getPod())
				fids.add(fid);
			else if (src.getPod() == dest.getPod() && src.getEdge() != addr.getEdge())
				fids.add(fid);
		}
		
		EventManager.shuffle(fids);
		
		for (Enumeration<FlowId> enums = fids.elements(); enums.hasMoreElements(); ) {
			FlowId fid = enums.nextElement();
			int fin = table.ins[upports.indexOf(table.getFlowInPort(fid))];
			if (fin - min > 1) {
				// System.out.println(this.getName() + " Imbalance detected");
				FlowControlMessage fm = new FlowControlMessage();
				fm.fid = fid;
				fm.core = 0;
				fm.aggr = mini;
				NetworkEvent ne = new NetworkEvent(NetworkEvent.RECEIVE, fm, Simusys.time(), this);
				// System.out.println(this.getName() + " send fctl @" + Simusys.time());
				receive(ne);
				num_ear++;
				break;
			}

			// System.out.println("hint a flow " + fm.fid + " to core " + mini);
		}
		
		return true;
	}

	@Override
	public String getState() {
		String res = this.getName() + " inter: {";
		for (int i = 1; i <= k / 2; i++) {
			res += "[";
			for (int j = 1; j <= k; j++)
				res += table.inter_out[i][j] + " ";
			res += "]";
		}
		res += "}";
		
		res += " intra: {";
		for (int i = 1; i <= k / 2; i++) {
			res += "[";
			for (int j = 1; j <= k / 2; j++)
				res += table.intra_out[i][j] + " ";
			res += "]";
		}
		res += "| in [";
		for (int i = 1; i <= k / 2; i++) {
			res += table.ins[i] + " ";
		}
		res += "]";
		res += "| host [";
		for (int i = 1; i <= k / 2; i++) {
			res += table.hosts[i] + " ";
		}
		res += "]";
		// res += bufsz;
		res += "}";
		return res;
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


	public boolean ok4difs(long tick) {
		long diff1 = tick % PERIOD;
		long diff2 = PERIOD - diff1;
		double gap = -1;
		
		if (!(diff1 <= PERIOD * RANGE || diff2 <= PERIOD * RANGE))
			return false;
		
		if (tick - lastDifsTick < PERIOD * (1 - 2 * RANGE))
			return false;
		
		if (diff1 <=  PERIOD * RANGE)
			gap = PERIOD * RANGE + diff1;
		else
			gap = PERIOD * RANGE - diff2;
		return gap == preDifsShift;
	}
	
	@Override
	public boolean performPendingEventsAt(long tick) {
		if (ok4difs(tick)) {
			lastDifsTick  = tick;
			preDifsShift = Trace.rand.nextInt((int) (2 * PERIOD * RANGE + 1));
			this.performEvent(new NetworkEvent(NetworkEvent.REFLECT, null, Simusys.time(), this));
		}
		
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

	@Override
	public double[] portmeasurmentOut(boolean up) {
		if (up)
			return upm.getMeasurementOut();
		else
			return downm.getMeasurementOut();
	}

	@Override
	public double[] portmeasurmentIn(boolean up) {
		if (up)
			return upm.getMeasurementIn();
		else
			return downm.getMeasurementIn();
	}

	@Override
	public Address[] peers(boolean up) {
		Address[] pa = new Address[k / 2 + 1];
		
		if (up) {
			for (int i = 1; i <= k / 2; i++)
				pa[i] = upports.get(i).getPeerAddress(addr);
		} else {
			for (int i = 1; i <= k / 2; i++)
				pa[i] = downports.get(i).getPeerAddress(addr);
		}
		
		return pa;
	}


	@Override
	public void disableLink(Link l) {
		if (upports.contains(l))
			upm.disable(upports.indexOf(l));
		else
			downm.disable(downports.indexOf(l));
	}

}
