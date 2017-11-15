package informationRetrieval;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

public class FilteringServices {
	//List of stopwords
	List<String> Stopwords;
	
	public FilteringServices() throws IOException {
		//Loading stopwords
		if (AppParameters.STOPWORDS) {
			Stopwords = new ArrayList<String>();
			File stopwordsFile = new File("stopwords/stopwords.txt");
			LineIterator it0 = FileUtils.lineIterator(stopwordsFile, "UTF-8");
			try {
				while(it0.hasNext()) Stopwords.add(it0.nextLine());
			}
			finally {
				it0.close();
			}
		}
	}

	
	//Returns true if a string is considered alphabetical
	public static final boolean isAlphabetical(String text) {		
		//We consider an alphabetical token a token with at least
		//An ALPHABETICAL_RATIO of chars in letters
		int letterCount = 0;
		for (char c : text.toCharArray()) {
			//If the string contains a space it is alphabetical
			//(e.g. name of organizations with more words)
			if (Character.isLetter(c) || Character.isSpaceChar(c)) letterCount++;
		}
		
		if (((double)(letterCount) / text.length()) >= AppParameters.ALPHABETICAL_RATIO)
			return true;
		else {
			return false;
		}
	}
	
	
	
	//Returns true if a String has only special chars
	public static boolean hasOnlySpecialChars(String text) {
		Pattern p = Pattern.compile("[^a-z0-9 ]", Pattern.CASE_INSENSITIVE);
		for (int i=0; i<text.length(); i++) {
			Matcher m = p.matcher(text.substring(i, i+1));
			if (!(m.find())) return false;
		}
		return true;
	}
	
	
	//Returns true if a string is not between stopwords
	public boolean isInStopwords(String text) {
		return Stopwords.contains(text.toLowerCase());
	}
}
