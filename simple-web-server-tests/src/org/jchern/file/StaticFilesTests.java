package org.jchern.file;

import java.io.File;

import org.jchern.file.StaticFiles.FileInfo;

import junit.framework.TestCase;

public class StaticFilesTests extends TestCase {

	private static final File here = new File("./res/test");
	private StaticFiles fileRoot;
	
	public void setUp() {
		fileRoot = new StaticFiles(here);
	}
	
	public void testExtensions() {
		final String[] paths = {"/blah/blah/blah.txt",
								"/abc.",
								"/no-extension-whatsoever",
								"/some.dir/something.zzz",
								"/some.dir/then-no-file-extension"};
		final String[] expected = {"txt",
								   "",
								   "",
								   "zzz",
								   ""};

		for (int i = 0; i < paths.length; i++) {
			final FileInfo info = fileRoot.getInfoForUrl(paths[i]);
			
			final String actual = info.getExtension();
			final String failureMessage = String.format("Expected \"%s\" to have extension \"%s\", but it was \"%s\".",
					paths[i], expected[i], actual);
			assertEquals(failureMessage, expected[i], actual);
		}
	}
	
	/* These Index finding tests depends on the file structure I set up in the res/test dir. */

	public void testIndexFileHtml() {
		final String requestedDir = "dir1";
		final String expectedFile = "index.html";
		verifyIndexFileInDir(requestedDir, expectedFile);
	}

	public void testIndexFileHtm() {
		final String requestedDir = "dir2";
		final String expectedFile = "index.htm";
		verifyIndexFileInDir(requestedDir, expectedFile);
	}

	public void testIndexFileNonexistent() {
		final String requestedDir = "dir3";
		final FileInfo dirInfo = fileRoot.getInfoForUrl(requestedDir);
		assertTrue(dirInfo.isDir());
		assertFalse(dirInfo.hasIndexFile());
	}
	
	private void verifyIndexFileInDir(final String requestedDir, final String expectedFile) {
		final FileInfo dirInfo = fileRoot.getInfoForUrl(requestedDir);
		assertTrue(dirInfo.isDir());
		assertTrue(dirInfo.hasIndexFile());
		
		final FileInfo indexInfo = dirInfo.getIndexFile();
		assertTrue(indexInfo.found());
		assertTrue(indexInfo.isFile());
		assertEquals(expectedFile, indexInfo.getPath().getName());
	}
}
