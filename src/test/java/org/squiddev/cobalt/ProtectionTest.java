package org.squiddev.cobalt;

import org.junit.Before;
import org.junit.Test;
import org.squiddev.cobalt.debug.DebugHandler;

import java.io.IOException;

/**
 * Tests protection
 */
public class ProtectionTest {
	private final ScriptDrivenHelpers helpers = new ScriptDrivenHelpers("/");

	@Before
	public void setup() {
		helpers.setup();

		long time = System.currentTimeMillis();
		helpers.state.debug = new DebugHandler(helpers.state) {
			@Override
			public void poll() {
				if (System.currentTimeMillis() > time + 500) throw new LuaError("Timed out");
			}
		};
	}

	@Test(timeout = 2000)
	public void run() throws IOException {
		helpers.loadScript("protection").call(helpers.state);
	}
}
