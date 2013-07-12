package org.jchern.http;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for configuring the properties of an immutable HttpRequest 
 * before instantiating it.
 */
public class HttpRequestFactory {
	private String method, version, url;
	private Map<String, String> fields; // keyed by lower-case fieldnames
	// I don't care about the order b/c we only need to consume requests (aside from some rare fields, which are allowed to occur multiple times) 

	// Prefer not to let clients instantiate factories manually, in case they one day become complicated and heavy.
	private HttpRequestFactory() {
		method = version = url = "";
		fields = new HashMap<String, String>();
	}
	
	public void setMethod(final String method) {
		this.method = method;
	}
	
	public void setVersion(final String version) {
		this.version = version;
	}
	
	public void setUrl(final String url) {
		this.url = url;
	}
	
	public void setHeaderField(final String key, final String value) {
		fields.put(key.toLowerCase(), value);
	}
	
	public HttpRequest getRequest() {
		final Map<String, String> headersCopy = new HashMap<String, String>();
		headersCopy.putAll(fields); // defensive copy, in case this Factory's fields change
		return new HttpRequest(method, version, url, headersCopy);
	}
	
	/**
	 * Returns an instance of the factory.
	 */
	public static HttpRequestFactory instance() {
		return new HttpRequestFactory();
	}
}


