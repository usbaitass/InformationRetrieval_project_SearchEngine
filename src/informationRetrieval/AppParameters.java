package informationRetrieval;

public final class AppParameters {
	//Dictionary size limit in chars
	public static final long DICTIONARY_SIZE_LIMIT = 250000;
	
	//Applying or not Named Entity Recognition
	public static final boolean NER = false;
	//Filtering parameters
	public static final int MINIMUM_TOKEN_CHARS = 3;
	public static final boolean ONLY_ALPHABETICAL = true;
	public static final boolean CAPITALIZATION = true;
	public static final boolean STOPWORDS = true;
	
	//The ratio of letters in a String to be considered alphabetical
	public static final double ALPHABETICAL_RATIO = 0.9;
	
	//Probabilistic parameters
	public static final double B_PARAMETER = 0.5;
	public static final double K_PARAMETER = 0.5;
}
