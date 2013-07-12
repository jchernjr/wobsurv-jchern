package org.jchern.utils;

import java.io.PrintStream;
import java.util.EnumMap;
import java.util.Map;

public class BasicLogger implements ILogger {

	private LogLevel minimumLevel;
	private Map<LogLevel, PrintStream> outputters;
	
	public BasicLogger(final LogLevel initialLogLevel) {
		this.minimumLevel = initialLogLevel;
		outputters = new EnumMap<LogLevel, PrintStream>(LogLevel.class);
		outputters.put(LogLevel.DEBUG, System.out);
		outputters.put(LogLevel.INFO, System.out);
		outputters.put(LogLevel.WARN, System.out);
		outputters.put(LogLevel.ERROR, System.err);
		outputters.put(LogLevel.FATAL, System.err);
	}
	
	@Override
	public void debug(String msg) {
		logAsLevel(LogLevel.DEBUG, msg);
	}

	@Override
	public void info(String msg) {
		logAsLevel(LogLevel.INFO, msg);
	}

	@Override
	public void warn(String msg) {
		logAsLevel(LogLevel.WARN, msg);
	}

	@Override
	public void error(String msg) {
		logAsLevel(LogLevel.ERROR, msg);
	}

	@Override
	public void fatal(String msg) {
		logAsLevel(LogLevel.FATAL, msg);
	}

	@Override
	public void error(Exception e) {
		logAsLevel(LogLevel.ERROR, e);
	}

	@Override
	public void fatal(Exception e) {
		logAsLevel(LogLevel.FATAL, e);
	}
	
	@Override
	public void setLogLevel(LogLevel level) {
		
	}
	
	private void logAsLevel(LogLevel level, String msg) {
		if (shouldLogLevel(level)) {
			outputters.get(level).println(now() + ": " + msg);
		}
	}
	
	private void logAsLevel(LogLevel level, Exception e) {
		if (shouldLogLevel(level)) {
			e.printStackTrace(outputters.get(level));
		}
	}
	
	private boolean shouldLogLevel(final LogLevel requestedLevel) {
		return (requestedLevel.compareTo(minimumLevel) >= 0); // allowed if it's at least the minimum level
	}
	
	private String now() {
		return TimeUtils.timestampLogger();
	}
}
