package cc.tweaked.cobalt.benchmark;

import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Warmup;
import org.squiddev.cobalt.Constants;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Generates a Lua program which attempts to execute all parts of the Lua interpreter, ensuring that our actual
 * benchmarks don't overspecialise for a specific case.
 */
@Warmup(time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 0)
public class WarmupBenchmarks extends LuaBenchmark {
	public WarmupBenchmarks() {
		super(makeFile(), s -> Constants.NONE);
	}

	private static String makeFile() {
		var out = new StringBuilder();
		out.append("local x, y, _ = 5.2, '123'\n");

		// Arithmetic and Lua operations
		for (var op : List.of("+", "-", "*", "/", "%", "^", "..")) {
			addOperation(out, "x", op, "x");
			addOperation(out, "x", op, "y");
			addOperation(out, "x", op, "0");
			addOperation(out, "0", op, "x");
		}

		// Comparison operations
		for (var op : List.of("==", "<", "<=")) {
			addOperation(out, "x", op, "x");
			addOperation(out, "x", op, "0");
			addOperation(out, "0", op, "x");

			addOperation(out, "y", op, "y");
			addOperation(out, "y", op, "''");
			addOperation(out, "''", op, "y");
		}

		// Basic control flow
		out.append("for i = 1, 10 do _ = 0 end\n");
		out.append("for k in pairs(_G) do _ = _G[k] end\n");

		return out.toString();
	}

	private static void addOperation(StringBuilder out, String lhs, String op, String rhs) {
		out.append("_ = ").append(lhs).append(" ").append(op).append(" ").append(rhs).append("\n");
	}
}
