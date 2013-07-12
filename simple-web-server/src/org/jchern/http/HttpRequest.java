package org.jchern.http;
// TODO: make this an interface, so we can impl other ways
import java.util.Map;

/**
 * Contains the fields of a parsed HTTP request, 
 * such as method (GET, POST, HEAD, DELETE, etc)
 * requested URL, HTTP version, etc.
 * @author Jeff Chern
 */
public class HttpRequest {
	
	private final HttpMethod method;
	private final String version;
	private final String url;
	private Map<String, String> fields;
	// TODO: ignoring all other request header fields for now
	
	public HttpRequest(final String method, final String version, final String url, final Map<String, String> fields) {
		this.method = HttpMethod.getMethod(method);
		this.version = version;
		this.url = url;
		this.fields = fields;
	}

	public HttpMethod getMethod() {
		return method;
	}

	public String getVersion() {
		return version;
	}

	public String getUrl() {
		return url;
	}
	
	/**
	 * Strips all text after-and-including the first '?' char.
	 * @return
	 */
	public String getUrlWithoutParams() {
		final int index = url.indexOf('?');
		return url.substring(0, index);
	}
}
