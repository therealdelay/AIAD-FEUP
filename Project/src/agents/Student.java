package agents;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.AMSAgentDescription;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

public class Student extends Agent {
	
	int didEat = 0;
	DFAgentDescription[] canteens = null;
	DFAgentDescription[] students = null;
	int canteenOption = -1;
	
	boolean sent_proposal = false;
	boolean canteen_chosen = false;
	int votes = 0;
	
	protected void setup() {
		registerOnDF();
		searchAllCanteens();
		searchAllStudents();
		canteenOption = chooseCanteen();
		//addBehaviour(new EatingBehaviour());
		//addBehaviour(new ListeningBehaviour());
		addBehaviour(new ProposeCanteenBehaviour());
		
	}
	
	protected int chooseCanteen() {
		Random r = new Random();
		return r.nextInt(canteens.length);
	}
	
	protected void searchAllCanteens() {
		DFAgentDescription template = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType("canteen");
		template.addServices(sd);
		try {
			canteens = DFService.search(this, template);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		} 
	}
	
	public void searchAllStudents() {
		DFAgentDescription template = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType("student");
		template.addServices(sd);
		try {
			students = DFService.search(this, template);
			
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
	}
	
	protected void registerOnDF() {
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("student");
		sd.setName(getLocalName());
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		} catch(FIPAException fe) {
			fe.printStackTrace();
		}
	}
	
	class EatingBehaviour extends Behaviour {

		@Override
		public void action() {
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			msg.setContent("1 lunch.");
			msg.addReceiver(new AID(canteens[canteenOption].getName().getLocalName(), AID.ISLOCALNAME));
			send(msg);
			didEat = 1;
		}

		@Override
		public boolean done() {
			return didEat == 0 ? false : true;
		}
		
	}
	
	class ListeningBehaviour extends CyclicBehaviour {
		
		public void action() {
			ACLMessage msg = receive();
			if(msg != null) {
				String msgCnt = msg.getContent();
				System.out.println("Resposta da cantina: " + msgCnt);
				didEat = 2;
			} else {
				block();
			}
		}

	}
	
	class ProposeCanteenBehaviour extends Behaviour {
		
		Timer timer;
		
		public void action() {
			Random timeout = new Random();
			int timeoutTime = timeout.nextInt(10000);
			timer = new Timer();
			timer.schedule(new TimerTask() {

				public void run() {
					if(!sent_proposal && !canteen_chosen) {
						
						searchAllStudents();
						ACLMessage msg = new ACLMessage(ACLMessage.PROPOSE);
						msg.setContent("FEUP");
						
						for(int i = 0; i < students.length; i++) {
							if(!students[i].getName().getLocalName().equals(getLocalName())) {
								msg.addReceiver(new AID(students[i].getName().getLocalName(), AID.ISLOCALNAME));
							}
						}			
						
						send(msg);
						sent_proposal = true;						
					}
				  }
				}, timeoutTime);
			
			
			
			ACLMessage msg = receive();
			if(msg != null) {
				
				if(msg.getPerformative() == ACLMessage.PROPOSE) {
					
					timer.cancel();
					timer.purge();
					ACLMessage reply = msg.createReply();
					reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
					send(reply);
					canteen_chosen = true;
				}
				else if (msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL && sent_proposal) {
					votes++;
				}
				
			} else {
				block();
			}
			
			
		}

		@Override
		public boolean done() {
			if(students != null)
				
				if(votes > students.length/2) {
					
					votes = 0;
					canteen_chosen = false;
					sent_proposal = false;
					return true;
					
				}
				else 
					return false;
			return canteen_chosen;
		}
		
	}
	
	
	
}
