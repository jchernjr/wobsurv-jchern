package org.jchern.file;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.jchern.utils.ILogger;
import org.jchern.utils.SingleLogger;

/**
 * A mapping of file extensions to Internet MIME content-types,
 * loaded from a text file.
 * @author Jeff Chern
 *
 */
public class ContentTypes {

	private static ILogger logger = SingleLogger.instance();
	
	// Configurable text file, so admins can add/adjust types without rebuilding code.
	public static final String MIME_TYPES_FILE = "mime-types.tsv";
	public static final String DEFAULT_TYPE_FILE = "mime-type-default.txt";
	public static final String DEFAULT_FALLBACK_TYPE = "application/octet-stream"; // if the default file is missing
	public static final String DEFAULT_HTML_TYPE = "text/html";

	// Mapping of file extensions to reported types
	private static Map<String, String> types;
	private static String defaultType;

	public static String getMimeTypeForExtension(String ext) {
		ext = ext.toLowerCase();
		if (types.containsKey(ext)) {
			return types.get(ext);
		} else {
			return defaultType;
		}
	}

	public static synchronized void init() {
		if (types == null) {
			initDefaultType();
			initMappedTypes();
		}
	}
	
	private static void initDefaultType() {
		// Determine default type for all unregistered file extensions (or use fallback)
		try {
			final Scanner src = new Scanner(new BufferedReader(new FileReader(DEFAULT_TYPE_FILE)));
			if (src.hasNextLine()) {
				defaultType = src.nextLine().trim();
			} else {
				useFallbackDefaultAndWarn();
			}
			src.close();
		} catch (FileNotFoundException e) {
			useFallbackDefaultAndWarn();
		}
	}
	
	private static void initMappedTypes() {
		// Load file extensions to type mapping
		types = new HashMap<String, String>();
		try {
			final Scanner src = new Scanner(new BufferedReader(new FileReader(MIME_TYPES_FILE)));
			while (src.hasNextLine()) {
				final String line = src.nextLine();
				final String[] split = line.split("\t+");
				if (split.length != 2) {
					logger.warn("Error parsing MIME type definition: " + line);
				} else {
					types.put(split[0].toLowerCase(), split[1]);
				}
			}
			src.close();
		} catch (FileNotFoundException e) {
			logger.warn("Couldn't read MIME type definition file: " + MIME_TYPES_FILE);
			logger.warn("Will use default MIME type (" + defaultType + ") for all files.");
		}
	}
	
	private static void useFallbackDefaultAndWarn() {
		defaultType = DEFAULT_FALLBACK_TYPE;
		logger.warn("Couldn't read default MIME type file: " + DEFAULT_TYPE_FILE);
		logger.warn("Will use default type of: " + DEFAULT_FALLBACK_TYPE + " for unrecognized extensions.");
	}
}
