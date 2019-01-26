package org.squiddev.cobalt;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.concurrent.TimeUnit;

import static org.squiddev.cobalt.ValueFactory.valueOf;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(jvmArgsAppend = {"-server", "-disablesystemassertions"})
public class PerformanceBenchmark {
	@State(Scope.Thread)
	public static class ScriptScope {
		final ScriptDrivenHelpers helpers = new ScriptDrivenHelpers("/perf/");

		@Setup(Level.Iteration)
		public void setup() {
			helpers.setupQuiet();
		}
	}

	@Benchmark
	public void binarytrees(ScriptScope scope) throws Exception {
		scope.helpers.loadScript("binarytrees").call(scope.helpers.state, valueOf(10));
	}

	@Benchmark
	public void fannkuch(ScriptScope scope) throws Exception {
		scope.helpers.loadScript("fannkuch").call(scope.helpers.state, valueOf(8));
	}

	@Benchmark
	public void nbody(ScriptScope scope) throws Exception {
		scope.helpers.loadScript("nbody").call(scope.helpers.state, valueOf(50000));
	}

	@Benchmark
	public void nsieve(ScriptScope scope) throws Exception {
		scope.helpers.loadScript("nsieve").call(scope.helpers.state, valueOf(8));
	}

	public static void main(String... args) throws RunnerException {
		Options opts = new OptionsBuilder()
			.include("org.squiddev.cobalt.PerformanceBenchmark.*")
			.warmupIterations(5)
			.measurementIterations(5)
			.measurementTime(TimeValue.milliseconds(12000))
			.jvmArgsPrepend("-server")
			.forks(3)
			.build();
		new Runner(opts).run();
	}
}
