package informationRetrieval;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.xml.sax.SAXException;

public class InformationRetrieval {	
	//Application structures
	List<Article> articles;
	HashMap<String, List<String>> dictionary;
	
	//Filtering services
	FilteringServices fs;
	
	//Acquiring data an generating SPIMI index
	public void generateIndex() throws ParserConfigurationException, SAXException, IOException {
		//Getting all sgm files in the directory reuters
		System.out.println("");
		System.out.println("Starting reading sgm files...");
		List<String> sgmFiles = new ArrayList<String>();
		File folder = new File("reuters");
		File[] listOfFiles = folder.listFiles();
		
	    for (int i = 0; i < listOfFiles.length; i++) {
	      if (listOfFiles[i].isFile()) {
	    	  if (listOfFiles[i].getName().split("\\.")[1].toLowerCase().equals("sgm")) {
	    	    sgmFiles.add(listOfFiles[i].getPath());
	    	  }
	      }
	    }
	    
	    //Acquiring articles
	    articles = new ArrayList<Article>();
	    //Initializing filtering services
	    fs = new FilteringServices();
	    for (String sgmFile : sgmFiles) {
			//Opening file
			String fileContent = new String(Files.readAllBytes(Paths.get(sgmFile)));
			
			//Extracting everything in REUTERS tags using a regular expression
			List<String> tagValues = RegexHandler.getValuesBetween(fileContent, "<REUTERS", "</REUTERS>");
		    System.out.println(Integer.toString(tagValues.size()) + " articles found in " + sgmFile);
		    
		    //Removing carriage returns and final mark
		    //Creating article objects
		    for (String reutersContent : tagValues) {
		    	//Identifying article ID
		    	String articleID =
		    			RegexHandler.getValueBetween(reutersContent, "NEWID=\"", "\"");
		    	//Identifying article title
		    	String articleTitle = "";
		    	try {
			    	articleTitle =
			    			RegexHandler.getValueBetween(reutersContent, "<TITLE>", "</TITLE>");
		    	} catch (IndexOutOfBoundsException ibe) {
		    		//If there's even no title, ignore a malformed article
		    		continue;
		    	}
		    	//Identifying article body
		    	String articleBody = "";
		    	try {
		    	articleBody =
		    			RegexHandler.getValueBetween(reutersContent, "<BODY>", "</BODY>");
		    	} catch (IndexOutOfBoundsException ibe) {
		    		//If there's no body, just keep title
		    	}
		    	//Final article text
		    	String articleText = articleTitle + ". " + articleBody;
		    	//Removing carriage return
		    	articleText = articleText.replaceAll("\\n", " ").replaceAll("\\r", " ");
		    	//Removing last 11 chars (not useful)
		    	articleText = articleText.substring(0, articleText.length() - 11);
		    	//Creating article and tokenizing
		    	articles.add(new Article(articleText, articleID, fs));
		    }
	    }
	    
	    
	    //Generating SPIMI reverse indexing
	    System.out.println("Doing SPIMI reverse indexing...");
	    long usedMemory = 0;
	    
	    //Removing files in spimi and final index folder
	    File spimiFolder = new File("spimiBlocks");
	    File finalIndexFolder = new File("finalIndex");
	    FileUtils.cleanDirectory(spimiFolder);
	    FileUtils.cleanDirectory(finalIndexFolder);
	    
	    int fileCounter = 0;
	    //Generating first block
	    MyFileWriter mfw = new MyFileWriter("spimiBlocks/" + "block" + String.valueOf(fileCounter) + ".txt");
	    //Creating dictionary
	    dictionary = new HashMap<String, List<String>>();
	    //SPIMI Indexing
	    long tokenCount = 0;
	    long distinctTokenCount = 0;
	    for (Article article : articles) {
	    	for (String token : article.getTokens()) {
	    		//Postings list to be added
	    		//Counting tokens
	    		tokenCount++;
	    		if (!(dictionary.containsKey(token))) {
	    			//Counting tokens
	    			distinctTokenCount++;
	    			usedMemory += token.length();
	    			//Creating postingsList and adding article
	    			List<String> postingsList = new ArrayList<String>();
	    			postingsList.add(article.getID());
	    			usedMemory += article.getID().length();
	    			//Adding term to list
	    			dictionary.put(token, postingsList);
	    		}
	    		//Postings list to be recovered
	    		else {
	    			List<String> postingsList = dictionary.get(token);
	    			if (!(postingsList.contains(article.getID()))) {
	    				postingsList.add(article.getID());
	    				usedMemory += article.getID().length();
	    			}
	    		}
	    	
	    		//Checking memory size
	    		//If ran out of memory, write file and empty dictionary
	    		if (usedMemory >= AppParameters.DICTIONARY_SIZE_LIMIT) {
	    			//Writing
	    			writeDictionaryToBlock(mfw);
	    			mfw.close();
	    			//New File
	    			fileCounter++;
	    			mfw = new MyFileWriter("spimiBlocks/" + "block" + String.valueOf(fileCounter) + ".txt");
	    			//Emptying dictionary
	    		    dictionary = new HashMap<String, List<String>>();
	    		    //Emptying memory
	    		    usedMemory = 0;
	    		}
	    	}
	    }
	    
	    //Final write to last file
		writeDictionaryToBlock(mfw);
		mfw.close();	
		
		//Merging blocks
		System.out.println("Merging blocks...");
		//Moving blocks
		File srcDir = new File("spimiBlocks");
		File destDir = new File("finalIndex");
		FileUtils.copyDirectory(srcDir, destDir);
		
		//Merging blocks in a single file
		fileCounter = 0;
		listOfFiles = destDir.listFiles();
		while(listOfFiles.length > 1) {
			LineIterator it0 = FileUtils.lineIterator(listOfFiles[0], "UTF-8");
			LineIterator it1 = FileUtils.lineIterator(listOfFiles[1], "UTF-8");
			try {
				mfw = new MyFileWriter("finalIndex/" + "finalIndex" + String.valueOf(fileCounter) + ".txt");
	    		String line0 = it0.nextLine();
	    		String line1 = it1.nextLine();
			    while (it0.hasNext() || it1.hasNext()) {
		    		String firstTerm = line0.split(";")[0];
		    		String secondTerm = line1.split(";")[0];
		    		
		    		if (firstTerm.compareTo(secondTerm) > 0) {
		    			mfw.println(line1);
		    			if (it1.hasNext())
		    				line1 = it1.nextLine();
		    			//If no more lines in 1, finish with 0
		    			else {
		    				while (it0.hasNext()) {
		    					mfw.println(line0);
		    					line0 = it0.nextLine();
		    				}
		    			}
		    		}
		    		
		    		else if (firstTerm.compareTo(secondTerm) < 0) {
		    			mfw.println(line0);
		    			//If no more lines in 0, finish with 1
		    			if (it0.hasNext())
		    				line0 = it0.nextLine();
		    			else {
		    				while (it1.hasNext()) {
		    					mfw.println(line1);
		    					line1 = it1.nextLine();
		    				}
		    			}
		    		}
		    		//Case merge
		    		else {
		    			String data1 = line0.substring(firstTerm.length() + 1);
		    			String data2 = line1.substring(secondTerm.length() + 1);
		    			String mergeData = firstTerm + ";" + data1 + ";" + data2;
		    			mfw.println(mergeData);
		    			if (it0.hasNext())
		    				line0 = it0.nextLine();
		    			else {
			    			//If no more lines in 0, finish with 1
		    				while (it1.hasNext()) {
		    					line1 = it1.nextLine();
		    					mfw.println(line1);
		    				}
		    			}
		    			if (it1.hasNext())
		    				line1 = it1.nextLine();
		    			else {
			    			//If no more lines in 1, finish with 0
		    				mfw.println(line0);
		    				while (it0.hasNext()) {
		    					line0 = it0.nextLine();
		    					mfw.println(line0);
		    				}
		    			}
		    		}
			    }
			} finally {
			    it0.close();
			    it1.close();
			    mfw.close();
			}
			
			FileUtils.deleteQuietly(listOfFiles[0]);
			FileUtils.deleteQuietly(listOfFiles[1]);
		    
			fileCounter++;
			listOfFiles = destDir.listFiles();
		}
		System.out.println("Indexing finished.");
		System.out.println(String.valueOf(distinctTokenCount) + " distinct tokens acquired");
		System.out.println(String.valueOf(tokenCount) + " tokens acquired");
	}

	
	//Writing dictionary to a block file
	private void writeDictionaryToBlock(MyFileWriter mfw) {
	    //Sorting key list
	    List<String> dictionaryKeys = new ArrayList<String>(dictionary.keySet());
	    dictionaryKeys.sort(String::compareTo);
	    for (String key : dictionaryKeys) {
	    	mfw.print(key);
	    	
	    	for (String articleID : dictionary.get(key)) {
	    		mfw.print(";");
	    		mfw.print(articleID);
	    	}
	    	mfw.print("\n");
	    }
	}
	
