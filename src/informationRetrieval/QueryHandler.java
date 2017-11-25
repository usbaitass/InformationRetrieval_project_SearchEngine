package informationRetrieval;

import java.io.*;
import java.util.*;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

public class QueryHandler {
	File dictionary;
	FilteringServices fs;
	int querySentiment = 0;
	
	
	public QueryHandler() throws IOException {
		//Opening dictionary
		File dictionaryFolder = new File("finalIndex");
		dictionary = dictionaryFolder.listFiles()[0];
		fs = new FilteringServices();
	}
	
	//Returning an iterator for reading dictionary lines
	private LineIterator getDictionaryLineIterator() throws IOException {
		LineIterator it = FileUtils.lineIterator(dictionary, "UTF-8");
		return it;
	}

	//Getting term from a line
	private String getTermFromDictionaryLine(String line) {
		return line.split(";")[0];
	}
	
	//Getting page IDs from a line
	private List<String> getPageIDsFromDictionaryLine(String line) {
		ArrayList<String> pageids = new ArrayList<String>();
		String[] lineSplit = line.split(";");
		
		for(int i=2; i < lineSplit.length; i++) {//changed here from i=1 to i=2
			pageids.add(lineSplit[i]);
		}
		return pageids;
	}

	
	//Simple query for single match
	public List<String> getMatches(String queryText) throws IOException {
		ArrayList<String> matches = new ArrayList<String>();
		
		//Lowercase if it is necessary
		if (AppParameters.CAPITALIZATION) {
			queryText = queryText.toLowerCase();
		}
		
		LineIterator it = getDictionaryLineIterator();
		while(it.hasNext()) {
			String currentLine = it.nextLine();
			if (getTermFromDictionaryLine(currentLine).equals(queryText)) {
				matches.addAll(getPageIDsFromDictionaryLine(currentLine));
				break;
			}
		}
		
		return orderNumerical(matches);
	}
	
	
	//OR query
	public List<String> getORMatches(String[] queryTexts) throws IOException {
		ArrayList<String> queryTextsList = new ArrayList<String>();
		HashMap<String, ArrayList<String>> results = new HashMap<String, ArrayList<String>>();
		
		for (int i=0; i<queryTexts.length; i++) {
			if (AppParameters.CAPITALIZATION) {
				queryTextsList.add(queryTexts[i].toLowerCase());
			}
			else queryTextsList.add(queryTexts[i]);
		}
		
		//Retrieving all matches
		ArrayList<String> currentResults;
		LineIterator it = getDictionaryLineIterator();
		int matchCount = 0;
		while(it.hasNext()) {
			String currentLine = it.nextLine();
			String currentTerm = getTermFromDictionaryLine(currentLine);
			if (queryTextsList.contains(currentTerm)) {
				currentResults = new ArrayList<String>();
				currentResults.addAll(getPageIDsFromDictionaryLine(currentLine));
				results.put(currentTerm, currentResults);
				querySentiment += Integer.parseInt(currentLine.split(";")[1]);
				
				//If all terms are retrieved, end loop
				matchCount++;
				if (matchCount == queryTextsList.size()) break;
			}
		}
		
		ArrayList<String> finalResult = new ArrayList<String>();
		
		for (String key : results.keySet()) {
			finalResult.addAll(results.get(key));
		}
		
		//Removing duplicates by copying ArrayList into a Set
		Set<String> hs = new HashSet<>();
		hs.addAll(finalResult);
		finalResult.clear();
		finalResult.addAll(hs);
		
		return orderNumerical(finalResult);
	}
	
	
	//AND query
	//Can not assume that pageids are order, as they are not
	public List<String> getANDMatches(String[] queryTexts) throws IOException {
		ArrayList<String> queryTextsList = new ArrayList<String>();
		HashMap<String, ArrayList<String>> results = new HashMap<String, ArrayList<String>>();
		
		for (int i=0; i<queryTexts.length; i++) {
			if (AppParameters.CAPITALIZATION) {
				queryTextsList.add(queryTexts[i].toLowerCase());
			}
			else queryTextsList.add(queryTexts[i]);
		}
		
		//Retrieving all matches
		ArrayList<String> currentResults;
		LineIterator it = getDictionaryLineIterator();
		int matchCount = 0;
		while(it.hasNext()) {
			String currentLine = it.nextLine();
			String currentTerm = getTermFromDictionaryLine(currentLine);
			if (queryTextsList.contains(currentTerm)) {
				currentResults = new ArrayList<String>();
				currentResults.addAll(getPageIDsFromDictionaryLine(currentLine));
				results.put(currentTerm, currentResults);
				
				//If all terms are retrieved, end loop
				matchCount++;
				if (matchCount == queryTextsList.size()) break;
			}
		}
		
		//Being pageids not ordered, just doing intersection between
		//arrays, as there's not a more efficient way
		ArrayList<String> finalResult = new ArrayList<String>();
		
		boolean first = true;
		for (String key : results.keySet()) {
			if (first) {
				finalResult.addAll(results.get(key));
				first = false;
			}
			else {
				finalResult.retainAll(results.get(key));
			}
		}
		
		return orderNumerical(finalResult);
	}
	
	
	//Order a numerical list
	private ArrayList<String> orderNumerical(ArrayList<String> numbersList) {
		ArrayList<Integer> integerList = new ArrayList<Integer>();
		
		for (String s : numbersList) {
			integerList.add(Integer.parseInt(s));
		}
		
		integerList.sort(Integer::compareTo);
		
		ArrayList<String> newList = new ArrayList<String>();
		
		for (Integer i : integerList) {
			newList.add(i.toString());
		}
		
		return newList;
	}
}
