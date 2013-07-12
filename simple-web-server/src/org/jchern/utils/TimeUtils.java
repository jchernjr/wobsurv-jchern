package org.jchern.utils;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

public class TimeUtils {
	
	private final static SimpleDateFormat loggerDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss.SSS z");
	private final static SimpleDateFormat httpDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);

	public static String timestampHttp() {
	    return timestampHttp(System.currentTimeMillis());
	}

	public static String timestampHttp(final long timeMs) {
	    httpDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
	    return httpDateFormat.format(timeMs);
	}
	
	public static String timestampLogger() {
		return loggerDateFormat.format(System.currentTimeMillis());
	}
}
