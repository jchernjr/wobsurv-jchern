package org.jchern.http;

/**
 * A selected subset of HTTP status codes.
 * I ended up not using all of them in this program.
 * @author Jeff Chern
 */
public enum ResponseCode {
	
	SUCCESS_200(200, "OK"),
	REDIRECTION_304(304, "Not Modified"),
	ERROR_400(400, "Bad Request"), // malformed request
	ERROR_404(404, "File not Found"),
	ERROR_405(405, "Method not Allowed"), // for methods we recognize, but don't allow
	ERROR_406(406, "Not Acceptable"), // if the request's Content-Accept header doesn't match the content we can return (TODO: ignored for now)
	SERVER_500(500, "Internal Server Error"),
	SERVER_501(501, "Not Implemented"), // for methods we don't recognize
	SERVER_505(505, "HTTP Version not Supported");
	
	final int number;
	final String description;
	private ResponseCode(final int number, final String desc) {
		this.number = number;
		this.description = desc;
	}
	
	@Override
	public String toString() {
		return String.format("%d %s", number, description);
	}
}