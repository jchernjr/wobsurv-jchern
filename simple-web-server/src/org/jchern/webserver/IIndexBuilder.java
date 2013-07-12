package org.jchern.webserver;

import java.io.File;

/**
 * Generates an HTML index listing for a given directory, using some character encoding.
 * The resulting IGeneratedIndex instance provides the generated content, as well as its length and encoding.
 * @author jchern
 *
 */
public interface IIndexBuilder {
	public IGeneratedIndex buildIndexForDir(final File dir);
}
