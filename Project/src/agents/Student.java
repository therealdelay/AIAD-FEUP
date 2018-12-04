package agents;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
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
import java.util.Timer;
import java.util.TimerTask;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

/**
 * 
 * Student class, that represents a student from a faculty and group, which main goal is to eat lunch at a canteen.
 * The Student has favorite dishes, past experiences at canteens and different distances to each of them.
 * He has to factor all this when discussing with its group about which canteen they will go to to have lunch.
 *
 */
public class Student extends Agent {

	int didEat = 0;
	DFAgentDescription[] canteens = null;
	DFAgentDescription[] students = null;
	int canteenOption = -1;

	long TIMEOUT_WAITING = 5000;
	double MAX_DISTANCE = 1.5; 
	double DECISION_HEURISTIC = 0.5;

	JSONObject studentInfo;
	String faculty;
	String groupID;

	//String - Dish Name, Integer - 1-10 to rate dishes from most to least favorite
	HashMap<String, Integer> favoriteDishes;
	JSONObject pastExperiences;
	boolean hasEaten;

	HashMap<String, Double> canteenHeuristics;
	HashMap<String, ArrayList<String>> canteenDishes = new HashMap<>();
	HashMap<String, Double> canteenDistances = new HashMap<>();
	ArrayList<String> rejectedProposals = new ArrayList<>();

	long startTime = 0;
	long endTime = 0;
	long goingCanteenTime = 0;
	int votes_in_favor_stats = 0;

	FileWriter dataFile = null;

