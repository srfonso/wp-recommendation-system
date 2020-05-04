package es.uam.eps.tfg.bmi.index;

import java.io.IOException;

/**
 * Interfaz
 * Estrcutura para empaquetar bajo un mismo tipo todas las posibles clases que va a 
 * tener un proyecto sobre creación de índices. 
 * 
 * @author Alfonso de Paz
 *
 */
public interface IndexBuilder {
	
	/**
	 * Método que obtiene solicita documentos a partir de una petición GET a la REST API recibida como argumento
	 *  y crea un índice que almacenará en la ruta especificada en indexPath
	 *  
	 *  Al final, se calculará para tenerlo offline los módulos de los documentos y las similitudes entre estos
	 *  
	 * @param rest_api_ip Ip de la Rest API
	 * @param rest_api_port Puerto de la Rest API
	 * @param api_method Método de la Rest API
	 * @param indexPath Ruta donde guardará el índice
	 * @throws IOException
	 */
	public void build(String rest_api_ip, String rest_api_port, String api_method, String indexPath) throws IOException;
}
