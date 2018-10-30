package agents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jdk.internal.util.xml.impl.Pair;

public class Canteen extends Agent {

	int quantity = 1;
	int day = 1;
	HashMap<String, MealPair<String, Integer>> dayMenu = new HashMap<>();
	ArrayList<String> meatMenus = new ArrayList<>();
	ArrayList<String> fishMenus = new ArrayList<>();
	ArrayList<String> vegMenus = new ArrayList<>();
	ArrayList<String> dietMenus = new ArrayList<>();
	ArrayList<String> lastWeekMenus = new ArrayList<>();

	protected void setup() {
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

	private void setMenus() { //se calhar depois de ter o parser feito vai receber um array com os pratos e é só fazer this.meatMnus = meatMenus, etc
		this.meatMenus.add("Meat1");
		this.meatMenus.add("Meat2");
		this.meatMenus.add("Meat3");
		this.meatMenus.add("Meat4");
		this.meatMenus.add("Meat5");
		this.meatMenus.add("Meat6");
		this.meatMenus.add("Meat7");
		this.meatMenus.add("Meat8");
		this.meatMenus.add("Meat9");
		this.meatMenus.add("Meat10");

		this.fishMenus.add("Fish1");
		this.fishMenus.add("Fish2");
		this.fishMenus.add("Fish3");
		this.fishMenus.add("Fish4");
		this.fishMenus.add("Fish5");
		this.fishMenus.add("Fish6");
		this.fishMenus.add("Fish7");
		this.fishMenus.add("Fish8");
		this.fishMenus.add("Fish9");
		this.fishMenus.add("Fish10");

		this.vegMenus.add("Veg1");
		this.vegMenus.add("Veg2");
		this.vegMenus.add("Veg3");
		this.vegMenus.add("Veg4");
		this.vegMenus.add("Veg5");
		this.vegMenus.add("Veg6");
		this.vegMenus.add("Veg7");
		this.vegMenus.add("Veg8");
		this.vegMenus.add("Veg9");
		this.vegMenus.add("Veg10");

		this.dietMenus.add("Diet1");
		this.dietMenus.add("Diet2");
		this.dietMenus.add("Diet3");
		this.dietMenus.add("Diet4");
		this.dietMenus.add("Diet5");
		this.dietMenus.add("Diet6");
		this.dietMenus.add("Diet7");
		this.dietMenus.add("Diet8");
		this.dietMenus.add("Diet9");
		this.dietMenus.add("Diet10");
	}

	private void decMeatMeals() {
		String meatMenu = dayMenu.get("meat").getMenu();
		int meatMeals = dayMenu.get("meat").getQuantity();
		dayMenu.put("meat", new MealPair(meatMenu, meatMeals-1));
		System.out.println(dayMenu.get("meat"));
	}

	private void decFishMeals() {
		String fishMenu = dayMenu.get("fish").getMenu();
		int fishMeals = dayMenu.get("fish").getQuantity();
		dayMenu.put("fish", new MealPair(fishMenu, fishMeals-1));
		System.out.println(dayMenu.get("fish"));
	}

	private void decVegMeals() {
		String vegMenu = dayMenu.get("veg").getMenu();
		int vegMeals = dayMenu.get("veg").getQuantity();
		dayMenu.put("veg", new MealPair(vegMenu, vegMeals-1));
		System.out.println(dayMenu.get("veg"));
	}

	private void decDietMeals() {
		String dietMenu = dayMenu.get("diet").getMenu();
		int dietMeals = dayMenu.get("diet").getQuantity();
		dayMenu.put("diet", new MealPair(dietMenu, dietMeals-1));
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
				reply.setPerformative(ACLMessage.INFORM);
				if (quantity > 0) {
					reply.setContent("Cantina " + getLocalName() + " OK!");
					quantity--;
				} else
					reply.setContent("Cantina " + getLocalName() + " NO!");
				send(reply);
			} else {
				block();
			}
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