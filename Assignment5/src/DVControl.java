// the skeleton control algorithm (routing) of a node that will
// use the distance-vector (DV) routing algorithm

import cnss.simulator.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import cnss.lib.*;

public class DVControl extends AbstractControlAlgorithm {

	// Do not modify
	static int INFINITY = 1000000;
	static int REFERENCE_BAND = 100000000; // reference bandwidth = 100 Mbps
	static int UPDATE_INTERVAL = 1000;

	// variables that will contain the parameters transmitted by the CNSS node code
	// and
	// assigned by the initialise upcall()
	private Node nodeObj; // the CNSS node code that supports this node algorithm
	private int nodeId; // my Id or address
	private GlobalParameters parameters; // configuration file parameters
	private Link[] links; // this node links, numbered from 0 .. nInterfaces - 1
	private int nInterfaces; // this node number of interfaces, each with a link
								// link attached

	// variables containing the flags that drive traces - calls to the trace()
	// method are
	// shown or not and stages executed by the algorithm besides the base line
	// solution
	// these flags are set by the initialise() upcall and their values are driven by
	// the
	// configuration file content
	private boolean tracingOn;
	private boolean triggered; // if true we should use triggered updates
	private boolean preverse; // if true split horizon with poison reverse should be implemented
	private boolean expire; // if true it should expire useless routing table entries
	// that is, those with metric or cost infinity for more 3 times the update
	// interval

	// YOUR VARIABLES HERE, NAMELY THE ROUTING TABLE
	private Map<Integer, DVRoutingTableEntry> rt;

	public DVControl() {
		super("dv control");
	}

	/**
	 * Initializes the control algorithm and returns the required
	 * control_clock_tick_period. If control_clock_tick_period == 0, no clock_ticks
	 * will be submitted; Interfaces are numbered 0 to nint-1. Each has a link
	 * attached: links[i] Interface; -1 is virtual and denotes, when needed, the
	 * local loop interface.
	 * 
	 * DV code: initialises the routing table DV Code: if ( triggered ) sends first
	 * announcements
	 * 
	 * @param now        the current virtual clock value
	 * @param id         this node id
	 * @param nodeObj    a reference to the node object executing this algorithm
	 * @param parameters the collection of global parameters
	 * @param links      the nodes links array
	 * @param nint       the number of interfaces (or links) of this node, not
	 *                   including the LOCAL one
	 * @return the requested clock_tick_period
	 */
	public int initialise(int now, int node_id, Node mynode, GlobalParameters parameters, Link[] links, int nint) {
		super.initialise(now, node_id, mynode, parameters, links, nint);
		this.parameters = parameters;
		nodeId = node_id;
		nodeObj = mynode;
		this.links = links;
		nInterfaces = nint;

		tracingOn = parameters.containsKey("trace");
		preverse = parameters.containsKey("preverse");
		expire = parameters.containsKey("expire");
		triggered = parameters.containsKey("triggered");

		super.set_trace(tracingOn);
		trace(now, "starting w/ period / trig. upds / shpr / expire: " + UPDATE_INTERVAL + "," + triggered + ","
				+ preverse + "," + expire);

		// DV CODE HERE:
		rt = new HashMap<>();
		DVRoutingTableEntry init = new DVRoutingTableEntry(nodeId, LOCAL, 0, now);
		rt.put(nodeId, init);
		if (triggered) {
			sendAnnouncements();
		}

		return UPDATE_INTERVAL; // do not touch this line
	}

	/**
	 * A periodic clock interrupt.
	 * 
	 * DV code: sends announcements to neighbors DV code: if ( expire ) expires
	 * staled entries in the routing table
	 * 
	 * @param now the current virtual clock value
	 */
	public void on_clock_tick(int now) {
		// trace(now, "on clock tick ");

		// DV CODE HERE:
		sendAnnouncements();
	}

	private void sendAnnouncements() {
		int tot = rt.size();
		int[] announcements = constructAnnouncements();
		DVControlPayload payload = new DVControlPayload(tot, announcements);
		Packet p = nodeObj.createControlPacket(nodeId, Packet.ONEHOP, payload.toByteArray());
		for (int i = 0; i < nInterfaces; i++) {
			if (links[i].isUp()){
				nodeObj.send(p, i);
			}
		}
	}

