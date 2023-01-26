package org.squiddev.cobalt.compiler;

enum ExpKind {
	/**
	 * no value
	 */
	VVOID,

	/**
	 * Constant nil
	 */
	VNIL,

	/**
	 * Constant true
	 */
	VTRUE,

	/**
	 * Constant false.
	 */
	VFALSE,

	/**
	 * info = index of constant in `k'
	 */
	VK,

	/**
	 * nval = numerical value
	 */
	VKNUM,

	/**
	 * info = local register
	 */
	VLOCAL,

	/**
	 * info = index of upvalue in `upvalues'
	 */
	VUPVAL,

	/**
	 * info = index of table, aux = index of global name in `k`.
	 */
	VGLOBAL,

	/**
	 * info = table register, aux = index register (or `k`)
	 */
	VINDEXED,

	/**
	 * info = instruction pc
	 */
	VJMP,

	/**
	 * info = instruction pc
	 */
	VRELOCABLE,

	/**
	 * info = result register
	 */
	VNONRELOC,

	/**
	 * info = instruction pc
	 */
	VCALL,

	/**
	 * info = instruction pc
	 */
	VVARARG;

	boolean hasMultiRet() {
		return this == VCALL || this == VVARARG;
	}

	boolean isVar() {
		return VLOCAL.ordinal() <= ordinal() && ordinal() <= VINDEXED.ordinal();
	}
}
