package agents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;


/**
 * 
 * Canteen class, responsible for providing the students with menu information and meals. 
 * Has a waiting queue, in which the students that have decicided in eating at the canteen will have to wait until 
 * being served their meal.
 *
 */
public class Canteen extends Agent {

	String canteenName = "";
	int quantity = 0;
	int day = 1;
	boolean nextStudent = false;
	HashMap<String, MealPair<String, Long>> dayMenu = new HashMap<>();
	HashMap<String, Double> distances = new HashMap<>();
	ArrayList<MealPair<String, Long>> meatMenus = new ArrayList<>();
	ArrayList<MealPair<String, Long>> fishMenus = new ArrayList<>();
	ArrayList<MealPair<String, Long>> vegMenus = new ArrayList<>();
	ArrayList<MealPair<String, Long>> dietMenus = new ArrayList<>();
	ArrayList<MealPair<String, Long>> lastWeekMenus = new ArrayList<>();
	LinkedList<AID> waitingQueue = new LinkedList<AID>();
	JSONObject canteenInfo;
	JSONArray dishes;
	

	protected void setup() {
		loadJSON();
		registerOnDF();
		setDistances();
		setMenus();
		setDayMenu();
		addBehaviour(new ListeningBehaviour());
		addBehaviour(new CanteenTickerBehaviour(this, 100));
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
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
	}

	private void loadJSON() {
		Object[] args = getArguments();
		canteenInfo = (JSONObject) args[0];
		dishes = (JSONArray) args[1];
	}
	
	
	/**
	 * Accesses JSON Object in JSON file and get all information about the dishes of the canteens.
	 */
	private void setMenus() {

		JSONArray meatDishes = (JSONArray) ((JSONObject) dishes.get(0)).get("meat");		// [0] -> meat
		JSONArray fishDishes = (JSONArray) ((JSONObject) dishes.get(1)).get("fish");		// [1] -> fish
		JSONArray vegDishes = (JSONArray) ((JSONObject) dishes.get(2)).get("veg");			// [2] -> veg
		JSONArray dietDishes = (JSONArray) ((JSONObject) dishes.get(3)).get("diet");		// [3] -> diet

		for(int i = 0; i < meatDishes.size(); i++) {
			JSONObject currDish = (JSONObject) meatDishes.get(i);
			this.meatMenus.add(new MealPair((String) currDish.get("dishname"), currDish.get("amount")));
		}

		for(int i = 0; i < fishDishes.size(); i++) {
			JSONObject currDish = (JSONObject) fishDishes.get(i);
			this.fishMenus.add(new MealPair((String) currDish.get("dishname"), currDish.get("amount")));
		}

		for(int i = 0; i < vegDishes.size(); i++) {
			JSONObject currDish = (JSONObject) vegDishes.get(i);
			this.vegMenus.add(new MealPair((String) currDish.get("dishname"), currDish.get("amount")));
		}

		for(int i = 0; i < dietDishes.size(); i++) {
			JSONObject currDish = (JSONObject) dietDishes.get(i);
			this.dietMenus.add(new MealPair((String) currDish.get("dishname"), currDish.get("amount")));
		}
	}
	
	
	/**
	 * Chose 4 dishes, one for each type (meat, fish, vegetarian and diet). Each dish is chosen randomly.
	 */
	private void setDayMenu() {
		Random r = new Random();

		dayMenu.clear();
		if (day == 5) { // end of week
			day = 1;
			lastWeekMenus.clear();
		}

		// choose meat meal
		int chosen = 0;
		while (chosen == 0) {
			MealPair<String, Long> meatMenu = meatMenus.get(r.nextInt(meatMenus.size()));
			if (!lastWeekMenus.contains(meatMenu)) {
				quantity += meatMenu.getQuantity();
				dayMenu.put("meat", meatMenu);
				lastWeekMenus.add(meatMenu);
				chosen = 1;
			}
		}

		// choose fish meal
		chosen = 0;
		while (chosen == 0) {
			MealPair<String, Long> fishMenu = fishMenus.get(r.nextInt(fishMenus.size()));
			if (!lastWeekMenus.contains(fishMenu)) {
				quantity += fishMenu.getQuantity();
				dayMenu.put("fish", fishMenu);
				lastWeekMenus.add(fishMenu);
				chosen = 1;
			}
		}

		// choose veg meal
		chosen = 0;
		while (chosen == 0) {
			MealPair<String, Long> vegMenu = vegMenus.get(r.nextInt(vegMenus.size()));
			if (!lastWeekMenus.contains(vegMenu)) {
				quantity += vegMenu.getQuantity();
				dayMenu.put("veg", vegMenu);
				lastWeekMenus.add(vegMenu);
				chosen = 1;
			}
		}

		// choose diet meal
		chosen = 0;
		while (chosen == 0) {
			MealPair<String, Long> dietMenu = dietMenus.get(r.nextInt(dietMenus.size()));
			if (!lastWeekMenus.contains(dietMenu)) {
				quantity += dietMenu.getQuantity();
				dayMenu.put("diet", dietMenu);
				lastWeekMenus.add(dietMenu);
				chosen = 1;
			}
		}

	}

