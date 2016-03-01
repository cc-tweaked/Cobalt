package org.luaj.vm2.lib.platform;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A resource manipulator that accesses the file system
 */
public class FileResourceManipulator implements ResourceManipulator {
	@Override
	public InputStream findResource(String filename) {
		File f = new File(filename);
		if (!f.exists()) {
			Class c = getClass();
			return c.getResourceAsStream(filename.startsWith("/") ? filename : "/" + filename);
		}

		try {
			return new FileInputStream(f);
		} catch (IOException ioe) {
			return null;
		}
	}

	@Override
	public int execute(String command) {
		Runtime r = Runtime.getRuntime();
		try {
			final Process p = r.exec(command);
			try {
				p.waitFor();
				return p.exitValue();
			} finally {
				p.destroy();
			}
		} catch (IOException ioe) {
			return EXEC_IOEXCEPTION;
		} catch (InterruptedException e) {
			return EXEC_INTERRUPTED;
		} catch (Throwable t) {
			return EXEC_ERROR;
		}
	}

	@Override
	public void rename(String from, String to) throws IOException {
		File f = new File(from);
		if (!f.exists()) {
			throw new IOException("No such file or directory");
		}
		if (!f.renameTo(new File(to))) {
			throw new IOException("Failed to rename");
		}
	}

	@Override
	public void remove(String file) throws IOException {
		File f = new File(file);
		if (!f.exists()) {
			throw new IOException("No such file or directory");
		}
		if (!f.delete()) {
			throw new IOException("Failed to delete");
		}
	}

	@Override
	public String tmpName() throws IOException {
		File f = File.createTempFile(TMP_PREFIX, TMP_SUFFIX);
		f.deleteOnExit();
		return f.getName();
	}
}
