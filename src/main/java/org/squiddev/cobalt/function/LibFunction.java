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

import org.squiddev.cobalt.*;
import org.squiddev.cobalt.debug.DebugFrame;
import org.squiddev.cobalt.lib.BaseLib;
import org.squiddev.cobalt.lib.TableLib;

/**
 * Subclass of {@link LuaFunction} common to Java functions exposed to lua.
 *
 * To provide for common implementations in JME and JSE,
 * library functions are typically grouped on one or more library classes
 * and an opcode per library function is defined and used to key the switch
 * to the correct function within the library.
 *
 * Since lua functions can be called with too few or too many arguments,
 * and there are overloaded {@link LuaFunction#call(LuaState)} functions with varying
 * number of arguments, a Java function exposed in lua needs to handle  the
 * argument fixup when a function is called with a number of arguments
 * differs from that expected.
 *
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
 *
 * To be a Java library that can be loaded via {@code require}, it should have
 * a public constructor that returns a {@link LuaValue} that, when executed,
 * initializes the library.
 *
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
 * </pre>
 * The default constructor is used to instantiate the library
 * in response to {@code require 'hyperbolic'} statement,
 * provided it is on Javas class path.
 * This instance is then invoked with the name supplied to require()
 * as the only argument, and library should initialized whatever global
 * data it needs to and place it into the environment if needed.
 * In this case, it creates two function, 'sinh', and 'cosh', and puts
 * them into a global table called 'hyperbolic.'
 * It placed the library table into the globals via the {@link #env}
 * local variable which corresponds to the globals that apply when the
 * library is loaded.
 *
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
 *
 * It should produce something like:
 * <pre> {@code
 * t    table: 3dbbd23f
 * hyperbolic	table: 3dbbd23f
 * k,v	cosh	function: 3dbbd128
 * k,v	sinh	function: 3dbbd242
 * sinh(.5)	0.5210953
 * cosh(.5)	1.127626
 * }</pre>
 *
 * See the source code in any of the library functions
 * such as {@link BaseLib} or {@link TableLib} for other examples.
 */
public abstract class LibFunction extends LuaFunction {
	public static final ResumeError<?> DEFAULT_ERROR = (state, object, error) -> {
		throw error;
	};
	private static final Resume<?> DEFAULT_RESUME = (state, object, value) -> value;

	/**
	 * The common name for this function, useful for debugging.
	 *
	 * Binding functions initialize this to the name to which it is bound.
	 */
	protected final String name;

	LibFunction(LuaTable env, String name) {
		super(env);
		this.name = name;
	}

	@SuppressWarnings("unchecked")
	public static <T> Resume<T> defaultResume() {
		return (Resume<T>) DEFAULT_RESUME;
	}

	@SuppressWarnings("unchecked")
	public static <T> ResumeError<T> defaultResumeError() {
		return (ResumeError<T>) DEFAULT_ERROR;
	}

	@Override
	public String debugName() {
		return name != null ? name : super.toString();
	}

	public static LuaTable getActiveEnv(LuaState state) {
		LuaThread thread = state.getCurrentThread();
		if (thread != null && thread.getStatus().equals("running")) {
			DebugFrame frame = thread.getDebugState().getFrame(0);
			if (frame != null) return frame.func.getfenv();
		}

		thread = state.getMainThread();
		return thread == null ? null : thread.getfenv();
	}

	public static LibFunction of0(LuaTable env, String name, ZeroArgs func) {
		return new ZeroArgFunction(env, name, func);
	}

	public static LibFunction of1(LuaTable env, String name, OneArg func) {
		return new OneArgFunction(env, name, func);
	}

	public static LibFunction of2(LuaTable env, String name, TwoArgs func) {
		return new TwoArgFunction(env, name, func);
	}

	public static LibFunction of3(LuaTable env, String name, ThreeArgs func) {
		return new ThreeArgFunction(env, name, func);
	}

	public static LibFunction ofV(LuaTable env, String name, AnyArgs func) {
		return new VarArgFunction(env, name, func);
	}

	public static <T> LibFunction ofR(LuaTable env, String name, Resumable<T> func) {
		return new ResumableFunction<>(env, name, func, defaultResume(), defaultResumeError());
	}

	public static <T> LibFunction ofR(LuaTable env, String name, Resumable<T> func, Resume<T> resume) {
		return new ResumableFunction<>(env, name, func, resume, defaultResumeError());
	}

	public static <T> LibFunction ofR(LuaTable env, String name, Resumable<T> func, Resume<T> resume, ResumableFunction.ResumeError<T> error) {
		return new ResumableFunction<>(env, name, func, resume, error);
	}

	public static void bind0(LuaTable table, String name, ZeroArgs func) {
		table.rawset(name, of0(table, name, func));
	}

	public static void bind1(LuaTable table, String name, OneArg func) {
		table.rawset(name, of1(table, name, func));
	}

	public static void bind2(LuaTable table, String name, TwoArgs func) {
		table.rawset(name, of2(table, name, func));
	}

	public static void bind3(LuaTable table, String name, ThreeArgs func) {
		table.rawset(name, of3(table, name, func));
	}

	public static void bindV(LuaTable table, String name, AnyArgs func) {
		table.rawset(name, ofV(table, name, func));
	}

	public static <T> void bindR(LuaTable table, String name, Resumable<T> func) {
		table.rawset(name, ofR(table, name, func));
	}

	public static <T> void bindR(LuaTable table, String name, Resumable<T> func, Resume<T> resume) {
		table.rawset(name, ofR(table, name, func, resume));
	}

	public static <T> void bindR(LuaTable table, String name, Resumable<T> func, Resume<T> resume, ResumeError<T> error) {
		table.rawset(name, ofR(table, name, func, resume, error));
	}

	@FunctionalInterface
	public interface ZeroArgs {
		LuaValue call(LuaState state) throws LuaError;
	}

	@FunctionalInterface
	public interface OneArg {
		LuaValue call(LuaState state, LuaValue arg) throws LuaError;
	}

	@FunctionalInterface
	public interface TwoArgs {
		LuaValue call(LuaState state, LuaValue arg1, LuaValue arg2) throws LuaError;
	}

	@FunctionalInterface
	public interface ThreeArgs {
		LuaValue call(LuaState state, LuaValue arg1, LuaValue arg2, LuaValue arg3) throws LuaError;
	}

	@FunctionalInterface
	public interface AnyArgs {
		Varargs call(LuaState state, Varargs arg) throws LuaError;
	}

	@FunctionalInterface
	public interface Resumable<T> {
		Varargs invoke(LuaState state, DebugFrame di, Varargs args) throws LuaError, UnwindThrowable;
	}

	@FunctionalInterface
	public interface Resume<T> {
		Varargs resume(LuaState state, T object, Varargs args) throws LuaError, UnwindThrowable;
	}

	@FunctionalInterface
	public interface ResumeError<T> {
		Varargs resumeError(LuaState state, T obj, LuaError error) throws LuaError, UnwindThrowable;
	}
}
