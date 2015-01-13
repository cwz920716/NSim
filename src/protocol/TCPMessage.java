package protocol;

import java.util.Random;

import simulator.FlowId;
import support.PDU;

public class TCPMessage extends PDU {
	
	public static final int SYN = 1; // unused
	public static final int ACK = 2; // unused
	public static final int FIN = 3; // unused
	
	public long src, dest;
	private short sport, dport;
	public int ttl;
	private long seq;
	private long ack;
	private int op = 0; // unused
	
	private long ts;
	private TCPBony tcpdst = null;

	public TCPMessage(int size, long src, long dest, short sport, short dport, int ttl) {
		super(TCP, size, NIL, null);
		this.src = src;
		this.sport = sport;
		this.dest = dest;
		this.dport = dport;
		this.ttl = ttl;
	}
	
	public void setOp(int op) {
		this.op = op;
	}
	
	public int getOp() {
		return op;
	}
	
	public void setSeq(long seq) {
		this.seq = seq;
	}
	
	public void setAck(long ack) {
		this.ack = ack;
	}

	public long getAck() {
		return ack;
	}

	public long getSeq() {
		return seq;
	}
	
	public short getSport() {
		return sport;
	}

	public short getDport() {
		return dport;
	}
	
	@Override
	public int hash(int k) {
		FlowId fid = new FlowId(src, dest, sport, dport);
		return fid.hash(k);
	}
	
	public int hash(int k, long addr) {
		FlowId fid = new FlowId(src, dest, sport, dport);
		return fid.hash(k, addr);
	}

	public long getTs() {
		return ts;
	}

	public void setTs(long ts) {
		this.ts = ts;
	}

	public TCPBony getTcpdst() {
		return tcpdst;
	}

	public void setTcpdst(TCPBony tcpdst) {
		this.tcpdst = tcpdst;
	}

}
