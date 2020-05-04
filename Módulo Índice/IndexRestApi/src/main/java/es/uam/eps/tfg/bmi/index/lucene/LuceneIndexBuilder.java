package es.uam.eps.tfg.bmi.index.lucene;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.json.JSONArray;
import org.json.JSONObject;

import es.uam.eps.tfg.bmi.index.Index;
import es.uam.eps.tfg.bmi.index.IndexBuilder;
import es.uam.eps.tfg.bmi.index.IndexConfiguration;


/**
 * 
 * @author Alfonso de Paz
 *
 */
public class LuceneIndexBuilder implements IndexBuilder {

	private IndexWriter indexBuilder;
	private String indexPath;
	
	// Diccionario de palabra totales en cada campo
	private Map<String, Set<String>> diccionary; // [Field -> diccionario de ese campo]
	
	// Almacenamos el modulo de un campo de cada documento
	private Map<Integer,Map<String, Double>> doc_mod; // [DOCID -> [FIELD -> MOD]]

	// Guardamos el vector de frecuencias de cada documento
	private Map<Integer, Map<String, Map<String, Integer>>> doc_freq_vector; // [DOCID -> [FIELD -> [Term - freq]]]
	
	
	@Override
	public void build(String rest_api_ip, String rest_api_port, String api_method, String indexPath) throws IOException {
		
		// Comprobamos que hemos recibido toda la informaci�n necesaria
		if( rest_api_ip == null || rest_api_port == null || api_method == null|| indexPath == null) {
			throw new IOException();
		}
		
		this.indexPath = indexPath;
		
		// Creamos el directorio donde almacenar el indice, los m�dulos y similitudes
		Directory directory = FSDirectory.open(Paths.get(indexPath));
			
		// Configuraci�n del IndexWriter
		Analyzer analyzer = new StandardAnalyzer();
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		config.setOpenMode(OpenMode.CREATE);

		this.indexBuilder = new IndexWriter(directory,config);
		
		// Ahora leemos los documentos y los insertamos en el �ndice con los campos correspondientes
		this.restAPIConnection(rest_api_ip, rest_api_port, api_method);
		
		// Cerramos 
		this.indexBuilder.close();
		
		//Calculamos cosas offline
		this.calculateModule(indexPath);
		this.calculateSimilarity(indexPath);
		
		/*for(String field : IndexField.INDEXFIELDS) {
			System.out.println("--"+ field +"--");
			System.out.println(this.diccionary.get(field));
			System.out.println("----");
		}*/
		
	}
	
	
	
