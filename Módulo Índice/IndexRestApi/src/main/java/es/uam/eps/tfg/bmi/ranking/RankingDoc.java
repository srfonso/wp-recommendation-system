package es.uam.eps.tfg.bmi.ranking;

import java.io.IOException;

public abstract class RankingDoc implements Comparable<RankingDoc>{
	public abstract double getScore();
	public abstract int getDocID();
	public abstract String getPath() throws IOException;
	
	public int compareTo(RankingDoc d) {
		int comparation = (int) Math.signum(d.getScore() - getScore());
		try {
			// En caso de empate desempatamos por nombre
			comparation = comparation == 0 ? getPath().compareTo(d.getPath()) : comparation;
		}catch (IOException e) {
			e.printStackTrace();
		}
		return comparation;
	}
}
