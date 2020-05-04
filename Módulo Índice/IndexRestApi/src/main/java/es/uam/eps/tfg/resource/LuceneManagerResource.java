package es.uam.eps.tfg.resource;

import java.io.IOException;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import es.uam.eps.tfg.bmi.index.Index;
import es.uam.eps.tfg.bmi.index.IndexBuilder;
import es.uam.eps.tfg.bmi.index.IndexConfiguration;
import es.uam.eps.tfg.bmi.index.lucene.LuceneIndex;
import es.uam.eps.tfg.bmi.index.lucene.LuceneIndexBuilder;
import es.uam.eps.tfg.bmi.ranking.Ranking;
import es.uam.eps.tfg.bmi.ranking.RankingDoc;

/**
 * 
 * @author Alfonso de Paz
 *
 */
@Path("/index/")
public class LuceneManagerResource {

	private Index ilucene; 	
	private String indexPath = "index/postIndex";
	
	public LuceneManagerResource() {}
	
	
	@GET
	@Path("/create")
	public Response createIndex() {
		
		// Creamos el constructor
		IndexBuilder ibuilder = new LuceneIndexBuilder();
		
		try {
			//System.out.println(LuceneRestApi.REST_API_IP + ":" + LuceneRestApi.REST_API_PORT + "/" + LuceneRestApi.API_METHOD);

			ibuilder.build(IndexConfiguration.REST_API_IP, IndexConfiguration.REST_API_PORT, IndexConfiguration.REST_API_METHOD, indexPath);
			
			// Leemos el índice 
			this.ilucene = new LuceneIndex(indexPath);
			
			
		} catch (IOException e) {
			this.ilucene = null;
			e.printStackTrace();
			//Devolvemos un código de error 5XX
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
		}
		
		return Response.status(Response.Status.CREATED).build();
	}
	
	@GET
	@Path("/read")
	public Response readIndex() {
		
			
		// Leemos el índice 
		try {
			this.ilucene = new LuceneIndex(indexPath);
		} catch (IOException e) {
			this.ilucene = null;
			e.printStackTrace();
			// Devolvemos el error 5XX
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
		}		
		
		return Response.status(Response.Status.CREATED).build();
	} 
	
	
	
	@POST
	@Path("/sim")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response getSim(@FormParam("urli") String urli, @FormParam("urlj") String urlj) {
		
		double sim = 0;
		String json;
		
	    if(this.ilucene == null) {
	        return Response.status(Response.Status.NOT_FOUND).entity("Index not found").build();
	    }
	    
		
		//Comprobamos si son correctos los argumentos
		if((urli == null) || (urlj == null)) {
			return Response.status(Response.Status.BAD_REQUEST).build();
		}
		
		System.out.println(urli+" "+urlj);
		
		sim = this.ilucene.getDocSimilarity(urli, urlj);
		
		json = "{\r\n" + 
				"  \"doci\": \"" + LuceneIndex.getNormalizedPath(urli) + "\",\r\n" + 
				"  \"docj\": \"" + LuceneIndex.getNormalizedPath(urlj) + "\",\r\n" + 
				"  \"sim\": " + sim + "\r\n" + 
				"}";
		
		
		return Response.ok(json, MediaType.APPLICATION_JSON).build();
	}

	

	@POST
	@Path("/sim_top")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response getTopSim(@FormParam("url") String url, @FormParam("cutoff") int cutoff) throws IOException {
		
		Ranking ranking;
		String json;
		int i;
		
	    if(this.ilucene == null) {
	        return Response.status(Response.Status.NOT_FOUND).entity("Index not found").build();
	    }
	    
		
		//Comprobamos si son correctos los argumentos
		if((url == null) || (cutoff <= 0)) {
			return Response.status(Response.Status.BAD_REQUEST).build();
		}
		
		json = "[";
		ranking = this.ilucene.getDocSimilarityTop(url, cutoff);
		i = 1;
		for(RankingDoc rd : ranking) {
			json += "{\r\n" + 
					"  \"pos\": " + i + ",\r\n" + 
					"  \"doc\": \"" + rd.getPath() + "\"\r\n" + 
					//"  \"sim\": " + rd.getScore() + "\r\n" +    // Ocultamos esta información, no es necesaria
					"},";
			i++;
		}
		
		if(json.endsWith(",")) {
			json = json.substring(0, json.length() - 1);
		}
		json+="]";
		
		return Response.ok(json, MediaType.APPLICATION_JSON).build();
	}
}
