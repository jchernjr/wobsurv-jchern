package org.jchern.webserver;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.List;

import org.jchern.utils.ILogger;
import org.jchern.utils.SingleLogger;
import org.jchern.utils.TimeUtils;

import com.google.common.collect.Lists;

public class IndexBuilder {

	public GeneratedIndex buildIndexForDir(final File dirToList) {
    	final StringWriter stringWriter = new StringWriter();
        final PrintWriter out = new PrintWriter(stringWriter);

        // Consider a directory listing's effective mod time to be the latest of all its contents' modified times
        long newestModTime = 0L;
        
        // I want to print subdirs first, so sort them into bins
        final List<File> subdirs = Lists.newArrayList();
        final List<File> files = Lists.newArrayList();

        for (File f : dirToList.listFiles()) {
        	if (f.isDirectory()) {
        		subdirs.add(f);
        	} else {
        		files.add(f);
        	}
        }
        
        // Then sort alphabetically
        Collections.sort(subdirs);
        Collections.sort(files);
        
        // Then print links to each subdir or file
        final List<File> contents = Lists.newArrayList(subdirs);
        contents.addAll(files);

        // TODO: add table css here to make it look better
        out.println("<html><head><meta charset='UTF-8'/></head><body>");
        out.println("<table border='0'>");
        out.println("<tr><th>File</th><th>Size</th><th>Last Modified</th></tr>");

        for (File f : contents) {
        	final String size = f.isDirectory() ? "DIR" : String.valueOf(f.length());
        	final String time = TimeUtils.timestampHttp(f.lastModified());
        	newestModTime = Math.max(newestModTime, f.lastModified());
        	
        	// TODO: use table css here to make it look better
        	out.println(String.format("<tr><td><a href='%s/%s'>%s</a></td><td>%s</td><td>%s</td></tr>",
        			dirToList.getName(), f.getName(), f.getName(), size, time));
        }
        
        out.println("</table>");
        out.println("</html>");

        return new GeneratedIndex(newestModTime, stringWriter.toString());
    }
	
    private static class GeneratedIndex implements IGeneratedIndex {
    	private static final ILogger logger = SingleLogger.instance();
    	private static final String ENCODING = "UTF-8";

    	final long modTime;
    	final String content;

    	public GeneratedIndex(final long modTime, final String content) {
    		this.modTime = modTime;
    		this.content = content;
    	}
    	
    	@Override
    	public long getModifiedTime() {
    		return modTime;
    	}
    	
    	@Override
    	public InputStream getContent() {
    		try {
				return new ByteArrayInputStream(content.getBytes(ENCODING));
			} catch (UnsupportedEncodingException e) {
				logger.error(e);
				return null; // TODO: better handling and auditing of errors.
			}
    	}

		@Override
		public long getContentLength() {
			try {
				return content.getBytes(ENCODING).length;
			} catch (UnsupportedEncodingException e) {
				logger.error(e);
				return 0;
			}
		}

		@Override
		public String getEncoding() {
			return ENCODING;
		}
    }
	
}
