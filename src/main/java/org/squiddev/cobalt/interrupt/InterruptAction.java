package org.squiddev.cobalt.interrupt;

/**
 * The action to perform after the Lua VM was interrupted.
 *
 * @see InterruptHandler
 */
public enum InterruptAction {
	/**
	 * Continue executing the VM as normal.
	 */
	CONTINUE,

	/**
	 * Suspend execution of the running VM.
	 */
	SUSPEND,
}
