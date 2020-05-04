package es.uam.eps.tfg.bmi.index;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Clase que act�a de libreria donde se encuentran las constantes 
 * de todos los campos disponibles para usar.
 * 
 * El indice solo creara los campos que se encuentren en la lista INDEXFIELDS que
 * tendrán asociados unos pesos en FIELDWEIGHT
 * 
 * @author Alfonso de Paz
 *
 */
public class IndexConfiguration {
	
	// Variables de configuracion para la conexion con la Rest Api donde se encuentran los posts (CoreRestApi)
	public static String REST_API_IP = "";
	public static String REST_API_PORT = "";
	public static String REST_API_METHOD = "";
	
	// ** Campos necesarios de los JSON recibidos (los de la REST API) **
	public static String JSON_PATH_FIELD = "path";
	
	
	// ** Campos del JSON que va a usar nuestro indice **
	public static List<String> INDEXFIELDS;
	static { //Inicializamos. Para aniadir y eliminar campos y no repetir codigo
		INDEXFIELDS = new ArrayList<>(); 
	}

	
	// ** Campos del JSON con sus respectivos pesos **
	public static Map<String, Double> FIELDWEIGHT;
	static { // Inicializamos el Map statico
		FIELDWEIGHT = new HashMap<>();
    }
	
	// Peso que pone en el archivo de configuracion, se usa para sacar el total y normalizar (p.e.j renormalizar al aniadir un nuevo campo)
	private static double totalWeight = 0.0;
	private static Map<String, Double> fieldweight_not_normalized;
	static { // Inicializamos el Map statico
		fieldweight_not_normalized= new HashMap<>();
	}
	
	
	// Hacemos privado el constructor para que no se pueda instanciar
	private IndexConfiguration() {}
	
	
	/**
	 * Aniade un campo con su peso a la configuración del Indice
	 * @param name
	 * @param weight
	 */
	public static void addField(String name, double weight) {
		
		// Anidimos el nuevo campo 
		IndexConfiguration.INDEXFIELDS.add(name);
		IndexConfiguration.fieldweight_not_normalized.put(name, weight);
		
		// Aumentamos el contador
		IndexConfiguration.totalWeight += weight;
		
		// Recalculamos el resto de pesos para que esten normalizados (0-1)
		IndexConfiguration.normalizeWeight();
	}
	
	
	/**
	 * Normaliza los pesos 
	 */
	private static void normalizeWeight() {
		
		double normalizeWeight = 0.0;
		
		// Recorremos cada campo
		for(String field : IndexConfiguration.INDEXFIELDS) {
			normalizeWeight = (IndexConfiguration.fieldweight_not_normalized.get(field))/(IndexConfiguration.totalWeight);
			IndexConfiguration.FIELDWEIGHT.put(field, normalizeWeight);
		}
	}
}
