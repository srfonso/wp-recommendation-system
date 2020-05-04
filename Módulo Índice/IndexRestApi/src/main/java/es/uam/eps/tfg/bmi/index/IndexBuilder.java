package es.uam.eps.tfg.bmi.index;

import java.io.IOException;

/**
 * Interfaz
 * Estrcutura para empaquetar bajo un mismo tipo todas las posibles clases que va a 
 * tener un proyecto sobre creaci�n de �ndices. 
 * 
 * @author Alfonso de Paz
 *
 */
public interface IndexBuilder {
	
	/**
	 * M�todo que obtiene solicita documentos a partir de una petici�n GET a la REST API recibida como argumento
	 *  y crea un �ndice que almacenar� en la ruta especificada en indexPath
	 *  
	 *  Al final, se calcular� para tenerlo offline los m�dulos de los documentos y las similitudes entre estos
	 *  
	 * @param rest_api_ip Ip de la Rest API
	 * @param rest_api_port Puerto de la Rest API
	 * @param api_method M�todo de la Rest API
	 * @param indexPath Ruta donde guardar� el �ndice
	 * @throws IOException
	 */
	public void build(String rest_api_ip, String rest_api_port, String api_method, String indexPath) throws IOException;
}
