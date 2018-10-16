package agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

public class Canteen extends Agent {
	
	int quantity = 1;
	
	protected void setup() {
		registerOnDF();
		addBehaviour(new ListeningBehaviour());
	}
	
	protected void registerOnDF() {
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("canteen");
		sd.setName(getLocalName());
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		} catch(FIPAException fe) {
			fe.printStackTrace();
		}
	}
	
	class ListeningBehaviour extends CyclicBehaviour {
		
		public void action() {
			ACLMessage msg = receive();
			if(msg != null) {
				ACLMessage reply = msg.createReply();
				reply.setPerformative(ACLMessage.INFORM);
				if(quantity > 0) {
					reply.setContent("Cantina " + getLocalName() + " OK!");
					quantity--;
				}
				else
					reply.setContent("Cantina " + getLocalName() + " NO!");
				send(reply);
			} else {
				block();
			}
		}

	}
}