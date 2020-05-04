package es.uam.eps.tfg.bmi.index.lucene;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.FSDirectory;

import es.uam.eps.tfg.bmi.index.Index;
import es.uam.eps.tfg.bmi.index.IndexConfiguration;
import es.uam.eps.tfg.bmi.ranking.Ranking;
import es.uam.eps.tfg.bmi.ranking.heap.HeapRanking;

public class LuceneIndex implements Index {

	private String indexPath;
	private IndexReader indexReader;
	
	private Map<String, Collection<String>> coleccion; // Colecci�n de t�rminos para cada campo
	private Map<String, List<Double>> docsMod; // Modulo para cada campo
	
	// Similitudes entre documentos para cada campo
	private Map<String, Map<Integer, Map<Integer , Double>>> docsSim; // field -> i,j -> sim
	private Map<String, Integer> path_to_id;
	
	/**
	 * Llamada r�pida para cargar todo el �ndice (con m�dulos y similitudes)
	 * @param indexPath
	 * @throws IOException 
	 */
	public LuceneIndex(String indexPath) throws IOException {		
		this(indexPath,true);	
	}

	// 
	/**
	 * Para contruir el indice de forma personalizada (con/sin cargar modulos ni similitudes), gracias al
	 * par�metro quickstart.
	 * 
	 * @param indexPath
	 * @param quickstart - Elegir si cargar solo lo necesario (true) o cargar todo (false)
	 * @throws IOException 
	 */
	public LuceneIndex(String indexPath, Boolean quickstart) throws IOException {
		// Inicializamos las componentes
		this.indexPath = indexPath;
		this.coleccion = new HashMap<>();
		this.path_to_id = new HashMap<>();
		
		this.indexReader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));

		// Rellenamos la colecci�n para cada campo
		for (String field : IndexConfiguration.INDEXFIELDS) {
			TermsEnum terms = MultiFields.getFields(this.indexReader).terms(field).iterator();

			this.coleccion.put(field, new ArrayList<>());
			while (terms.next() != null) {
				coleccion.get(field).add((terms.term().utf8ToString()));
			}
		}
			
		
		if(quickstart) {
			this.loadPathToId();
			this.loadModules();
			this.loadSimilarity();
		}
		
	}
	
	/**
	 * Carga el archivo de traducciones path-docid
	 * @return
	 */
	private boolean loadPathToId() {
		// Creamos un map con las traducciones
		this.path_to_id = new HashMap<>();
		File file = new File(this.indexPath + "/pathtoid.id");
		if(!file.exists()) {
			return false;
		}
		
		try {
			List<String> lines = Files.readAllLines(file.toPath());
			for(String l : lines) {
				// Cada linea contiene - path + " " + docID
				String[] elementos = l.split(" ");
				this.path_to_id.put(elementos[0], Integer.valueOf(elementos[1]));
			}
			
			
		} catch(IOException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	
	
	/**
	 * Carga el archivo de m�dulos
	 * @return boolean
	 */
	private boolean loadModules() {
		// Creamos una lista con los m�dulos calculados offline
		this.docsMod = new HashMap<>();
		File modPath = new File(this.indexPath + "/modulo.mod");
		if(!modPath.exists()) {
			return false;
		}
		
		try {
			List<String> lines = Files.readAllLines(modPath.toPath());
			for(String l : lines) {
				// Cada linea contiene - docID + " " + mod_field1 + " " + mod_field2 + ...
				String[] elementos = l.split(" ");
				
				// Nos saltamos el docID pues corresponde con el �ndice en la lista
				int i = 1;
				for(String field : IndexConfiguration.INDEXFIELDS) {
					this.docsMod.putIfAbsent(field, new ArrayList<>());
					// Guardamos para ese campo el modulo de elemento[0] (docID)
					this.docsMod.get(field).add(Double.valueOf(elementos[i]));
					i++;
				}
			}
			
			
		} catch(IOException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	/**
	 * Carga los archivos de similitudes
	 * @return
	 */
	private boolean loadSimilarity() {
		// Creamos el Map con las similitudes calculados offline
		this.docsSim = new HashMap<>();
		
		// Para cada campo (recorremos todos los campos)
		for(String field : IndexConfiguration.INDEXFIELDS) {
			this.docsSim.putIfAbsent(field, new HashMap<>());
			
			// Cargamos el archivo
			File simPath = new File(this.indexPath + "/sim_"+ field +".sim");
			if(!simPath.exists()) {
				return false;
			}
			
			try {
				List<String> lines = Files.readAllLines(simPath.toPath());
				for(String l : lines) {
					// Cada linea contiene - docID + " " + docID + " " + sim
					String[] elementos = l.split(" ");
					
					// Inicializamos el HashMap de ese docID para el campo field
					this.docsSim.get(field).putIfAbsent(Integer.valueOf(elementos[0]), new HashMap<>());
					
					// Insertamos los datos
					this.docsSim.get(field).get(Integer.valueOf(elementos[0])).put(Integer.valueOf(elementos[1]), Double.valueOf(elementos[2]));
				}
			} catch(IOException e) {
				e.printStackTrace();
				return false;
			}
		}
		
		return true;
	}
	
	@Override
	public Collection<String> getAllTerms(String field) {
		return this.coleccion.get(field);
	}

	@Override
	public long getTotalFreq(String term, String iField) throws IOException {
		Term t = new Term(iField, term);
		return this.indexReader.totalTermFreq(t);
	}

	@Override
	public Terms getTermVector(int docID, String iField) throws IOException {
		return this.indexReader.getTermVector(docID, iField);
	}

	@Override
	public String getDocPath(int docID) throws IOException {
		return this.indexReader.document(docID).get("path");
	}
	
	
	/**
	 * Devuelve el path normalizado para evitar diferencias entre path iguales.
	 * 
	 * Se debe usar en el builder cuando se crea el archivo path_to_id
	 * 
	 * El path debe empezar y acabar por /
	 * 
	 * @param path
	 * @return
	 */
	public static String getNormalizedPath(String path) {
		path = path.replaceAll("\\s+",""); // Quita todos los espacios
		path = ((path.startsWith("/") ? path : "/" + path));
		path = ((path.endsWith("/")) ? path : path + "/");
		
		return path;
	}
	
	@Override
	public int getDocID(String path) {
		if(path == null) {
			return -1;
		}
		
		if(this.path_to_id.containsKey(LuceneIndex.getNormalizedPath(path))) {
			return this.path_to_id.get(LuceneIndex.getNormalizedPath(path));
		}
			
		return -1;
	}
	

	@Override
	public long getTermFreq(String term, int docID, String iField) throws IOException {
		Terms ts =  this.getTermVector(docID, iField);
		TermsEnum iterator = ts.iterator();
		
		while(iterator.next() != null) {
			if(iterator.term().utf8ToString() == term) {
				return iterator.docFreq();
			}
		}
		
		throw new IOException("Term not found");
	}

	@Override
	public int getDocFreq(String term, String iField) throws IOException {
		Term t = new Term("iField", term);
		return this.indexReader.docFreq(t);
	}
	
	
	@Override
	public String getDocTitle(int docID) throws IOException {
		Document d = this.indexReader.document(docID);
		return d.get("title");
	}

	@Override
	public int getTotalDocs() {
		return this.indexReader.numDocs();
	}

	@Override
	public double getDocModule(int docID, String iField) {
		List<Double> list = docsMod.get(iField);
		if(list != null && list.size() > docID) {
			return list.get(docID);
		}
		return 0;
	}

	@Override
	public double getDocSimilarity(int i, int j, String iField) {
		Map<Integer, Map<Integer, Double>> field_par = this.docsSim.get(iField);
		
		if(i == j) {
			return 1;
		}
		
		// Busamos en una tabla sim�trica
		Double sim = getParSim(field_par, i,j);
		if(sim == 0) {
			return getParSim(field_par, j,i);
		}
		
		return sim;
	}
	
	private double getParSim(Map<Integer, Map<Integer, Double>> map, int i, int j) {
		
		if(i == j) {
			return 1;
		}
		
		if(map.containsKey(i)) {
			if(map.get(i).containsKey(j)) {
				return map.get(i).get(j);
			}
		}
		
		return 0;
		
	}

	
	@Override
	public double getDocSimilarity(String urli, String urlj) {
		
		if(urli == null || urlj == null) {
			return 0;
		}
		
		
		return this.getDocSimilarity(this.getDocID(urli), this.getDocID(urlj));
	}
	
	
	@Override
	public double getDocSimilarity(int i, int j) {
		
		double totalsim = 0;
		
		if(i == j) {
			return 1;
		}
		
		
		// La similitud de cada campo por su peso
		for(String field : IndexConfiguration.INDEXFIELDS) {
			totalsim += this.getDocSimilarity(i, j, field) * IndexConfiguration.FIELDWEIGHT.get(field);
		}
		
		return totalsim;
	}

	@Override
	public Ranking getDocSimilarityTop(String url, int cutoff) {
		
		double sim;
		int docID;
		HeapRanking ranking;
		
		if(url == null || cutoff <= 0) {
			return null;
		}
		
		docID = this.getDocID(url);
		if(docID == -1) {
			return new HeapRanking(this, cutoff);
		}
		
		// Vamos creando el Ranking recorriendo todos los documentos y obteniendo su similitud
		ranking = new HeapRanking(this, cutoff);
		
		// Cogemos todos los ids y le quitamos el docID
		Set<Integer> doc_pool = IntStream.rangeClosed(0, this.indexReader.numDocs() - 1).boxed().collect(Collectors.toSet());
		doc_pool.remove(docID);
		
		for(int docj : doc_pool) {
			sim = this.getDocSimilarity(docID, docj);
			ranking.add(docj, sim);
		}
		
		return ranking;
	}
	
	
	

}
