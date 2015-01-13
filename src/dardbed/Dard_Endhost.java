package dardbed;

import java.util.Vector;

import protocol.TCPBony;
import protocol.TCPMessage;
import simulator.Address;
import simulator.Endhost;
import simulator.FlowId;
import support.NetworkEvent;
import support.Simusys;

public class Dard_Endhost extends Endhost {
	private Dard dard = new Dard();
	private double intv = 5 * Simusys.ticePerSecond();
	private long last = 0;
	
	public Dard_Endhost(long address) {
		super(address);
	}
	
	public Dard_Endhost(Address address) {
		super(address);
	}

	@Override
	public void addEvent(NetworkEvent e) {
		// System.out.println(this.getName() + " addEve");
		if (e.getType() == NetworkEvent.SEND && buffer_sz > e.getPDU().size) {
			TCPBony kid = (TCPBony) e.getTarget();
			TCPMessage m = (TCPMessage) e.getPDU();
			FlowId fid = new FlowId(m.src, m.dest, m.getSport(), m.getDport());
			if (kid.isElephant()) {
				dard.addElephantFlow(fid);
			} else
				dard.addMiceFlow(fid);
			
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
	
	@Override
	public boolean performPendingEventsAt(long tick) {
		if (Simusys.time() - last >= intv) {
			dard.SelfishPathSelector();
			last = Simusys.time();
		}
		
		while (!received_buffer.isEmpty()) {
			// System.out.println(this.getName() + " receive At" + tick);
			NetworkEvent re = received_buffer.get(0);
			if (re.getType() != NetworkEvent.RECEIVE) {
				System.err.println("NON-RECEIVE event in rb!");
				System.exit(0);
			}
			
			if (!this.performEvent(re))
				break;
			received_buffer.remove(0);
		}
		
		return true;
	}

}
