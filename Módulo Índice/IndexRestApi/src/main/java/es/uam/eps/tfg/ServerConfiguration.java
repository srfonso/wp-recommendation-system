package es.uam.eps.tfg;


import java.util.List;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.dropwizard.Configuration;

public class ServerConfiguration extends Configuration {
	@NotEmpty
	private String restApiIp;
	
	@NotEmpty
	private String restApiPort;
	
	@NotEmpty
	private String restApiMethod;
	
	@NotEmpty
	private String jsonPathField;
	
	@NotNull
	private List<IndexField> indexFields;
	
	
	
	@JsonProperty
	public String getRestApiIp() {
		return this.restApiIp;
	}
	
	@JsonProperty
	public String getRestApiPort() {
		return this.restApiPort;
	}
	
	@JsonProperty
	public String getRestApiMethod() {
		return this.restApiMethod;
	}
	
	@JsonProperty
	public String getJsonPathField() {
		return this.jsonPathField;
	}
	
	@JsonProperty
	public List<IndexField> getIndexFields() {
		return this.indexFields;
	}
	
	
	
	
	@JsonProperty
	public void setRestApiIp(String restApiIp) {
		this.restApiIp = restApiIp;
	}
	
	@JsonProperty
	public void setRestApiPort(String restApiPort) {
		this.restApiPort = restApiPort;
	}
	
	@JsonProperty
	public void setRestApiMethod(String restApiMethod) {
		this.restApiMethod = restApiMethod;
	}
	
	@JsonProperty
	public void setJsonPathField(String jsonPathField) {
		this.jsonPathField = jsonPathField;
	}
	
	@JsonProperty
	public void setIndexFields(List<IndexField> indexfields) {
		this.indexFields = indexfields;
	}
	
	
	
	public static class IndexField {
		private String field;
		private double weight;
		
		public IndexField() {
			
		}
		
		public String getField() {
			return field;
		}
		
		public Double getWeight() {
			return weight;
		}
	}
	
}
