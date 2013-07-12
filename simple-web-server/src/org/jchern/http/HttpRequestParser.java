package org.jchern.http;


import java.io.BufferedReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jchern.strings.Strings;

/**
 * Each parser instance tracks how many chars consumed so far, 
 * for security against infinitly/maliciously long requests.
 * 
 * These parsers are NOT threadsafe (i.e. can only be used by one thread).
 */
public class HttpRequestParser {
	// Considered using ANTLR to generate a parser, but minimal HTTP can be simple enough to hand-write.
	
	private static final String TEXT =    "(\\S+)"; // parens to capture it as a group
	private static final String LWS =     "[ \\t]+"; // mandatory Linear WhiteSpace
	private static final String LWS_OPT = "[ \\t]*"; // optional Linear WhiteSpace
	private static final String HEADER_VALUE = "(.*?)"; // reluctant quantifier, to avoid capturing trailing whitespace; allow empty value.
	
	private static class RequestLinePattern {
		private static final String REQUEST_REGEX = TEXT + LWS + TEXT + LWS + "HTTP/" + TEXT + LWS_OPT;
		private static final Pattern PATTERN = Pattern.compile(REQUEST_REGEX);
		
		// matcher group indices (starting at 1, because 0 is the entire string)
		public static final int METHOD = 1;
		public static final int URL = 2;
		public static final int VERSION = 3;
		
		public static Matcher matcher(String s) {
			return PATTERN.matcher(s);
		}
	}
	
	// TODO: request header fields, untested, because not using them in this project.
	private static class RequestHeaderPattern {
		private static final String FIRST_LINE_REGEX = TEXT + ":" + LWS_OPT + HEADER_VALUE + LWS_OPT;
		private static final String CONTINUED_LINE_REGEX = LWS + HEADER_VALUE + LWS_OPT; 
		
		private static final Pattern FIRST_LINE = Pattern.compile(FIRST_LINE_REGEX);
		private static final Pattern CONTINUED_LINE = Pattern.compile(CONTINUED_LINE_REGEX);
		
		// matcher group indices
		public static final int NAME = 1;
		public static final int VALUE = 2;
		
		public static final int CONTINUED_VALUE = 1;
	}
	
	// For security: reject requests that are too long, have infinite / maliciously long line length, 
	// or infinite / maliciously long sequence of newlines at beginning.
	public static final int MAX_REQUEST_LENGTH = 3000; // in chars
	private int charsConsumedSoFar;
	
	public HttpRequestParser() {
	}
	
	/**
	 * Parses an HTTP request from the given BufferedReader.
	 * @param in
	 * @return
	 * @throws ParsingException if there was something wrong with the input content
	 * @throws IOException if there was something wrong with the input reader
	 */
	public HttpRequest parse(BufferedReader in) throws ParsingException, IOException {
		// Reset request length security counter
		charsConsumedSoFar = 0;
		
		final HttpRequestFactory factory = HttpRequestFactory.instance();
		
		// Get and parse first line (ignoring any prepended newlines, e.g. CR/LF/CRLF's)
		final String requestLine = getFirstNonemptyLine(in);
		if (requestLine == null) throw new ParsingException();
		parseRequestLine(factory, requestLine);

		// TODO: Parse and store fields.  Not implemented in this project, because we just want to return files.
		// Even ignoring Content-Accept headers, which specify acceptable MIME types for the client.
		// We "should" send 406 if the types don't match, but I'm skipping even that for the sake of simplicity. 
		
		// TODO: The first blank line indicates the end of general/request/entity header fields, and begins the request body.
		// Ignoring this, too.

		return factory.getRequest();
	}

	/**
	 * Returns the first non-empty line from the input,
	 * or null if the end-of-input was reached before a non-empty line was found.
	 * @param in
	 * @return
	 * @throws ParsingException
	 */
	private String getFirstNonemptyLine(final BufferedReader in) throws ParsingException, IOException {
		String line = "";

		final char[] buffer = new char[MAX_REQUEST_LENGTH + 1];
		while (line != null && line.isEmpty()) {
			final int maxLen = MAX_REQUEST_LENGTH - charsConsumedSoFar;
			line = secureReadLine(in, maxLen, buffer);
		}
		return line;
	}
	
