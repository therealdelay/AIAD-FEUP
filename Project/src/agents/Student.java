package agents;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

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

	JSONObject studentInfo;

	protected void setup() {
		loadJSON();
		registerOnDF();
		searchAllCanteens();
		searchAllStudents();
		canteenOption = 0;
		addBehaviour(new ProposalBehaviour());

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

	
	class ProposalBehaviour extends Behaviour {
		
		int step = 0;
		ACLMessage msg;
		Timer timer;
		
		boolean sent_proposal = false;
		int votes_in_favor = 0;
		int votes = 0;

		@Override
		public void action() {
			switch(step) {

			case 0:

				Random timeout = new Random();
				int timeoutTime = timeout.nextInt(1000);
				timer = new Timer();

				timer.schedule(new TimerTask() {

					public void run() {
						
						msg = new ACLMessage(ACLMessage.PROPOSE);
						msg.setContent("FEUP");

						for (int i = 0; i < students.length; i++) {
							if (!students[i].getName().getLocalName().equals(getLocalName())) {
								msg.addReceiver(students[i].getName());
							}
						}

						send(msg);
						
						sent_proposal = true;

						System.out.println("Student " + getLocalName() + " proposes ");

					}
				}, timeoutTime);

				step = 1;

				break;

			case 1:

				msg = receive();

				if (msg != null && msg.getSender().getLocalName().contains("FMUP") && didEat == 1) {

					String msgCnt = msg.getContent();
					System.out.println("Resposta da cantina: " + msgCnt);
					didEat = 2;
					
					votes = 0;
					votes_in_favor = 0;
					sent_proposal = false;
					step = 0;

				} else if (msg != null) {

					if (msg.getPerformative() == ACLMessage.PROPOSE) {

						timer.cancel();
						timer.purge();

						// TODO: Check preferences and decide based on them
						ACLMessage reply = msg.createReply();
						reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
						send(reply);

						System.out.println("Student " + getLocalName() + " accepts");

					} else if (msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL && sent_proposal) {

						votes++;
						votes_in_favor++;

						if(students != null) {

							if (votes_in_favor >= students.length / 2) {
								
								step = 2;
								
							} else if (votes == students.length - 1) {

								// TODO: Change for proposed canteen
								discardCanteen("FEUP");

							}
						}

						System.out.println("Student " + getLocalName() + " proposal has " + votes + " votes");

					} else if(msg.getPerformative() == ACLMessage.REJECT_PROPOSAL && sent_proposal) {

						votes++;

						if(students != null) {

							if(votes == students.length - 1) {

								System.out.println("Proposal Rejected");
								
								// TODO: Change for proposed canteen
								discardCanteen("FEUP");

							}
						}

					} else if(msg.getPerformative() == ACLMessage.CONFIRM) {

						System.out.println("Student " + getLocalName() + " receives confirmation of canteen");
						//TODO: get canteen option

						step = 3;


					} else if(msg.getPerformative() == ACLMessage.DISCONFIRM) {

						step = 1;

					}

				} else {
					block();
				}
				break;
			
			case 2: 
				
				System.out.println("Student " + getLocalName() + " sending canteen choice confirmation!");
				ACLMessage msg = new ACLMessage(ACLMessage.CONFIRM);
				msg.setContent("Chosen Canteen-" + "ISEP");

				for (int i = 0; i < students.length; i++) {
					if (!students[i].getName().getLocalName().equals(getLocalName())) {
						msg.addReceiver(students[i].getName());
					}
				}

				send(msg);
				
				step = 3;
				
				break;
				
			case 3:
								
				msg = new ACLMessage(ACLMessage.INFORM);
				msg.setContent("1 lunch.");
				msg.addReceiver(new AID(canteens[canteenOption].getName().getLocalName(), AID.ISLOCALNAME));
				send(msg);
				didEat = 1;
				
				step = 1;
				
				break;

			default:
				break;

			}

		}

		@Override
		public boolean done() {
			return didEat == 2 ? true : false;
		}

		public void discardCanteen(String canteen) {

			ACLMessage msg = new ACLMessage(ACLMessage.DISCONFIRM);

			msg.setContent("Discarded Canteen-" + canteen);

			for (int i = 0; i < students.length; i++) {
				if (!students[i].getName().getLocalName().equals(getLocalName())) {
					msg.addReceiver(students[i].getName());
				}
			}

			send(msg);

			try {
				Thread.sleep(200);
				
				sent_proposal = false;
				votes = 0;
				votes_in_favor = 0;
				step = 0;

			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}

}
