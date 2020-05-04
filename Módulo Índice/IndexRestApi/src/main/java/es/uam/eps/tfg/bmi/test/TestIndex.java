package es.uam.eps.tfg.bmi.test;

import java.io.IOException;

import es.uam.eps.tfg.bmi.index.Index;
import es.uam.eps.tfg.bmi.index.IndexBuilder;
import es.uam.eps.tfg.bmi.index.lucene.LuceneIndex;
import es.uam.eps.tfg.bmi.index.lucene.LuceneIndexBuilder;
import es.uam.eps.tfg.bmi.ranking.Ranking;
import es.uam.eps.tfg.bmi.ranking.RankingDoc;

public class TestIndex {

	public static void main(String[] args) throws IOException {
    	System.out.println("=================================================================\n");
        System.out.println("Testing indices");
        
        // Creamos la carpeta si no existe la borramos
        //clear("api/posts/");
        IndexBuilder posts = new LuceneIndexBuilder();
    	posts.build("127.0.0.1", "8000", "api/posts/", "index/postsArreglada");
    	
    	System.out.println("=================================================================\n");
        System.out.println("END");
        
    	System.out.println("=================================================================\n");
        System.out.println("Loading indices");
        
        // Creamos la carpeta si no existe la borramos
        //clear("api/posts/");
        Index index = new LuceneIndex("index/postsArreglada");
        
    	System.out.println("=================================================================\n");
        System.out.println("END");
        
        
        Ranking r = index.getDocSimilarityTop("/wordpress/matematicas/logica/condicionales-y-probabilidades/", 5);
        int i = 1;
        for(RankingDoc rd : r) {
        	System.out.println(i+".-" + rd.getPath() + " - " + rd.getScore());
        	i++;
        }
        
        /*for(String f : IndexField.INDEXFIELDS) {
        	System.out.println(index.getAllTerms(f));
        	for(int i = 0; i<index.getTotalDocs(); i++) {
        		for(int j = 0; j < index.getTotalDocs(); j++) {
        			System.out.println(i+ "-"+ j + ":" + index.getDocSimilarity(i, j, IndexField.JSON_CONTENT_FIELD));
        		}
        		//System.out.println("docID:" + i + "  mod: " + index.getDocModule(i, f));
        	}
        }*/
    	
    	
    	System.out.println(index.getDocPath(3)+ index.getDocTitle(3));
	}

}
