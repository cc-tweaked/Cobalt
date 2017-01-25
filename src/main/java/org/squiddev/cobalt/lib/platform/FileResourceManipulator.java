/*
 * ****************************************************************************
 * Original Source: Copyright (c) 2009-2011 Luaj.org. All rights reserved.
 * Modifications: Copyright (c) 2015-2017 SquidDev
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * ****************************************************************************
 */

package org.squiddev.cobalt.lib.platform;

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
			Class<?> c = getClass();
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
