package org.jchern.utils;

import org.jchern.utils.ILogger.LogLevel;

public class SingleLogger {

	private static ILogger logger = null;
	
	public static synchronized ILogger instance() {
		if (logger == null) {
			logger = new BasicLogger(LogLevel.INFO);
		}
		return logger;
	}
}