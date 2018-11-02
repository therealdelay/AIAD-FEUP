package agents;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

public class Canteen extends Agent {

	int quantity = 10;
	int day = 1;
	HashMap<String, MealPair<String, Integer>> dayMenu = new HashMap<>();
	ArrayList<String> meatMenus = new ArrayList<>();
	ArrayList<String> fishMenus = new ArrayList<>();
	ArrayList<String> vegMenus = new ArrayList<>();
	ArrayList<String> dietMenus = new ArrayList<>();
	ArrayList<String> lastWeekMenus = new ArrayList<>();
	JSONObject canteenInfo;
	JSONArray dishes;

	protected void setup() {
		loadJSON();
		registerOnDF();
		setMenus();
		setDayMenu(5, 5, 5, 5);
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
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
	}

	private void loadJSON() {
		Object[] args = getArguments();
		canteenInfo = (JSONObject) args[0];
		dishes = (JSONArray) args[1];
		// System.out.println("PRATOS: " + dishes.toJSONString());
	}

	private void setMenus() { // se calhar depois de ter o parser feito vai receber um array com os pratos e é
		// só fazer this.meatMnus = meatMenus, etc

		JSONArray meatDishes = (JSONArray) ((JSONObject) dishes.get(0)).get("meat");		// [0] -> meat
		JSONArray fishDishes = (JSONArray) ((JSONObject) dishes.get(1)).get("fish");		// [1] -> fish
		JSONArray vegDishes = (JSONArray) ((JSONObject) dishes.get(2)).get("veg");			// [2] -> veg
		JSONArray dietDishes = (JSONArray) ((JSONObject) dishes.get(3)).get("diet");		// [3] -> diet

		for(int i = 0; i < meatDishes.size(); i++) {
			JSONObject currDish = (JSONObject) meatDishes.get(i);
			this.meatMenus.add((String) currDish.get("dishname"));
		}

		for(int i = 0; i < fishDishes.size(); i++) {
			JSONObject currDish = (JSONObject) fishDishes.get(i);
			this.fishMenus.add((String) currDish.get("dishname"));
		}

		for(int i = 0; i < vegDishes.size(); i++) {
			JSONObject currDish = (JSONObject) vegDishes.get(i);
			this.vegMenus.add((String) currDish.get("dishname"));
		}

		for(int i = 0; i < dietDishes.size(); i++) {
			JSONObject currDish = (JSONObject) dietDishes.get(i);
			this.dietMenus.add((String) currDish.get("dishname"));
		}
	}

	private void decMeatMeals() {
		String meatMenu = dayMenu.get("meat").getMenu();
		int meatMeals = dayMenu.get("meat").getQuantity();
		dayMenu.put("meat", new MealPair(meatMenu, meatMeals - 1));
		System.out.println(dayMenu.get("meat"));
	}

	private void decFishMeals() {
		String fishMenu = dayMenu.get("fish").getMenu();
		int fishMeals = dayMenu.get("fish").getQuantity();
		dayMenu.put("fish", new MealPair(fishMenu, fishMeals - 1));
		System.out.println(dayMenu.get("fish"));
	}

	private void decVegMeals() {
		String vegMenu = dayMenu.get("veg").getMenu();
		int vegMeals = dayMenu.get("veg").getQuantity();
		dayMenu.put("veg", new MealPair(vegMenu, vegMeals - 1));
		System.out.println(dayMenu.get("veg"));
	}

	private void decDietMeals() {
		String dietMenu = dayMenu.get("diet").getMenu();
		int dietMeals = dayMenu.get("diet").getQuantity();
		dayMenu.put("diet", new MealPair(dietMenu, dietMeals - 1));
		System.out.println(dayMenu.get("diet"));
	}

	private void setDayMenu(int meatMeals, int fishMeals, int vegMeals, int dietMeals) {
		Random r = new Random();

		dayMenu.clear();
		if (day == 5) { // final da semana
			day = 1;
			lastWeekMenus.clear();
		}

		// choose meat meal
		int chosen = 0;
		while (chosen == 0) {
			String meatMenu = meatMenus.get(r.nextInt(meatMenus.size()));
			if (!lastWeekMenus.contains(meatMenu)) {
				dayMenu.put("meat", new MealPair(meatMenu, meatMeals));
				lastWeekMenus.add(meatMenu);
				chosen = 1;
			}
		}

		// choose fish meal
		chosen = 0;
		while (chosen == 0) {
			String fishMenu = fishMenus.get(r.nextInt(fishMenus.size()));
			if (!lastWeekMenus.contains(fishMenu)) {
				dayMenu.put("fish", new MealPair(fishMenu, fishMeals));
				lastWeekMenus.add(fishMenu);
				chosen = 1;
			}
		}

		// choose veg meal
		chosen = 0;
		while (chosen == 0) {
			String vegMenu = vegMenus.get(r.nextInt(vegMenus.size()));
			if (!lastWeekMenus.contains(vegMenu)) {
				dayMenu.put("veg", new MealPair(vegMenu, vegMeals));
				lastWeekMenus.add(vegMenu);
				chosen = 1;
			}
		}

		// choose diet meal
		chosen = 0;
		while (chosen == 0) {
			String dietMenu = dietMenus.get(r.nextInt(dietMenus.size()));
			if (!lastWeekMenus.contains(dietMenu)) {
				dayMenu.put("diet", new MealPair(dietMenu, dietMeals));
				lastWeekMenus.add(dietMenu);
				chosen = 1;
			}
		}
	}

	public HashMap<String, MealPair<String, Integer>> getDayMenu() {
		return dayMenu;
	}

	class ListeningBehaviour extends CyclicBehaviour {

		public void action() {
			ACLMessage msg = receive();
			if (msg != null) {
				ACLMessage reply = msg.createReply();

				if(msg.getPerformative() == ACLMessage.REQUEST) {

					reply.setPerformative(ACLMessage.INFORM);
					
					//TODO: Get distance from canteen info
					double distance = 0;
					reply.setContent("Canteen info:" + distance);
					
					try {
						
						reply.setContentObject(getCanteenDishes());
					
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						return;
					}
					
				} else {

					reply.setPerformative(ACLMessage.INFORM);
					
					if (quantity > 0) {
						reply.setContent("Cantina " + getLocalName() + " OK!");
						quantity--;
					} else
						reply.setContent("Cantina " + getLocalName() + " NO!");

				}

				send(reply);
			} else {
				block();
			}
		}


		public ArrayList<String> getCanteenDishes() {
			ArrayList<String> dishes = new ArrayList();

			//TODO: Get insto dishes all the names of the dishes of the canteen

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