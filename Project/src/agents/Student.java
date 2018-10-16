package agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class Student extends Agent {
	
	boolean didEat = false;
	
	protected void setup() {
		addBehaviour(new EatingBehaviour());
		addBehaviour(new ListeningBehaviour());
		
	}
	
	class EatingBehaviour extends Behaviour {

		@Override
		public void action() {
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			msg.setContent("1 lunch.");
			msg.addReceiver(new AID("canteen1", AID.ISLOCALNAME));
			send(msg);
		}

		@Override
		public boolean done() {
			return didEat;
		}
		
	}
	
	class ListeningBehaviour extends CyclicBehaviour {
		
		public void action() {
			ACLMessage msg = receive();
			if(msg != null) {
				String msgCnt = msg.getContent();
				System.out.println("Resposta da cantina: " + msgCnt);
				didEat = true;
			} else {
				block();
			}
		}

	}
	
	
	
}
