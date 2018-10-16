package agents;

import java.util.Random;

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
	int canteenOption = -1;
	
	protected void setup() {
		registerOnDF();
		searchAllCanteens();
		canteenOption = chooseCanteen();
		addBehaviour(new EatingBehaviour());
		addBehaviour(new ListeningBehaviour());
		
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
	
	
	
}
