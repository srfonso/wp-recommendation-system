package es.uam.eps.tfg.bmi.ranking.heap;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

import es.uam.eps.tfg.bmi.index.Index;
import es.uam.eps.tfg.bmi.ranking.Ranking;
import es.uam.eps.tfg.bmi.ranking.RankingDoc;

/**
 * Ranking con un heap implementado
 * @author Alfonso de Paz
 *
 */
public class HeapRanking implements Ranking{

	Index index;
	int cutoff;
	PriorityQueue<RankingDoc> heap;
	
	public HeapRanking(Index index, int cutoff) {
		this.index = index;
		this.cutoff = cutoff;
		// El heap vendrá gestionado por el RankingDoc
		this.heap = new PriorityQueue<RankingDoc>(Comparator.reverseOrder());
	}

	public void add(int docID, double score) {
		RankingDoc doc = new PostRankingDoc(this.index, docID, score);
		
		if(heap.size() < this.cutoff) // Si el heap no está lleno
			heap.offer(doc);		  // Metemos el doc
		else {
			PostRankingDoc aux = (PostRankingDoc) this.heap.peek();
			if(aux.getScore() < score) { // Si el heap está lleno pero el score es mayor se sustituye
				heap.remove();
				heap.offer(doc);
			}
		}
	}
	
	
	@Override
	public Iterator<RankingDoc> iterator() {
		RankingDoc[] aux = this.heap.toArray(new RankingDoc[0]);
        List<RankingDoc> list = Arrays.asList(aux);
        list.sort(RankingDoc::compareTo); //Ordenamos la colección
		
		return list.iterator();
	}

	@Override
	public int size() {
		return this.heap.size();
	}

}
