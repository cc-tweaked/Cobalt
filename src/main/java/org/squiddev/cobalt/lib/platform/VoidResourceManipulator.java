package org.squiddev.cobalt.lib.platform;

import java.io.IOException;
import java.io.InputStream;

/**
 * A resource manipulator which errors on any action.
 */
public class VoidResourceManipulator implements ResourceManipulator {
	@Override
	public InputStream findResource(String filename) {
		return null;
	}

	@Override
	public int execute(String command) {
		return 1;
	}

	@Override
	public void rename(String from, String to) throws IOException {
		throw new IOException("file could not be renamed");
	}

	@Override
	public void remove(String file) throws IOException {
		throw new IOException("file could not be removed");
	}

	@Override
	public String tmpName() throws IOException {
		throw new IOException("cannot create temporary file");
	}
}
