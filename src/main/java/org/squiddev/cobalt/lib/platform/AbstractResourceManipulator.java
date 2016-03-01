package org.squiddev.cobalt.lib.platform;

import java.io.IOException;

/**
 * A resource manipulator where nothing is implemented
 */
public abstract class AbstractResourceManipulator implements ResourceManipulator {
	public int latest;

	@Override
	public int execute(String command) {
		return 0;
	}

	@Override
	public void rename(String from, String to) throws IOException {
		throw new IOException("not implemented");
	}

	@Override
	public void remove(String file) throws IOException {
		throw new IOException("not implemented");
	}

	@Override
	public String tmpName() throws IOException {
		return TMP_PREFIX + (latest++) + TMP_SUFFIX;
	}
}
