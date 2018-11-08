package agents;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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


	//TODO: Put this variables in JSON
	long TIMEOUT_WAITING = 5000;
	double MAX_DISTANCE = 1.5; 
	double DECISION_HEURISTIC = 0.9;

	JSONObject studentInfo;
	String faculty;
	String groupID;
	HashMap<String, Integer> favoriteDishes;	//string - nome do prato, integer - escala de 1 a 10 para "ordenar" os pratos favoritos
	JSONObject pastExperiences;
	boolean hasEaten;

	HashMap<String, Double> canteenHeuristics;
	HashMap<String, ArrayList<String>> canteenDishes = new HashMap<>();
	ArrayList<String> rejectedProposals = new ArrayList<>();

	protected void setup() {
		loadJSON();
		registerOnDF();
		searchAllCanteens();
		searchAllStudents();
		canteenOption = 0;
		canteenHeuristics = new HashMap<>();
		addBehaviour(new HeuristicsBehaviour());
		//addBehaviour(new WaitingForLunchBehaviour());

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
		this.hasEaten = (boolean) studentInfo.get("hasEaten");
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
		if(this.hasEaten) {
			sdLunch.setType("has-eaten");
		} else {
			sdLunch.setType("hasnt-eaten");
		}
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


	class WaitingForLunchBehaviour extends Behaviour {

		long startingTime = System.currentTimeMillis();

		@Override
		public void action() {

			ACLMessage msg = receive();

			if(msg != null) {

				if(msg.getPerformative() == ACLMessage.INFORM && msg.getContent().contains("Today Experience")) {

					//Today Experience:<Faculty Name>:<Rating>
					String[] todayExperience = msg.getContent().split(":");

					Double currentHeuristic = canteenHeuristics.get(todayExperience[1].trim());
					Double newHeuristic = currentHeuristic*0.9 + Double.parseDouble(todayExperience[2].trim())*0.1;
					canteenHeuristics.put(todayExperience[1].trim(), newHeuristic);

				}

			}

		}

		@Override
		public boolean done() {

			return (System.currentTimeMillis() - startingTime >= TIMEOUT_WAITING*Integer.parseInt(groupID));
		}



	}


	class HeuristicsBehaviour extends Behaviour {

		int step = 0;
		int currentFaculty = -1;
		String currentFacultyName = null;

		@Override
		public void action() {
			switch(step) {

			case 0:

				currentFaculty++;

				if(currentFaculty >= canteens.length) {
					return;
				}
				ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
				msg.setContent("Canteen Info : " + faculty);
				msg.addReceiver(canteens[currentFaculty].getName());
				send(msg);

				currentFacultyName = canteens[currentFaculty].getName().getLocalName();

				step = 1;

				break;

			case 1:

				ACLMessage answer = receive();

				if(answer != null) {

					if(answer.getPerformative() == ACLMessage.INFORM) {

						String info = answer.getContent();
						String[] infoArray = info.split(":");

						double distance = Double.parseDouble(infoArray[infoArray.length - 1]);
						canteenDishes.put(currentFacultyName, new ArrayList<String>(Arrays.asList(Arrays.copyOf(infoArray, infoArray.length-1))));
												
						distance = 1 - distance / MAX_DISTANCE;

						int favoriteDishesCounter = 0;
						int dishesHeuristics = 0;

						//System.out.println("Student " + this.getAgent().getAID().getLocalName() + " favorite dishes: " + favoriteDishes + ".\ncanteenDishes: " + info);

						for(int i = 0; i < infoArray.length - 1; i++) {

							if(favoriteDishes.containsKey(infoArray[i])) {
								favoriteDishesCounter++;
								//dishesHeuristics += favoriteDishes.get(infoArray[i])*0.1;
							}
						}

						dishesHeuristics = favoriteDishesCounter / (infoArray.length - 1);

						JSONArray canteenInfo = (JSONArray) ((JSONObject) studentInfo.get("past-experience"))
								.get(currentFacultyName);

						double pastExperienceHeuristic = 0;
						for(int i = 0; i < canteenInfo.size(); i++) {
							pastExperienceHeuristic += (Double) canteenInfo.get(i);
						}

						pastExperienceHeuristic = pastExperienceHeuristic / canteenInfo.size();

						//System.out.println("distance: " + distance + "\ndishes: " + dishesHeuristics + "\npastExp: " + pastExperienceHeuristic + "\nnumFavDishes: " + favoriteDishesCounter);

						double heuristic = distance*0.15 + dishesHeuristics*0.20 + pastExperienceHeuristic*0.65;

						canteenHeuristics.put(currentFacultyName, heuristic);

						step = 0;

					} 

				} 
				else {
					block();
				}

				break;

			default:

				break;

			}
		}

		@Override
		public boolean done() {

			if(currentFaculty >= canteens.length) {
				System.out.println(getLocalName() + " HEURISTICS: "+ canteenHeuristics);
				this.getAgent().addBehaviour(new ProposalBehaviour());
			}

			return currentFaculty >= canteens.length;
		}

	}


	class ProposalBehaviour extends Behaviour {

		int step = 0;
		ACLMessage msg;
		Timer timer;

		String canteen;
		HashMap<String, Double> updatedCanteenHeuristics = canteenHeuristics;
		ArrayList<String> dishes = new ArrayList<>();
		String chosenDish;

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

						ACLMessage proposal = new ACLMessage(ACLMessage.PROPOSE);

						System.out.println("Reject Proposals " + rejectedProposals);


						Map.Entry<String, Double> maxEntry = null;
						for(Map.Entry<String, Double> entry : updatedCanteenHeuristics.entrySet()) {

							if(maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0) {
								maxEntry = entry;
							}
						}

						canteen = maxEntry.getKey();

						if(maxEntry.getValue() == 0.0) {

							canteen = rejectedProposals.get(0);
							step = 2;

						} else if(maxEntry.getValue() == 0.0 && rejectedProposals.size() == 0) {

							System.out.println("There are no more canteens available with the necessary dishes");
							return;

						} else {

							System.out.println("Proposer " + getLocalName() + " heuristics " + updatedCanteenHeuristics);

							System.out.println("Proposer " + getLocalName() + " proposes " + canteen);

							proposal.setContent(canteen);

							for (int i = 0; i < students.length; i++) {
								if (!students[i].getName().getLocalName().equals(getLocalName())) {
									proposal.addReceiver(students[i].getName());
								}
							}

							send(proposal);

							sent_proposal = true;

						}
					}
				}, timeoutTime);

				step = 1;

				break;

			case 1:

				msg = receive();

				if (msg != null) {

					if(msg.getPerformative() == ACLMessage.INFORM && msg.getSender().getLocalName().contains(canteen)) {

						//After getting the majority of votes in favor, proposer asks canteen for it's quantity of dishes

						String msgCnt = msg.getContent();
						System.out.println("Resposta da cantina: " + msgCnt);

						if(Integer.parseInt(msgCnt) >= students.length) {
							step = 3;
						} else {
							discardCanteen(canteen, 1);							
						}

					} else if (msg.getPerformative() == ACLMessage.PROPOSE) {

						// Students receives proposal

						timer.cancel();
						timer.purge();

						ACLMessage reply = msg.createReply();
						String proposal = msg.getContent().trim();

						Double heuristic = canteenHeuristics.get(proposal);

						if(heuristic >= DECISION_HEURISTIC) {

							reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
							System.out.println("Student " + getLocalName() + " accepts proposal");

						} else {

							reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
							System.out.println("Student " + getLocalName() + " rejects proposal");
						}

						send(reply);


					} else if (msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL && sent_proposal) {

						// Proposer receives a positive vote to its proposal

						votes++;
						votes_in_favor++;

						if(students != null) {

							if (votes_in_favor >= students.length / 2) {

								step = 2;

							} else if (votes == students.length - 1) {

								discardCanteen(canteen, 0);

							}
						}

						System.out.println("Student " + getLocalName() + " proposal has " + votes + " votes");

					} else if(msg.getPerformative() == ACLMessage.REJECT_PROPOSAL && sent_proposal) {

						// Proposer receives a negative vote to its proposal

						votes++;

						if(students != null) {

							if (votes_in_favor >= students.length / 2) {

								step = 2;

							} else if (votes == students.length - 1) {

								discardCanteen(canteen, 0);

							}
						}

					} else if(msg.getPerformative() == ACLMessage.CONFIRM) {

						// Other students of the group receive confirmation from the proposer that the proposal was accepted by the majority

						canteen = msg.getContent().split("-")[1].trim();

						System.out.println("Student " + getLocalName() + " receives confirmation of canteen " + canteen);
						
						dishes = getDishesOrderedByRaking();
						step = 4;


					} else if(msg.getPerformative() == ACLMessage.DISCONFIRM) {

						// Other students of the group receive confirmation from the proposer that the proposal was NOT accepted by the majority

						String canteenDiscarded = msg.getContent().split("-")[1].trim();
						rejectedProposals.add(canteenDiscarded);
						updatedCanteenHeuristics.put(canteen, 0.0);
						step = 1;

					} else if(msg.getPerformative() == ACLMessage.AGREE) {

						// The student can eat the chosen dish at the canteen
						System.out.println("Student " + getLocalName() + " eats " + chosenDish + " at " + canteen);
						step = 5;

					} else if(msg.getPerformative() == ACLMessage.CANCEL) {

						// The student cannot eat the chosen dish at the canteen
						step = 4;
						System.out.println("Student " + getLocalName() + " CANNOT eat " + chosenDish + " at " + canteen);
						dishes.remove(chosenDish);

					}

				} 

				break;

			case 2: 

				// After everyone accepts proposal, see if canteen has enough dishes

				ACLMessage askCanteen = new ACLMessage(ACLMessage.REQUEST);

				//TODO: Change to number of elements in group
				askCanteen.setContent("3");
				askCanteen.addReceiver(new AID(canteen, AID.ISLOCALNAME));
				send(askCanteen);

				step = 1;

				break;

			case 3:

				// After getting the majority of votes in favor, the proposer send confirmation of the proposal

				System.out.println("Student " + getLocalName() + " sending canteen choice confirmation!");
				ACLMessage msg = new ACLMessage(ACLMessage.CONFIRM);
				msg.setContent("Chosen Canteen-" + canteen);

				for (int i = 0; i < students.length; i++) {
					if (!students[i].getName().getLocalName().equals(getLocalName())) {
						msg.addReceiver(students[i].getName());
					}
				}

				send(msg);

				dishes = getDishesOrderedByRaking();
				step = 4;

				break;

			case 4:

				// Ask canteen if it can eat the chosen dish

				if(dishes.size() == 0) {

					step = 6;

				} else {

					chosenDish = dishes.get(0);

					System.out.println("Student " + getLocalName() + " asking canteen " + canteen + " for dish " + chosenDish);
					msg = new ACLMessage(ACLMessage.REQUEST);
					msg.setContent("Eating:" + chosenDish);
					msg.addReceiver(new AID(canteen, AID.ISLOCALNAME));
					send(msg);

					step = 1;

				}


				break;

			case 5:

				System.out.println("Student " + getLocalName() + " has finished eating!");
				this.getAgent().removeBehaviour(this);
				break;

			case 6:

				System.out.println("Student " + getLocalName() + " couldn't eat because the canteen have no available dishes");
				this.getAgent().removeBehaviour(this);
				break;

			default:
				break;

			}

		}

		public void discardCanteen(String canteen, int phase) {


			ACLMessage msg = new ACLMessage(ACLMessage.DISCONFIRM);

			msg.setContent("Discarded Canteen-" + canteen);

			for (int i = 0; i < students.length; i++) {
				if (!students[i].getName().getLocalName().equals(getLocalName())) {
					msg.addReceiver(students[i].getName());
				}
			}

			send(msg);

			if(phase == 0) {
				rejectedProposals.add(canteen);
				updatedCanteenHeuristics.put(canteen, 0.0);				
			}
			else {
				rejectedProposals.remove(canteen);
			}

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

		public ArrayList<String> getDishesOrderedByRaking() {
			
			favoriteDishes = sortFavoriteDishes();
			
			System.out.println("Favourite Dishes " + getLocalName() + " : " + favoriteDishes);
			
			ArrayList<String> dishesOrdered = new ArrayList<>();
			ArrayList<String> canteenDishesTmp = (ArrayList<String>) canteenDishes.get(canteen).clone();
			
			for(Map.Entry<String, Integer> entry : favoriteDishes.entrySet()) {

				if(canteenDishesTmp.contains(entry.getKey())) {
					dishesOrdered.add(entry.getKey());
					canteenDishesTmp.remove(entry.getKey());
				}
			}
			
			for(int i = 0; i < canteenDishesTmp.size(); i++) {
				dishesOrdered.add(canteenDishesTmp.get(i));
			}
			
			System.out.println("Canteen Dishes " + getLocalName() + " : " + canteenDishes.get(canteen));
			System.out.println("Dishes Ordered " + getLocalName() + " : " + dishesOrdered);
			
			return dishesOrdered;
			


		}

		public HashMap<String, Integer> sortFavoriteDishes() {
			List list = new LinkedList(favoriteDishes.entrySet());
			
			Collections.sort(list, new Comparator() {
				public int compare(Object o1, Object o2) {
					return ((Comparable) ((Map.Entry) (o2)).getValue())
							.compareTo(((Map.Entry) (o1)).getValue());
				}
			});
			
			HashMap<String, Integer> sortedHashMap = new LinkedHashMap<>();
			for (Iterator it = list.iterator(); it.hasNext();) {
				Map.Entry entry = (Map.Entry) it.next();
				sortedHashMap.put((String) entry.getKey(), (Integer) entry.getValue());
			} 
						
			return sortedHashMap;
		}

		@Override
		public boolean done() {
			return didEat == 2 ? true : false;
		}

	}



}
