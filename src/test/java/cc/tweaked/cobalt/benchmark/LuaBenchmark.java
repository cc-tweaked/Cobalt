package cc.tweaked.cobalt.benchmark;

import org.openjdk.jmh.annotations.*;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaThread;
import org.squiddev.cobalt.Varargs;
import org.squiddev.cobalt.compiler.CompileException;
import org.squiddev.cobalt.compiler.LoadState;
import org.squiddev.cobalt.function.LuaFunction;
import org.squiddev.cobalt.lib.CoreLibraries;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(time = 3, timeUnit = TimeUnit.SECONDS)
@Measurement(time = 3, timeUnit = TimeUnit.SECONDS)
public abstract class LuaBenchmark {
	private final String program;
	private final Function<LuaState, Varargs> argsFactory;

	private LuaState state;
	private LuaFunction function;
	private Varargs args;

	protected LuaBenchmark(String program, Function<LuaState, Varargs> argsFactory) {
		this.program = program;
		this.argsFactory = argsFactory;
	}

	@Setup(Level.Iteration)
	public final void setup() throws LuaError, CompileException {
		state = LuaState.builder().build();
		CoreLibraries.standardGlobals(state);

		args = argsFactory.apply(state);
		function = LoadState.load(state, new ByteArrayInputStream(program.getBytes(StandardCharsets.UTF_8)), "=input", state.globals());
	}

	@Benchmark
	public final Varargs run() throws Exception {
		return LuaThread.runMain(state, function, args);
	}
}
