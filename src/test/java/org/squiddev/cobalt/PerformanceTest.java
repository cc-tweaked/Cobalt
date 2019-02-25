package org.squiddev.cobalt;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.squiddev.cobalt.compiler.CompileException;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class PerformanceTest {
	private static final int TOTAL = Integer.parseInt(System.getProperty("cobalt.perfTotal", "1"));
	private static final int DISCARD = Integer.parseInt(System.getProperty("cobalt.perfDiscard", "0"));

	private final String name;
	private final ScriptDrivenHelpers helpers = new ScriptDrivenHelpers("/perf/");

	public PerformanceTest(String name) {
		this.name = name;
	}

	@Before
	public void setup() {
		helpers.setupQuiet();
	}

	@Parameterized.Parameters(name = "{0}")
	public static Collection<Object[]> getTests() {
		Object[][] tests = {
			{"binarytrees"},
			{"fannkuch"},
			{"nbody"},
			{"nsieve"},
			{"primes"},
		};

		return Arrays.asList(tests);
	}

	@Test()
	public void run() throws IOException, CompileException, LuaError, InterruptedException {
		System.out.println("[" + name + "]");

		for (int i = 0; i < TOTAL; i++) {
			long start = System.nanoTime();
			LuaThread.runMain(helpers.state, helpers.loadScript(name));
			long finish = System.nanoTime();

			if (i >= DISCARD) System.out.println("  Took " + (finish - start) / 1.0e9);
		}
	}
}
