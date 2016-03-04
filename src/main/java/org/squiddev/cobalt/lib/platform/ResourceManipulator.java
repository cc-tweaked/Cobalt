/*
 * ****************************************************************************
 * Original Source: Copyright (c) 2009-2011 Luaj.org. All rights reserved.
 * Modifications: Copyright (c) 2015-2016 SquidDev
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

import org.squiddev.cobalt.lib.BaseLib;

import java.io.IOException;
import java.io.InputStream;

/**
 * Interface for manipulating files and resources
 */
public interface ResourceManipulator {
	/**
	 * Prefix for temporary file names
	 */
	String TMP_PREFIX = ".luaj";

	/**
	 * Suffix for temporary file names
	 */
	String TMP_SUFFIX = "tmp";

	/**
	 * return code indicating the execute() threw an I/O exception
	 */
	int EXEC_IOEXCEPTION = 1;

	/**
	 * return code indicating the execute() was interrupted
	 */
	int EXEC_INTERRUPTED = -2;

	/**
	 * return code indicating the execute() threw an unknown exception
	 */
	int EXEC_ERROR = -3;

	/**
	 * Try to open a file, or return null if not found.
	 *
	 * @param filename Filename to open
	 * @return InputStream, or null if not found.
	 * @see BaseLib
	 */
	InputStream findResource(String filename);

	/**
	 * This function is equivalent to the C function system.
	 * It passes command to be executed by an operating system shell.
	 * It returns a status code, which is system-dependent.
	 * If command is absent, then it returns nonzero if a shell
	 * is available and zero otherwise.
	 *
	 * @param command command to pass to the system
	 * @return The command's exit code
	 */
	int execute(String command);

	/**
	 * Renames file or directory named oldname to newname.
	 * If this function fails,it throws and IOException
	 *
	 * @param from old file name
	 * @param to   new file name
	 * @throws IOException if it fails
	 */
	void rename(String from, String to) throws IOException;

	/**
	 * Deletes the file or directory with the given name.
	 * Directories must be empty to be removed.
	 * If this function fails, it throws and IOException
	 *
	 * @param file The filename to delete
	 * @throws IOException if it fails
	 */
	void remove(String file) throws IOException;

	/**
	 * Returns a string with a file name that can be used for a temporary file.
	 * The file must be explicitly opened before its use and explicitly removed
	 * when no longer needed.
	 *
	 * On some systems (POSIX), this function also creates a file with that name,
	 * to avoid security risks. (Someone else might create the file with wrong
	 * permissions in the time between getting the name and creating the file.)
	 * You still have to open the file to use it and to remove it (even if you
	 * do not use it).
	 *
	 * @return String filename to use
	 * @throws IOException If the file name cannot be generated
	 */
	String tmpName() throws IOException;
}
