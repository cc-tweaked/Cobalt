package org.squiddev.cobalt.unwind;

import org.squiddev.cobalt.Varargs;


/**
 * The state of an object being unwound.
 *
 * @deprecated This should not be referenced from user code.
 */
@Deprecated
public class UnwindState {
	public int state;
	public Varargs resumeArgs;
	public Object child;

	public static UnwindState getOrCreate(Object x) {
		return x == null ? new UnwindState() : (UnwindState) x;
	}
}
