package org.squiddev.cobalt;

abstract sealed class DepthVarargs extends Varargs permits DepthVarargs.ArrayVarargs, DepthVarargs.SubVarargs, DepthVarargs.PairVarargs {
	protected final int depth;

	protected DepthVarargs(int depth) {
		this.depth = depth;
	}

	public static int depth(Varargs varargs) {
		return varargs instanceof DepthVarargs ? ((DepthVarargs) varargs).depth : 1;
	}

	/**
	 * Implementation of Varargs for use in the Varargs.subargs() function.
	 *
	 * @see Varargs#subargs(int)
	 */
	static final class SubVarargs extends DepthVarargs {
		private final Varargs v;
		private final int start;
		private final int end;

		public SubVarargs(Varargs varargs, int start, int end) {
			super(depth(varargs));
			this.v = varargs;
			this.start = start;
			this.end = end;
		}

		@Override
		public LuaValue arg(int i) {
			i += start - 1;
			return i >= start && i <= end ? v.arg(i) : Constants.NIL;
		}

		@Override
		public LuaValue first() {
			return v.arg(start);
		}

		@Override
		public void fill(LuaValue[] array, int offset) {
			int size = end + 1 - start;
			for (int i = 0; i < size; i++) array[offset + i] = v.arg(start + i);
		}

		@Override
		public int count() {
			return end + 1 - start;
		}
	}

	/**
	 * Varargs implementation backed by an array of LuaValues
	 * <p>
	 * This is an internal class not intended to be used directly.
	 * Instead use the corresponding static methods on LuaValue.
	 *
	 * @see ValueFactory#varargsOf(LuaValue[])
	 */
	static final class ArrayVarargs extends DepthVarargs {
		private final LuaValue[] v;
		private final Varargs r;

		/**
		 * Construct a Varargs from an array of LuaValue.
		 * <p>
		 * This is an internal class not intended to be used directly.
		 * Instead use the corresponding static methods on LuaValue.
		 *
		 * @param v The initial values
		 * @param r Remaining arguments
		 * @see ValueFactory#varargsOf(LuaValue[])
		 */
		public ArrayVarargs(LuaValue[] v, Varargs r) {
			super(depth(r) + 1);
			this.v = v;
			this.r = r;
		}

		@Override
		public LuaValue arg(int i) {
			return i >= 1 && i <= v.length ? v[i - 1] : r.arg(i - v.length);
		}

		@Override
		public int count() {
			return v.length + r.count();
		}

		@Override
		public LuaValue first() {
			return v.length > 0 ? v[0] : r.first();
		}

		@Override
		public void fill(LuaValue[] array, int offset) {
			System.arraycopy(v, 0, array, offset, v.length);
			r.fill(array, offset + v.length);
		}
	}

	/**
	 * Varargs implementation backed by two values.
	 * <p>
	 * This is an internal class not intended to be used directly.
	 * Instead use the corresponding static method on LuaValue.
	 *
	 * @see ValueFactory#varargsOf(LuaValue, Varargs)
	 */
	static final class PairVarargs extends DepthVarargs {
		private final LuaValue v1;
		private final Varargs v2;

		/**
		 * Construct a Varargs from an two LuaValues.
		 * <p>
		 * This is an internal class not intended to be used directly.
		 * Instead use the corresponding static method on LuaValue.
		 *
		 * @param v1 First argument
		 * @param v2 Remaining arguments
		 * @see ValueFactory#varargsOf(LuaValue, Varargs)
		 */
		public PairVarargs(LuaValue v1, Varargs v2) {
			super(depth(v2) + 1);
			this.v1 = v1;
			this.v2 = v2;
		}

		@Override
		public LuaValue arg(int i) {
			return i == 1 ? v1 : v2.arg(i - 1);
		}

		@Override
		public int count() {
			return 1 + v2.count();
		}

		@Override
		public LuaValue first() {
			return v1;
		}

		@Override
		public void fill(LuaValue[] array, int offset) {
			array[offset] = v1;
			v2.fill(array, offset + 1);
		}
	}
}
