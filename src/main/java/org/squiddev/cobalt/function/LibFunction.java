/*
 * The MIT License (MIT)
 *
 * Original Source: Copyright (c) 2009-2011 Luaj.org. All rights reserved.
 * Modifications: Copyright (c) 2015-2020 SquidDev
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.squiddev.cobalt.function;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.debug.DebugFrame;
import org.squiddev.cobalt.debug.DebugState;
import org.squiddev.cobalt.lib.BaseLib;
import org.squiddev.cobalt.lib.TableLib;
import org.squiddev.cobalt.unwind.SuspendedFunction;
import org.squiddev.cobalt.unwind.SuspendedTask;

/**
 * Subclass of {@link LuaFunction} common to Java functions exposed to lua.
 * <p>
 * To provide for common implementations in JME and JSE,
 * library functions are typically grouped on one or more library classes
 * and an opcode per library function is defined and used to key the switch
 * to the correct function within the library.
 * <p>
 * Since lua functions can be called with too few or too many arguments,
 * and there are overloaded {@link LuaFunction#call(LuaState)} functions with varying
 * number of arguments, a Java function exposed in lua needs to handle  the
 * argument fixup when a function is called with a number of arguments
 * differs from that expected.
 * <p>
 * To simplify the creation of library functions,
 * there are 5 direct subclasses to handle common cases based on number of
 * argument values and number of return return values.
 * <ul>
 * <li>{@link ZeroArgFunction}</li>
 * <li>{@link OneArgFunction}</li>
 * <li>{@link TwoArgFunction}</li>
 * <li>{@link ThreeArgFunction}</li>
 * <li>{@link VarArgFunction}</li>
 * </ul>
 * <p>
 * To be a Java library that can be loaded via {@code require}, it should have
 * a public constructor that returns a {@link LuaValue} that, when executed,
 * initializes the library.
 * <p>
 * For example, the following code will implement a library called "hyperbolic"
 * with two functions, "sinh", and "cosh":
 * <pre> {@code
 * import org.luaj.vm2.LuaValue;
 * import org.luaj.vm2.lib.OneArgFunction;
 *
 * public class hyperbolic extends OneArgFunction {
 *
 * 	public hyperbolic() {}
 *
 * 	public LuaValue call(LuaValue libname) {
 * 		LuaValue library = tableOf();
 * 		library.set( "sinh", new sinh() );
 * 		library.set( "cosh", new cosh() );
 * 		env.set( "hyperbolic", library );
 * 		return library;
 *  }
 *
 * 	static class sinh extends OneArgFunction {
 * 		public LuaValue call(LuaValue x) {
 * 			return LuaValue.valueOf(Math.sinh(x.checkdouble()));
 *      }
 *  }
 *
 * 	static class cosh extends OneArgFunction {
 * 		public LuaValue call(LuaValue x) {
 * 			return LuaValue.valueOf(Math.cosh(x.checkdouble()));
 *      }
 *  }
 * }
 * }</pre>
 * The default constructor is used to instantiate the library
 * in response to {@code require 'hyperbolic'} statement,
 * provided it is on Javas class path.
 * This instance is then invoked with the name supplied to require()
 * as the only argument, and library should initialized whatever global
 * data it needs to and place it into the environment if needed.
 * In this case, it creates two function, 'sinh', and 'cosh', and puts
 * them into a global table called 'hyperbolic.'
 * <p>
 * To test it, a script such as this can be used:
 * <pre> {@code
 * local t = require('hyperbolic')
 * print( 't', t )
 * print( 'hyperbolic', hyperbolic )
 * for k,v in pairs(t) do
 * 	print( 'k,v', k,v )
 * end
 * print( 'sinh(.5)', hyperbolic.sinh(.5) )
 * print( 'cosh(.5)', hyperbolic.cosh(.5) )
 * }</pre>
 * <p>
 * It should produce something like:
 * <pre> {@code
 * t    table: 3dbbd23f
 * hyperbolic	table: 3dbbd23f
 * k,v	cosh	function: 3dbbd128
 * k,v	sinh	function: 3dbbd242
 * sinh(.5)	0.5210953
 * cosh(.5)	1.127626
 * }</pre>
 * <p>
 * See the source code in any of the library functions
 * such as {@link BaseLib} or {@link TableLib} for other examples.
 */
