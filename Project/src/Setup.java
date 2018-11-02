import jade.Boot;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Setup {

	private static ContainerController studentController, canteenController;

	public static void jadeSetup() {

		Runtime runtime = Runtime.instance();

		String[] args = { "-gui", "-local-host 127.0.0.1", "-container" };

		Boot.main(args);

		Profile profileStudent = new ProfileImpl();
		Profile profileCanteen = new ProfileImpl();

		studentController = runtime.createAgentContainer(profileStudent);
		canteenController = runtime.createAgentContainer(profileCanteen);

	}

	public static void agentsSetup(int canteenNumber, int studentNumber, String scenarioName) {

		JSONParser parser = new JSONParser();

		try {
			JSONObject obj = (JSONObject) parser.parse(new FileReader("scenarios/" + scenarioName + ".json"));
			JSONArray students = (JSONArray) obj.get("students");
			JSONArray canteens = (JSONArray) obj.get("canteens");
			JSONArray dishes = (JSONArray) obj.get("dishes");

			for(int i = 0; i < canteens.size(); i++) {
				AgentController cont;

				try {
					JSONObject canteen = (JSONObject) canteens.get(i);
					Object[] objs = {(Object) canteen, (Object) dishes};
					cont = canteenController.createNewAgent((String) canteen.get("name"), "agents.Canteen", objs);
					cont.start();
				} catch (StaleProxyException e) {
					System.out.println("Error creating canteen agents: " + e.getMessage());
					return;
				}
			}

			for (int i = 0; i < students.size(); i++) {
				AgentController cont;

				try {
					JSONObject student = (JSONObject) students.get(i);
					Object[] objs = {(Object) student};
					cont = studentController.createNewAgent((String) student.get("name"), "agents.Student", objs);
					cont.start();
				} catch (StaleProxyException e) {
					System.out.println("Error creating student agents: " + e.getMessage());
					return;
				}
			}

		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (ParseException e1) {
			e1.printStackTrace();
		}

	}

	public static void main(String[] args) {

		jadeSetup();
		agentsSetup(Integer.parseInt(args[0]), Integer.parseInt(args[1]), args[2]);

	}

}
