package protocol;

import java.util.Enumeration;
import java.util.Vector;

import simulator.Endhost;
import support.Entity;
import support.NetworkEvent;
import support.Simusys;

public class TCPBony implements Entity {
	
	private Endhost parent;
	private short port;
	private Vector<NetworkEvent> events = new Vector<NetworkEvent>();
	private TCPBony peer = null;
	private boolean active;
	
	private static final int PACKET_SIZE = 1024; // in byte
	private boolean[] buffer = new boolean[W]; 
	private static final int ELEPHANT = 500; // in KByte
	private static final int TTL = 24;
	private static final int W = 128;
	private long LFS = -1; // Last Frame Send
	private long LAR = -1; // Last Ack Received 
	private long LAS = -1; // Last Ack Send
	private long LASindex = LAS + 1;
	private long cwnd = 1;
	private long frac = 0;
	private long ssthresh = 64;
	private long repeated = 0;
	private boolean fastrecovery = false;
	private long highest = -1;
	private long lastTimeOut = -1;
	private double RTOi = 48;
	private double Ri = RTOi; // Smoothed RTTime
	private double Vi = RTOi;
	private long firstSend = 0;
	private long[] sentTime = new long[W];
	private int filesize = 0; // in MB;
	private boolean ok = true;
	
	//retransmission bytes
	private long re = 0, out = 0;
	private long lossw = 0, nloss = 0, sumw = 0;
	private boolean inloss = false;
	
	// rate related
	private long oldLAR = 0;
	
	private QuotaManager qm = new QuotaManager(50);
	
	public TCPBony(Endhost parent, short port) {
		this.parent = parent;
		parent.addConnection(this);
		this.port = port;
		// EventManager.register(this);
	}
	
	public TCPBony(Endhost parent, short port, int filesize) {
		this(parent, port);
		this.filesize = filesize;
		this.ok = false;
	}
	
	public Endhost getParent() {
		return parent;
	}
	
	public void setPeer(TCPBony peer, boolean active) {
		this.peer = peer;
		this.active = active;
	}
	
	public short getPort() {
		return port;
	}
	
	public boolean finished() {
		if (ok)
			return false;
		else
			return LAR > filesize * 1024;
	}

	@Override
	public String getName() {
		return "Port " + port + "@" + parent.getName();
	}

	@Override
	public boolean performEvent(NetworkEvent event) {
		if (event.getTarget() == this) {
			if (event.getTime() == Simusys.time() && event.getType() == NetworkEvent.TIMEOUT)
				timeout(event);
		} else
			System.err.println("Scheduler fail: Unmatched event " + event.getTarget().getName()
					+ " expected at " + event.getTime()
					+ ", at " + Simusys.time());
		
		return false;
	}

	private void retransmit(TCPMessage m) {
		cancelTimeout((int) m.getSeq());
		
		NetworkEvent nev = new NetworkEvent(NetworkEvent.SEND, m, Simusys.time(), this);
		parent.addEvent(nev);
		addEvent(new NetworkEvent(NetworkEvent.TIMEOUT, m, (long) (Simusys.time() + RTOi), this));
		sentTime[(int) (m.getSeq() % W)] = Simusys.time();
		
		re++;
		out++;
	}

	private boolean timeout(NetworkEvent event) {
		// System.out.println(this.getName() + " timeout at " + ((TCPMessage) event.getPDU()).getSeq());
		// ((TCPMessage) event.getPDU()).setSeq(LAR);
		retransmit((TCPMessage) event.getPDU());
		if (Simusys.time() == lastTimeOut)
			return false;
		
		lastTimeOut = Simusys.time();

		if (cwnd > 1) {
			ssthresh = cwnd / 2;
			cwnd = ssthresh;
			frac = 0;
		} else
			ssthresh = cwnd = 1;
		
		return true;
	}
	