public sealed abstract class LibFunction extends LuaFunction
	permits ZeroArgFunction, OneArgFunction, TwoArgFunction, ThreeArgFunction, VarArgFunction {
	/**
	 * The common name for this function, useful for debugging.
	 * <p>
	 * Binding functions initialize this to the name to which it is bound.
	 */
	@Nullable String name;

	/**
	 * Default constructor for use by subclasses
	 */
	LibFunction() {
	}

	@Override
	public String debugName() {
		return name != null ? name : super.toString();
	}


	/**
	 * Call {@code this} with 0 arguments, including metatag processing,
	 * and return only the first return value.
	 * <p>
	 * If {@code this} is a {@link LuaFunction}, call it,
	 * and return only its first return value, dropping any others.
	 * Otherwise, look for the {@link Constants#CALL} metatag and call that.
	 * <p>
	 * If the return value is a {@link Varargs}, only the 1st value will be returned.
	 * To get multiple values, use {@link #invoke(LuaState, Varargs)} instead.
	 *
	 * @param state The current lua state
	 * @return First return value {@code (this())}, or {@link Constants#NIL} if there were none.
	 * @throws LuaError        If the invoked function throws an error.
	 * @throws UnwindThrowable If this function transfers control to another coroutine.
	 * @see #call(LuaState, LuaValue)
	 * @see #call(LuaState, LuaValue, LuaValue)
	 * @see #call(LuaState, LuaValue, LuaValue, LuaValue)
	 * @see #invoke(LuaState, Varargs)
	 */
	protected abstract LuaValue call(LuaState state) throws LuaError, UnwindThrowable;

	/**
	 * Call {@code this} with 1 argument, including metatag processing,
	 * and return only the first return value.
	 * <p>
	 * If {@code this} is a {@link LuaFunction}, call it,
	 * and return only its first return value, dropping any others.
	 * Otherwise, look for the {@link Constants#CALL} metatag and call that.
	 * <p>
	 * If the return value is a {@link Varargs}, only the 1st value will be returned.
	 * To get multiple values, use {@link #invoke(LuaState, Varargs)} instead.
	 *
	 * @param state The current lua state
	 * @param arg   First argument to supply to the called function
	 * @return First return value {@code (this(arg))}, or {@link Constants#NIL} if there were none.
	 * @throws LuaError        If the invoked function throws an error.
	 * @throws UnwindThrowable If this function transfers control to another coroutine.
	 * @see #call(LuaState)
	 * @see #call(LuaState, LuaValue, LuaValue)
	 * @see #call(LuaState, LuaValue, LuaValue, LuaValue)
	 * @see #invoke(LuaState, Varargs)
	 */
	protected abstract LuaValue call(LuaState state, LuaValue arg) throws LuaError, UnwindThrowable;

	/**
	 * Call {@code this} with 2 arguments, including metatag processing,
	 * and return only the first return value.
	 * <p>
	 * If {@code this} is a {@link LuaFunction}, call it,
	 * and return only its first return value, dropping any others.
	 * Otherwise, look for the {@link Constants#CALL} metatag and call that.
	 * <p>
	 * If the return value is a {@link Varargs}, only the 1st value will be returned.
	 * To get multiple values, use {@link #invoke(LuaState, Varargs)} instead.
	 *
	 * @param state The current lua state
	 * @param arg1  First argument to supply to the called function
	 * @param arg2  Second argument to supply to the called function
	 * @return First return value {@code (this(arg1, arg2))}, or {@link Constants#NIL} if there were none.
	 * @throws LuaError        If the invoked function throws an error.
	 * @throws UnwindThrowable If this function transfers control to another coroutine.
	 * @see #call(LuaState)
	 * @see #call(LuaState, LuaValue)
	 * @see #call(LuaState, LuaValue, LuaValue, LuaValue)
	 */
	protected abstract LuaValue call(LuaState state, LuaValue arg1, LuaValue arg2) throws LuaError, UnwindThrowable;

	/**
	 * Call {@code this} with 3 arguments, including metatag processing,
	 * and return only the first return value.
	 * <p>
	 * If {@code this} is a {@link LuaFunction}, call it,
	 * and return only its first return value, dropping any others.
	 * Otherwise, look for the {@link Constants#CALL} metatag and call that.
	 * <p>
	 * If the return value is a {@link Varargs}, only the 1st value will be returned.
	 * To get multiple values, use {@link #invoke(LuaState, Varargs)} instead.
	 *
	 * @param state The current lua state
	 * @param arg1  First argument to supply to the called function
	 * @param arg2  Second argument to supply to the called function
	 * @param arg3  Second argument to supply to the called function
	 * @return First return value {@code (this(arg1, arg2, arg3))}, or {@link Constants#NIL} if there were none.
	 * @throws LuaError        If the invoked function throws an error.
	 * @throws UnwindThrowable If this function transfers control to another coroutine.
	 * @see #call(LuaState)
	 * @see #call(LuaState, LuaValue)
	 * @see #call(LuaState, LuaValue, LuaValue)
	 */
	protected abstract LuaValue call(LuaState state, LuaValue arg1, LuaValue arg2, LuaValue arg3) throws LuaError, UnwindThrowable;

	/**
	 * Call {@code this} with variable arguments, including metatag processing,
	 * and retain all return values in a {@link Varargs}.
	 * <p>
	 * If {@code this} is a {@link LuaFunction}, call it, and return all values.
	 * Otherwise, look for the {@link Constants#CALL} metatag and call that.
	 * <p>
	 * To get a particular return value, us {@link Varargs#arg(int)}
	 *
	 * @param state The current lua state
	 * @param args  Varargs containing the arguments to supply to the called function
	 * @return All return values as a {@link Varargs} instance.
	 * @throws LuaError        If the invoked function throws an error.
	 * @throws UnwindThrowable If this function transfers control to another coroutine.
	 * @see ValueFactory#varargsOf(LuaValue[])
	 * @see #call(LuaState, LuaValue)
	 */
	protected abstract Varargs invoke(LuaState state, Varargs args) throws LuaError, UnwindThrowable;

	public static void setGlobalLibrary(LuaState state, LuaTable env, String name, LuaValue library) throws LuaError {
		env.rawset(name, library);
		state.registry().getSubTable(Constants.LOADED).rawset(name, library);
	}

	// region Factories
	public static LibFunction create(ZeroArg fn) {
		return new ZeroArgFunction(fn);
	}

	public static LibFunction create(OneArg fn) {
		return new OneArgFunction(fn);
	}

	public static LibFunction create(TwoArg fn) {
		return new TwoArgFunction(fn);
	}

	public static LibFunction create(ThreeArg fn) {
		return new ThreeArgFunction(fn);
	}

	public static LibFunction createV(ManyArgs fn) {
		return new VarArgFunction() {
			@Override
			protected Varargs invoke(LuaState state, Varargs args) throws LuaError {
				return fn.invoke(state, args);
			}
		};
	}

	public static LibFunction createS(Suspended fn) {
		return new ResumableVarArgFunction<SuspendedTask<Varargs>>() {
			@Override
			protected Varargs invoke(LuaState state, DebugFrame frame, Varargs args) throws LuaError, UnwindThrowable {
				return fn.invoke(state, DebugState.get(state).getStackUnsafe(), args);
			}

			@Override
			public Varargs resume(LuaState state, SuspendedTask<Varargs> object, Varargs value) throws LuaError, UnwindThrowable {
				return object.resume(value);
			}
		};
	}

	public interface ZeroArg {
		LuaValue call(LuaState state) throws LuaError;
	}

	public interface OneArg {
		LuaValue call(LuaState state, LuaValue arg) throws LuaError;
	}

	public interface TwoArg {
		LuaValue call(LuaState state, LuaValue arg1, LuaValue arg2) throws LuaError;
	}

	public interface ThreeArg {
		LuaValue call(LuaState state, LuaValue arg1, LuaValue arg2, LuaValue arg3) throws LuaError;
	}

	public interface ManyArgs {
		Varargs invoke(LuaState state, Varargs args) throws LuaError;
	}

	/**
	 * A {@link ResumableVarArgFunction} implementation which works with {@link SuspendedTask}/{@link SuspendedFunction}.
	 */
	public interface Suspended {
		Varargs invoke(LuaState state, DebugFrame di, Varargs args) throws LuaError, UnwindThrowable;
	}
	// endregion
}
