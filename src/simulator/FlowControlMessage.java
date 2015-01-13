package simulator;

import support.PDU;

public class FlowControlMessage extends PDU {

	public FlowControlMessage() {
		super(PDU.FCTRL, 25, PDU.TCP, null);
	}

	public FlowId fid;
	public int core;
	public int aggr;
	

}
