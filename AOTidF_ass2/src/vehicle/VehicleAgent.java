package vehicle;
import java.text.SimpleDateFormat;

import java.util.*;

import jade.core.Agent;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.proto.ContractNetInitiator;
import jade.domain.FIPANames;

import java.util.Date;
import java.util.Vector;
import java.util.Enumeration;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.Property;
import java.util.Date;


/**
 * @author anoulis
 * The car has goal time or money
 * Mode charging or moving
 * Booking preferences for the time-slots
 * 
 *
 */
public class VehicleAgent extends Agent{
	//Battery status of the agent
	private int battery_life;
	// Goal to save money "m" or save time "t"
	private String goal = RandomGoal();

	private int nResponders;
	
	//Position of the agent
	
	//Vehicle Initialization
	@Override
	protected void setup() {
		//Object[] args = getArguments();
		//String goal = (String) args[0]
		// msg of created agent
		//System.out.println("Vehicle Agent "+getAID().getName()+" is ready with goal " + goal);
	    //System.out.println("Agent "+getLocalName()+" searching for services of type \"Charging-Points\"");
		
		ACLMessage msg = new ACLMessage(ACLMessage.CFP);
		nResponders = 2;
		
		// Check in yellow pages every 5s
		addBehaviour(new TickerBehaviour(this, 5000) {
			protected void onTick() {
				try {
					// Build the description used as template for the search
					DFAgentDescription template = new DFAgentDescription();
					ServiceDescription templateSd = new ServiceDescription();
					templateSd.setType("Charging-Points");
					createYellowPageEntry(templateSd);
					template.addServices(templateSd);
					//SearchConstraints sc = new SearchConstraints();
					//sc.setMaxResults();
					
					DFAgentDescription[] results = DFService.search(this.getAgent(),template);
					yellowPagesResults(msg,results);

				}
		        
				catch (FIPAException fe) {
					fe.printStackTrace();
				}
			}
		} );
	}
	
	/** 
	 * Function to do the Registrations in Yellow Pages
	 * for every vehicle agent.
	 * It should use vehicle object to add properties.
	 * We should start with the following ones:
	 * mode: fast or slow (type of charging that we want)
	 * start/end : the one of the available time-slot for charging (12-14,14-16)
	 * booked: no : we want only free place
	 */
	public void createYellowPageEntry(ServiceDescription templateSd) {
		
		// this should be completely changed, to be iterative for every Vehicle Agent
		if (getLocalName().equalsIgnoreCase("VehicleAgent1")) {
			templateSd.addProperties(new Property("mode", "fast"));
			templateSd.addProperties(new Property("start", "12"));
			templateSd.addProperties(new Property("end", "14"));
			templateSd.addProperties(new Property("booked", "no"));
		}
		else if (getLocalName().equalsIgnoreCase("VehicleAgent2")) {
			templateSd.addProperties(new Property("mode", "fast"));
			templateSd.addProperties(new Property("start", "14"));
			templateSd.addProperties(new Property("end", "16"));
			templateSd.addProperties(new Property("booked", "no"));
		}
		else {
			templateSd.addProperties(new Property("mode", "slow"));
			templateSd.addProperties(new Property("start", "14"));
			templateSd.addProperties(new Property("end", "16"));
			templateSd.addProperties(new Property("booked", "no"));
		}
	}
	
	
    /**
     * Taking the results for searching for the suitable services in yellow-pages
     * the vehicle agent start contractNet with the appropriate CS Agents.
     * 	
     * @param msg
     * @param results
     */
	public void yellowPagesResults(ACLMessage msg,DFAgentDescription[] results) {
		System.out.println("");
		String timeStamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
		System.out.println("On Time " + timeStamp);

		if (results.length > 0) {
			System.out.println("Agent "+getLocalName()+" found the following Charging-Points services:");
			for (int i = 0; i < results.length; ++i) {
				DFAgentDescription dfd = results[i];
				AID provider = dfd.getName();
				// The same agent may provide several services; we are only interested
				// in the Charging-Points one
				Iterator it = dfd.getAllServices();
				while (it.hasNext()) {
					ServiceDescription sd = (ServiceDescription) it.next();
					if (sd.getType().equalsIgnoreCase("Charging-Points")) {
						System.out.println("- Service \""+sd.getName()+"\" provided by agent "+provider.getLocalName());
						msg.addReceiver(new AID((String) provider.getLocalName(), AID.ISLOCALNAME));
					}
				}
			}
			msg.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
			// We want to receive a reply in 10 secs
			msg.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
			msg.setContent("charging-action");
			contractNet(msg);	
		}	
		else {
			System.out.println("Agent "+getLocalName()+" did not find any Charging-Points service");
		}
	}
	