	/**
	 * Recibibe el path del �ndice, lo lee (usando LuceneIndex) y comienza a calcular el m�dulo de
	 * cada documento.
	 * 
	 * @param indexPath
	 * @throws IOException
	 */
	private void calculateModule(String indexPath) throws IOException {
		LuceneIndex iLucene = new LuceneIndex(indexPath);
		BufferedWriter br = null;
		
		double module; // Modulo de cada campo de cada documento
		String str_line = "";
		
		// Inicializamos los map que vamos a usar
		this.diccionary = new HashMap<>();
		this.doc_mod = new HashMap<>();
		this.doc_freq_vector = new HashMap<>();
		
		try {
			// Creamos el buffer para ir escribiendo el archivo de m�dulos
			br = new BufferedWriter(new FileWriter(new File(indexPath + "/modulo.mod")));
			
			// Recorremos cada documento y calculamos su m�dulo
			for(int docID = 0; docID < iLucene.getTotalDocs(); docID++) {
				
				// Inicializamos el map para ese docID
				this.doc_mod.putIfAbsent(docID, new HashMap<>());
				this.doc_freq_vector.putIfAbsent(docID, new HashMap<>());
				
				//Al comenzar el bucle reiniciamos
				str_line = "" + docID;
				
				// Calculamos el m�dulo de cada campo necesario del documento
				for(String field : IndexConfiguration.INDEXFIELDS) {
					str_line += " ";
					
					// Inicializamos el map para ese field
					this.doc_freq_vector.get(docID).putIfAbsent(field, new HashMap<>());
					
					module = this.calcFieldModule(iLucene, docID, field);
					str_line += module;
					
					// Metemos para ese docID y ese field el modulo
					this.doc_mod.get(docID).put(field, module);
				}			
				
				// Calculamos finalmente el modulo (faltaba la raiz del sumatorio) y lo escribimos
				br.write(str_line + "\n");
			}
			
			// Al finalizar cerramos el fichero del m�dulo
			br.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Calculamos el m�dulo para el campo dado
	 * 
	 * @param iLucene
	 * @param docID
	 * @param field
	 * @return
	 * @throws IOException
	 */
	private double calcFieldModule(Index iLucene, int docID, String field) throws IOException {
		
		double tf = 0, idf = 0, preTotalModule = 0, totalModule = 0;		
		
		if(this.doc_freq_vector == null || this.diccionary == null) {
			throw new IOException("El map de vector de frecuencias no est� inicializado.");
		}
		
		
		// Si es la primera vez lo inicializamos
		this.diccionary.putIfAbsent(field, new HashSet<>());
		this.doc_freq_vector.get(docID).putIfAbsent(field, new HashMap<>());
		
		// Para cada documento recorremos cada t�rmino
		TermsEnum iterator = iLucene.getTermVector(docID, field).iterator();
		while (iterator.next() != null) {
			//El iterador apunta al termino
			
			// Rellenamos el diccionario para el campo (si ya estaba se ignora)
			this.diccionary.get(field).add(iterator.term().utf8ToString());
			
			// Vamos rellenando el vector de frecuencias 
			this.doc_freq_vector.get(docID).get(field).put(iterator.term().utf8ToString(), iterator.docFreq());
			
			// Calculamos el tf (probabilidad del t�rmino en el documento)
			if(iterator.docFreq() == 0) {
				tf = 0;
			}else {
				tf = 1 + (Math.log(iterator.docFreq()) / Math.log(2));
			}
			
			// Calculamos idx = log(totaldocs + 1 / docswithT + 0.5)
			// iLucene.getDocFreq(iterator.term().utf8ToString()) el String del termino que apunta el iterador
			idf = Math.log((iLucene.getTotalDocs() + 1.0) / (iLucene.getDocFreq(iterator.term().utf8ToString(), field) + 0.5));
			
			preTotalModule += Math.pow(tf*idf, 2);
		}
		
		// Finalmente hacemos la raiz
		totalModule = Math.sqrt(preTotalModule);
		
		return totalModule;
	}
	
	
	/**
	 * Calcula la similitud entre documentos, escribe en un fichero en el �ndice
	 * 
	 * Requiere haber llamado previamente a calculateModule(...) para tener todos los maps del diccionario listos para usar
	 * 
	 * @param indexPath - Para almacenar las similitudes en la misma ruta que el �ndice
	 * @throws IOException
	 */
	private void calculateSimilarity(String indexPath) throws IOException{
		LuceneIndex iLucene = new LuceneIndex(indexPath);
		double sim = 0;
		
		// Mini map para almacenar field - buffer y hacerlo mas flexible a cambios en los campos
		Map<String, BufferedWriter> br_map = new HashMap<>();
		
		// Antes de calcular la similitud hay que llamar a this.calculateModule(...)
		if(this.diccionary == null || this.doc_mod == null || this.doc_freq_vector == null) {
			throw new IOException("Los maps del �ndice deben estar creados previamente. (Ejecutar calculateModule antes)");
		}
		
		
		try {
			
			// Abrimos un documento de similitud por cada campo
			for(String field : IndexConfiguration.INDEXFIELDS) {
				br_map.put(field, new BufferedWriter(new FileWriter(new File(indexPath + "/sim_"+ field +".sim"))));
			}
			
			//Creamos el pool de docID para hacer las parejas necesarias sin repetir  (sim(i,j) = sim(j,i))
			Set<Integer> doc_pool = IntStream.rangeClosed(0, this.doc_mod.size() - 1).boxed().collect(Collectors.toSet());
			Set<Integer> doc_pool_aux = IntStream.rangeClosed(0, this.doc_mod.size() - 1).boxed().collect(Collectors.toSet());
			
			//Recorremos los documentos para hacer parejas
			for(int i : doc_pool) {
				
				
				/* Quitamos el doc i de la lista para no volver a cogerlo
				   La similitud es sim�trica, nos ahorramos la mitad de c�lculos */
				doc_pool_aux.remove(i);
				
				for(int j : doc_pool_aux) {
					
					//Calculamos la similitud para cada campo
					for(String field : IndexConfiguration.INDEXFIELDS) {
						sim = this.calcFieldSimilarity(iLucene, i, j, field);
						
						// La guardamos en el documento usando el buffer correcto
						br_map.get(field).write(i + " " + j + " " + sim + "\n");
					}
				}
			}
			
			// Finalmente cerramos los buffers
			for(String field: IndexConfiguration.INDEXFIELDS) {
				br_map.get(field).close();
			}
		
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private double calcFieldSimilarity(Index iLucene, int i, int j, String field) throws IOException {
		
		double p_es = 0.0, sim = 0.0, tf_i, tf_j, idf;
		
		//Cogemos el vector de frecuencias de cada documento
		Map<String, Integer> vector_i = this.doc_freq_vector.get(i).get(field);
		Map<String, Integer> vector_j = this.doc_freq_vector.get(j).get(field); 
		
		//Cogemos el modulo de cada documento
		
		// ** Aplicamos la formula (p_escalar(i,j)/modi�modj) **
		
		/* 
		 * Producto escalar:
		 * 
		 * Recorremos todo el diccionario multiplicando las frecuencias de esa palabra y haciendo el sumatorio de todo
		 * 
		 * Si un termino no se encuentra en algun vector, esa multiplicaci�n es 0
		 */
		for(String term : this.diccionary.get(field)) {
			
			tf_i = ( (vector_i.get(term) == null) ? 0 : (1 + (Math.log(vector_i.get(term)) / Math.log(2))));
			tf_j = ( (vector_j.get(term) == null) ? 0 : (1 + (Math.log(vector_j.get(term)) / Math.log(2))));
			
			idf = Math.log((iLucene.getTotalDocs() + 1.0) / (iLucene.getDocFreq(term, field) + 0.5));
			
			p_es += (tf_i * tf_j)*idf*idf;
			
			//p_es += ((vector_i.get(term) == null) ? 0 : vector_i.get(term)) * ((vector_j.get(term) == null) ? 0 : vector_j.get(term));
		}
		sim = p_es / (this.doc_mod.get(i).get(field)*this.doc_mod.get(j).get(field));
		
		return sim;
	}
	
	
	/**
	 * 
	 * @param rest_api_ip
	 * @param rest_api_port
	 * @param api_method
	 * @throws IOException
	 */
	private void restAPIConnection(String rest_api_ip, String rest_api_port, String api_method) throws IOException {
		
		//Abrimos una connexi�n
    	CloseableHttpClient httpClient = HttpClients.createDefault();
    	
    	try {
    		
    		// Enviamos petici�n GET para obtener los docs
    		this.sendGET(httpClient, rest_api_ip, rest_api_port, api_method);
    		
    	} finally {
    		httpClient.close();
    	}
	}
	
	
	
	/**
	 * 
	 * @param httpClient
	 * @param rest_api_ip
	 * @param rest_api_port
	 * @param api_method
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	private void sendGET(CloseableHttpClient httpClient, String rest_api_ip, String rest_api_port, String api_method) throws ClientProtocolException, IOException{
        
		// HashMap con el contenido de cada campo para un documento
		Map<String, String> doc_fields = new HashMap<>();
		
		// Creamos la URL
		String url = "http://" + rest_api_ip + ":" + rest_api_port;
		url += ((api_method.startsWith("/") ? api_method : "/" + api_method));
		url = ((url.endsWith("/")) ? url : url + "/");
		
		// Abrimos el archivo de traducciones path - docid
		BufferedWriter br = new BufferedWriter(new FileWriter(new File(this.indexPath + "/pathtoid.id")));
		
		
		HttpGet request = new HttpGet(url);

        // Hacemos la petici�n HTTP
        try (CloseableHttpResponse response = httpClient.execute(request)) {

        	// Obtenemos el contenido
            HttpEntity entity = response.getEntity();
            if (entity == null) {
            	return;                
            }
       
            // Devolvemos el contenido en String y lo transformamos a JSON
            String result = EntityUtils.toString(entity);
        	JSONArray jsonArray = new JSONArray(result);
        	
        	//Obtenemos cada entrada 
        	for (int i = 0; i < jsonArray.length(); i++) {
        		// Transformamos la entrada en JSON object
        	    JSONObject object = jsonArray.getJSONObject(i);
        	    
        	    // Cogemos los campos necesarios
        	    
        	    // Campo que obtiene la informaci�n de la ruta del documento
        	    String path = object.getString(IndexConfiguration.JSON_PATH_FIELD); 
        	    
        	    // Campos con el contenido
        	    for(String field : IndexConfiguration.INDEXFIELDS) {
        	    	doc_fields.put(field, object.getString(field));
        	    }
        	    
        	    // Indexamos el documento
        	    this.indexDoc(br, path, doc_fields);
        	    
        	    // Limpiamos el map para el siguiente documento
        	    doc_fields.clear();
        	    
        	    
        	}
        }
        
        br.close();
	}

	

	/**
	 * 
	 * Inserta el documento en el �ndice, el documento se inserta en el �ndice en
	 * diferentes campos.
	 * @param br 
	 * 
	 * @param path
	 * @param doc_fields - Contiene al documento separado por campos
	 * @throws IOException
	 */
	private void indexDoc(BufferedWriter br, String path, Map<String, String> doc_fields) throws IOException {
		Document doc = new Document();
		
		// Normalizamos el path
		path = LuceneIndex.getNormalizedPath(path);
		
		// Campo especial de la ruta del documento
		doc.add(new TextField(IndexConfiguration.JSON_PATH_FIELD, path, Field.Store.YES));
		
		// Opciones de los campos
		FieldType type = new FieldType();
		type.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
		type.setStoreTermVectors(true);
		
		/*FieldType typeCat = new FieldType();
		typeCat.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
		typeCat.setStoreTermVectors(true);*/
		
		// Creamos los campos y le aniadimos el contenido al documento
		for(String field : IndexConfiguration.INDEXFIELDS) {
			doc.add(new Field(field, doc_fields.get(field), type));
		}
		
		/*doc.add(new Field(IndexField.JSON_TITLE_FIELD, title, type));
		doc.add(new Field(IndexField.JSON_CONTENT_FIELD, content, type));
		doc.add(new Field(IndexField.JSON_CATEGORY_FIELD, categories, typeCat));*/
		
		// Aniadimos el documento
		this.indexBuilder.addDocument(doc);
		
		// Una vez insertado escribimos la traducción
		if(br != null) {
			br.write(path + " " + Integer.toString(this.indexBuilder.getDocStats().maxDoc - 1) + "\n");
		}
		
	}

}
