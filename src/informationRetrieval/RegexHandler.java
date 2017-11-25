package informationRetrieval;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexHandler {
	
	//Returns a list of string between two identifiers
	public static ArrayList<String> getValuesBetween(String text, String start, String end) {
		ArrayList<String> results = new ArrayList<String>(); 
		Pattern articlesRegex = Pattern.compile(start + "([\\s\\S]*?)" + end);
		Matcher matcher = articlesRegex.matcher(text);
	    while (matcher.find()) {
	        results.add(matcher.group(1));
	    }
	    return results;
	}
	
	public static String getValueBetween(String text, String start, String end) {
		ArrayList<String> results = getValuesBetween(text, start, end);
		return results.get(0);
	}
}
