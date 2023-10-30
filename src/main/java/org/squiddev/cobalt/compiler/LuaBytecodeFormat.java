package org.squiddev.cobalt.compiler;

import org.squiddev.cobalt.LuaString;
import org.squiddev.cobalt.Prototype;
import org.squiddev.cobalt.unwind.SuspendedFunction;
import org.squiddev.cobalt.unwind.SuspendedTask;

import java.io.IOException;
import java.io.OutputStream;

public final class LuaBytecodeFormat implements BytecodeFormat {
	/**
	 * Signature byte indicating the file is a compiled binary chunk
	 */
	static final byte[] LUA_SIGNATURE = {27, 'L', 'u', 'a'};

	/**
	 * The current Lua bytecode format, currently Lua 5.1
	 */
	static final int LUAC_VERSION = 0x51;

	/**
	 * The format for binary files. 0 denotes the "official" format.
	 */
	static final int LUAC_FORMAT = 0;


	private static final LuaBytecodeFormat INSTANCE = new LuaBytecodeFormat();

	private LuaBytecodeFormat() {
	}

	public static LuaBytecodeFormat instance() {
		return INSTANCE;
	}

	@Override
	public void writeFunction(OutputStream output, Prototype function, boolean strip) throws IOException {
		BytecodeDumper.dump(function, output, strip);
	}

	@Override
	public SuspendedFunction<Prototype> readFunction(LuaString name, InputReader input) {
		var loader = new BytecodeLoader(input);

		return SuspendedTask.toFunction(() -> {
			try {
				loader.checkSignature();
				loader.loadHeader();
				return loader.loadFunction(LoadState.getSourceName(name));
			} catch (CompileException e) {
				throw throwUnchecked0(e);
			}
		});
	}

	@SuppressWarnings("unchecked")
	private static <T extends Throwable> T throwUnchecked0(Throwable t) throws T {
		throw (T) t;
	}
}