	/**
	 * ContractNet function where we do the negotiations from the Vehicle Agent Part.
	 * Functions to handle the responses
	 * Code for the evaluation of the offers.
	 * 
	 * @param msg
	 */
	public void contractNet(ACLMessage msg) {
		addBehaviour(new ContractNetInitiator(this, msg) {
			
			protected void handlePropose(ACLMessage propose, Vector v) {
				System.out.println("Agent "+propose.getSender().getLocalName()+" proposed "+propose.getContent());
			}
			
			protected void handleRefuse(ACLMessage refuse) {
				System.out.println("Agent "+refuse.getSender().getLocalName()+" refused");
			}
			
			protected void handleFailure(ACLMessage failure) {
				if (failure.getSender().equals(myAgent.getAMS())) {
					// FAILURE notification from the JADE runtime: the receiver
					// does not exist
					System.out.println("Responder does not exist");
				}
				else {
					System.out.println("Agent "+failure.getSender().getLocalName()+" failed");
				}
				// Immediate failure --> we will not receive a response from this agent
				nResponders--;
			}
			
			protected void handleAllResponses(Vector responses, Vector acceptances) {
				if (responses.size() < nResponders) {
					// Some responder didn't reply within the specified timeout
					System.out.println("Timeout expired: missing "+(nResponders - responses.size())+" responses");
				}
				
				
				// Evaluate proposals.
				// Consider that the offer is an int ratio of an importance combination 
				// of time, money, distance or whatever.
				int bestProposal  =   -1;
				AID bestProposer  = null;
				ACLMessage accept = null;
				Enumeration e = responses.elements();
				while (e.hasMoreElements()) {
					ACLMessage msg = (ACLMessage) e.nextElement();
					if (msg.getPerformative() == ACLMessage.PROPOSE) {
						ACLMessage reply = msg.createReply();
						reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
						acceptances.addElement(reply);
						int proposal = Integer.parseInt(msg.getContent());
						if (proposal > bestProposal) {
							bestProposal = proposal;
							bestProposer = msg.getSender();
							accept = reply;
						}
					}
				}
				
				// Accept the proposal of the best proposer
				if (accept != null) {
					System.out.println("Accepting proposal "+bestProposal+" from responder "+bestProposer.getLocalName());
					accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
					assignment();
				}						
			}
			
			protected void handleInform(ACLMessage inform) {
				System.out.println("Agent "+inform.getSender().getLocalName()+" successfully performed the requested action");
			}			
		} );		
	}
	
	/**
	 * This functions should make an assignment.
	 * Something that will say that the cars is not moving, is charging now or whatever
	 */
	public void assignment() {
		
	}
	

	//Generates with 80% probability goal as "save money" (= "m")
	//and with 20% probability goal to "save time" (= "t")
	private String RandomGoal() {
		List<String> list = Arrays.asList("m", "m", "m", "m", "m", "m", "m", "m", "t", "t");
		Random rand = new Random();
		String randomgoal = list.get(rand.nextInt(list.size()));
		
		return randomgoal;
	}
	
	public int get_battery_life() {
		return battery_life;
	}
	
	/**private class ChargingStationRequest extends Behaviour{
		 
		@Override
		public void action() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public boolean done() {
			// TODO Auto-generated method stub
			return false;
		}
		
	}**/

}