	//Executing queries
	public void doQuery(Scanner reader) throws IOException {
		QueryHandler qh = new QueryHandler();
		List<String> outputDocuments = new ArrayList<String>();
		
		while(true) {
			System.out.print("Write your query (q for terminating): ");
			String queryText = reader.nextLine();
			
			//Exiting with q
			if (queryText.toLowerCase().equals("q")) break;
			
			else {
				//Case AND
				if (queryText.contains("AND")) {
					outputDocuments = qh.getANDMatches(queryText.split(" AND "));
				}
				//Case OR
				else if (queryText.contains("OR")) {
					outputDocuments = qh.getORMatches(queryText.split(" OR "));
				}
				else {
					outputDocuments = qh.getMatches(queryText);
				}
			}
			
			//Showing results
			System.out.print("Articles found: ");
			int documentCount = 0;
			for (String pageid : outputDocuments) {
				System.out.print(pageid);
				if (documentCount < outputDocuments.size() - 1)
					System.out.print(", ");
				documentCount++;
			}
			
			System.out.println("");
		}
		
		reader.close();
		
	}

	
	//Probabilistic query
	public void doProbabilisticQuery(Scanner reader) throws IOException {
		QueryHandler qh = new QueryHandler();
		List<String> outputDocuments = new ArrayList<String>();
		
		//Statistics
		ProbabilisticEvaluation pe = new ProbabilisticEvaluation(articles);
		System.out.println("");
		
		while(true) {
			System.out.print("Write your query (q for terminating): ");
			String queryText = reader.nextLine();
			//Removing non-alphanumerical text
			queryText = queryText.replaceAll("[^A-Za-z0-9 ]", "");
			String[] queryTerms = queryText.split(" ");
			String correctQuery = "";
			String[] correctQueryTerms;
			
			//Exiting with q
			if (queryText.toLowerCase().equals("q")) break;
			else {
				
				for (int i=0; i<queryTerms.length; i++) {
					if (queryTerms[i].length() < AppParameters.MINIMUM_TOKEN_CHARS) continue;
					if (AppParameters.STOPWORDS)
						if (fs.isInStopwords(queryTerms[i])) continue;
					if (AppParameters.CAPITALIZATION) {
						correctQuery += queryTerms[i].toLowerCase() + " ";
					}
					else {
						correctQuery += queryTerms[i] + " ";
					}
				}
				
				if (correctQuery.length() < 1) {
					System.out.println("No available terms in the query, just stopwords or short words");
					System.exit(0);
				}
				correctQuery = correctQuery.substring(0, correctQuery.length() - 1);
				correctQueryTerms = correctQuery.split(" ");
				outputDocuments = qh.getORMatches(correctQueryTerms);
			}
			
			

			
			//Obtaining results
			System.out.println("Evaluating " + outputDocuments.size() + " documents...\n");
			HashMap<String, Double> scores = new HashMap<>();
			for (String pageid : outputDocuments) {
				//Obtaining article score
				double score = pe.getDocumentScore(correctQueryTerms, pageid);
				scores.put(pageid, new Double(score));
			}
			
			//Showing results
			if (outputDocuments.size() > 20)
				System.out.println("The 20 most important articles found: ");
			else
				System.out.println("Articles found: ");
			int articleCount = 0;
			while(!scores.isEmpty() && articleCount < 20) {
				double maxScore = -10000;
				String maxPageid = "";
				
				for (String pageid : scores.keySet()) {
					if (scores.get(pageid) > maxScore) {
						maxScore = scores.get(pageid);
						maxPageid = pageid;
					}
				}
				
				System.out.println(maxPageid + "\t\tBM25 Score: " + maxScore);
				scores.remove(maxPageid);
				articleCount++;
			}
			
			System.out.println("");
		}
		
		reader.close();
	}
	
	
	//Main class constructor
	public InformationRetrieval() throws ParserConfigurationException, SAXException, IOException {
		Scanner reader = new Scanner(System.in);
		System.out.print("(1)Generate index, (2)Query, (3) Index and Probabilistic query or (other)Exit? ");
		int n = Integer.parseInt(reader.nextLine());
		
		if (n == 1) {
			reader.close();
			generateIndex();
		}
		else if (n == 2) {
			doQuery(reader);
		}
		else if (n == 3) {
			generateIndex();
			doProbabilisticQuery(reader);
		}
		else {
			reader.close();
			System.exit(0);
		}
	}
	
	
	public static void main(String[] args) {
		//Calling own class' constructor
		try {
			InformationRetrieval ir = new InformationRetrieval();
			
		} catch (ParserConfigurationException | SAXException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		loadAFINN();

	}
	
//NEW CODE
	public static void loadAFINN(){
		
		Scanner in;
		try {
			in = new Scanner(new File("AFINN/AFINN-111.txt"));
			
			while(in.hasNextLine()){
				System.out.println(in.nextLine());
			}
			
			in.close();			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			System.out.println("Exception at reading AFINN file has occured!");
			e.printStackTrace();
		}
	}
	
	
	
}
