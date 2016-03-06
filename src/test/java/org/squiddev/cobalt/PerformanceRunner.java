package org.squiddev.cobalt;


import org.squiddev.cobalt.function.LuaFunction;
import org.squiddev.cobalt.function.ZeroArgFunction;
import org.squiddev.cobalt.lib.jse.JsePlatform;
import org.squiddev.cobalt.lib.platform.FileResourceManipulator;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;

import static org.squiddev.cobalt.Constants.NONE;

/**
 * Main file to test performance of compilers
 */
public class PerformanceRunner {
	public static boolean QUIET = true;

	public static void main(String[] args) throws Exception {
		int times = 5;
		boolean luaj = true;
		boolean cobalt = true;

		// Ugly parse arguments
		if (args.length > 0) {
			Queue<String> arg = new ArrayDeque<String>(Arrays.asList(args));
			String next;
			while ((next = arg.poll()) != null) {
				if (next.startsWith("--")) {
					next = next.substring(2);
				} else if (next.startsWith("-")) {
					next = next.substring(1);
				}
				if (next.equals("t") || next.equals("times")) {
					String number = arg.poll();
					if (number == null) throw new IllegalArgumentException();
					times = Integer.parseInt(number);
				} else if (next.equals("j") || next.equals("luaj")) {
					luaj = false;
				} else if (next.equals("c") || next.equals("cobalt")) {
					cobalt = false;
				} else if (next.equals("v") || next.equals("verbose")) {
					QUIET = false;
				} else if (next.equals("q") || next.equals("quiet")) {
					QUIET = true;
				} else if (next.equals("p") || next.equals("prompt")) {
					System.out.print("Waiting for key...");
					while (true) {
						int key = System.in.read();
						if (key == -1) throw new IOException("Hit EOF");
						if (key == '\n') break;
					}
				} else {
					System.out.print(
						"Args\n" +
							"  -t|--times <number> Run this n times\n" +
							"  -j|--luaj           Don't run LuaJ\n" +
							"  -c|--cobalt         Don't run Cobalt\n" +
							"  -v|--verbose        Verbose output\n" +
							"  -q|--quiet          Quiet output\n" +
							"  -p|--prompt         Prompt to begin"
					);
					return;
				}
			}
		}

		for (int i = 0; i < times; i++) {
			System.out.println("Iteration " + (i + 1) + "/" + times);

			if (luaj) executeLuaJ();
			if (cobalt) executeCobalt();
		}
	}

	public static void executeLuaJ() {
		System.out.println("LuaJ");
		org.luaj.vm2.LuaTable globals = org.luaj.vm2.lib.jse.JsePlatform.debugGlobals();

		if (QUIET) {
			globals.rawset("print", new org.luaj.vm2.lib.ZeroArgFunction() {
				@Override
				public org.luaj.vm2.LuaValue call() {
					System.out.print("#");
					return org.luaj.vm2.LuaValue.NONE;
				}
			});
		}

		try {
			InputStream aesStream = PerformanceRunner.class.getResourceAsStream("/aes/AesLua.lua");
			InputStream speedStream = PerformanceRunner.class.getResourceAsStream("/aes/AesSpeed.lua");

			long start = System.nanoTime();
			org.luaj.vm2.LuaFunction aes = org.luaj.vm2.LoadState.load(aesStream, "AesLua.lua", globals);
			org.luaj.vm2.LuaFunction speed = org.luaj.vm2.LoadState.load(speedStream, "AesSpeed.lua", globals);

			long compiled = System.nanoTime();

			aes.call();
			for (int i = 0; i < 10; i++) {
				speed.call();
			}

			long finished = System.nanoTime();

			System.out.printf("\n\tCompilation: %1$f\n\tRunning: %2$f\n", (compiled - start) / 1e9, (finished - compiled) / 1e9);
			System.out.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void executeCobalt() {
		System.out.println("Cobalt");
		LuaState state = new LuaState(new FileResourceManipulator());
		LuaTable globals = JsePlatform.debugGlobals(state);

		if (QUIET) {
			globals.rawset("print", new ZeroArgFunction() {
				@Override
				public LuaValue call(LuaState state) {
					System.out.print("#");
					return NONE;
				}
			});
		}

		try {
			InputStream aesStream = PerformanceRunner.class.getResourceAsStream("/aes/AesLua.lua");
			InputStream speedStream = PerformanceRunner.class.getResourceAsStream("/aes/AesSpeed.lua");

			long start = System.nanoTime();
			LuaFunction aes = LoadState.load(state, aesStream, "AesLua.lua", globals);
			LuaFunction speed = LoadState.load(state, speedStream, "AesSpeed.lua", globals);

			long compiled = System.nanoTime();

			aes.call(state);
			for (int i = 0; i < 10; i++) {
				speed.call(state);
			}

			long finished = System.nanoTime();

			System.out.printf("\n\tCompilation: %1$f\n\tRunning: %2$f\n", (compiled - start) / 1e9, (finished - compiled) / 1e9);
			System.out.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