	protected void setup() {

		try {

			dataFile = new FileWriter("data.csv", true);
			loadJSON();
			startTime = System.nanoTime();
			registerOnDF();
			searchAllCanteens();
			searchGroupStudents();
			canteenOption = 0;
			canteenHeuristics = new HashMap<>();
			addBehaviour(new HeuristicsBehaviour());

		} catch (IOException e) {
			System.err.println("Error getting data file: " + e.getMessage());
		}

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

	public void searchGroupStudents() {
		DFAgentDescription template = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		ServiceDescription sd2 = new ServiceDescription();
		sd.setType(this.faculty + "-Group" + this.groupID);
		sd2.setType("hasnt-eaten");
		template.addServices(sd);
		template.addServices(sd2);
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

		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
	}

	/**
	 * Registers on services, such as the student faculty, student group and if
	 * it has already eaten.
	 * 
	 */
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

	/**
	 * Updates the student services, when the student has finished having its meal.
	 */
	protected void registerOnEaten() {
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());

		DFAgentDescription dfd1 = new DFAgentDescription();
		dfd1.setName(getAID());

		ServiceDescription sd = new ServiceDescription();
		sd.setType("student");
		sd.setName(getLocalName());

		ServiceDescription sdFaculty = new ServiceDescription();
		sdFaculty.setType(this.faculty);
		sdFaculty.setName(getLocalName());

		ServiceDescription sdGroup = new ServiceDescription();
		sdGroup.setType(this.faculty + "-Group" + this.groupID);
		sdGroup.setName(getLocalName());

		ServiceDescription sdEaten = new ServiceDescription();
		sdEaten.setType("has-eaten");
		sdEaten.setName(getLocalName());

		ServiceDescription sdNotEaten = new ServiceDescription();
		sdNotEaten.setType("hasnt-eaten");
		sdNotEaten.setName(getLocalName());

		dfd.addServices(sd);
		dfd.addServices(sdFaculty);
		dfd.addServices(sdGroup);
		dfd.addServices(sdNotEaten);

		dfd1.addServices(sd);
		dfd1.addServices(sdFaculty);
		dfd1.addServices(sdGroup);
		dfd1.addServices(sdEaten);
		try {
			DFService.deregister(this, dfd);
			DFService.register(this, dfd1);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
	}


	/**
	 * 
	 * WaitingForLunchBehaviour is a behavior responsible for listening to the opinions of other students who 
	 * have already eaten, and updating the heuristics of the student according to said opinions. This behavior
	 * is only active during a specific time period.
	 *
	 */
	class WaitingForLunchBehaviour extends Behaviour {

		long startingTime = System.currentTimeMillis();

		@Override
		public void action() {

			ACLMessage msg = receive();

			if(msg != null) {

				if(msg.getPerformative() == ACLMessage.INFORM && msg.getContent().contains("Today Experience")) {

					//Today Experience:<Faculty Name>:<Rating>
					String[] todayExperience = msg.getContent().split(":");

					System.out.println("Agent " + getLocalName() + " (" + faculty + " group " + groupID + ") received feedback '" + msg.getContent() + "' from " + msg.getSender().getLocalName());

					Double currentHeuristic = canteenHeuristics.get(todayExperience[1].trim());
					Double newHeuristic = currentHeuristic*0.9 + Double.parseDouble(todayExperience[2].trim())*0.1;
					canteenHeuristics.put(todayExperience[1].trim(), newHeuristic);
				}

			}

		}

		@Override
		public boolean done() {
			if(System.currentTimeMillis() - startingTime >= TIMEOUT_WAITING*(Integer.parseInt(groupID) - 1)) {
				this.getAgent().addBehaviour(new ProposalBehaviour());
			}
			return (System.currentTimeMillis() - startingTime >= TIMEOUT_WAITING*(Integer.parseInt(groupID) - 1));
		}



	}

	/**
	 * 
	 * HeuristicsBehavior is a behavior responsible for communicating with the canteens to get each daily Menu and distance to
	 * and, with that information and with the past experiences of the student in the canteen, calculate the 
	 * heuristic value for said canteen.
	 *
	 */
	class HeuristicsBehaviour extends Behaviour {

		int step = 0;
		int currentFaculty = -1;
		String currentFacultyName = null;

		@Override
		public void action() {
			switch(step) {

			case 0:
				// Sends request to canteen to get its daily menu and distance to the student faculty
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
				//Listens to canteen response and calculates heuristic
				ACLMessage answer = receive();

				if(answer != null) {

					if(answer.getPerformative() == ACLMessage.INFORM) {

						String info = answer.getContent();
						String[] infoArray = info.split(":");

						double distance = Double.parseDouble(infoArray[infoArray.length - 1]);
						canteenDishes.put(currentFacultyName, new ArrayList<String>(Arrays.asList(Arrays.copyOf(infoArray, infoArray.length-1))));
						canteenDistances.put(currentFacultyName, distance);

						distance = 1 - distance / MAX_DISTANCE;

						int favoriteDishesCounter = 0;
						int dishesHeuristics = 0;


						for(int i = 0; i < infoArray.length - 1; i++) {

							if(favoriteDishes.containsKey(infoArray[i])) {
								favoriteDishesCounter++;
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
				System.out.println("--------------------------------------------");
				System.out.println("Agent " + getLocalName() + " from " + faculty + " Group " + groupID +
						"\nCalculated heuristics: " + canteenHeuristics +
						"\nFavorite dishes: " + favoriteDishes);
				System.out.println("--------------------------------------------");
				this.getAgent().addBehaviour(new WaitingForLunchBehaviour());
			}

			return currentFaculty >= canteens.length;
		}

	}

	/**
	 * 
	 * ProposalBehaviour is a behavior responsible for proposing or being proposed to a canteen in which to have lunch.
	 * This behaviour starts with a student waiting a random number of time to make its proposal. If it doens't
	 * receive a proposal in the meantime, the student will propose a canteen with the biggest heuristic value that
	 * hasn't been rejected before. The other members of the group will agree or not, based on their own heuristics.
	 * If the proposal is accepted, the proposer will verify if the chosen canteen has enough dishes for all and if so,
	 * confirms the proposal to everyone on the group. If the proposal is rejected or there aren't enough dishes,
	 * the proposal will be rejected and the cycle begins again.
	 * Once the proposal is confirmed, each student will chose a dish of the daily meny(based on the student favorite 
	 * dishes) and request the dish to the canteen. If the request is denied, the student will chose other dish.
	 * If all canteen options are rejected, the first option to have been made will be the one chosen.
	 *
	 */
	class ProposalBehaviour extends Behaviour {

		int step = 0;
		ACLMessage msg;
		Timer timer;

		String canteen;
		HashMap<String, Double> updatedCanteenHeuristics = canteenHeuristics;
		ArrayList<String> dishes = new ArrayList<>();
		String chosenDish;

		boolean sent_proposal = false;
		boolean decided = false;
		int votes_in_favor = 0;
		int votes = 0;
		double canteenFeedback = 0.0;

		@Override
		public void action() {
			switch(step) {

			case 0:
				// Student creates timer to make a proposal
				Random timeout = new Random();
				int timeoutTime = timeout.nextInt(1000);
				timer = new Timer();

				timer.schedule(new TimerTask() {

					public void run() {

						ACLMessage proposal = new ACLMessage(ACLMessage.PROPOSE);

						System.out.println("Group " + groupID + " from " + faculty + " has already rejected " + rejectedProposals);

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

							System.out.println("Proposer " + getLocalName() + " PROPOSES " + canteen + " to " + faculty + " Group " + groupID);

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

					if(msg.getPerformative() == ACLMessage.INFORM && canteen != null && msg.getSender().getLocalName().contains(canteen)) {
						//After getting the majority of votes in favor, proposer asks canteen for it's quantity of dishes

						String msgCnt = msg.getContent();

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
							System.out.println("Student " + getLocalName() + " ACCEPTS proposal from " + faculty + " group " + groupID);

						} else {

							reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
							System.out.println("Student " + getLocalName() + " REJECTS proposal from " + faculty + " group " + groupID);
						}

						send(reply);


					} else if (msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL && sent_proposal) {
						// Proposer receives a positive vote to its proposal

						votes++;
						votes_in_favor++;

						if(students != null) {

							if (votes_in_favor >= students.length / 2 && !decided) {

								decided = true;
								step = 2;

							} else if (votes == students.length - 1 && votes_in_favor < students.length / 2) {

								discardCanteen(canteen, 0);

							}
						}


					} else if(msg.getPerformative() == ACLMessage.REJECT_PROPOSAL && sent_proposal) {
						// Proposer receives a negative vote to its proposal

						votes++;

						if(students != null) {

							if (votes_in_favor >= students.length / 2 && !decided) {

								decided = true;
								step = 2;

							} else if (votes == students.length - 1 && votes_in_favor < students.length / 2) {

								discardCanteen(canteen, 0);

							}
						}

					} else if(msg.getPerformative() == ACLMessage.CONFIRM) {
						// Other students of the group receive confirmation from the proposer that the proposal was accepted by the majority
						String[] arr = msg.getContent().split("-");
						canteen = arr[1].trim();

						votes_in_favor_stats = Integer.parseInt(arr[3].trim());

						dishes = getDishesOrderedByRanking();
						step = 4;


					} else if(msg.getPerformative() == ACLMessage.DISCONFIRM) {
						// Other students of the group receive confirmation from the proposer that the proposal was NOT accepted by the majority

						String canteenDiscarded = msg.getContent().split("-")[1].trim();
						rejectedProposals.add(canteenDiscarded);
						updatedCanteenHeuristics.put(canteen, 0.0);
						step = 1;

					} else if(msg.getPerformative() == ACLMessage.AGREE) {
						// The student eats the chosen dish at the canteen

						String[] parsedRating = msg.getContent().split(" ");
						canteenFeedback = Double.parseDouble(parsedRating[3].trim());

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
				// After everyone accepts proposal, proposer sees if canteen has enough dishes

				ACLMessage askCanteen = new ACLMessage(ACLMessage.REQUEST);

				askCanteen.setContent(Integer.toString(students.length));
				askCanteen.addReceiver(new AID(canteen, AID.ISLOCALNAME));
				send(askCanteen);

				step = 1;

				break;

			case 3:
				// After getting the majority of votes in favor, the proposer send confirmation of the proposal

				ACLMessage msg = new ACLMessage(ACLMessage.CONFIRM);
				votes_in_favor_stats = votes_in_favor + 1;
				msg.setContent("Chosen Canteen-" + canteen + "-votes-" + votes_in_favor_stats);

				for (int i = 0; i < students.length; i++) {
					if (!students[i].getName().getLocalName().equals(getLocalName())) {
						msg.addReceiver(students[i].getName());
					}
				}

				send(msg);

				dishes = getDishesOrderedByRanking();
				step = 4;

				break;

			case 4:
				// Student asks canteen if the chosen dish is available

				try {

					//Student walks to canteen - waiting distance
					Thread.sleep(Math.round(canteenDistances.get(canteen) * 100));
					
					if(dishes.size() == 0) {
						step = 6;

					} else {

						chosenDish = dishes.get(0);
						
						endTime = System.nanoTime() - startTime;

						goingCanteenTime = System.nanoTime();
						System.out.println("Student " + getLocalName() + " asking canteen " + canteen + " for dish " + chosenDish);
						msg = new ACLMessage(ACLMessage.REQUEST);
						msg.setContent("Eating:" + chosenDish);
						msg.addReceiver(new AID(canteen, AID.ISLOCALNAME));
						send(msg);

						step = 1;

					}
					break;

				} catch (InterruptedException e) {
					System.out.println("Error walking to canteen!");
				}


			case 5:
				// Student has finished eating and gives feedback about the canteen service to all students who haven't eaten 
				System.out.println("Student " + getLocalName() + " has finished eating!");

				// Get data
				
				ArrayList<String> facultiesNames = new ArrayList<String>(Arrays.asList("FEUP", "ESE", "FMUP", "FEP", "ISEP", "FADEUP"));
				
				double endTimeD = (double) endTime / 1_000_000_000.0;
				double timeSpentInCanteen = (System.nanoTime() - goingCanteenTime)/ 1_000_000_000.0; 
				double perc_votes = (double) votes_in_favor_stats/students.length;
				
				int isFavoriteDish = 0;
				if(favoriteDishes.containsKey(chosenDish)) {
					isFavoriteDish = 1;
				}

				JSONArray canteenInfo = (JSONArray) ((JSONObject) studentInfo.get("past-experience"))
						.get(canteen);

				double pastExperienceHeuristic = 0;
				for(int i = 0; i < canteenInfo.size(); i++) {
					pastExperienceHeuristic += (Double) canteenInfo.get(i);
				}
				pastExperienceHeuristic = pastExperienceHeuristic/canteenInfo.size();

				StringBuilder sb = new StringBuilder();
				try {
					
					sb.append(String.format("%.02f", perc_votes));
					sb.append(",");
					sb.append(canteenDistances.get(canteen));
					sb.append(",");
					sb.append(isFavoriteDish);
					sb.append(",");
					sb.append(endTimeD);
					sb.append(",");
					sb.append(groupID);
					sb.append(",");
					sb.append(timeSpentInCanteen);
					sb.append(",");
					sb.append(pastExperienceHeuristic);
					sb.append(",");
					sb.append(canteenFeedback);
					sb.append(",");
					sb.append(facultiesNames.indexOf(canteen));
					sb.append("\n");
					dataFile.write(sb.toString());
					dataFile.flush();
					
					System.out.println(String.format("%.02f", perc_votes) + "," + canteenDistances.get(canteen) + "," 
							+ isFavoriteDish + "," + endTimeD + "," + groupID + "," + timeSpentInCanteen + "," + pastExperienceHeuristic + "," 
							+ canteenFeedback + "," + canteen);

				} catch (IOException e) {
					System.err.println("Error writing to data file: " + e.getMessage());
				}

				hasEaten = true;
				registerOnEaten();

				students = null;
				searchFacultyStudentsWhoHaventEaten();

				ACLMessage canteenReview = new ACLMessage(ACLMessage.INFORM);
				canteenReview.setContent("Today Experience:" + canteen + ":" + canteenFeedback);
				for (int i = 0; i < students.length; i++) {
					if (!students[i].getName().getLocalName().equals(getLocalName())) {
						canteenReview.addReceiver(students[i].getName());
					}
				}
				send(canteenReview);

				this.getAgent().removeBehaviour(this);
				break;

			case 6:
				// It is impossible for the student to eat at any canteen
				System.out.println("Student " + getLocalName() + " couldn't eat because the canteen have no available dishes");
				this.getAgent().removeBehaviour(this);
				break;

			default:
				break;

			}

		}

		/*
		 * Proposal wan't accepted, so the canteen option will be added to the rejected proposals and every 
		 * variable needed for the cycle will be reseted. A message to all the group students will be sent,
		 * informing them of the failure of the proposal.
		 */
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
				decided = false;
				votes = 0;
				votes_in_favor = 0;
				step = 0;

			} catch (InterruptedException e) {
				System.err.println("An error occured in the discardCanteen function");
				return;
			}

		}

		/**
		 * Orders dishes available at the chosen canteen from most favorite to least favorite, according to the student
		 * preferences.
		 * 
		 * @return dishes ordered from most favorite to least favorite
		 */
		public ArrayList<String> getDishesOrderedByRanking() {

			favoriteDishes = sortFavoriteDishes();

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
