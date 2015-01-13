package p2pbed;

import java.util.Enumeration;
import java.util.Vector;

import protocol.TCPBony;
import simulator.Endhost;
import simulator.Link;
import support.Entity;
import support.EventManager;
import support.Simusys;

public class P2PBed {
	public static void main(String[] args) {
		Endhost e1 = new Endhost(1);
		Endhost e2 = new Endhost(2);
		Link l = new Link(e1, e2, 10, 1);
		e1.setEdgeLink(l);
		e2.setEdgeLink(l);
		
		TCPBony client = new TCPBony(e1, (short) 1);
		TCPBony server = new TCPBony(e2, (short) 1);
		client.setPeer(server, true);
		server.setPeer(client, false);
		TCPBony client2 = new TCPBony(e1, (short) 2);
		TCPBony server2 = new TCPBony(e2, (short) 2);
		client2.setPeer(server2, true);
		server2.setPeer(client2, false);
		
		Simusys.reset();
		client.send();
		client2.send();
		long end = 6000;//  * Simusys.ticePerSecond();
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
			
			if (true || Simusys.time() % Simusys.ticePerSecond() == 0) {
				System.out.println(client.getName() + " : " + client.getState());
				System.out.println(server.getName() + " : " + server.getState());
				System.out.println(client2.getName() + " : " + client2.getState());
				System.out.println(server2.getName() + " : " + server2.getState());
				System.out.println(l.getName() + " : " + l.getState());
				System.out.println();
			}
			Simusys.iterate();
		}
	}
}