	public boolean receive(TCPMessage received) {
		// System.out.println(this.getName() + "recv");
		
		if (active) {
			int ack = (int) received.getAck();
			if (!fastrecovery && ack > LAR) {
				double R = 0;
				if (LAR + 1 == 0) {
					Ri = Simusys.time() - firstSend;
				} else {
					R = Simusys.time() - getSentTime(ack);
					// R = (R <= 4.0) ? (R + 4) : R;
					// System.out.println("R=" + Ri);
					Ri = 0.825 * Ri + 0.125 * R; 
				}
				
				long offset = ack - LAR;
				LAR = ack;
				repeated = 0;
				if (cwnd <= ssthresh)
					cwnd = cwnd + 1;
				else {
					frac += offset;
					while (frac >= cwnd) {
						frac -= cwnd;
						cwnd++;
					}
				}
				
				Vi = (long) (0.75 * Vi + 0.25 * Math.abs(Ri - R));
				RTOi = Ri * 16;
			} else if (!fastrecovery && ack == LAR) {
				repeated++;
				if (repeated >= 3) {
					repeated = 0;
					congestion();
				}
			} else if (fastrecovery && ack >= LAR) {
				if (ack >= highest) {
					fastrecovery = false;
					cwnd = ssthresh;
					// ssthresh = 16;
				} else if (ack > LAR) {
					fastRetransmit(ack);
					cwnd = cwnd - (ack - LAR) + 1;
				}
				LAR = ack;
			}
			cancelTimeout();
			// performEvent(new NetworkEvent(NetworkEvent.SEND, null, Simusys.time(), this));
			send();
		} else {
			if (received.getDport() != port || received.dest != parent.getAddress())
				return false;
			
			long seq = received.getSeq();
			long offset = seq - (LAS + 1);
			
			if (seq > LAS + 1 && !inloss) {
				lossw = offset + 1;
				inloss = true;
				nloss++;
				sumw += lossw;
			} else if (seq == LAS + 1 && inloss) {
				inloss = true;
			}
			
			if (offset >= W)
				return false; // out of buffer, drop the packet
			
			if (offset >= 0)
				buffer[(int) ((LASindex + offset) % W)] = true;
			if (seq == LAS + 1) {
				int i = (int) LASindex;
				while (buffer[i]) {
					LAS++;
					buffer[i] = false;
					i = (i + 1) % W;
				}
				LASindex = i;
			}
			
			TCPMessage m = new TCPMessage(28, parent.getAddress(), peer.parent.getAddress(), port, peer.port, TTL);
			m.setAck(LAS);
			m.setTcpdst(peer);
			NetworkEvent nev = new NetworkEvent(NetworkEvent.SEND, m, Simusys.time(), this);
			parent.addEvent(nev);
		}
		
		return true;
	}

	private double getSentTime(int ack) {
		// System.out.println("senttime=" + sentTime[ack % W]);
		
		return sentTime[ack % W];
	}

	private void fastRetransmit(long ack) {
		TCPMessage m = new TCPMessage(PACKET_SIZE, parent.getAddress(), peer.parent.getAddress(), port, peer.port, TTL);
		m.setSeq(ack + 1);
		m.setTcpdst(peer);
		retransmit(m);
	}

	private void cancelTimeout(int seq) {
		Vector<NetworkEvent> es = new Vector<NetworkEvent>();
		for (Enumeration<NetworkEvent> enums = events.elements(); enums.hasMoreElements(); ) {
			NetworkEvent ne = enums.nextElement();
			if (ne.getType() == NetworkEvent.TIMEOUT && ((TCPMessage) ne.getPDU()).getSeq() == seq) {
				// EventManager.unregister(ne);
				es.add(ne);
			}
		}
		
		events.removeAll(es);
	}

	private void cancelTimeout() {
		Vector<NetworkEvent> es = new Vector<NetworkEvent>();
		for (Enumeration<NetworkEvent> enums = events.elements(); enums.hasMoreElements(); ) {
			NetworkEvent ne = enums.nextElement();
			if (ne.getType() == NetworkEvent.TIMEOUT && ((TCPMessage) ne.getPDU()).getSeq() <= LAR) {
				// EventManager.unregister(ne);
				es.add(ne);
			}
		}
		
		events.removeAll(es);
	}

	private void congestion() {
		if (fastrecovery)
			return;

		// System.out.println(this.getName() + " enter congestion at LAR=" + LAR + ", LFS=" + LFS);
		fastrecovery = true;
		fastRetransmit(LAR);
		
		if (cwnd > 1)
			cwnd = ssthresh = cwnd / 2;
		else
			cwnd = ssthresh = 1;
		cwnd += 3;
		frac = 0;
		
		highest = LFS;
	}
	