	/**
	 * Reads up to maxLen chars from the input, or until the first CR / LF / CRLF is encountered.
	 * Throws a ParsingException if a newline was not found within maxLen chars.
	 * @param in
	 * @param maxLen
	 * @param buffer - reusable buffer of sufficient length (maxChars + 1)
	 * @return
	 * @throws ParsingException - if something was wrong with the input content (length too long, or couldn't find a line ending)
	 * @throws IOException - if something was wrong with the input stream
	 */
	/*package*/ String secureReadLine(final BufferedReader in, final int maxLen, final char[] buffer) throws ParsingException, IOException {
		// Remember present position, because we are going to read ahead, possibly very far.
		// Allow an extra char, so we can test if the stream is too long.
		in.mark(maxLen + 1);

		final int charsRead = in.read(buffer, 0, maxLen + 1);
		if (charsRead == -1) return null; // EOS
		if (charsRead > maxLen) throw new ParsingException(Strings.get(Strings.SECURITY_REQUEST_LENGTH_EXCEEDED));
		// charsRead can be at most maxLen from this point forward.
        
		// Search for EOL markers (CR / LF / CRLF) and consume up to that point.
		// Throw an exception if there is no EOL marker.
		final int index = findFirstEol(buffer, Math.min(maxLen, charsRead));
		if (index == -1) throw new ParsingException(Strings.get(Strings.NO_EOL_MARKER));
		final boolean CRLF = (buffer[index] == '\r') && 
				(index + 1 < charsRead) && (buffer[index + 1] == '\n');
		
		// Construct the string, and consume the line, including EOL chars.
        // HTTP protocol uses only ASCII encoding, so it's ok to convert bytes into Java chars (Unicode).
		final String line = new String(buffer, 0, index); // up to, but not including, the EOL marker
		final int consumeLen = index + (CRLF ? 2 : 1); // up to, and including, the EOL marker
		
		in.reset(); // Go back to marked position, and consume from there.
		in.read(buffer, 0, consumeLen);
		charsConsumedSoFar += consumeLen; 
		
		return line;
	}
	
	/**
	 * Returns the index of the first '\r' or '\n' char in the buffer,
	 * up to maxLen chars into the buffer. 
	 * @param buffer
	 * @param maxLen
	 * @return -1 if not found within the allowed search length
	 */
	private static int findFirstEol(final char[] buffer, int maxLen) {
		for (int i = 0; i < maxLen; i++) {
			final char c = buffer[i];
			if (c == '\r' || c == '\n') {
				return i;
			}
		}
		return -1;
	}
	
	/**
	 * Attempts to parse the given request line, e.g. "GET /path/url HTTP/1.1"
	 * and sets the parsed Method, Url, and Version on the provided Factory. 
	 * @param factory
	 * @param line
	 * @throws ParsingException
	 */
	/*package*/ static void parseRequestLine(final HttpRequestFactory factory, final String line)
			throws ParsingException {
		// package scope to allow easier unit testing, from test project.
		
		final Matcher matcher = RequestLinePattern.matcher(line);
		if (matcher.matches()) {
			factory.setMethod(matcher.group(RequestLinePattern.METHOD));
			factory.setUrl(matcher.group(RequestLinePattern.URL));
			factory.setVersion(matcher.group(RequestLinePattern.VERSION));
		} else {
			throw new ParsingException(Strings.get(Strings.COULD_NOT_PARSE_REQUEST_LINE, line));
		}
	}
	
	/*package*/ static void parseRequestHeaders(final HttpRequestFactory factory, final BufferedReader in) {
		// Keep in mind these requirements from (RFC 2616 section 4.2):
		// 1. Field names should be case-INsensitive
		// 2. Fields can be continued on subsequent lines, if those lines start with linear whitespace.
		// 3. There can be multiple fields with the same name, but different values: interpret them in order.
		
		// TODO: Since I aim to write a minimalist file server, I'll choose not to care about any header fields for now.
	}
	
}
