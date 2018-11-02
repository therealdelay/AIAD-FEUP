package agents;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import org.json.simple.JSONObject;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

public class Student extends Agent {

	int didEat = 0;
	DFAgentDescription[] canteens = null;
	DFAgentDescription[] students = null;
	int canteenOption = -1;
	Timer timer;

	boolean sent_proposal = false; // Sender of proposal
	boolean canteen_chosen = false; // If canteen option has majority of votes
	boolean waiting_confirm = false; // If after voting, the students are waiting for the proposer verdict
	boolean declare_proposal = false; // If proposal sender has sent the declaration of the acceptance of said proposal
	int votes = 0;
	
	JSONObject studentInfo;

	protected void setup() {
		loadJSON();
		registerOnDF();
		searchAllCanteens();
		searchAllStudents();
		canteenOption = chooseCanteen();
		addBehaviour(new EatingBehaviour());
		addBehaviour(new ListeningBehaviour());
		addBehaviour(new ProposeCanteenBehaviour());
		addBehaviour(new AnnounceChosenCanteenBehaviour());

	}

	protected int chooseCanteen() {
		Random r = new Random();
		return r.nextInt(canteens.length);
	}
	
	protected void loadJSON() {
		Object[] args = getArguments();
		studentInfo = (JSONObject) args[0];
		// System.out.println("estudante: " + studentInfo.toJSONString());
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
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
	}
	
	
	class EatingBehaviour extends Behaviour {

		@Override
		public void action() {
			if (canteen_chosen) {
				ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
				msg.setContent("1 lunch.");
				msg.addReceiver(new AID(canteens[canteenOption].getName().getLocalName(), AID.ISLOCALNAME));
				send(msg);
				didEat = 1;
			}
		}

		public boolean done() {
			return didEat == 0 ? false : true;
		}

	}
	
	
	/**
	 * 
	 * Listening Behaviour, responsible for listening all messages and updating the necessary information
	 *
	 */
	class ListeningBehaviour extends CyclicBehaviour {

		public void action() {
			ACLMessage msg = receive();
			if (msg != null && msg.getSender().getLocalName().contains("canteen") && didEat == 1) {

				String msgCnt = msg.getContent();
				System.out.println("Resposta da cantina: " + msgCnt);
				didEat = 2;

			} else if (msg != null) {

				if (msg.getPerformative() == ACLMessage.PROPOSE) {
					
					timer.cancel();
					timer.purge();

					ACLMessage reply = msg.createReply();
					reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
					send(reply);
					
					waiting_confirm = true;
					
					System.out.println("Student " + getLocalName() + " accepts");
					
				} else if (msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL && sent_proposal) {
					
					votes++;
					if(students != null) {
						
						if (votes > students.length / 2) {

							canteen_chosen = true;
							
						}
					}
					
					System.out.println("Student " + getLocalName() + " proposal has " + votes + " votes");
					
				} else if(msg != null && msg.getPerformative() == ACLMessage.CONFIRM) {
					
					canteen_chosen = true;
					waiting_confirm = false;
					System.out.println("Student " + getLocalName() + " receives confirmation of canteen");
					
				} 
				
			} else {
				block();
			}
		}

	}
	
	
	/**
	 * 
	 * Propose Canteen Behaviour, responsible for making a proposal to other group students
	 * with the canteen the user prefers.
	 * 
	 * TODO: Chose canteen based on likes and experience
	 * TODO: Make proposal to same group students
	 *
	 */
	class ProposeCanteenBehaviour extends Behaviour {

		public void action() {
			Random timeout = new Random();
			int timeoutTime = timeout.nextInt(10000);
			timer = new Timer();
			timer.schedule(new TimerTask() {

				public void run() {
					if (!sent_proposal && !canteen_chosen && !waiting_confirm && didEat == 0) {

						searchAllStudents();
						ACLMessage msg = new ACLMessage(ACLMessage.PROPOSE);
						msg.setContent("FEUP");

						for (int i = 0; i < students.length; i++) {
							if (!students[i].getName().getLocalName().equals(getLocalName())) {
								msg.addReceiver(students[i].getName());
							}
						}

						send(msg);
						sent_proposal = true;

						System.out.println("Student " + getLocalName() + " proposes");
					}
				}
			}, timeoutTime);

		}

		public boolean done() {
			return canteen_chosen;
		}
	}
	
	/**
	 * 
	 * Announce Chosen Canteen behaviour, responsible for informing other students of same group
	 * that the voted canteen had makority of votes and is the one they will go to.
	 * 
	 * TODO: Confirm to same group students
	 *
	 */
	class AnnounceChosenCanteenBehaviour extends Behaviour {

		@Override
		public void action() {
			if(sent_proposal && canteen_chosen) {
				
				System.out.println("Student " + getLocalName() + " sending canteen choice confirmation!");
				ACLMessage msg = new ACLMessage(ACLMessage.CONFIRM);
				msg.setContent("Chosen Canteen-" + "FEUP");

				for (int i = 0; i < students.length; i++) {
					if (!students[i].getName().getLocalName().equals(getLocalName())) {
						msg.addReceiver(students[i].getName());
					}
				}

				send(msg);

				declare_proposal = true;
			}

		}

		@Override
		public boolean done() {
			return declare_proposal;
		}

	}

}
