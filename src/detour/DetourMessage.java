package detour;

import simulator.Address;
import support.PDU;

public class DetourMessage extends PDU {
	
	public Address dsrc, ddest, hop1, hop2;

	public DetourMessage(int type, int size, int subtype, PDU sdu, Address dsrc, Address hop1, Address hop2, Address ddest) {
		super(type, size, subtype, sdu);
		// TODO Auto-generated constructor stub
		this.dsrc = dsrc;
		this.hop1 = hop1;
		this.hop2 = hop2;
		this.ddest = ddest;
	}

}
