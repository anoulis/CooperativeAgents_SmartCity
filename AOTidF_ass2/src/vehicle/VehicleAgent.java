package vehicle;

import java.io.IOException;
import java.text.SimpleDateFormat;

import java.util.*;

import charging.station.Charging_Station_Agent;
import simulation.*;
import charging.station.*;

import jade.core.Agent;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.proto.ContractNetInitiator;
import jade.domain.FIPANames;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.Property;

/**
 * @author anoulis The car has goal time or money Mode charging or moving
 *         Booking preferences for the time-slots
 * 
 *
 */
public class VehicleAgent extends Agent {
	// Battery status of the agent
	private double battery_life;
	// Goal to save money "m" or save time "t"
	private String goal = RandomGoal();

	private int[] schedule = new int[(24 * 60) - 1];
	private int step = 0;
	private double battery_decay = 0.9905;
	private int nResponders;
	
	private List<Charging_Station_Agent> nearestCS;
	

	// Position of the agent

	// Vehicle Initialization

	private Field field;
	public Location location;
	public Charging_Station_Agent CS;

	public VehicleAgent(Field field, Location location) {
		this.field = field;
		setLocation(location);
	}


	// Vehicle Initialization
	@Override
	protected void setup() {

		battery_life = getRandomNumberInRange(65, 80);
		System.out.println("Vehicle Agent " + getLocalName() + " is ready with goal " + goal + " and location "
				+ location.toString() + " and battery life is: " + battery_life);
		nResponders = 2;

		fillSchedule(4);
		
//		addBehaviour(new CyclicBehaviour(this) {
//			public void action() {
////				battery_life = battery_life * battery_decay;
//				//System.out.println("battery life is:" + battery_life);
//				int job = schedule[step];
//				if(step == 1438) {
//					step = 0;
//				}
//				//checkCharging();
//
//				//random_move();
//				if (battery_life > 80.00) {
//					// pay attention to schedule and go do what needs to be done.
//
//					if (job == 1) {
//						System.out.println("random moving");
//						// Random_move()
//					} else {
//						System.out.println("just chilling");
//						// chill()
//					}
//				}
//				// else if (battery_life > 60.00 && battery_life < 80.00) {
//				// pay attention to schedule and go do what needs to be done.
//				// }
//				else if (battery_life > 30.00 && battery_life < 80.00) {
//					// forget schedule just go charge
//					// here we want to do a cfp for a charging spot
//					// based on our position and our goals.
//					try { // Build the description used as template for the search
//						DFAgentDescription template = new DFAgentDescription();
//						ServiceDescription sd = new ServiceDescription();
//						sd.setType("Charging-Points");
//						createYellowPageEntry(sd);
//						template.addServices(sd);
//						// SearchConstraints sc = new SearchConstraints(); //sc.setMaxResults();
//						// System.out.print("step is now " + step);
//
//						DFAgentDescription[] results = DFService.search(this.getAgent(), template);
//						yellowPagesResults(msg, results);
//
//					}
//
//					catch (FIPAException fe) {
//						fe.printStackTrace();
//					}
//
//				} else if (battery_life < 30.00) {
//					get_job(job);
//					System.out.println("Battery dangerously low, go charge now!");
//					// forget schedule just go charge
//					// move_to_charging_station()
//				}
//
//				step++;
//			}
//		} );
	}

