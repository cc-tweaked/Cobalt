package cc.tweaked.cobalt.benchmark;

import org.squiddev.cobalt.LuaTable;
import org.squiddev.cobalt.ValueFactory;

/**
 * Creates a table mapping strings to numbers, then loops over adding them all up.
 */
public class SumPairs extends LuaBenchmark {
	public SumPairs() {
		super("""
			local tbl = ...
			local sum = 0
			for k, v in pairs(tbl) do sum = sum + v end
			return sum
			""", state -> {
			var tbl = new LuaTable();
			for (int i = 1; i < 256; i++) tbl.rawset("key_" + i, ValueFactory.valueOf(i));
			return tbl;
		});
	}
}
