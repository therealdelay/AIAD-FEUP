import jade.Boot;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

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

	public static void agentsSetup(int canteenNumber, int studentNumber) {

		for (int i = 0; i < canteenNumber; i++) {
			AgentController cont;

			try {
				cont = canteenController.createNewAgent("canteen -" + i, "agents.Canteen", null);
				cont.start();
			} catch (StaleProxyException e) {
				System.out.println("Error creating canteen agents: " + e.getMessage());
				return;
			}
		}

		for (int i = 0; i < studentNumber; i++) {
			AgentController cont;

			try {
				cont = studentController.createNewAgent("student -" + i, "agents.Student", null);
				cont.start();
			} catch (StaleProxyException e) {
				System.out.println("Error creating student agents: " + e.getMessage());
				return;
			}
		}

	}

	public static void main(String[] args) {

		jadeSetup();
		agentsSetup(Integer.parseInt(args[0]), Integer.parseInt(args[1]));

	}

}
