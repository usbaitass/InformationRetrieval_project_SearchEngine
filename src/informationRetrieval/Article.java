package informationRetrieval;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.TokenizerFactory;

public class Article {
	private static final String serializedClassifier = "lib/stanford/classifiers/english.all.3class.distsim.crf.ser.gz";
	private static final AbstractSequenceClassifier classifier = CRFClassifier.getClassifierNoExceptions(serializedClassifier);
	private static final List<String> stanfordClasses = Arrays.asList("ORGANIZATION", "PERSON", "LOCATION");
	private String text;
	private String ID;
	
	private List<String> tokens;
	private FilteringServices fs;
	
	private int sentimentValue;
	
	
	//Article constructor
	public Article(String text, String ID, FilteringServices fs) {
		this.text = text;
		this.ID = ID;
		this.fs = fs;
		
		
		//Call tokenization
		this.tokens = textTokenize(text);
		
		//Call NER (Named Entity Recognition)
		if (AppParameters.NER) textNer();
		
		//ApplyFilters
		applyFilters();
		
		this.sentimentValue = evaluateSentimentArticle();
	}
	
	
	//Tokenizing article
	private List<String> textTokenize(String text) {
	    //Tokenizing using the Stanford tokenizer library
		TokenizerFactory<Word> tf = PTBTokenizer.factory();
		List<Word> tokensWords = tf.getTokenizer(new StringReader(text)).tokenize();
	    
		ArrayList<String> myTokens = new ArrayList<String>();
	    
		for (Word tokenWord : tokensWords) {
			//Removing semicolon as a sensible char
			//(It is needed for data storing in CSV format)
			String tokenString = tokenWord.toString().replace(";", "");
			
			//If filters are passed, token is added finally
			myTokens.add(tokenString);
		}
		
		return myTokens;
	}
	
	
	//Using Stanford Named Entity Recognition to keep together nouns
	//that are entities in the tokenzation (e.g. San Francisco)
	private void textNer() {
		String classifiedText = classifier.classifyWithInlineXML(getText());
		
		//Putting together organizations, persons and locations that
		//have been divided by tokenization
		for (String stanfordClass : stanfordClasses) {
			ArrayList<String> matches =
					RegexHandler.getValuesBetween(classifiedText, "<" + stanfordClass + ">", "</" + stanfordClass + ">");
			
		    for (String matchingString : matches) {
		        String[] splitString = matchingString.split(" ");
		        
		        //Going in tokenization to put together entity words
		        if (splitString.length > 1) {
			        List<String> newTokens = new ArrayList<String>();
			        int jumpNext = 0;
		        	for (int i=0; i<tokens.size(); i++) {
		        		if (jumpNext > 0) {
		        			jumpNext--;
		        			continue;
		        		}
		        		boolean foundSequence = false;
		        		for (int j=0; j<splitString.length; j++) {
		        			try {
			        			if (tokens.get(i+j).equals(splitString[j]))
			        				foundSequence = true;
			        			else {
			        				foundSequence = false;
			        				break;
			        			}
		        			} catch (IndexOutOfBoundsException ibe) {
		        				foundSequence = false;
		        				break;
		        			}
		        		}
		        		
		        		if (foundSequence) {
		        			newTokens.add(matchingString);
		        			jumpNext = splitString.length - 1;
		        		} else {
		        			newTokens.add(tokens.get(i));
		        		}
		        	}
		        	this.tokens = newTokens;
		        }
		    }
		}
	}
	
	
	//Applying filters to tokens
	private void applyFilters() {
		ArrayList<String> newTokens = new ArrayList<String>();
		
		for (String tokenString : tokens) {
			//Applying filters
			//short strings removal
			if (tokenString.length() < AppParameters.MINIMUM_TOKEN_CHARS) continue;
			
			//Only special chars removal
			if (FilteringServices.hasOnlySpecialChars(tokenString))
				continue;
	
			//Only alphabetical filter
			if (AppParameters.ONLY_ALPHABETICAL) {
				if (!(FilteringServices.isAlphabetical(tokenString)))
					continue;
			}
			
			//Capitalization
			if (AppParameters.CAPITALIZATION) {
				tokenString = tokenString.toLowerCase();
			}
			
			//Stopwords
			if (AppParameters.STOPWORDS) {
				if (fs.isInStopwords(tokenString)) continue;
			}
			
			//If all filters passed, add token to list
			newTokens.add(tokenString);
		}
		
		this.tokens = newTokens;
	}

	
	//Getter and setter methods
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	
	public String getID() {
		return ID;
	}
	public void setID(String iD) {
		ID = iD;
	}

	public String toString() {
		return text;
	}

	public List<String> getTokens() {
		return tokens;
	}
	
	public int getArticleLength() {
		return tokens.size();
	}
	
	public boolean isTermInArticle(String term) {
		return tokens.contains(term);
	}
	
	public double getTermFrequency(String term) {
		int instancesCount = 0;
		for (String token : tokens) {
			if (token.equals(term)) instancesCount++;
		}
		
		double termFrequency = (double)instancesCount / (double)tokens.size();
		//System.out.println("Term " + term + " found in article " + this.getID() + " with frequency " + termFrequency);
		return termFrequency;
	}
	
	public int evaluateSentimentArticle(){
		int sum = 0;
		for(int i=0; i<tokens.size(); i++){
			if (AppParameters.mapAFINN.containsKey(tokens.get(i))) {
				//System.out.println("HERE: "+tokens.get(i)+" "+AppParameters.mapAFINN.get(tokens.get(i)));
				sum += Integer.parseInt(AppParameters.mapAFINN.get(tokens.get(i)));
			}
		}
		return sum;
	}
	
	public int getSentimentValue(){
		return sentimentValue;
	}
}
