package ecmpbed;

import protocol.TCPBony;
import simulator.Address;
import simulator.Endhost;
import support.NetworkEvent;

public class ECMP_Endhost extends Endhost {
	
	public ECMP_Endhost(long address) {
		super(address);
	}
	
	public ECMP_Endhost(Address address) {
		super(address);
	}

	@Override
	public void addEvent(NetworkEvent e) {
		// System.out.println(this.getName() + " addEve");
		if (e.getType() == NetworkEvent.SEND && buffer_sz > e.getPDU().size) {
			TCPBony kid = (TCPBony) e.getTarget();
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

}
