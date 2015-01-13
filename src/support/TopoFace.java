package support;

import simulator.Address;
import simulator.Endhost;
import simulator.Link;

public interface TopoFace {
	public Endhost getHost(Address addr);
	
	public Iterable<Link> relatedLinks(Address addr);
}