	private int[] constructAnnouncements() {
		int tot = rt.size();
		int[] announcements = new int[tot * 2]; // ((destination, metric))
		Set<Entry<Integer, DVRoutingTableEntry>> entries = rt.entrySet();
		Iterator<Entry<Integer, DVRoutingTableEntry>> it = entries.iterator();
		for (int j = 0; j < tot * 2 && it.hasNext(); j += 2) {
			Entry<Integer, DVRoutingTableEntry> aux = it.next();
			int destination = aux.getKey();
			int metric = aux.getValue().getMetric();
			announcements[j] = destination;
			announcements[j + 1] = metric;
		}
		return announcements;
	}

	/**
	 * rt.forEach((k,v) -> help()) help(int k, DVRoutingTableEntry v, int[]
	 * announcements, int j){ int metric = v.getMetric(); announcements[j] =
	 * destination; announcements[j+1] = metric; j+=2; }
	 */

	/**
	 * Given a packet from another node, forward it to the appropriate interfaces
	 * using nodeObj.control_send(Packet p, int iface); Packet ttl has already been
	 * decreased and controlled. If the algorithm has no solution to route this
	 * packet, send it to the UNKNOWN interface using nodeObj.send(p,UNKNOWN)
	 * 
	 * DV code: use the routing table contents to forward packets
	 * 
	 * @param now   the current virtual clock value
	 * @param p     the packet to process
	 * @param iface the interface it was received by the node
	 */
	public void forward_packet(int now, Packet p, int iface) {

		// DV CODE HERE:
		int destination = p.getDestination();
		int interfaceD = UNKNOWN;
		if (rt.containsKey(destination) && rt.get(destination).getMetric() != INFINITY) {
			interfaceD = rt.get(destination).getInterface();
		}

		nodeObj.send(p, interfaceD);

	}

	/**
	 * Signals a link up event; links[iface].isUp() becomes true
	 * 
	 * DV code: if ( triggered ) sends announcements to neighbors
	 * 
	 * @param now   the current virtual clock value
	 * @param iface interface id where this link is connected
	 */
	public void on_link_up(int now, int iface) {
		trace(now, "interface " + iface + " link up");

		// DV CODE HERE:
		// TODO
		links[iface].setState(true);
		if (triggered) {
			sendAnnouncements();
		}
		
	}

	/**
	 * Signals a link down event; links[iface].isUp() becomes false
	 * 
	 * DV code: update routing table entries DV code: if ( triggered ) sends
	 * announcements to neighbors
	 * 
	 * @param now   the current virtual clock value
	 * @param iface interface id where this link is connected
	 */
	public void on_link_down(int now, int iface) {
		trace(now, "interface " + iface + " link down");

		// DV CODE HERE:
		// TODO
		links[iface].setState(false);
		Collection<DVRoutingTableEntry> entries = rt.values();
		Iterator<DVRoutingTableEntry> it = entries.iterator();
		//int n = 0;
		//int[] announcements = new int[rt.size()*2];
		boolean update = false;
		while (it.hasNext()) {
			DVRoutingTableEntry e = it.next();
			if (e.getInterface() == iface && e.getTime()<INFINITY) {
				e.setMetric(INFINITY);
				//e.setTime(now);
				/*announcements[n] = e.getDestination();
				announcements[n+1] = INFINITY;
				n+=2;*/
				update = true;
			}

		}
		if (triggered) {
			//triggeredAnnouncement(announcements, n/2);
			if(update) {
				sendAnnouncements();
			}
		}
	}

