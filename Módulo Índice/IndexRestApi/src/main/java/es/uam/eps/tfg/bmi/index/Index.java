package es.uam.eps.tfg.bmi.index;

import java.io.IOException;
import java.util.Collection;

import org.apache.lucene.index.Terms;

import es.uam.eps.tfg.bmi.ranking.Ranking;

/**
 * Interfaz
 * Estructura para empaquetar bajo una misma clase todos los posibles indices
 * que va a tener el proyecto.
 * 
 * @author Alfonso de Paz
 *
 */
public interface Index {

	/**
	 * Devuelve la collecci�n de t�rminos
	 * 
	 * @param iField 
	 * @return Colleci�n de toods los t�rminos
	 */
	Collection<String> getAllTerms(String iField);
	
	
	
	/**
	 * Devuelve la frecuencia total del t�rmino en el campo solicitado
	 * @param term
	 * @param iField
	 * @return long - Frecuencia total del t�rmino 
	 * @throws IOException
	 */
	long getTotalFreq(String term, String iField) throws IOException;
	
	
	/**
	 * Devuelve el vector de terminos-frecuencias del documento en el campo solicitado
	 * @param docID
	 * @param iField
	 * @return vector de terminos-frecuencia del docID
	 * @throws IOException
	 */
	Terms getTermVector(int docID, String iField) throws IOException;
	
	
	/**
	 * Devuelve la ruta del documento
	 * @param docID
	 * @return vector de terminos-frecuencia del docID
	 * @throws IOException
	 */
	String getDocPath(int docID) throws IOException;
	
	
	/**
	 * Devuelve el docID a partir del path
	 * @param path
	 * @return
	 */
	int getDocID(String path);
	
	
	/**
	 * Devuelve la frecuencia de un t�rmino en el documento en el campo dado
	 * @param term
	 * @param docID
	 * @param iField
	 * @return
	 * @throws IOException
	 */
	long getTermFreq(String term, int docID, String iField) throws IOException;
	
	
	/**
	 * Devuelve el n�mero de documentos que contienen ese t�rmino en el campo solicitado
	 * @param term
	 * @param iField
	 * @return
	 * @throws IOException
	 */
	int getDocFreq(String term, String iField) throws IOException;


	/**
	 * Devuelve el titulo del docID
	 * @param docID
	 * @return
	 * @throws IOException
	 */
	public String getDocTitle(int docID) throws IOException;
	
	/**
	 * Devuelve el n�mero total de documentos en el �ndice
	 * @return int
	 */
	int getTotalDocs();
	
	
	/**
	 * 
	 * @param docID
	 * @param iField
	 * @return
	 */
	double getDocModule(int docID, String iField);
	
	
	/**
	 * Devuelve la similitud entre los doc i,j para el campo field
	 * @param i
	 * @param j
	 * @param iField
	 * @return
	 */
	double getDocSimilarity(int i, int j, String iField);
	
	
	/**
	 * Devuelve la similitud total entre los doc i,j (obtenida con cada uno de los pesos)
	 * @param i
	 * @param j
	 * @return
	 */
	double getDocSimilarity(int i, int j);
	
	
	/**
	 * Devuelve la similitud total entre los doc i,j (obtenida con cada uno de los pesos)
	 * @param i
	 * @param j
	 * @return
	 */
	double getDocSimilarity(String urli, String urlj);
	
	
	
	/**
	 * Devuelve el top de documentos similares a url
	 * @param url
	 * @param cutoff
	 * @return
	 */
	Ranking getDocSimilarityTop(String url, int cutoff);
	
	
	
	
	
}
