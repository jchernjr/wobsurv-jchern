package org.jchern.webserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URLDecoder;

import org.jchern.file.ContentTypes;
import org.jchern.file.StaticFiles;
import org.jchern.file.StaticFiles.FileInfo;
import org.jchern.http.HttpMethod;
import org.jchern.http.HttpRequest;
import org.jchern.http.HttpRequestParser;
import org.jchern.http.HttpResponse;
import org.jchern.http.ParsingException;
import org.jchern.http.ResponseCode;
import org.jchern.strings.Strings;
import org.jchern.utils.ILogger;
import org.jchern.utils.SingleLogger;

public class HttpRequestHandler implements Runnable {

    private static final ILogger logger = SingleLogger.instance();
    private static final int READ_BUFFER_SIZE = 8192;
    
    private final Socket socket;
    
    // TODO: store re-usable parser instances somewhere persistent, if we don't want to create a new instance for every single request handler.
    private final HttpRequestParser parser;
    
    private final StaticFiles fileRoot;
    
    public HttpRequestHandler(final Socket cxn, StaticFiles fileRoot) {
        this.socket = cxn;
        this.parser = new HttpRequestParser();
        this.fileRoot = fileRoot;
    }

    @Override
    public void run() {
        try {
            final BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            final HttpRequest request = parser.parse(in);

            // Get the URL, but discard any query params (after '?'), and convert escaped chars.
            final String requestedUrl = URLDecoder.decode(discardUrlParams(request.getUrl()), "UTF-8");
            
            // I assume I'm already in a worker thread, so I will just respond to the request here directly.
            switch (request.getMethod()) {
            case HEAD:
                // Look for the URL, see if it's there.
                // just send a response Header with info.
            	logRequest(HttpMethod.HEAD, requestedUrl, socket);
                fulfillRequestedUrl(requestedUrl, false);
                break;
                
            case GET:
                // Then write it to output stream.
            	logRequest(HttpMethod.GET, requestedUrl, socket);
                fulfillRequestedUrl(requestedUrl, true);                
                break;

            case PUT:
            case POST:
            case DELETE:
                // We don't support methods other than GET and HEAD.
                respondWithError(ResponseCode.ERROR_405, requestedUrl);
                break;
                
            default:
                // No other methods recognized.
                respondWithError(ResponseCode.SERVER_501, requestedUrl);
            }

        } catch (ParsingException e) {
            // TODO: log the problematic request, if needed for auditing
        	logger.error(e.getMessage());
        	respondWithError(ResponseCode.ERROR_400, "a request rejected by parser");
        } catch (IOException e) {
            logger.error(Strings.get(Strings.SOCKET_IN_ERROR, socket.getRemoteSocketAddress().toString()));
            logger.error(e);
        } finally {
        	closeQuietly(socket);
        }
    }
    
    private String discardUrlParams(final String url) {
        int index = url.indexOf('?', 0);
        return (index > 0) ? url.substring(0, index) : url; 
    }
    
    /**
     * Generates and sends a complete HTTP response to the request for url.
     * @param requestUrl
     * @param sendContent false for HEAD request
     * @return
     */
    private void fulfillRequestedUrl(final String requestUrl, final boolean sendContent) {
        final FileInfo requestedUrl = fileRoot.getInfoForUrl(requestUrl);

        if (requestedUrl.found()) {
            if (requestedUrl.isDir()) {
                // Search for index.htm or index.html: send file or generate an index listing
                if (requestedUrl.hasIndexFile()) {
                    logger.debug("Sending index file: " + requestedUrl.getIndexFile().getPath());
                    respondWithFile(requestedUrl.getIndexFile(), sendContent);
                } else {
                    logger.debug("Generating index file: " + requestedUrl.getPath());
                    respondWithGeneratedIndex(requestedUrl, sendContent);
                }
            } else {
                // Send file
                logger.debug("Sending file: " + requestedUrl.getPath());
                respondWithFile(requestedUrl, sendContent);
            }
        } else {
            logger.debug("Url NOT found " + requestUrl);
            respondWithError(ResponseCode.ERROR_404, requestUrl);
        }
    }

