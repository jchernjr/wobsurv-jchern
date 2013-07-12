package org.jchern.http;

public class ParsingException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ParsingException() {
		super();
	}
	
	public ParsingException(String reason) {
		super(reason);
	}
	
	public ParsingException(Exception wrapped) {
		super(wrapped);
	}
}
