package cc.tweaked.cobalt.benchmark;

import org.squiddev.cobalt.ValueFactory;

/**
 * Evaluates the nth Fibonacci number using a recursive function.
 */
public class FibRecursive extends LuaBenchmark {
	public FibRecursive() {
		super("""
			local function fib(n)
				if n <= 2 then return 1 end
				return fib(n - 1) + fib(n - 2)
			end
			return fib(...)
			""", state -> ValueFactory.valueOf(10)
		);
	}
}
