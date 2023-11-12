package cc.tweaked.cobalt.memory;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaThread;
import org.squiddev.cobalt.compiler.CompileException;
import org.squiddev.cobalt.compiler.LoadState;
import org.squiddev.cobalt.lib.CoreLibraries;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

import static java.time.temporal.ChronoField.*;

public class MemoryCounterBenchmark {
	@State(Scope.Thread)
	public static abstract class MemoryBenchmark {
		LuaState state;

		public MemoryBenchmark(String contents) {
			var state = this.state = new LuaState();
			try {
				CoreLibraries.debugGlobals(state);
				var function = LoadState.load(
					state, new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8)), "@program.lua",
					state.globals()
				);
				LuaThread.runMain(state, function);
			} catch (LuaError | CompileException e) {
				throw new RuntimeException(e);
			}
		}

		@Benchmark
		public long run() {
			return MemoryCounter.count(state);
		}
	}

	public static class LongTable extends MemoryBenchmark {
		public LongTable() {
			super("""
				local tbl = {}
				for i = 1, 1e5 do tbl[i] = {} end
				_G.tbl = tbl""");
		}
	}

	public static class DeepTable extends MemoryBenchmark {
		public DeepTable() {
			super("""
				local tbl = {}
				for i = 1, 1e5 do tbl = {tbl} end
				_G.tbl = tbl""");
		}
	}

	public static class DeepTableMixed extends MemoryBenchmark {
		public DeepTableMixed() {
			super("""
				local tbl = {}
				for i = 1, 1e5 do tbl = {tbl, 1} end
				_G.tbl = tbl""");
		}
	}

	public static void main(String... args) throws RunnerException {
		Options opts = new OptionsBuilder()
			.include(MemoryCounterBenchmark.class.getName() + ".*")
			.warmupIterations(3)
			.measurementIterations(5)
			.measurementTime(TimeValue.seconds(5))
			.forks(1)
			.resultFormat(ResultFormatType.JSON)
			.result(LocalDateTime.now().format(new DateTimeFormatterBuilder()
				.appendLiteral("jmh_")
				.append(DateTimeFormatter.ISO_LOCAL_DATE)
				.appendLiteral('_')
				.appendValue(HOUR_OF_DAY, 2)
				.appendValue(MINUTE_OF_HOUR, 2)
				.appendValue(SECOND_OF_MINUTE, 2)
				.appendLiteral(".json")
				.toFormatter()
			))
			.build();
		new Runner(opts).run();
	}
}
