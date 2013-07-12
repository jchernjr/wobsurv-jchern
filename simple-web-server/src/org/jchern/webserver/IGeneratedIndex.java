package org.jchern.webserver;

import java.io.InputStream;

/**
 * Returns the content of the generated index as an InputStream.
 * 
 * Also reports:
 * - the effective modified time,
 * - the character encoding used,
 * - the length of the generated index, encoded with this charset.
 * @author jchern
 *
 */
public interface IGeneratedIndex {
	public InputStream getContent();
	public long getModifiedTime();
	public long getContentLength();
	public String getEncoding();
}