	public HashMap<String, MealPair<String, Long>> getDayMenu() {
		return dayMenu;
	}
	
	/**
	 * Get from JSON object the distances to every other canteen.
	 */
	public void setDistances() {
		canteenName = (String) canteenInfo.get("name");

		JSONObject distancesObj = (JSONObject) canteenInfo.get("distances");
		for(Object key : distancesObj.keySet()) {
			this.distances.put((String) key, (Double) distancesObj.get(key));
		}

	}
	
	/**
	 * Generates a random service value of the canteen, which corresponds to the quality of service and
	 * overral happiness  of the students with the meal and service of the canteen.
	 * 
	 * bad service -> value [0.1, 0.3]
	 * mediocre -> [0.4, 0.7]
	 * good-> [0.8, 1.0]
	 * 
	 * @return serviceValue of canteen
	 * 
	 */
	public double generateService() {
		JSONObject serviceInfo = (JSONObject) canteenInfo.get("service");
		
		double serviceValue = 0.0;
		
		double bad = (Double) serviceInfo.get("bad");
		double mediocre = (Double) serviceInfo.get("mediocre");
		double good = (Double) serviceInfo.get("good");
		
		Random r = new Random();
		double serviceType = 0.0 + (1.0 - 0.0) * r.nextDouble();
		
		if (serviceType >= 0.0 && serviceType < bad) {
			
			serviceValue = 0.1 + (0.3 - 0.1) * r.nextDouble();
			
		} else if (serviceType >= bad && serviceType < mediocre) {
			
			serviceValue = 0.4 + (0.7 - 0.4) * r.nextDouble();
			
		} else if (serviceType >= mediocre && serviceType <= good) {
			
			serviceValue = 0.7 + (1.0 - 0.7) * r.nextDouble();
			
		}
		return (double) Math.round(serviceValue * 10) / 10;
	}
	
	
	/**
	 * 
	 * CanteenTickerBehaviour is a behaviour responsible for removing the first element of the waiting queue
	 * after a specific time period. This only happens when the queue isn't empty and this behaviour is cyclic.
	 *
	 */
	class CanteenTickerBehaviour extends TickerBehaviour {

		int ticks;

		public CanteenTickerBehaviour(Agent a, long period) {
			super(a, period);
			ticks = 0;
		}

