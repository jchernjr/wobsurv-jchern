package org.jchern.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import junit.framework.TestCase;
import mockit.Expectations;
import mockit.Mocked;

public class HttpRequestParserTests extends TestCase {

	@Mocked HttpRequestFactory factory;
	
	public void setUp() {
		// empty
	}

	public void tearDown() {
		// empty
	}

	private static final String LINE = "LINE LINE LINE LINE LINE";  
	
	public void testSecureReadLine_WithinLengthLimit() {
		final String expected = LINE; 
		final String inputCRLF = expected + "\r\n";
		final String inputCR = expected + "\r";
		final String inputLF = expected + "\n";
		
		verifySecureReadLineSucceeds(inputCRLF, expected, 1000);
		verifySecureReadLineSucceeds(inputCR, expected, 1000);
		verifySecureReadLineSucceeds(inputLF, expected, 1000);
	}
	
	public void testSecureReadLine_ExactlyLengthLimit() {
		final String expected = LINE + LINE + LINE;
		final String input = expected + "\r\n";
		verifySecureReadLineSucceeds(input, expected, input.length());
	}
	
	public void testSecureReadLine_WithoutLineEnding() {
		final String input = LINE; 
		verifySecureReadLineFails(input, 1000);
	}
	
	public void testSecureReadLine_ExceedLengthLimit() {
		final String input = LINE + "\r\n";
		verifySecureReadLineFails(input, LINE.length());
	}
	
	public void testSecureReadLine_ExceedLengthLimitWithCRLF() {
		final String input = LINE + "\r\n";
		verifySecureReadLineFails(input, LINE.length() + 1);
	}
	
	private void verifySecureReadLineSucceeds(final String input, final String expected, final int maxLen) {
		final char[] buffer = new char[maxLen + 1];
		final BufferedReader reader = new BufferedReader(new StringReader(input));
		
		try {
			final String actual = new HttpRequestParser().secureReadLine(reader, maxLen, buffer);
			assertEquals(expected, actual);

		} catch (ParsingException e) {
			fail("Did not expect a ParsingException.");
		} catch (IOException e) {
			fail("IOException not expected during any of my tests.");
		}
	}

	private void verifySecureReadLineFails(final String input, final int maxLen) {
		final char[] buffer = new char[maxLen + 1];
		final BufferedReader reader = new BufferedReader(new StringReader(input));
		
		try {
			new HttpRequestParser().secureReadLine(reader, maxLen, buffer);
			fail("Expected an exception.");
		} catch (ParsingException e) {
		} catch (IOException e) {
			fail("Should have thrown ParsingException, not IOException.");
		}
	}
	
	public void testParseRequestLine_SingleSpaced() {
		final String testLine = "GET /url1/url2/url3.txt HTTP/1.0";

		setRequestLineExpectations("GET", "/url1/url2/url3.txt", "1.0");
		verifyParseRequestLine(testLine, false);
	}
	
	public void testParseRequestLine_SingleTabbed() {
		final String testLine = "POST\t/url1/url2/url3.txt\tHTTP/1.1";

		setRequestLineExpectations("POST", "/url1/url2/url3.txt", "1.1");
		verifyParseRequestLine(testLine, false);
	}
	
	public void testParseRequestLine_MixedSpacesAndTabs() {
		final String testLine = "HEAD \t /url1/url2/url3.txt\t\tHTTP/2.0";
		
		setRequestLineExpectations("HEAD", "/url1/url2/url3.txt", "2.0");	
		verifyParseRequestLine(testLine, false);
	}
	
	public void testParseRequestLine_PrecededBySpace() {
		// Should fail, because the request line should not start with whitespace
		final String testLine = " POST /url.txt HTTP/1.1";
		verifyParseRequestLine(testLine, true);
	}
	
	public void testParseRequestLine_MissingVersion() {
		final String testLine = "GET /url";
		verifyParseRequestLine(testLine, true);
	}
	
	public void testParseRequestLine_MalformedVersion() {
		final String testLine = "GET /url HTTP";
		verifyParseRequestLine(testLine, true);
	}
	
	
	public void testParseRequestLine_WrongProtocol() {
		final String testLine = "GET /url ZZZ/1.0";
		verifyParseRequestLine(testLine, true);
	}
		
