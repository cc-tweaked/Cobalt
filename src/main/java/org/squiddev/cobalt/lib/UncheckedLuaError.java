package org.squiddev.cobalt.lib;

import org.squiddev.cobalt.LuaError;

/**
 * An unchecked version of {@link org.squiddev.cobalt.LuaError}.
 *
 * This should only be used when you need to propagate across a Java call boundary (say within
 * an {@link java.io.InputStream}.
 *
 * @see org.squiddev.cobalt.LuaError#wrap(Exception)
 */
public final class UncheckedLuaError extends RuntimeException {
	private static final long serialVersionUID = -2431451026200110553L;

	public UncheckedLuaError(LuaError error) {
		super(error);
	}

	@Override
	public LuaError getCause() {
		return (LuaError) super.getCause();
	}
}
