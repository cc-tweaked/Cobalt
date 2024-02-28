package cc.tweaked.cobalt.benchmark;

import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Runs all benchmarks with a minimal number of warmup runs and forks, allowing for quick testing.
 */
public final class BenchmarkFast {
	private BenchmarkFast() {
	}

	public static void main(String... args) throws RunnerException {
		Options opts = new OptionsBuilder()
			.include("cc.tweaked.cobalt.benchmark.*")
			.warmupIterations(1)
			.measurementIterations(2)
			.jvmArgsPrepend("-server")
			.resultFormat(ResultFormatType.JSON)
			.forks(1)
			.build();
		new Runner(opts).run();
	}
}
