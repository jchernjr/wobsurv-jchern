package org.jchern.http;

public class HttpField {

	public final String name;
	public final String value;
	
	public HttpField(final String name, final String value) {
		this.name = name;
		this.value = value;
	}
	
	/**
	 * Encodes this field as a line in an HTTP header. 
	 */
	public String toString() {
		return name + ": " + value;
	}
}