	public boolean send() {
		if (active) {
			// System.out.println(this.getName() + "send");
			
			if (LFS == -1)
				firstSend = Simusys.time();
			
			while (LFS - LAR < cwnd && LFS - LAR <= W && (ok || LAR <= filesize * 1024) && qm.hasQuota(LFS)) {
				// System.out.println(this.getName() + " send a new packet");
				TCPMessage m = new TCPMessage(PACKET_SIZE, parent.getAddress(), peer.parent.getAddress(), port, peer.port, TTL);
				m.setSeq(LFS + 1);
				m.setTcpdst(peer);
				NetworkEvent nev = new NetworkEvent(NetworkEvent.SEND, m, Simusys.time(), this);
				parent.addEvent(nev);
				addEvent(new NetworkEvent(NetworkEvent.TIMEOUT, m, (long) (Simusys.time() + RTOi), this));
				sentTime[(int) (m.getSeq() % W)] = Simusys.time();
				LFS++;
				// System.out.println(this.getName() + " has sent a new packet " + m.getSeq());
				out++;
			}
			
			return true;
		} else 
			return false;
	}

	@Override
	public boolean performPendingEventsAt(long tick) {
		for (Enumeration<NetworkEvent> enums = events.elements(); enums.hasMoreElements(); ) {
			NetworkEvent ne = enums.nextElement();
			if (ne.getTime() == tick) {
				this.performEvent(ne);
			}
		}
		
		return true;
	}

	@Override
	public void addEvent(NetworkEvent e) {
		events.add(e);
	}

	@Override
	public long getAddress() {
		return 0;
	}
	
	public boolean isElephant() {
		return LFS >= ELEPHANT;
	}

	@Override
	public String getState() {
		String res = "";
		
		if (active) {
			res += "active tcp, LFS=" + LFS;
			res += ", LAR=" + LAR;
			res += ", cwnd=" + cwnd;
			res += ", frac=" + frac;
			res += ", ssthresh=" + ssthresh;
			res += ", repeatd=" + repeated;
			res += ", RTOi=" + RTOi;
			if (fastrecovery)
				res += ", in fast recovery, highest=" + highest;
			double rate = (double) LAR * PACKET_SIZE * Simusys.ticePerSecond() * 8 / Simusys.time();
			res += ", rate = " + rate;
		} else {
			res += "inactive tcp, LAS=" + LAS;
			res += ", LASindex=" + LASindex;
		}
		
		return res;
	}

	public double getRate() {
		return (double) LAR * PACKET_SIZE * Simusys.ticePerSecond() * 8 / Simusys.time();
	}
	
	public double getRate(long ref) {
		return (double) LAR * PACKET_SIZE * Simusys.ticePerSecond() * 8 / (Simusys.time() - ref);
	}
	
	public double getInstantRate() {
		double res = (double) (LAR - oldLAR) * PACKET_SIZE * 8;
		oldLAR = LAR;
		return res;
	}
	
	public double getAmount() {
		return (double) LAR * PACKET_SIZE;
	}
	
	public double getOIRatio() {
		return (double) re / out;
	}
	
	public double getOOWindow() {
		if (nloss == 0)
			return 0;
		return (double) sumw / nloss;
	}

	@Override
	public boolean performEventsAt(long tick) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isActive() {
		return active;
	}
	
	private class QuotaManager {
		public double rate; // in MByste/Sec
		public long lastLFS;
		public long lastTime;
		
		public QuotaManager(double rate) {
			this.rate = rate;
			this.lastTime = -1;
			this.lastLFS = -1;
		}
		
		public boolean hasQuota(long LFS) {
			if (Simusys.time() - lastTime >= 5 * Simusys.ticePerSecond() || this.lastTime < 0) {
				this.lastTime = Simusys.time();
				this.lastLFS = LFS;
			}
			
			if (LFS - lastLFS > rate * 1000000L * (Simusys.time() - lastTime) / (PACKET_SIZE * Simusys.ticePerSecond())) {
				return false;
			}
			return true;
		}
	}

}
