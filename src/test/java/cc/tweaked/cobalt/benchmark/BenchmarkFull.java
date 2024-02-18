package cc.tweaked.cobalt.benchmark;

import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.runner.options.WarmupMode;

/**
 * Runs all benchmarks within the {@code cc.tweaked.cobalt.benchmark} package.
 */
public final class BenchmarkFull {
	private BenchmarkFull() {
	}

	public static void main(String... args) throws RunnerException {
		Options opts = new OptionsBuilder()
			.include("cc.tweaked.cobalt.benchmark.*")
			.exclude("cc.tweaked.cobalt.benchmark.WarmupBenchmarks.*")
			.includeWarmup("cc.tweaked.cobalt.benchmark.WarmupBenchmarks.*")
			.warmupIterations(3)
			.warmupTime(TimeValue.milliseconds(1000))
			.measurementIterations(5)
			.measurementTime(TimeValue.milliseconds(3000))
			.jvmArgsPrepend("-server")
			.resultFormat(ResultFormatType.JSON)
			.forks(3)
			.build();
		new Runner(opts).run();
	}
}
