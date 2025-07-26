import java.time.LocalDate;
import java.util.ArrayList;

import com.google.common.base.Objects;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.Objects;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ActivitiesValidator {
	
	//private String textToCheck;
	private String initDayToCheck;
	private String endDayToCheck;
	private ArrayList<String> daysAffected;
	private ArrayList<String> affectedAnalysts;
	
	private void normalizeDates() {
		initDayToCheck = LocalDate.parse(initDayToCheck).toString();
		if(Objects.isNull(endDayToCheck)) endDayToCheck = LocalDate.now().minusDays(1).toString();
	}
	
	public void findDayToAffect(String textToCheck){
		// String textToCheck
		textToCheck.indexOf(textToCheck, 0, 0);
	}
	
	
}
