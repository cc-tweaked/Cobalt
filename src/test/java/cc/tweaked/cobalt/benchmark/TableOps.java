package cc.tweaked.cobalt.benchmark;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.squiddev.cobalt.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static org.squiddev.cobalt.Constants.NIL;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(time = 1, timeUnit = TimeUnit.SECONDS)
public class TableOps {
	/**
	 * The number of keys we'll attempt to access.
	 */
	private static final int KEY_COUNT = 100;

	@Param({"5", "20", "100"})
	private int tableSize;

	/**
	 * A dense integer-indexed table.
	 */
	private LuaTable denseIntTable;

	/**
	 * A sparse integer-indexed table.
	 */
	private LuaTable sparseIntTable;

	/**
	 * A string-string table.
	 */
	private LuaTable stringTable;

	/**
	 * A table indexed by a mixture of strings and tables.
	 */
	private LuaTable mixedHashTable;

	/**
	 * A random collection of keys from {@link #mixedHashTable}.
	 */
	private final LuaValue[] mixedKeysHit = new LuaValue[KEY_COUNT];

	/**
	 * A random collection of keys not in {@link #mixedHashTable}
	 */
	private final LuaValue[] mixedKeysMiss = new LuaValue[KEY_COUNT];

	@Setup
	public void setup() throws LuaError {
		denseIntTable = new LuaTable();
		for (int i = 0; i < tableSize; i++) denseIntTable.rawset(i + 1, ValueFactory.valueOf("v" + i));

		var random = new Random();

		sparseIntTable = new LuaTable();
		for (int i = 0; i < tableSize; i++) {
			sparseIntTable.rawset(random.nextInt(1, 10_000), ValueFactory.valueOf("v" + i));
		}

		stringTable = new LuaTable();
		for (int i = 0; i < tableSize; i++) stringTable.rawset("k" + i, ValueFactory.valueOf("v" + i));

		List<LuaValue> keys = new ArrayList<>(tableSize);
		mixedHashTable = new LuaTable();
		for (int i = 0; i < Math.max(1, tableSize / 2); i++) {
			keys.add(new LuaTable());
			keys.add(ValueFactory.valueOf("k" + i));
		}
		for (int i = 0; i < keys.size(); i++) mixedHashTable.rawset(keys.get(i), ValueFactory.valueOf("v" + i));

		for (int i = 0; i < KEY_COUNT; i++) {
			mixedKeysHit[i] = keys.get(random.nextInt(keys.size()));
			mixedKeysMiss[i] = random.nextBoolean() ? new LuaTable() : ValueFactory.valueOf("u" + i);
		}
	}

	private int consumeTableWithNext(Blackhole bh, LuaTable table) throws LuaError {
		int count = 0;
		LuaValue k = NIL;
		while (true) {
			Varargs n = table.next(k);
			if ((k = n.first()).isNil()) break;
			bh.consume(k);
			count++;
		}

		if (count != tableSize) throw new IllegalStateException(count + " != expected size " + tableSize);
		return count;
	}

	private int consumeTableWithLength(Blackhole bh, LuaTable table) {
		int count = table.length();
		for (int i = 1; i <= count; i++) bh.consume(table.rawget(i));
		if (count != tableSize) throw new IllegalStateException(count + " != expected size " + tableSize);
		return count;
	}

	@Benchmark
	public int denseIntTableLength() {
		return denseIntTable.length();
	}

	@Benchmark
	public int denseIntTableConsumeWithNext(Blackhole bh) throws LuaError {
		return consumeTableWithNext(bh, denseIntTable);
	}

	@Benchmark
	public int denseIntTableConsumeWithLength(Blackhole bh) throws LuaError {
		return consumeTableWithLength(bh, denseIntTable);
	}

	@Benchmark
	public int sparseIntTableConsumeWithNext(Blackhole bh) throws LuaError {
		return consumeTableWithNext(bh, sparseIntTable);
	}

	@Benchmark
	public int stringTableConsumeWithNext(Blackhole bh) throws LuaError {
		return consumeTableWithNext(bh, stringTable);
	}

	@Benchmark
	public int mixedTableConsumeWithNext(Blackhole bh) throws LuaError {
		return consumeTableWithNext(bh, mixedHashTable);
	}

	@Benchmark
	@OperationsPerInvocation(KEY_COUNT)
	public void mixedTableFetchKeysHit(Blackhole bh) {
		for (LuaValue key : mixedKeysHit) bh.consume(mixedHashTable.rawget(key));
	}

	@Benchmark
	@OperationsPerInvocation(KEY_COUNT)
	public void mixedTableFetchKeysMiss(Blackhole bh) {
		for (LuaValue key : mixedKeysMiss) bh.consume(mixedHashTable.rawget(key));
	}
}
