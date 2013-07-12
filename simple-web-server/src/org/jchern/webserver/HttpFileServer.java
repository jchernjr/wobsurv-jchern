package org.jchern.webserver;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jchern.file.ContentTypes;
import org.jchern.file.StaticFiles;
import org.jchern.utils.ILogger;
import org.jchern.utils.SingleLogger;

public class HttpFileServer {

	private final static int DEFAULT_THREADPOOL_SIZE = 5;

	private final int port;
	private final StaticFiles fileRoot;
	
	private boolean started = false;
	private ServerSocket serverSocket;
	
	private final ILogger logger;
	
	public HttpFileServer(final int port, final String root) {
		this.port = port;
		logger = SingleLogger.instance();
		
		// Set up root dir
		fileRoot = new StaticFiles(new File(root));

		// Load known MIME types
		ContentTypes.init();
	}
	
	/**
	 * Starts the server on another thread and returns.
	 * @throws IOException
	 */
	public synchronized void start() throws IOException {
		if (started) return; // only start once
		
		// Listen on this port
		serverSocket = new ServerSocket(port);
		final ExecutorService threadPool = Executors.newFixedThreadPool(DEFAULT_THREADPOOL_SIZE);

		// Start listener thread
		final Thread runner = new Thread() {
			@Override
			public void run() {
				// Repeatedly handle connection requests on the server socket
				while (true) {
					try {
						final Socket connection = serverSocket.accept();
						connection.setKeepAlive(false);
						
						// Respond to request
						logger.debug("Connected socket to: " + connection.getRemoteSocketAddress().toString());
						threadPool.submit(new HttpRequestHandler(connection, fileRoot));
					} catch (IOException e) {
						logger.error("HttpServer: Error accepting incoming connection.");
						logger.error(e);
					}
				}
			}
		};
		runner.start();
		started = true;
	}

	public synchronized void stop() {
		if (started) {
			try {
				serverSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			started = false;
		}
	}
}
