package org.jchern.webserver;

import java.io.IOException;

import org.jchern.utils.ILogger;
import org.jchern.utils.SingleLogger;

public class Launcher {

	private static final int PORT_ARG = 0;
	private static final int WORKING_DIR_ARG = 1;

	private static final ILogger logger = SingleLogger.instance();
	
	/**
	 * Arguments required:
	 * 1. port-num (listening)
	 * 2. working-dir (where WWW will be found)
	 */
	public static void main(String[] args) {
		if (args.length < 2) {
			printUsage();
			return;
		}

		// parse args
		final int port;
		try {
			port = Integer.valueOf(args[PORT_ARG]);
		} catch (NumberFormatException e) {
			System.err.println("Invalid port: " + args[PORT_ARG]);
			printUsage();
			return;
		}

		try {
			new HttpFileServer(port, args[WORKING_DIR_ARG]).start();
		} catch (IOException e) {
			logger.fatal("Error starting server.");
			logger.fatal(e);
		}
	}
	
	private static void printUsage() {
		System.out.println("Usage:   java  org.jchern.webserver.Launcher  <port>  <directory to serve>");
	}
}
