package org.jchern.http;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.jchern.utils.TimeUtils;

// TODO: perhaps make this an interface, so we can impl it in other ways
// TODO: perhaps make a factory for responses, so we can more easily configure lots of other fields
public class HttpResponse {

	private static final String DUMMY_HTTP_VERSION = "HTTP/1.1";
	private static final String DUMMY_SERVER_TYPE = "WobSurb-jchern";
	
	private final ResponseCode code;
	private final List<HttpField> fields;

	public HttpResponse(final ResponseCode code, final String contentType, final long contentLength, final long lastModified,
			final boolean allowCaching) {
		fields = new ArrayList<HttpField>();

		// Header line (can generate the HTTP version and Code description later)
		this.code = code;

		// General Headers
		addField("Date", TimeUtils.timestampHttp());
		addField("Connection", "close"); // We don't support persistent connections

		// Response Headers
		addField("Server", DUMMY_SERVER_TYPE);

		// Entity Headers
		if (contentType != null) {
			addField("Content-Type", contentType);
			addField("Content-Length", String.valueOf(contentLength));
			addField("Last-Modified", TimeUtils.timestampHttp(lastModified));
			if (!allowCaching) {
				addField("Cache-Control", "no-cache");
			}
		}
	}

	/**
	 * Constructor for a response with no content (e.g. for NOT FOUND situations).
	 * @param code
	 */
	public HttpResponse(final ResponseCode code) {
		this(code, null, 0L, 0L, false);
	}

	public ResponseCode getStatus() {
		return code;
	}

	private void addField(final String name, final String value) {
		fields.add(new HttpField(name, value));
	}

	/**
	 * Writes this HttpResponse to the given output stream,
	 * starting with the response line, then followed by each field on its own line.
	 * Writes a blank line after all fields have been written, so that a message body may then be sent.
	 */
	public void writeToOutput(OutputStream os) {
		final PrintStream out = new PrintStream(os);

		// Status line
		out.println(String.format("%s %d %s", DUMMY_HTTP_VERSION, code.number, code.description));

		// Fields, in order
		for (HttpField field : fields) {
			out.println(field.toString());
		}
		out.println();
		out.flush();
	}
}
