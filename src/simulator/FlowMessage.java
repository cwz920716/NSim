package simulator;

import support.PDU;

public class FlowMessage extends PDU {
	
	public FlowId fid;
	public long timestamp;

	public FlowMessage(PDU sdu, long timestamp) {
		super(PDU.LBDAR, 25 + sdu.size, PDU.TCP, sdu);
		this.timestamp = timestamp;
	}

}