	/**
	 * Given a control packet from another node, process it
	 * 
	 * DV code: process the DV announcement contained in packet p DV code: if (
	 * triggered && routing table updated ) sends announcements to neighbors
	 * 
	 * @param now   the current virtual clock value
	 * @param p     the packet to process
	 * @param iface the interface it came in from
	 */
	public void on_receive(int now, Packet p, int iface) {

		// DV CODE HERE:
		DVControlPayload controlPayload = new DVControlPayload(p.getPayload());
		int t = controlPayload.getTotal();
		int[] announcements = controlPayload.getAnnouncements(t);
		int reachability = getInterfaceMetric(iface);
		boolean sendAll = false;

		//int[] send = new int[t*2];
		//int n = 0;

		for (int i = 0; i < t * 2; i += 2) {
			int destination = announcements[i];
			int metric = announcements[i + 1];
			//int newMetric = 0;
			DVRoutingTableEntry e;
			if (!rt.containsKey(destination)) {
				e = new DVRoutingTableEntry(destination, iface, metric + reachability, now);
				rt.put(destination, e);
				//newMetric = metric + reachability;
				sendAll =true;
			} else {
				e = rt.get(destination);
				int distance = e.getMetric();
				if (distance > metric + reachability) {
					e.setInterface(iface);
					e.setMetric(metric + reachability);
					e.setTime(now);
					//newMetric = metric + reachability;
					sendAll =true;

				} else if (distance < metric + reachability && e.getInterface() == iface) {
					if(metric<INFINITY) {
						e.setMetric(metric + reachability);
						e.setTime(now);
						//newMetric = metric + reachability;
						sendAll =true;
						
					} else if(distance<INFINITY){
						e.setMetric(INFINITY);
						e.setTime(now);
						//newMetric = INFINITY;
						sendAll =true;

					}
					
				} else if (distance == metric + reachability  && e.getInterface() == iface) {
					//e.setInterface(iface);
					e.setTime(now);
					//newMetric = 0;

				}
			
			}
			
			/*if(newMetric!=0) {
				send[n] = e.getDestination();
				send[n+1] = newMetric;
				n+=2;
			}
			newMetric = 0;*/
			
			/*if (triggered) {
				if(newMetric != 0) {
					triggeredAnnouncement(destination, newMetric);
					newMetric = 0;
				}	
			}*/

		}
		
		/*if(triggered ) {
			if(sendAll)
				sendAnnouncements();
			else if(n>0)
				triggeredAnnouncement(send, n/2);
		}*/
		if(triggered) {
			if(sendAll)
				sendAnnouncements();
		}
	}

	private void triggeredAnnouncement(int destination, int metric) {
		int[] announcement = {destination, metric};
		DVControlPayload payload = new DVControlPayload(1, announcement);
		Packet p = nodeObj.createControlPacket(nodeId, Packet.ONEHOP, payload.toByteArray());
		for (int i = 0; i < nInterfaces; i++) {
			if (links[i].isUp()){
				nodeObj.send(p, i);
			}
		}

	}

	private void triggeredAnnouncement(int [] announcement, int size) {
		DVControlPayload payload = new DVControlPayload(size, announcement);
		Packet p = nodeObj.createControlPacket(nodeId, Packet.ONEHOP, payload.toByteArray());
		for (int i = 0; i < nInterfaces; i++) {
			if (links[i].isUp()){
				nodeObj.send(p, i);
			}
		}

	}
	
	public void showControlState(int now) {
		trace(now, "has no extra state to show");
	}

	/**
	 * Prints the routing table to the screen. The format is : <blank line> Node
	 * <id> routing table at time stamp d <destination> i <interface> m <metric> lu
	 * <last_update> d <destination> i <interface> m <metric> lu <last_update> d
	 * <destination> i <interface> m <metric> lu <last_update>
	 */
	public void showRoutingTable(int now) {
		System.out.println("\nRouter " + nodeId + " time " + now);

		// DV CODE HERE:
		Set<Entry<Integer, DVRoutingTableEntry>> entries = rt.entrySet();
		Iterator<Entry<Integer, DVRoutingTableEntry>> it = entries.iterator();
		while (it.hasNext()) {
			Entry<Integer, DVRoutingTableEntry> entry = it.next();
			int destination = entry.getKey();
			DVRoutingTableEntry e = entry.getValue();
			if (e.getMetric() < INFINITY)
				System.out.printf("d %d i %d m %d lu %d\n", destination, e.getInterface(), e.getMetric(), e.getTime());
			else {
				System.out.printf("d %d i %d m %s lu %d\n", destination, e.getInterface(), "INFINITY", e.getTime());
			}
		}

	}

	/**
	 * Auxiliary method
	 */

	/**
	 * Given the link connected to an interface, returns its weight or cost When the
	 * protocol metric is the cost all links is equal to 1 this method is useless
	 * 
	 * @param iface
	 * @return metric
	 */
	private int getInterfaceMetric(int iface) {
		// loopback interfaces are virtual with cost = 0
		// the array of interfaces has no entry for the loopback interface
		if (iface == LOCAL)
			return 0;
		return (int) (REFERENCE_BAND / links[iface].getBandWidth());
	}

}
