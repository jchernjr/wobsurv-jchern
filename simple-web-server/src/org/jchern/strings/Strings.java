package org.jchern.strings;

/**
 * A very primitive string table. 
 * @author Jeff Chern
 *
 */
public class Strings {

	/**
	 * Retrieves the requested string constant and formats it through String.format with the given args. 
	 * @param code
	 * @param args
	 * @return
	 */
	public static String get(final String message, Object... args) {
		return String.format(message, args);
	}
	
	public static final String SOCKET_IN_ERROR = "HttpRequestHandler: Input error when reading from socket bound to remote address: %s";
	public static final String COULD_NOT_PARSE_REQUEST_LINE = "HttpRequestParser: Could not parse request line \"%s\"";
	public static final String SECURITY_REQUEST_LENGTH_EXCEEDED = "HttpRequestParser: Input length exceeded request security limit, rejecting.";
	public static final String NO_EOL_MARKER = "HttpRequestParser: Could not find an EOL marker (CR / LF / CRLF) from input, rejecting.";
	public static final String ERROR_IN_OUTPUT_STREAM = "HttpRequestHandler: Output error when writing to socket bound to remote address: %s";
	public static final String ERROR_COPYING_FILE = "HttpRequestHandler: Error while sending \"%s\" to remote address: %s";
}
