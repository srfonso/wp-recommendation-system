package es.uam.eps.tfg;

import es.uam.eps.tfg.ServerConfiguration.IndexField;
import es.uam.eps.tfg.bmi.index.IndexConfiguration;
import es.uam.eps.tfg.resource.LuceneManagerResource;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewBundle;

/**
 * Main para iniciar Lucene Rest Api
 *
 */
public class LuceneRestApi extends Application<ServerConfiguration>{
    
	public static void main( String[] args ) throws Exception
    {
        new LuceneRestApi().run(args);
    }
    
    @Override
    public void run(ServerConfiguration configuration, Environment environment) {
    	   	
    	// Inicializamos las variables de configuración aniadidas
    	IndexConfiguration.REST_API_IP = configuration.getRestApiIp();
    	IndexConfiguration.REST_API_PORT = configuration.getRestApiPort();
    	IndexConfiguration.REST_API_METHOD = configuration.getRestApiMethod();
    	
    	IndexConfiguration.JSON_PATH_FIELD = configuration.getJsonPathField();
    	
    	// Rellenamos el fichero de configuracion IndexConfiguration
    	for(IndexField f : configuration.getIndexFields()) {
    		IndexConfiguration.addField(f.getField(), f.getWeight());
    	}
    	
    	
    	
    	//Registramos todas las resource que vayamos a usar
    	environment.jersey().register(new LuceneManagerResource());
    }
    
    @Override 
    public void initialize(Bootstrap<ServerConfiguration> bootstrap) {
    	bootstrap.addBundle(new ViewBundle<>());
    }
}