    /**
     * @param fileToSend info for a valid FILE that exists
     * @param sendFileContent
     */
    private void respondWithFile(final FileInfo fileToSend, final boolean sendFileContent) {
        final ResponseCode code = ResponseCode.SUCCESS_200;
        final File path = fileToSend.getPath();

        final String contentType = ContentTypes.getMimeTypeForExtension(fileToSend.getExtension());
        final long contentLen = path.length(); // Path length is 0 for an empty or non-existent file, but we already determined the file indeed exists.
        final long modifiedMs = path.lastModified();
        final boolean allowCache = true;

        final HttpResponse headers = new HttpResponse(code, contentType, contentLen, modifiedMs, allowCache);

        FileInputStream fileInputStream = null;
        try {
            // Don't need to open the file if we're not going to send content
            fileInputStream = (sendFileContent ? new FileInputStream(path) : null);
            sendResponse(headers, sendFileContent, fileInputStream, fileToSend.getPath().getAbsolutePath());
        } catch (FileNotFoundException e) {
            // Shouldn't happen
            logger.error(e);
        } finally {
            closeQuietly(fileInputStream);
        }
    }

    /**
     * Generates an HTML listing of all files and subdirs, and sends it to the client.
     * @param dirToList a valid DIR that exists
     * @param sendContent
     */
    private void respondWithGeneratedIndex(final FileInfo dirToList, final boolean sendContent) {
        final IGeneratedIndex indexInfo = new IndexBuilder().buildIndexForDir(dirToList.getPath());

        // Construct response params
        final ResponseCode code = ResponseCode.SUCCESS_200;
        final String contentType = ContentTypes.DEFAULT_HTML_TYPE + "; charset=" + indexInfo.getEncoding();
        final long contentLen = indexInfo.getContentLength();
        final long modTime = indexInfo.getModifiedTime();
        final boolean allowCache = true; // because we have a sensible mod-time for the dir listing.

        // Construct response and data
        final HttpResponse headers = new HttpResponse(code, contentType, contentLen, modTime, allowCache);
        final InputStream contents = indexInfo.getContent();
        sendResponse(headers, sendContent, contents, String.format("Directory Listing for %s", dirToList.getOriginalUrl()));
    }
    
    

    private void respondWithError(ResponseCode code, final String requestUrlForLogging) {
        final HttpResponse headers = new HttpResponse(code);
        sendResponse(headers, false, null, requestUrlForLogging);
    }
    
    private void sendResponse(HttpResponse headers, boolean sendContent, InputStream content, final String contentName) {
        logResponse(headers.getStatus(), contentName, socket);

        OutputStream writer = null;
        try {
            writer = socket.getOutputStream();

            // Write all the headers, automatically followed by an empty line.
            headers.writeToOutput(writer);

            // Send the message body (i.e. file contents), if required.
            if (sendContent) {
                copyToOutputStream(content, writer, contentName, socket.getRemoteSocketAddress().toString());
            }
        } catch (IOException e) {
            logger.error(Strings.get(Strings.ERROR_IN_OUTPUT_STREAM, contentName, socket.getRemoteSocketAddress()));
            logger.error(e);
        } finally {
            closeQuietly(writer);
        }
    }

    private static void copyToOutputStream(final InputStream in, final OutputStream out,
            final String contentName, final String remoteName) {
        try {
            final byte[] buffer = new byte[READ_BUFFER_SIZE];

            // Repeatedly read and copy bytes until reaching end of file.
            int readLen;
            while ((readLen = in.read(buffer)) != -1) {
                out.write(buffer, 0, readLen);
            }
        } catch (IOException e) {
            logger.error(Strings.get(Strings.ERROR_COPYING_FILE, contentName, remoteName));
        } finally {
            closeQuietly(in);
        }
    }

    private void logRequest(final HttpMethod method, final String url, final Socket cxn) {
    	logger.debug(String.format("(%s) %s %s", cxn.getRemoteSocketAddress(), method.name(), url));
    }

    private void logResponse(final ResponseCode status, final String contentDesc, final Socket cxn) {
    	logger.info(String.format("(%s) %s for %s", cxn.getRemoteSocketAddress(), status, contentDesc));
    }

    private static void closeQuietly(Socket s) {
        try {
            if (s != null) {
                s.close();
            }
        } catch (IOException e) {
            // silence...
        }
    }
    
    private static void closeQuietly(InputStream f) {
        try {
            if (f != null) {
                f.close();
            }
        } catch (IOException e) {
            // silence...
        }
    }
    
    private static void closeQuietly(OutputStream f) {
        try {
            if (f != null) {
                f.close();
            }
        } catch (IOException e) {
            // silence...
        }
    }
}
