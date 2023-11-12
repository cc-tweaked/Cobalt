package cc.tweaked.cobalt.memory;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaString;
import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.MarkedLuaValue;

import java.util.*;

public final class MemoryCounter {
	private final byte mark;
	private long used;
	private final Deque<AllocatedObject> toVisit = new ArrayDeque<>(128);
	private final Set<AllocatedObject> visited = Collections.newSetFromMap(new IdentityHashMap<>(1024));

	private MemoryCounter(byte mark) {
		this.mark = mark;
	}

	private void maybeTraceAllocated(AllocatedObject object, int depth) {
		if (visited.add(object)) doTraceAllocated(object, depth);
	}

	private void doTraceAllocated(AllocatedObject object, int depth) {
		if (depth == 0) {
			toVisit.addLast(object);
		} else {
			long size = object.traceObject(this, depth - 1);
			used += size;
		}
	}

	private void maybeTraceMarked(MarkedObject object, int depth) {
		if (object.markObject(mark)) doTraceMarked(object, depth);
	}

	private void doTraceMarked(MarkedObject object, int depth) {
		if (depth == 0) {
			toVisit.addLast(object);
		} else {
			long size = object.traceObject(this, depth - 1);
			used += size;
		}
	}

	public void trace(@Nullable AllocatedObject object, int depth) {
		if (object == null) return;
		if (object instanceof MarkedObject marked) {
			maybeTraceMarked(marked, depth);
		} else {
			maybeTraceAllocated(object, depth);
		}
	}

	public void traceValue(LuaValue value, int depth) {
		if (value instanceof MarkedLuaValue marked) {
			maybeTraceMarked(marked, depth);
		} else if (value instanceof LuaString string) {
			maybeTraceAllocated(string, depth);
		} else {
			assert !(value instanceof AllocatedObject);
		}
	}

	public void traceAnything(Object value, int depth) {
		if (value instanceof AllocatedObject object) trace(object, depth);
	}

	public static long count(LuaState state) {
		var counter = new MemoryCounter(state.lastMask ^= 1);

		counter.trace(state, 10);

		AllocatedObject object;
		while ((object = counter.toVisit.pollLast()) != null) {
			long size = object.traceObject(counter, 8);
			counter.used += size;
		}

		return counter.used;
	}
}
