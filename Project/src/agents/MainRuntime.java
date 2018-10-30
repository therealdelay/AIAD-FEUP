package agents;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

public class MainRuntime {

	public static void main(String[] args) {
		Runtime rt = Runtime.instance();
		
		Profile p1 = new ProfileImpl();
		ContainerController mainContainer = rt.createMainContainer(p1);
		
		Profile p2 = new ProfileImpl();
		ContainerController container = rt.createAgentContainer(p2);
		
		
		try {
			AgentController ac1 = container.acceptNewAgent("s1", new Student());
			ac1.start();
		} catch (StaleProxyException e) {
			e.printStackTrace();
		}
		
		
		

	}

}
