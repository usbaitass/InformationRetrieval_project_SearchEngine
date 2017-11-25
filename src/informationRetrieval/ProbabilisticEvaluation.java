package informationRetrieval;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class ProbabilisticEvaluation {
	private List<Article> articles;
	private HashMap<String, Article> articlesHashMap;
	private double averageArticleLength;
	private int totalArticleCount;
	
	ProbabilisticEvaluation(List<Article> articles) throws IOException {
		this.articles = articles;
		
		//Creating also the HashMap of articles
		articlesHashMap = new HashMap<String, Article>();
		
		for (Article article : articles) {
			articlesHashMap.put(article.getID(), article);
		}
		
		//Calculating average article length in tokens
		double lengthSum = 0;
		for (Article article : articles) {
			lengthSum += article.getArticleLength();
		}
		
		averageArticleLength = lengthSum / articles.size();
		
		//Acquiring total article count
		totalArticleCount = articles.size();
		
	}
	
	
	public double getAverageArticleLength() {
		return averageArticleLength;
	}


	public double getDocumentScore(String[] queryTerms, String pageid) {
		Article currentArticle = articlesHashMap.get(pageid);
		int currentArticleLength = currentArticle.getArticleLength();
		double documentScore = 0;
	
		//Sum for each term in the query
		for (int i=0; i<queryTerms.length; i++) {
			String currentTerm = queryTerms[i];
			int articlesCountWithTerm = getArticlesCountWithTerm(currentTerm);
			double currentTermFrequency = currentArticle.getTermFrequency(currentTerm);
			if (currentTermFrequency < 0.0000000001) continue;  
					
			double firstValue = (double)totalArticleCount / (double)articlesCountWithTerm;
			double secondValue = (AppParameters.K_PARAMETER + 1) * currentTermFrequency;
			double thirdValue = AppParameters.K_PARAMETER * ((1 - AppParameters.B_PARAMETER)
					+ AppParameters.B_PARAMETER * (currentArticleLength / averageArticleLength)) + currentTermFrequency;
			
			double termScore = Math.log(firstValue * (secondValue / thirdValue));
			documentScore += termScore;
		}
		
		return documentScore;
	}


	private int getArticlesCountWithTerm(String currentTerm) {
		int articlesNumberWithTerm = 0;
		for (Article article : articles) {
			if (article.isTermInArticle(currentTerm)) articlesNumberWithTerm++;
		}
		//System.out.println("Found " + articlesNumberWithTerm + " articles with the term " + currentTerm + " in them");
		return  articlesNumberWithTerm;
	}
}
