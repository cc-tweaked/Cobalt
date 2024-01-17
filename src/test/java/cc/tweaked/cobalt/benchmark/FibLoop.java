package cc.tweaked.cobalt.benchmark;

import org.squiddev.cobalt.ValueFactory;

/**
 * Evaluates the nth Fibonacci number using a loop.
 */
public class FibLoop extends LuaBenchmark {
	public FibLoop() {
		super("""
			local n = ...
			local a, b = 1, 1 for i = 1, n do a, b = a + b, a end
			return a
			""", state -> ValueFactory.valueOf(200)
		);
	}
}
