import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ActivityChecker {

    // Define a date format
    private final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yy");

    public Map<LocalDate, String> parseActivities(String text) {
        Map<LocalDate, String> activities = new TreeMap<>();
        
        // Regular expression to match the date and name
        String regex = "(\\d{2}/\\d{2}/\\d{2})\\s\\d{2}:\\d{2}:\\d{2}\\s.*?\\((.*?)\\):";
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);
        
        while (matcher.find()) {
            LocalDate date = LocalDate.parse(matcher.group(1), DATE_FORMAT);
            String name = matcher.group(2).trim();
            activities.put(date, name);
        }
        return activities;
    }

    public Map<LocalDate, String> checkMissingDates(Map<LocalDate, String> activities, String sDate) {
        LocalDate previousDate = null;
        boolean startChecking = false;
        LocalDate startDate = LocalDate.parse(sDate, DATE_FORMAT);
        Map<LocalDate, String> missingDate = new TreeMap<>();
        
        for (LocalDate date : activities.keySet()) {
        	if (date.isEqual(startDate) || date.isAfter(startDate)) {
                startChecking = true;
            }
        	
        	if (startChecking) {
                if (previousDate != null) {
                    LocalDate tempDate = previousDate.plusDays(1);
                    while (tempDate.isBefore(date)) {
                        missingDate.put(tempDate, activities.get(previousDate));
                        tempDate = tempDate.plusDays(1);
                    }
                }
            }
            previousDate = date;
        }
        return missingDate;
    }
}