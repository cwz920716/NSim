package trace;

import hederabed.GFF;

import java.util.Random;
import java.util.Vector;

import dardbed.SourceRouting;

import protocol.TCPBony;
import simulator.Address;
import simulator.FlowId;
import support.EventManager;
import support.NetworkEvent;
import support.Simusys;
import support.TopoFace;

public class Shuffle {
	public int k, portbase, next = 0;
	public long completion = -1, startt = -1;
	public Address addr;
	public Vector<Address> list = new Vector<Address>();
	public TCPBony reducer; // server
	public TCPBony mapper; // client
	public TopoFace topo;
	private static Random rand = new Random(Trace.seed);
	
	public Shuffle(int k, Address addr, int portbase, TopoFace topo) {
		this.k = k;
		this.addr = addr;
		this.topo = topo;
		
		this.portbase = portbase;
		for (int pod = 1; pod <= k; pod++)
			for (int edge = 1; edge <= k / 2; edge++)
				for (int host = 1; host <= k / 2; host++)
					if ( !(pod == addr.getPod() && edge == addr.getEdge() && host == addr.getHost()))
						list.add(new Address(pod, 0, edge, host));
		
		EventManager.shuffle(list, rand);
	}
	
	public void startNewShuffle() {
		if ((mapper != null && mapper.finished()) && next >= list.size() && completion == -1) {
			completion = Simusys.time();
			GFF.removeFlow(new FlowId(mapper.getAddress(), reducer.getPort(), mapper.getPort(), reducer.getPort()));
			SourceRouting.removeFlowPath(new FlowId(mapper.getAddress(), reducer.getPort(), mapper.getPort(), reducer.getPort()));
		}
		
		if ((mapper == null || mapper.finished()) && next < list.size()) {
			Address nexta = list.get(next);
			int filesz = 500; //rand.nextInt(925) + 100;
			mapper = new TCPBony(topo.getHost(nexta), (short) (portbase+next), filesz);
			reducer = new TCPBony(topo.getHost(addr), (short) (portbase+next), filesz);
			mapper.setPeer(reducer, true);
			reducer.setPeer(mapper, false);
			mapper.send();
			startt = Simusys.time();
			next++;
		}
	}
}
