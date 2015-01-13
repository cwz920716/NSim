package ecmpbed;

import java.util.Enumeration;
import java.util.Vector;

import protocol.TCPBony;
import simulator.Address;
import simulator.Link;
import support.Entity;
import support.EventManager;
import support.Simusys;

public class Corebed {
	public static void main(String[] args) {
		ECMP_Core c = new ECMP_Core(new Address((short) 1, (short) 0, (short) 0, (short) 0), 4);
		ECMP_Endhost e1 = new ECMP_Endhost(new Address((short) 1, (short) 0, (short) 1, (short) 1));
		ECMP_Endhost e2 = new ECMP_Endhost(new Address((short) 2, (short) 0, (short) 1, (short) 1));
		ECMP_Endhost e3 = new ECMP_Endhost(new Address((short) 3, (short) 0, (short) 1, (short) 1));
		ECMP_Endhost e4 = new ECMP_Endhost(new Address((short) 4, (short) 0, (short) 1, (short) 1));
		
		
		Link l1 = new Link(e1, c);
		e1.setEdgeLink(l1);
		c.setPodLink(1, l1);
		
		Link l2 = new Link(e2, c);
		e2.setEdgeLink(l2);
		c.setPodLink(2, l2);
		
		Link l3 = new Link(e3, c);
		e3.setEdgeLink(l3);
		c.setPodLink(3, l3);
		
		Link l4 = new Link(e4, c);
		e4.setEdgeLink(l4);
		c.setPodLink(4, l4);
		
		TCPBony client = new TCPBony(e1, (short) 1);
		TCPBony server = new TCPBony(e2, (short) 1);
		client.setPeer(server, true);
		server.setPeer(client, false);
		client.send();
		TCPBony client2 = new TCPBony(e2, (short) 2);
		TCPBony server2 = new TCPBony(e3, (short) 2);
		client2.setPeer(server2, true);
		server2.setPeer(client2, false);
		client2.send();
		TCPBony client3 = new TCPBony(e3, (short) 3);
		TCPBony server3 = new TCPBony(e4, (short) 3);
		client3.setPeer(server3, true);
		server3.setPeer(client3, false);
		client3.send();
		
		Simusys.reset();
		long end = 1 * 60 * Simusys.ticePerSecond();
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
			
			Simusys.iterate();

			if (Simusys.time() % (Simusys.ticePerSecond() / 10) == 0) {
				System.out.println(client.getName() + " : " + client.getState());
				// System.out.println(server.getName() + " : " + server.getState());
				System.out.println(client2.getName() + " : " + client2.getState());
				// System.out.println(server2.getName() + " : " + server2.getState());
				// System.out.println(l1.getName() + " : " + l1.getState());
				// System.out.println(l2.getName() + " : " + l2.getState());
				System.out.println(client3.getName() + " : " + client3.getState());
				System.out.println(c.getName() + " : " + c.getState());
				System.out.println();
			}
		}
	}
}
