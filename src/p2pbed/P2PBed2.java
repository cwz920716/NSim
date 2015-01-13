package p2pbed;

import java.util.Enumeration;
import java.util.Vector;

import protocol.TCPBony;
import simulator.Endhost;
import simulator.Link;
import support.Entity;
import support.EventManager;
import support.Simusys;

public class P2PBed2 {
	public static void main(String[] args) {
		Endhost e1 = new Endhost(1);
		Hub2 h2 = new Hub2(2);
		Endhost e3 = new Endhost(3);
		
		Link l1 = new Link(e1, h2);
		e1.setEdgeLink(l1);
		h2.setDown(l1);
		
		Link l2 = new Link(h2, e3, 10, 1);
		e3.setEdgeLink(l2);
		h2.setUp(l2);
		
		TCPBony client = new TCPBony(e1, (short) 1);
		TCPBony server = new TCPBony(e3, (short) 1);
		client.setPeer(server, true);
		server.setPeer(client, false);
		TCPBony client2 = new TCPBony(e1, (short) 2);
		TCPBony server2 = new TCPBony(e3, (short) 2);
		client2.setPeer(server2, true);
		server2.setPeer(client2, false);
		
		Simusys.reset();
		long end = 1000;// * Simusys.ticePerSecond();
		client.send();
		client2.send();
		while (Simusys.time() <= end) {
			Vector<Entity> entities = EventManager.getEntities();
			
			for (Enumeration<Entity> enums = entities.elements(); enums.hasMoreElements(); ) {
				Entity e = enums.nextElement();
				e.performPendingEventsAt(Simusys.time());
			}
			
			for (Enumeration<Entity> enums = entities.elements(); enums.hasMoreElements(); ) {
				Entity e = enums.nextElement();
				e.performEventsAt(Simusys.time());
			}
			
			if (true) {
				System.out.println(client.getName() + " : " + client.getState());
				System.out.println(server.getName() + " : " + server.getState());
				System.out.println(client2.getName() + " : " + client2.getState());
				System.out.println(server2.getName() + " : " + server2.getState());
				// System.out.println(l1.getName() + " : " + l1.getState());
				// System.out.println(l2.getName() + " : " + l2.getState());
				System.out.println();
			}
			Simusys.iterate();
		}
	}
}
