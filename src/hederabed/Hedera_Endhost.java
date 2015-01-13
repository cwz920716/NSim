package hederabed;

import java.util.Vector;

import protocol.TCPBony;
import protocol.TCPMessage;
import simulator.Address;
import simulator.Endhost;
import simulator.FlowId;
import support.NetworkEvent;

public class Hedera_Endhost extends Endhost {
	private Vector<FlowId> fids = new Vector<FlowId>();
	
	public Hedera_Endhost(long address) {
		super(address);
	}
	
	public Hedera_Endhost(Address address) {
		super(address);
	}

	@Override
	public void addEvent(NetworkEvent e) {
		// System.out.println(this.getName() + " addEve");
		if (e.getType() == NetworkEvent.SEND && buffer_sz > e.getPDU().size) {
			TCPBony kid = (TCPBony) e.getTarget();
			if (kid.isElephant()) {
				TCPMessage m = (TCPMessage) e.getPDU();
				FlowId fid = new FlowId(m.src, m.dest, m.getSport(), m.getDport());
				if (!fids.contains(fid)) {
					// Hedera.allflows.add(fid);
					GFF.addNewFlow(fid);
					fids.add(fid);
				}
			}
			
			int i = connections.indexOf(kid);
			e.setTarget(this);
			send_buffer.get(i).add(e);
			buffer_sz -= e.getPDU().size;
		} else if (e.getType() == NetworkEvent.RECEIVE) {
			received_buffer.add(e);
		} else {
			// System.out.println(this.getName() + " Drop packet due to buffer overflow: " + buffer_sz + 
			//	" from " + ((TCPMessage) e.getPDU()).getSport() + 
			//	" Seq: " + ((TCPMessage) e.getPDU()).getSeq());
		}
	}

	public Vector<FlowId> getFlows() {
		return fids;
	}

}