	public void testParseRequestLine_MissingMethod() {
		final String testLine = "/url HTTP/1.0";
		verifyParseRequestLine(testLine, true);
	}
	
	public void testParseRequestLine_MissingUrl() {
		final String testLine = "GET HTTP/1.0";
		verifyParseRequestLine(testLine, true);
	}
	
	private void setRequestLineExpectations(final String method, final String url, final String version) {
		new Expectations() {{
			factory.setMethod(method); times = 1;
			factory.setUrl(url); times = 1;
			factory.setVersion(version); times = 1;
		}};
	}
	
	private void setOverallExpectations(final String method, final String url, final String version, 
			final List<StringPair> fields) {
		
		// Only mock the 3 or 4 methods below
		new Expectations(HttpRequestFactory.class) {{
			factory.setMethod(method); times = 1;
			factory.setUrl(url); times = 1;
			factory.setVersion(version); times = 1;
			
			// Expect fields to be set in order, if there are any
			if (fields != null) {
				for (StringPair field: fields) {
					factory.setHeaderField(field.key, field.value);
				}
			}
		}};
	}
	
	private void verifyParseRequestLine(final String requestLine, final boolean expectException) {
		try {
			HttpRequestParser.parseRequestLine(factory, requestLine);
			if (expectException) fail("Parsing a malformed request line should have thrown an exception.");

		} catch (ParsingException e) {
			if (!expectException) fail("Parsing a valid request line should not throw an exception.");
		}
	}
	
	public void testParse_PrecededByEmptyLines() {
		final String testLinesWithCR = "\r\r\rGET /url HTTP/1.0 \r";
		final String testLinesWithLF = "\n\n\n\nGET /url HTTP/1.0 \r";
		final String testLinesWithCRLF = "\r\n\r\nGET /url HTTP/1.0 \r";
		
		setOverallExpectations("GET", "/url", "1.0", null);
		verifyParse(testLinesWithCR, false);
		
		setOverallExpectations("GET", "/url", "1.0", null);	
		verifyParse(testLinesWithLF, false);
		
		setOverallExpectations("GET", "/url", "1.0", null);	
		verifyParse(testLinesWithCRLF, false);
	}
	
	public void testParse_SingleLineTooLong() {
		final String input = makeLongerString("GET /blah/blah/blah", HttpRequestParser.MAX_REQUEST_LENGTH) + " HTTP/1.1";
		verifyParse(input, true);
	}
	
	public void testParse_PrecedingEmptyLinesTooLong() {
		// Preceding empty lines cause the overall input to be too long
		final String httpVersion = "HTTP/2.0";
		final String lineSuffix = String.format(" %s\r\n", httpVersion);
		
		final int lenWithUrl = HttpRequestParser.MAX_REQUEST_LENGTH - lineSuffix.length();
		final String validInput = makeExactLength("GET /blah/", lenWithUrl) + lineSuffix;
		
		final String invalidInput = "\r\r\r" + validInput;
		verifyParse(invalidInput, true);
	}
	
	/**
	 * Call setOverallExpectations before running this, if parsing is expected to succeed.
	 * @param input
	 * @param expectException
	 */
	private void verifyParse(final String input, final boolean expectException) {
		try {
			new HttpRequestParser().parse(new BufferedReader(new StringReader(input)));
			if (expectException) fail("Expected an exception.");

		} catch (ParsingException e) {
			if (!expectException) fail("Did not expect an exception.");
		} catch (IOException e) {
			fail("IOException not expected during any of my tests");
		}
	}
	
	/**
	 * For storing key-value pairs (of HTTP request fields), for testing.
	 */
	private static class StringPair {
		final String key, value;
		// TODO: not testing header fields, because not using them in this project
		public StringPair(final String key, final String value) {
			this.key = key;
			this.value = value;
		}
	}
	
	private String makeLongerString(final String startingWith, final int longerThan) {
		final StringBuilder sb = new StringBuilder();
		sb.append(startingWith);
		while (sb.length() < longerThan) {
			sb.append("1234567890");
		}
		return sb.toString();
	}
	
	private String makeExactLength(final String startingWith, final int len) {
		final String tooLong = makeLongerString(startingWith, len);
		return tooLong.substring(0, len);
	}
}