		@Override
		protected void onTick() {
			ticks++;

			if(ticks == 10) {
				if(waitingQueue.size() > 0) {
					ACLMessage reply = new ACLMessage(ACLMessage.AGREE);
					reply.setPerformative(ACLMessage.AGREE);
					reply.addReceiver(waitingQueue.element());

					reply.setContent("Cantina " + getLocalName() + " service: " + generateService());
					send(reply);
					waitingQueue.removeFirst();
					
					System.out.print("\n" + canteenName + " Queue: ");
					for(AID a : waitingQueue) {
						System.out.print(a.getLocalName() + ", ");
					}
					System.out.print("\n");
				}
				ticks = 0;
			}

		}

	}
	
	/**
	 * 
	 * ListenningBehaviour is a behavior responsible for three actions:
	 * 	- Answering request of student for canteen info: returns distance to the student faculty and the dayly menu of the canteen
	 *  - Answering request of student to eat a specific meal: if there is available numbers of the chosen dish,
	 *  	the canteen will put the student in the waiting queue. If there aren't available dishes, the canteen will give a 
	 *  	negative answer to the student 
	 *  - Answering if there are enough total dishes (of every dish) for the number of students in the student group.
	 *
	 */
	class ListeningBehaviour extends CyclicBehaviour {

		public void action() {
			ACLMessage msg = receive();

			if (msg != null) {
				ACLMessage reply = msg.createReply();

				if(msg.getPerformative() == ACLMessage.REQUEST && msg.getContent().contains("Canteen Info")) {

					// Request from student to send distance and menu dishes					
					int index = msg.getContent().indexOf(":");
					if(index == -1) {
						return;
					}	
					String studentFaculty = msg.getContent().split(":")[1].trim();
 
					double distance = distances.get(studentFaculty);

					reply.setPerformative(ACLMessage.INFORM);
					String answer = getCanteenDishes() + distance;

					reply.setContent(answer);
					send(reply);

				} else if(msg.getPerformative() == ACLMessage.REQUEST && msg.getContent().contains("Eating")){
					
					//Student request to eat specific fish
					String requestedDish = msg.getContent().split(":")[1].trim();
					boolean accepted = false;
					

					for (Map.Entry<String, MealPair<String, Long>> entry : dayMenu.entrySet()) {

						MealPair<String, Long> obj = entry.getValue();
						if(requestedDish.equals(obj.getMenu()) && obj.getQuantity() > 0) {

							waitingQueue.add(msg.getSender());
							System.out.print("\n" + canteenName + " Queue: ");
							for(AID a : waitingQueue) {
								System.out.print(a.getLocalName() + ", ");
							}
							System.out.print("\n\n");
							
							dayMenu.put(entry.getKey(), new MealPair<String, Long>(obj.getMenu(), obj.getQuantity() - 1));
							quantity--;
							accepted = true;

						}

					}
					
					if(!accepted) {
						// Denies student request
						System.out.println("Canteen " + getLocalName() + " denies request for " + requestedDish + " from student " + msg.getSender().getLocalName());
						reply.setPerformative(ACLMessage.CANCEL);
						reply.setContent("Cantina " + getLocalName() + " NO!");		
						send(reply);
					}




				} else if(msg.getPerformative() == ACLMessage.REQUEST) {

					// Request from student to check if there are enough dishes				

					reply.setPerformative(ACLMessage.INFORM);
					reply.setContent(""+ quantity);
					send(reply);


				} else {
					block();
				}
			}
		}

		public String getCanteenDishes() {
			String dishes = "";

			for (Map.Entry<String, MealPair<String, Long>> entry : dayMenu.entrySet()) {

				MealPair<String, Long> obj = entry.getValue();
				dishes += "" + obj.getMenu() + ":";

			}

			return dishes;
		}

	}

	class MealPair<F, S> {
		private F menu;
		private S quantity;

		public MealPair(F menu, S quantity) {
			this.menu = menu;
			this.quantity = quantity;
		}

		public F getMenu() {
			return menu;
		}

		public S getQuantity() {
			return quantity;
		}

		public String toString() {
			return menu.toString() + " : " + quantity.toString();
		}
	}
}