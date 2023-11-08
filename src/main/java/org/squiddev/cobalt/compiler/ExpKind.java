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
	 * info = localresult
	 */
	VNONRELOC,

	/**
	 * info = local register
	 */
	VLOCAL,

	/**
	 * info = index of upvalue in `upvalues'
	 */
	VUPVAL,

	/**
	 * info = table register/upvalue ("t"), aux = index register ("idx")
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

	boolean isInRegister() {
		return this == VNONRELOC || this == VLOCAL;
	}

	boolean isVar() {
		return VLOCAL.ordinal() <= ordinal() && ordinal() <= VINDEXED.ordinal();
	}
}
