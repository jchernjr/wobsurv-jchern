package org.jchern.utils;

public interface ILogger {

	public static enum LogLevel{
		DEBUG, INFO, WARN, ERROR, FATAL;
	}		

	public void debug(String msg);
	public void info(String msg);
	public void warn(String msg);
	public void error(String msg);
	public void fatal(String msg);

	public void error(Exception e);
	public void fatal(Exception e);

	public void setLogLevel(LogLevel level);
}
