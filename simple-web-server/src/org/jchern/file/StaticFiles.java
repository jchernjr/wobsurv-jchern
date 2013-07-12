package org.jchern.file;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;

import com.google.common.collect.Lists;

public class StaticFiles {

	private static final List<String> indexFilenames = Lists.newArrayList("index.htm", "index.html");
	private static final FilenameFilter indexFilenameFilter = new FilenameFilter() {

		@Override
		public boolean accept(File dir, String name) {
			// Note: There's a ridiculous case of "toLowerCase" respecting the current locale,
			// in a Turkish language locale, which must distinguish between dotted and dot-less I's.
			// A capital I would be lowercased to a dot-less i, which is a completely different char than the dotted i,
			// causing string comparisons to break. (Even for internal, non user-facing strings).
			// I am ignoring this case for now...
			return indexFilenames.contains(name.toLowerCase());
		}
	};
	
	private final File rootPath;
	
	/**
	 * Pass the directory, where files should be served from.
	 * @param root
	 */
	public StaticFiles(final File root) {
		this.rootPath = root;
	}

	public FileInfo getInfoForUrl(final String url) {
		// File's ctor treats absolute child 'url' as a relative path under 'rootPath'
		return new FileInfo(new File(rootPath, url), url);
	}

	/**
	 * Describes a file's path, and whether it is a DIRECTORY, FILE, or is NON-EXISTENT.
	 */
	public static class FileInfo {

		public enum Status {
			DIRECTORY,
			FILE,
			NON_EXISTENT;
		}

		final File path;
		final String originalUrl;
		
		private FileInfo(final File path, final String originalUrl) {
			this.path = path;
			this.originalUrl = originalUrl;
		}

		public File getPath() {
			return path;
		}
		
		public String getOriginalUrl() {
			return originalUrl;
		}

		private static Status getStatus(File path) {
			if (!path.exists() || !path.canRead()) {
				return Status.NON_EXISTENT;
			} else if (path.isDirectory()) {
				return Status.DIRECTORY;
			} else {
				return Status.FILE;
			}
		}

		public boolean found() {
			return getStatus(path) != Status.NON_EXISTENT;
		}

		public boolean isFile() {
			return getStatus(path) == Status.FILE;
		}

		public boolean isDir() {
			return getStatus(path) == Status.DIRECTORY;
		}

		/**
		 * For directory paths ONLY, returns whether an Index file was found in this dir.
		 * @return
		 */
		public boolean hasIndexFile() {
			return getIndexFile() != null;
		}

		/**
		 * For directory paths ONLY, returns a FileInfo object for the first index file found in this dir,
		 * or null if none was found.
		 * @return
		 */
		public FileInfo getIndexFile() {
			if (isDir()) {
				final File[] indexFiles = path.listFiles(indexFilenameFilter);
				if (indexFiles.length > 0) {
					// return first file matching index name filter
					final File firstFile = indexFiles[0];
					final String effectiveUrl = originalUrl + '/' + firstFile.getName();
					return new FileInfo(firstFile, effectiveUrl);
				}
				return null;
			}
			return null;
		}

		/**
		 * Returns the portion of the filename after the last '.' character,
		 * or empty string if there are no '.'s in the filename. 
		 */
		public String getExtension() {
			final String filename = path.getName();
			final int dotIndex = filename.lastIndexOf('.');
			if (dotIndex >= 0) {
				if (dotIndex == filename.length() - 1) {
					return ""; // dot is the last char in filename
				} else {
					return filename.substring(dotIndex + 1);
				}
			} else {
				return ""; // no dot in filename
			}
		}
	}
}