	public void step() {
		ACLMessage msg = new ACLMessage(ACLMessage.CFP);
		int job = schedule[step];

		if (battery_life >= 80.00) {
			// pay attention to schedule and go do what needs to be done.

			//randome movement
			if (job == 1) {
				random_move();
			//do nothing stay at your location
			} else {
				// chill()
			}
		}	
		// else if (battery_life > 60.00 && battery_life < 80.00) {
		// pay attention to schedule and go do what needs to be done.
		// }
		else if (battery_life >= 30.00 && battery_life < 80.00) {
			// forget schedule just go charge
			// here we want to do a cfp for a charging spot
			// based on our position and our goals.
			if(battery_life >= 65) {
				//search for the nearest two stations
				nearestCS = field.nearestChargingStation(location, 2);
			}
			else if(battery_life >= 45 && battery_life < 65 ) {
				//search for the nearest 4 stations
				nearestCS = field.nearestChargingStation(location, 4);
			}
			else if(battery_life < 45) {
				//search for the nearest 6 stations
				nearestCS = field.nearestChargingStation(location, 6);
			}
			
			try { 
				// Build the description used as template for the search
				DFAgentDescription dfAgent= new DFAgentDescription();
				ServiceDescription sd = new ServiceDescription();
				sd.setType("Charging-Points");
				
//				createYellowPageEntry(sd);
				dfAgent.addServices(sd);

				DFAgentDescription[] results = DFService.search(this, dfAgent);
				try {
					yellowPagesResults(msg, results);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

			catch (FIPAException fe) {
				fe.printStackTrace();
			}
		}
		step++;
	}

	public void fillSchedule(int numofBlocks) {
		int start = 0;
		int end = schedule.length;
		
		for(int i = 0; i < numofBlocks; i++) {
			int temp = (int) getRandomNumberInRange(start, (end / numofBlocks) * (i+1)); 
			
			int value = getRandomNumberInRange(1, 2);
			
			Arrays.fill(schedule, start, temp, value);
			start = temp;
		}
		
		Arrays.fill(schedule,start, end, 2);
	}

    /**
     * random function to return number between min and max for different
     * initialisations
     */
    private static int getRandomNumberInRange(int min, int max) {

        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min");
        }
        Random r = new Random();
        return r.nextInt((max - min) + 1) + min;
    }
	
	
//	/**
//	 * Function to do the Registrations in Yellow Pages for every vehicle agent. It
//	 * should use vehicle object to add properties. We should start with the
//	 * following ones: mode: fast or slow (type of charging that we want) start/end
//	 * : the one of the available time-slot for charging (12-14,14-16) booked: no :
//	 * we want only free place
//	 */
//	public void createYellowPageEntry(ServiceDescription sd) {
//		
//		Property mode_m = new Property("mode", "slow");
//		Property mode_t = new Property("mode", "fast");
//
//		if (this.goal.equalsIgnoreCase("m")) {
//			System.out.println("Agent: " + getLocalName() + 
//					" is looking for a slow charger.");
//			sd.addProperties(mode_m);
//		} else if (this.goal.equalsIgnoreCase("t")) {
//			System.out.println("Agent: " + getLocalName() + 
//					" is looking for a fast charger.");
//			sd.addProperties(mode_t);
//		} else {
//			System.out.println("Agent: " + getLocalName() + 
//					" has no goal defined.");
//		}
//	}

	/**
	 * Taking the results for searching for the suitable services in yellow-pages
	 * the vehicle agent start contractNet with the appropriate CS Agents.
	 * 
	 * @param msg
	 * @param results
	 * @throws IOException 
	 */
	public void yellowPagesResults(ACLMessage msg, DFAgentDescription[] results) throws IOException {
		System.out.println("");
		
		if (results.length > 0) {
			System.out.println("Agent " + getLocalName() + " found the following Charging-Points services:");
			for (int i = 0; i < results.length; ++i) {
				DFAgentDescription dfd = results[i];
				AID provider = dfd.getName();
				// The same agent may provide several services; we are only interested
				// in the Charging-Points one
				Iterator it = dfd.getAllServices();
				while (it.hasNext()) {
					ServiceDescription sd = (ServiceDescription) it.next();
					if (sd.getType().equalsIgnoreCase("Charging-Points")) {
						System.out.println(
								"- Service \"" + sd.getName() + "\" provided by agent " + provider.getLocalName());
						//Get all Provider but just add the nearest one to the Receiver list
						for(int j =0; j < nearestCS.size(); j++) {
							if(provider.getLocalName().contentEquals(nearestCS.get(j).getLocalName())) {
								msg.addReceiver(new AID((String) provider.getLocalName(), AID.ISLOCALNAME));
							}
						}
					}
				}
			}
			msg.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
			// We want to receive a reply in 10 secs
			msg.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
			
			System.out.println(schedule);
			getNextfreeSlot();
			
			String mode;
			if(goal == "t")
				mode = "fast";
			else
				mode = "slow";
			
			
			String[] oMsg=new String[7];
	         oMsg[0] = "charging-action";
	         oMsg[1] = String.valueOf(battery_life);
	         oMsg[2] = String.valueOf(getNextfreeSlot()[0]);
	         oMsg[3] = String.valueOf(getNextfreeSlot()[1]);
	         oMsg[4] = String.valueOf(location.getRow());
	         oMsg[5] = mode;
	         oMsg[6] = String.valueOf(location.getCol());
	         
	        msg.setContentObject(oMsg);
			contractNet(msg);
		} else {
			System.out.println("Agent " + getLocalName() + " did not find any Charging-Points service");
		}
	}
	
	private int[] getNextfreeSlot() {
		int temp = -1;
		int start = -1;
		int end = -1;
		for(int i=step; i< schedule.length; i++) {
//			System.out.println(" " + schedule[i] + " temp " + temp);
			if(schedule[i] == 1 && temp == 0) {
				temp = 1;
				end = i;
//				System.out.println("add end " + i);
				break;
			}
			else if (schedule[i] == 2 && temp != 0){
				temp = 0;
				start = i;
//				System.out.println("add start" + i);
			}
			else if(schedule[i] == 1 && temp == -1) {
				temp = 1;
			}
			
		}
		
		int[] start_end = new int[] {start, end};
//		System.out.println("start " + start + " end " + end );
		return start_end;
	}

	/**
	 * ContractNet function where we do the negotiations from the Vehicle Agent
	 * Part. Functions to handle the responses Code for the evaluation of the
	 * offers.
	 * 
	 * @param msg
	 */
	public void contractNet(ACLMessage msg) {
		addBehaviour(new ContractNetInitiator(this, msg) {

			protected void handlePropose(ACLMessage propose, Vector v) {
				System.out.println("Agent " + propose.getSender().getLocalName() + " proposed " + propose.getContent());
			}

			protected void handleRefuse(ACLMessage refuse) {
				System.out.println("Agent " + refuse.getSender().getLocalName() + " refused");
			}

			protected void handleFailure(ACLMessage failure) {
				if (failure.getSender().equals(myAgent.getAMS())) {
					// FAILURE notification from the JADE runtime: the receiver
					// does not exist
					System.out.println("Responder does not exist");
				} else {
					System.out.println("Agent " + failure.getSender().getLocalName() + " failed");
				}
				// Immediate failure --> we will not receive a response from this agent
				nResponders--;
			}

			protected void handleAllResponses(Vector responses, Vector acceptances) {
				if (responses.size() < nResponders) {
					// Some responder didn't reply within the specified timeout
					System.out.println("Timeout expired: missing " + (nResponders - responses.size()) + " responses");
				}

				// Evaluate proposals.
				// Consider that the offer is an int ratio of an importance combination
				// of time, money, distance or whatever.
				int bestProposal = -1;
				AID bestProposer = null;
				ACLMessage accept = null;
				Enumeration e = responses.elements();
				while (e.hasMoreElements()) {
					ACLMessage msg = (ACLMessage) e.nextElement();
					if (msg.getPerformative() == ACLMessage.PROPOSE) {
						ACLMessage reply = msg.createReply();
						reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
						acceptances.addElement(reply);
						int proposal = Integer.parseInt(msg.getContent());
						System.out.println("THE PROPOSAL IS " + proposal);
						if (proposal > bestProposal) {
							bestProposal = proposal;
							bestProposer = msg.getSender();
							accept = reply;
						}
					}
				}

				// Accept the proposal of the best proposer
				if (accept != null) {
					System.out.println(
							"Accepting proposal " + bestProposal + " from responder " + bestProposer.getLocalName());
					accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
					assignment();
				}
			}

			private void assignment() {
				// TODO Auto-generated method stub

			}

			protected void handleInform(ACLMessage inform) {
				System.out.println(
						"Agent " + inform.getSender().getLocalName() + " successfully performed the requested action");
			}
		});
	}

	public VehicleAgent getAgent(String name) {
		if (name == this.getName()) {
			return this;
		}
		return null;
	}

	// Generates with 80% probability goal as "save money" (= "m")
	// and with 20% probability goal to "save time" (= "t")
	private String RandomGoal() {
		List<String> list = Arrays.asList("m", "m", "m", "m", "m", "m", "m", "m", "t", "t");
		Random rand = new Random();
		String randomgoal = list.get(rand.nextInt(list.size()));

		return randomgoal;
	}

	public double get_battery_life() {
		return battery_life;
	}
	
	public void set_battery_life(double new_battery) {
		battery_life = new_battery;
	}
	
	
	public boolean checkCharging() {
		Location CSlocation = CS.getLocation();
		Location vLocation = getLocation();
		setLocation(CSlocation);
		if(CSlocation == vLocation) {
			System.out.println("car is on charge");
			return true;
		}
		return false;
	}
	

	/**
	 * Place the fox at the new location in the given field.
	 * 
	 * @param newLocation The fox's new location.
	 */
    
	private void setLocation(Location newLocation) {
		if (location != null) {
			field.clear(location);
		}
		location = newLocation;
		field.place(this, newLocation);
	}

    public Location getLocation() {
    	return location;
    }

	public void get_job(int job) {
		switch (job) {
		case 1:
			// Perform operation 1: print out a message
			System.out.println("Random Move on street");
			// here we put the random move function on the street.
			break;
		case 2:
			System.out.println("Do Nothing -- chill");
			// here nothing is happening.
			break;

		}
	}	
	public void random_move() {
		int row = location.getRow();
		int col = location.getCol();
		int new_col = getRandomNumberInRange(col -1,col+1);
		int new_row = getRandomNumberInRange(row-1,row+1);
		int s = field.getStreetAt(new_row, new_col);
		//System.out.println("street at " + s);
		// this checks we are on a street.
		if(s == 1) {
			field.clear(location);
			//field.place(this, new Location(new_row,new_col));
			Location newLocation = new Location(new_row, new_col);
			field.place(this, newLocation);
			setLocation(newLocation);
		}
	}

	
	/**
	 * private class ChargingStationRequest extends Behaviour{
	 * 
	 * @Override public void action() { // TODO Auto-generated method stub
	 * 
	 *           }
	 * 
	 * @Override public boolean done() { // TODO Auto-generated method stub return
	 *           false; }
	 * 
	 *           }
	 **/
//end of class 
}
