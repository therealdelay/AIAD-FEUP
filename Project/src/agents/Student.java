package agents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import agents.Canteen.MealPair;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import utils.CanteenAnswer;

public class Student extends Agent {

	int didEat = 0;
	DFAgentDescription[] canteens = null;
	DFAgentDescription[] students = null;
	int canteenOption = -1;

	JSONObject studentInfo;
	String faculty;
	String groupID;
	HashMap<String, Integer> favoriteDishes;	//string - nome do prato, integer - escala de 1 a 10 para "ordenar" os pratos favoritos
	JSONObject pastExperiences;

	HashMap<String, Double> canteenHeuristics;

	ConcurrentHashMap<String, ArrayList<Integer>> colleaguesExp;

	protected void setup() {
		loadJSON();
		registerOnDF();
		searchAllCanteens();
		searchAllStudents();
		canteenOption = 0;
		canteenHeuristics = new HashMap<>();
		addBehaviour(new HeuristicsBehaviour());
		//addBehaviour(new ProposalBehaviour());

	}

	protected int chooseCanteen() {
		Random r = new Random();
		return r.nextInt(canteens.length);
	}

	protected void loadJSON() {
		Object[] args = getArguments();
		studentInfo = (JSONObject) args[0];
		this.faculty = (String) studentInfo.get("current-university");
		this.groupID = String.valueOf(studentInfo.get("groupID"));
		this.pastExperiences = (JSONObject) studentInfo.get("past-experience");
		this.favoriteDishes = new HashMap<String, Integer>();

		JSONObject favoriteDishes = (JSONObject) studentInfo.get("favorite-dishes");

		JSONObject favoriteMeatDishes = (JSONObject) favoriteDishes.get("meat");
		for(Object key : favoriteMeatDishes.keySet()) {
			this.favoriteDishes.put((String) key, ((Long) favoriteMeatDishes.get(key)).intValue());
		}

		JSONObject favoriteFishDishes = (JSONObject) favoriteDishes.get("fish");
		for(Object key : favoriteFishDishes.keySet()) {
			this.favoriteDishes.put((String) key, ((Long) favoriteFishDishes.get(key)).intValue());
		}

		JSONObject favoriteVegDishes = (JSONObject) favoriteDishes.get("veg");
		for(Object key : favoriteVegDishes.keySet()) {
			this.favoriteDishes.put((String) key, ((Long) favoriteVegDishes.get(key)).intValue());
		}

		JSONObject favoriteDietDishes = (JSONObject) favoriteDishes.get("diet");
		for(Object key : favoriteDietDishes.keySet()) {
			this.favoriteDishes.put((String) key, ((Long) favoriteDietDishes.get(key)).intValue());
		}
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

	public void searchGroupStudents() {
		DFAgentDescription template = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType(this.faculty + "-Group" + this.groupID);
		template.addServices(sd);
		try {
			students = DFService.search(this, template);

		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
	}

	public void searchFacultyStudents() {
		DFAgentDescription template = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType(this.faculty);
		template.addServices(sd);
		try {
			students = DFService.search(this, template);
			for(DFAgentDescription dfad : students) {
				System.out.println("AGENT " + this.getLocalName() + ": " + dfad.getName().getLocalName());
			}
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
	}

	public void searchFacultyStudentsWhoHaventEaten() {
		DFAgentDescription template = new DFAgentDescription();
		ServiceDescription sd1 = new ServiceDescription();
		ServiceDescription sd2 = new ServiceDescription();
		sd1.setType(this.faculty);
		sd2.setType("hasnt-eaten");
		template.addServices(sd1);
		template.addServices(sd2);
		try {
			students = DFService.search(this, template);
			for(DFAgentDescription dfad : students) {
				System.out.println("AGENT " + this.getLocalName() + ": " + dfad.getName().getLocalName());
			}
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

		ServiceDescription sdFaculty = new ServiceDescription();
		sdFaculty.setType(this.faculty);
		sdFaculty.setName(getLocalName());

		ServiceDescription sdGroup = new ServiceDescription();
		sdGroup.setType(this.faculty + "-Group" + this.groupID);
		sdGroup.setName(getLocalName());

		ServiceDescription sdLunch = new ServiceDescription();
		sdLunch.setType("hasnt-eaten");
		sdLunch.setName(getLocalName());

		dfd.addServices(sd);
		dfd.addServices(sdFaculty);
		dfd.addServices(sdGroup);
		dfd.addServices(sdLunch);
		try {
			DFService.register(this, dfd);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
	}



	protected void canteensRating() {



	}

	protected void canteenRating(String canteenName, double distance, HashMap<String, MealPair<String, Integer>> menu) {

		JSONArray canteenInfo = (JSONArray) ((JSONObject) studentInfo.get("past-experience")).get(canteenName);

		double sum = 0;
		for(int i = 0; i < canteenInfo.size(); i++) {
			sum += (Double) canteenInfo.get(i);
		}

		double average = sum / canteenInfo.size();




		double heuristics = distance * 0.15 + average * 0.5;

	}

	class HeuristicsBehaviour extends Behaviour {

		int step = 0;
		int currentFaculty = 0;

		@Override
		public void action() {
			switch(step) {

			case 0:

				if(currentFaculty >= canteens.length) {
					return;
				}

				ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
				msg.setContent("Canteen Info : " + faculty);

				msg.addReceiver(canteens[currentFaculty].getName());
				send(msg);
				currentFaculty++;
				step = 1;

				break;

			case 1:

				ACLMessage answer = receive();

				if(answer != null) {

					if(answer.getPerformative() == ACLMessage.INFORM) {

						try {
							
							CanteenAnswer info = (CanteenAnswer) answer.getContentObject();

							double distance = info.getDistance();
							ArrayList<String> menu = info.getDishes();

							// TODO: calculate heuristic

							step = 0;

						} catch (UnreadableException e) {
							return;
						}


					} 

				} 

				break;

			default:

				break;

			}
		}

		@Override
		public boolean done() {
			// TODO Auto-generated method stub
			return currentFaculty >= canteens.length;
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

					//TODO: Check if number of dishes is enough
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

		@Override
		public boolean done() {
			return didEat == 2 ? true : false;
		}

	}



}
