package es.uam.eps.tfg.bmi.ranking.heap;

import java.io.IOException;

import es.uam.eps.tfg.bmi.index.Index;
import es.uam.eps.tfg.bmi.ranking.RankingDoc;

public class PostRankingDoc extends RankingDoc {
	
	Index index;
	Double score;
	int docID;
	
	
	PostRankingDoc (Index idx, int docID, Double score){
		index = idx;
		this.docID = docID;
		this.score = score;
	}
	
	@Override
	public double getScore() {
		return this.score;
	}

	@Override
	public int getDocID() {
		return this.docID;
	}

	@Override
	public String getPath() throws IOException {
		return index.getDocPath(this.docID);
	}

}
