package org.jchern.http;

public enum HttpMethod {
	HEAD,
	GET,
	PUT,
	POST,
	DELETE,
	
	UNKNOWN;
	
	public static HttpMethod getMethod(final String methodName) {
		try {
			return valueOf(HttpMethod.class, methodName.toUpperCase());
		} catch (IllegalArgumentException e) {
			return UNKNOWN;
		}
	}
}
