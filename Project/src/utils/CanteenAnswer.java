package utils;
import java.io.Serializable;
import java.util.ArrayList;

public class CanteenAnswer implements Serializable {
	
	Double distance;
	ArrayList<String> dishes;
	
	public CanteenAnswer(Double distance, ArrayList<String> dishes) {
		this.distance = distance;
		this.dishes = dishes;
	}

	public Double getDistance() {
		return distance;
	}

	public void setDistance(Double distance) {
		this.distance = distance;
	}

	public ArrayList<String> getDishes() {
		return dishes;
	}

	public void setDishes(ArrayList<String> dishes) {
		this.dishes = dishes;
	}
	
	
	
}
