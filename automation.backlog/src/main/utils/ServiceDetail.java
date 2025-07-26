import java.util.ArrayList;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@lombok
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructors
public class ServiceDetail {
	
	private String serviceNumber;
	private ArrayList<String> lastAnalystName;
	private ArrayList<String> daysAffected;
	
	private String arrayListToString(ArrayList<String> arr) {
		StringBuilder textToReturn = new StringBuilder("[\"");
		
		for (int i = 0; i < arr.size(); i++){
			textToReturn.append(arr.get(i));
			textToReturn.append("\"");
			
			if(i < arr.size() - 1) textToReturn.append(", \"");
			else textToReturn.append("]");
		}
		return textToReturn.toString();
	}
	
	@Override
	public String toString() {
		return "{"
				+ "serviceNumber: " + serviceNumber + ",\n"
				+ "lastAnalystName: " + this.arrayListToString(lastAnalystName) + ",\n"
				+ "daysAffected: " + this.arrayListToString(daysAffected) + ",\n"
				+ "}";
	}

}
