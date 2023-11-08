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
package org.squiddev.cobalt.compiler;

import org.squiddev.cobalt.*;
import org.squiddev.cobalt.compiler.Parser.ExpDesc;
import org.squiddev.cobalt.function.LocalVariable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.squiddev.cobalt.Constants.*;
import static org.squiddev.cobalt.Lua.*;
import static org.squiddev.cobalt.compiler.LuaC.*;
import static org.squiddev.cobalt.compiler.Parser.NO_JUMP;

/**
 * The state of the function being parsed and emitted.
 * <p>
 * This largely mirrors the same structure in {@code lparser.h}, but also handles emitting code (defined in lcode.h
 * in PUC Lua).
 */
final class FuncState {
	static class BlockCnt {
		BlockCnt previous;  /* chain */
		short firstLabel;  /* Index of first label in this block. */
		short firstGoto;  /* Index of first pending goto in this block. */
		short activeVariableCount;  /* active locals outside the breakable structure */
		boolean upval;  /* true if some variable in the block is an upvalue */
		boolean isLoop;  /* true if `block' is a loop */
	}

	final FuncState prev;  /* enclosing function */
	final Lex lexer;  /* lexical state */

	final List<LuaValue> constants = new ArrayList<>(0);
	private final Map<LuaValue, Integer> constantLookup = new HashMap<>();  /* table to find (and reuse) elements in `k' */
	final List<LocalVariable> locals = new ArrayList<>(0);
	final List<Prototype> children = new ArrayList<>(0);
	final List<Prototype.UpvalueInfo> upvalues = new ArrayList<>(0);  /* upvalues */

	int pc;  /* next position to code (equivalent to `ncode') */
	int[] code;
	private int[] lineInfo;
	private int[] columnInfo;

	int lineDefined;
	int lastLineDefined;
	int numParams;
	boolean isVararg;
	int maxStackSize = 2;

	BlockCnt block;  /* chain of current blocks */
	int lastTarget = 0;   /* `pc' of last `jump target' */
	final IntPtr jpc = new IntPtr(NO_JUMP);  /* list of pending jumps to `pc' */
	int freeReg;  /* first free register */

	short activeVariableCount;  /* number of active local variables */

	final int firstLocal, firstLabel;

	FuncState(Lex lexer, FuncState prev, int firstLocal, int firstLabel) {
		this.lexer = lexer;
		this.prev = prev;
		this.firstLocal = firstLocal;
		this.firstLabel = firstLabel;
	}

	Prototype toPrototype() {
		return new Prototype(
			lexer.source, lexer.shortSource,
			// Code
			constants.toArray(new LuaValue[0]), LuaC.realloc(code, pc),
			children.toArray(new Prototype[0]),
			numParams, isVararg, maxStackSize, upvalues.toArray(Prototype.UpvalueInfo[]::new),
			// Debug information
			lineDefined, lastLineDefined, LuaC.realloc(lineInfo, pc), LuaC.realloc(columnInfo, pc),
			locals.toArray(new LocalVariable[0])
		);
	}


	void setMultiRet(ExpDesc e) throws CompileException {
		setReturns(e, LUA_MULTRET);
	}

	// =============================================================
	// from lcode.c
	// =============================================================

	void nil(int from, int n) throws CompileException {
		int to = from + n - 1;
		if (pc > lastTarget) { /* no jumps to current position? */
			int previous = code[pc - 1];
			if (GET_OPCODE(previous) == OP_LOADNIL) {
				int pFrom = GETARG_A(previous);
				int pTo = pFrom + GETARG_B(previous);
				if ((pFrom <= from && from <= pTo + 1) || (from <= pFrom && pFrom <= to + 1)) { /* can connect both? */
					if (pFrom <= from) from = pFrom;
					if (pTo > to) to = pTo;

					previous = SETARG_A(previous, from);
					previous = SETARG_B(previous, to - from);
					code[pc - 1] = previous;
					return;
				}
			}
		}

		codeABC(OP_LOADNIL, from, n - 1, 0); // else no optimization
	}

	private int getJump(int pc) {
		int offset = GETARG_sBx(code[pc]);
		if (offset == NO_JUMP) { // point to itself represents end of list
			return NO_JUMP; // end of list
		} else { // turn offset into absolute position
			return pc + 1 + offset;
		}
	}

	private void fixJump(int pc, int dest) throws CompileException {
		int offset = dest - (pc + 1);
		assert dest != NO_JUMP;
		if (Math.abs(offset) > MAXARG_sBx) throw lexer.syntaxError("control structure too long");
		code[pc] = SETARG_sBx(code[pc], offset);
	}

	/**
	 * Concatenate jump list {@code l2} into jump-list {@code l1}
	 */
	void concat(IntPtr l1, int l2) throws CompileException {
		if (l2 == NO_JUMP) return;
		if (l1.value == NO_JUMP) {
			l1.value = l2;
		} else {
			int list = l1.value;
			// find last element
			int next;
			while ((next = getJump(list)) != NO_JUMP) list = next;
			fixJump(list, l2);
		}
	}

	/**
	 * Create a jump instruction and return its position, so it can e fixed up later with {@link #fixJump(int, int)}.
	 * <p>
	 * If there are jumps to this position (kept in {@link #jpc}) link them together so that
	 * {@link #patchListAux(int, int, int, int)} will fix them to their final destination.
	 */
	int jump() throws CompileException {
		int jpc = this.jpc.value; /* save list of jumps to here */
		this.jpc.value = NO_JUMP;
		IntPtr j = new IntPtr(codeAsBx(OP_JMP, 0, NO_JUMP));
		concat(j, jpc); /* keep them on hold */
		return j.value;
	}

	/**
	 * Code a {@link Lua#OP_RETURN} instruction.
	 */
	void ret(int first, int nret) throws CompileException {
		codeABC(OP_RETURN, first, nret + 1, 0);
	}

	/**
	 * Code a conditional jump namely {@link Lua#OP_TEST}/{@link Lua#OP_TESTSET}.
	 */
	private int condJump(int op, int A, int B, int C, long position) throws CompileException {
		codeABCAt(op, A, B, C, position);
		return jump();
	}


	/*
	 * Returns current `pc' and marks it as a jump target (to avoid wrong
	 * optimizations with consecutive instructions not in the same basic block).
	 */
	int getLabel() {
		lastTarget = pc;
		return pc;
	}

	/**
	 * Returns the position of the instruction "controlling" a given jump (that is, its condition), or the jump itself
	 * if it is unconditional.
	 */
	private int getJumpControl(int pc) {
		if (pc >= 1 && testTMode(GET_OPCODE(code[pc - 1]))) {
			return pc - 1;
		} else {
			return pc;
		}
	}


	/**
	 * Patch destination register for a {@link Lua#OP_TESTSET} instruction.
	 * <p>
	 * If instruction in position 'node' is not a {@link Lua#OP_TESTSET}, return {@code false} Otherwise, if 'reg'
	 * is not {@link Lua#NO_REG}, set it as the destination register. Otherwise, change instruction to a simple
	 * {@link Lua#OP_TEST} (produces no register value).
	 */
	private boolean patchTestReg(int node, int reg) {
		int jumpControlPc = getJumpControl(node);
		int op = code[jumpControlPc];

		if (GET_OPCODE(op) != OP_TESTSET) return false; // cannot patch other instructions

		if (reg != NO_REG && reg != GETARG_B(op)) {
			code[jumpControlPc] = SETARG_A(op, reg);
		} else {
			// no register to put value or register already has the value
			code[jumpControlPc] = CREATE_ABC(OP_TEST, GETARG_B(op), 0, GETARG_C(op));
		}

		return true;
	}

	/**
	 * Traverse a list of tests, ensuring no one produces a value.
	 *
	 * @param list The head of the jump list.
	 */
	private void removeValues(int list) {
		for (; list != NO_JUMP; list = getJump(list)) {
			patchTestReg(list, NO_REG);
		}
	}

	/**
	 * Traverse a list of tests, patching their destination address and registers: tests producing values jump to
	 * 'vtarget' (and put their values in 'reg'), other tests jump to 'dtarget'.
	 */
	private void patchListAux(int list, int vtarget, int reg, int dtarget) throws CompileException {
		while (list != NO_JUMP) {
			int next = getJump(list);
			if (patchTestReg(list, reg)) {
				fixJump(list, vtarget);
			} else {
				fixJump(list, dtarget); // jump to default target
			}
			list = next;
		}
	}

	/**
	 * Ensure all pending jumps to current position are fixed (jumping to current position with no values) and reset
	 * list of pending jumps
	 */
	private void dischargeJumpPc() throws CompileException {
		patchListAux(jpc.value, pc, NO_REG, pc);
		jpc.value = NO_JUMP;
	}

	/**
	 * Add elements in 'list' to list of pending jumps to "here" (current position)
	 */
	void patchToHere(int list) throws CompileException {
		getLabel(); // Mark here as a target
		concat(jpc, list);
	}

	/**
	 * Path all jumps in 'list' to jump to 'target'. (The assert means that we cannot fix a jump to a forward address
	 * because we only know addresses once code is generated.)
	 */
	void patchList(int list, int target) throws CompileException {
		if (target == pc) {
			patchToHere(list);
		} else {
			assert target < pc;
			patchListAux(list, target, NO_REG, target);
		}
	}

	/**
	 * Path all jumps in 'list' to close upvalues up to given 'level' (The assertion checks that jumps either were
	 * closing nothing or were closing higher levels, from inner blocks.)
	 */
	void patchClose(int list, int level) {
		level++;  /* argument is +1 to reserve 0 as non-op */
		for (; list != NO_JUMP; list = getJump(list)) {
			assert GET_OPCODE(code[list]) == OP_JMP && (GETARG_A(code[list]) == 0 || GETARG_A(code[list]) >= level);
			code[list] = SETARG_A(code[list], level);
		}
	}

	/**
	 * Emit the instruction at a specific location.
	 *
	 * @param instruction The bytecode of the instruction.
	 * @param position    The packed position of this instruction.
	 * @return The position of this instruction.
	 */
	private int code(int instruction, long position) throws CompileException {
		assert position > 0;

		dischargeJumpPc(); /* `pc' will change */

		// put new instruction in code array
		if (code == null || pc + 1 > code.length) code = LuaC.realloc(code, pc * 2 + 1);
		code[pc] = instruction;

		// save corresponding line information
		if (lineInfo == null || pc + 1 > lineInfo.length) {
			lineInfo = LuaC.realloc(lineInfo, pc * 2 + 1);
			columnInfo = LuaC.realloc(columnInfo, pc * 2 + 1);
		}
		lineInfo[pc] = Lex.unpackLine(position);
		columnInfo[pc] = Lex.unpackColumn(position);

		return pc++;
	}

	int codeABCAt(int o, int a, int b, int c, long position) throws CompileException {
		assert getOpMode(o) == iABC;
		assert getBMode(o) != OpArgN || b == 0;
		assert getCMode(o) != OpArgN || c == 0;
		return code(CREATE_ABC(o, a, b, c), position);
	}

	int codeABC(int o, int a, int b, int c) throws CompileException {
		return codeABCAt(o, a, b, c, lexer.lastPosition());
	}

	int codeABxAt(int o, int a, int bc, long position) throws CompileException {
		assert getOpMode(o) == iABx || getOpMode(o) == iAsBx;
		assert getCMode(o) == OpArgN;
		assert position > 0;
		return code(CREATE_ABx(o, a, bc), position);
	}

	int codeABx(int o, int a, int bc) throws CompileException {
		return codeABxAt(o, a, bc, lexer.lastPosition());
	}

	int codeAsBxAt(int o, int A, int sBx, long position) throws CompileException {
		return codeABxAt(o, A, sBx + MAXARG_sBx, position);
	}

	int codeAsBx(int o, int A, int sBx) throws CompileException {
		return codeABx(o, A, sBx + MAXARG_sBx);
	}

	int codeK(int reg, int k) throws CompileException {
		if (k <= MAXARG_Bx) {
			return codeABx(OP_LOADK, reg, k);
		} else {
			int p = codeABx(OP_LOADKX, reg, 0);
			code(CREATE_Ax(OP_EXTRAARG, k), lexer.lastPosition());
			return p;
		}
	}

	/**
	 * Check register stack level, keeping track of its maximum size.
	 */
	void checkStack(int n) throws CompileException {
		int newStack = freeReg + n;
		if (newStack > maxStackSize) {
			if (newStack >= MAXSTACK) throw lexer.syntaxError("function or expression too complex");
			maxStackSize = newStack;
		}
	}

	/**
	 * Reserve {@code n} registers.
	 */
	void reserveRegs(int n) throws CompileException {
		checkStack(n);
		freeReg += n;
	}

	/**
	 * Free the given register (this may be a constant index, in which case this is a no-op).
	 */
	private void freeReg(int reg) {
		if (!ISK(reg) && reg >= activeVariableCount) {
			freeReg--;
			assert reg == freeReg;
		}
	}

	/**
	 * Free the register used by expression {@code e}.
	 */
	private void freeExp(ExpDesc e) {
		if (e.kind == ExpKind.VNONRELOC) freeReg(e.info);
	}

	/**
	 * Free registers used by both expressions, in the correct order.
	 */
	private void freeExp(ExpDesc e1, ExpDesc e2) {
		int r1 = e1.kind == ExpKind.VNONRELOC ? e1.info : -1;
		int r2 = e2.kind == ExpKind.VNONRELOC ? e2.info : -1;
		if (r1 > r2) {
			freeReg(r1);
			freeReg(r2);
		} else {
			freeReg(r2);
			freeReg(r1);
		}
	}

	private int addConstant(LuaValue v) {
		Integer existing = constantLookup.get(v);
		if (existing != null) return existing;

		int idx = constants.size();
		constantLookup.put(v, idx);
		constants.add(v);
		return idx;
	}

	int stringK(LuaString s) {
		return addConstant(s);
	}

	int numberK(LuaNumber r) {
		if (r instanceof LuaDouble) {
			double d = r.toDouble();
			int i = (int) d;
			if (d == (double) i) r = LuaInteger.valueOf(i);
		}

		return addConstant(r);
	}

	private int boolK(boolean b) {
		return addConstant(b ? TRUE : FALSE);
	}

	private int nilK() {
		return addConstant(NIL);
	}

	/**
	 * Fix an expression to return the number of results 'nresults'.
	 * <p>
	 * Either 'e' is a multi-ret expression (function call or vararg) 'nresults' is LUA_MULTRET (as any expression can
	 * satisfy that).
	 */
	void setReturns(ExpDesc e, int nresults) throws CompileException {
		if (e.kind == ExpKind.VCALL) { /* expression is an open function call? */
			code[e.info] = SETARG_C(code[e.info], nresults + 1);
		} else if (e.kind == ExpKind.VVARARG) {
			int op = SETARG_B(code[e.info], nresults + 1);
			code[e.info] = SETARG_A(op, freeReg);
			reserveRegs(1);
		} else {
			assert nresults == LUA_MULTRET;
		}
	}

	/**
	 * Fix an expression to return one result.
	 * <p>
	 * If expression is not a multi-ret expression (function call or vararg), it already returns one result, so nothing
	 * needs to be done. Function calls become VNONRELOC expressions (as its result comes fixed in the base register of
	 * the call), while vararg expressions become VRELOCABLE (as OP_VARARG puts its results where it wants).
	 * <p>
	 * (Calls are created returning one result, so that does not need to be fixed.)
	 */
	void setOneRet(ExpDesc e) {
		if (e.kind == ExpKind.VCALL) { /* expression is an open function call? */
			e.kind = ExpKind.VNONRELOC;
			e.info = GETARG_A(code[e.info]);
		} else if (e.kind == ExpKind.VVARARG) {
			code[e.info] = SETARG_B(code[e.info], 2);
			e.kind = ExpKind.VRELOCABLE; /* can relocate its simple result */
		}
	}

	/**
	 * Ensure that expression 'e' is not a variable.
	 */
	void dischargeVars(ExpDesc e) throws CompileException {
		switch (e.kind) {
			case VLOCAL -> e.kind = ExpKind.VNONRELOC;
			case VUPVAL -> {
				e.info = codeABCAt(OP_GETUPVAL, 0, e.info, 0, e.position);
				e.kind = ExpKind.VRELOCABLE;
			}
			case VINDEXED -> {
				int op;
				freeReg(e.aux);
				if (e.tableType == ExpKind.VLOCAL) {
					freeReg(e.info);
					op = OP_GETTABLE;
				} else {
					assert e.tableType == ExpKind.VUPVAL;
					op = OP_GETTABUP;
				}
				e.info = codeABCAt(op, 0, e.info, e.aux, e.position);
				e.kind = ExpKind.VRELOCABLE;
			}
			case VVARARG, VCALL -> setOneRet(e);
			default -> {
				/* there is one value available (somewhere) */
			}
		}
	}

	/**
	 * Ensures expression value is in register 'reg' (and therefore 'e' will become a non-relocatable expression).
	 */
	private void discharge2Reg(ExpDesc e, int reg) throws CompileException {
		dischargeVars(e);
		switch (e.kind) {
			case VNIL -> nil(reg, 1);
			case VFALSE, VTRUE -> codeABC(OP_LOADBOOL, reg, e.kind == ExpKind.VTRUE ? 1 : 0, 0);
			case VK -> codeK(reg, e.info);
			case VKNUM -> codeK(reg, numberK(e.nval()));
			case VRELOCABLE -> code[e.info] = SETARG_A(code[e.info], reg);
			case VNONRELOC -> {
				if (reg != e.info) codeABC(OP_MOVE, reg, e.info, 0);
			}
			default -> {
				assert e.kind == ExpKind.VJMP;
				return;
			}
		}
		e.info = reg;
		e.kind = ExpKind.VNONRELOC;
	}

	/**
	 * Ensures expression is in a register.
	 */
	private void discharge2AnyReg(ExpDesc e) throws CompileException {
		if (e.kind != ExpKind.VNONRELOC) {
			reserveRegs(1);
			discharge2Reg(e, freeReg - 1);
		}
	}

	private int codeLoadBool(int A, int b, int jump) throws CompileException {
		getLabel(); // those instructions may be jump targets
		return codeABC(OP_LOADBOOL, A, b, jump);
	}

	private boolean needValue(int list) {
		for (; list != NO_JUMP; list = getJump(list)) {
			if (GET_OPCODE(code[getJumpControl(list)]) != OP_TESTSET) return true;
		}
		return false; // not found
	}

	/**
	 * Ensures final expression result (including results from its jump
	 * lists) is in register 'reg'.
	 * If expression has jumps, need to patch these jumps either to
	 * its final position or to "load" instructions (for those tests
	 * that do not produce values).
	 */
	private void exp2reg(ExpDesc e, int reg) throws CompileException {
		discharge2Reg(e, reg);
		if (e.kind == ExpKind.VJMP) concat(e.t, e.info); /* put this jump in `t' list */
		if (e.hasjumps()) {
			int pcFalse = NO_JUMP; // position of an eventual LOAD false
			int psTrue = NO_JUMP; // position of an eventual LOAD true
			if (needValue(e.t.value) || needValue(e.f.value)) {
				int fj = e.kind == ExpKind.VJMP ? NO_JUMP : jump();
				pcFalse = codeLoadBool(reg, 0, 1);
				psTrue = codeLoadBool(reg, 1, 0);
				patchToHere(fj);
			}
			int end = getLabel(); // position after whole expression
			patchListAux(e.f.value, end, reg, pcFalse);
			patchListAux(e.t.value, end, reg, psTrue);
		}
		e.f.value = e.t.value = NO_JUMP;
		e.info = reg;
		e.kind = ExpKind.VNONRELOC;
	}

	/**
	 * Ensures final expression result (including results from its jump lists) is in next available register.
	 */
	void exp2NextReg(ExpDesc e) throws CompileException {
		dischargeVars(e);
		freeExp(e);
		reserveRegs(1);
		exp2reg(e, freeReg - 1);
	}

	/**
	 * Ensures final expression result (including results from its jump lists) is in some (any) register and return
	 * that register.
	 */
	int exp2AnyReg(ExpDesc e) throws CompileException {
		dischargeVars(e);
		if (e.kind == ExpKind.VNONRELOC) {
			if (!e.hasjumps()) return e.info; /* exp is already in a register */
			if (e.info >= activeVariableCount) { /* reg. is not a local? */
				exp2reg(e, e.info); /* put value on it */
				return e.info;
			}
		}
		exp2NextReg(e); /* default */
		return e.info;
	}

	/**
	 * Ensures final expression result is either in a register or in an upvalue.
	 */
	void exp2AnyRegUp(ExpDesc e) throws CompileException {
		if (e.kind != ExpKind.VUPVAL || e.hasjumps()) exp2AnyReg(e);
	}

	/**
	 * Ensures final expression result is either in a register or it is a constant.
	 */
	void exp2Val(ExpDesc e) throws CompileException {
		if (e.hasjumps()) {
			exp2AnyReg(e);
		} else {
			dischargeVars(e);
		}
	}

	/**
	 * Ensures final expression result is in a valid R/K index (that is, it is either in a register or in 'k' with an
	 * index in the range of R/K indices). Returns R/K index.
	 */
	int exp2RK(ExpDesc e) throws CompileException {
		exp2Val(e);

		// Promote constants to VK.
		switch (e.kind) {
			case VNIL -> e.setConstant(nilK());
			case VTRUE -> e.setConstant(boolK(true));
			case VFALSE -> e.setConstant(boolK(false));
			case VKNUM -> e.setConstant(numberK(e.nval()));
		}

		// Then use if they fit within the given range.
		if (e.kind == ExpKind.VK && e.info <= MAXINDEXRK) return RKASK(e.info);

		// Otherwise doesn't fit in the range - promote to a register.
		return exp2AnyReg(e);
	}

	void storeVar(ExpDesc var, ExpDesc ex) throws CompileException {
		switch (var.kind) {
			case VLOCAL -> {
				freeExp(ex);
				exp2reg(ex, var.info);
				return;
			}
			case VUPVAL -> {
				int e = exp2AnyReg(ex);
				codeABCAt(OP_SETUPVAL, e, var.info, 0, var.position);
			}
			case VINDEXED -> {
				int opcode = var.tableType == ExpKind.VLOCAL ? OP_SETTABLE : OP_SETTABUP;
				int e = exp2RK(ex);
				codeABCAt(opcode, var.info, var.aux, e, var.position);
			}
			default -> throw new AssertionError("invalid var kind to store");
		}
		freeExp(ex);
	}

	void self(ExpDesc e, ExpDesc key) throws CompileException {
		exp2AnyReg(e);
		int eReg = e.info;
		freeExp(e);
		e.info = freeReg;
		e.kind = ExpKind.VNONRELOC;
		reserveRegs(2);
		codeABC(OP_SELF, e.info, eReg, exp2RK(key));
		freeExp(key);
	}

	private void negateCondition(ExpDesc e) {
		int pc = getJumpControl(e.info);
		int i = code[pc];
		assert testTMode(GET_OPCODE(i)) && GET_OPCODE(i) != OP_TESTSET && GET_OPCODE(i) != OP_TEST;

		code[pc] = SETARG_A(i, GETARG_A(i) != 0 ? 0 : 1);
	}

	/**
	 * Emit instruction to jump if 'e' is 'cond' (that is, if 'cond'
	 * is true, code will jump if 'e' is true.) Return jump position.
	 * Optimize when 'e' is 'not' something, inverting the condition
	 * and removing the 'not'.
	 */
	private int jumpOnCond(ExpDesc e, int cond) throws CompileException {
		if (e.kind == ExpKind.VRELOCABLE) {
			int ie = code[e.info];
			if (GET_OPCODE(ie) == OP_NOT) {
				pc--; /* remove previous OP_NOT */
				return condJump(OP_TEST, GETARG_B(ie), 0, cond != 0 ? 0 : 1, lexer.lastPosition());
			}
			/* else go through */
		}
		discharge2AnyReg(e);
		freeExp(e);
		return condJump(OP_TESTSET, NO_REG, e.info, cond, lexer.lastPosition());
	}

	/**
	 * Emit code to go through if 'e' is true, jump otherwise.
	 */
	void goIfTrue(ExpDesc e) throws CompileException {
		dischargeVars(e);
		int pc = switch (e.kind) {  /* pc of last jump */
			case VK, VKNUM, VTRUE -> NO_JUMP; /* always true; do nothing */
			case VJMP -> {
				negateCondition(e);
				yield e.info;
			}
			default -> jumpOnCond(e, 0);
		};
		concat(e.f, pc); /* insert last jump in `f' list */
		patchToHere(e.t.value);
		e.t.value = NO_JUMP;
	}

	/**
	 * Emit code to go through if 'e' is false, jump otherwise.
	 */
	void goIfFalse(ExpDesc e) throws CompileException {
		dischargeVars(e);
		int pc = switch (e.kind) { /* pc of last jump */
			case VNIL, VFALSE -> NO_JUMP; /* always false; do nothing */
			case VJMP -> e.info;
			default -> jumpOnCond(e, 1);
		};
		concat(e.t, pc); /* insert last jump in `t' list */
		patchToHere(e.f.value);
		e.f.value = NO_JUMP;
	}

	private void codeNot(ExpDesc e) throws CompileException {
		dischargeVars(e);
		switch (e.kind) {
			case VNIL, VFALSE -> e.kind = ExpKind.VTRUE;
			case VK, VKNUM, VTRUE -> e.kind = ExpKind.VFALSE;
			case VJMP -> negateCondition(e);
			case VRELOCABLE, VNONRELOC -> {
				discharge2AnyReg(e);
				freeExp(e);
				e.info = codeABC(OP_NOT, 0, e.info, 0);
				e.kind = ExpKind.VRELOCABLE;
			}
			default -> throw new AssertionError();
		}
		/* interchange true and false lists */
		{
			int temp = e.f.value;
			e.f.value = e.t.value;
			e.t.value = temp;
		}
		removeValues(e.f.value);
		removeValues(e.t.value);
	}

	void indexed(ExpDesc t, ExpDesc k, long pos) throws CompileException {
		assert !t.hasjumps() && (t.kind.isInRegister() || t.kind == ExpKind.VUPVAL);
		t.aux = exp2RK(k);
		t.tableType = t.kind == ExpKind.VUPVAL ? ExpKind.VUPVAL : ExpKind.VLOCAL;
		t.kind = ExpKind.VINDEXED;
		t.position = pos;
	}

	private static boolean constFolding(int op, ExpDesc e1, ExpDesc e2) {
		if (!e1.isnumeral() || !e2.isnumeral()) return false;

		double v1 = e1.nval().toDouble();
		double v2 = e2.nval().toDouble();
		double r;
		switch (op) {
			case OP_ADD -> r = v1 + v2;
			case OP_SUB -> r = v1 - v2;
			case OP_MUL -> r = v1 * v2;
			case OP_DIV -> {
				if (v2 == 0) return false;
				r = v1 / v2;
			}
			case OP_MOD -> {
				if (v2 == 0) return false;
				r = OperationHelper.mod(v1, v2);
			}
			case OP_POW -> r = Math.pow(v1, v2);
			case OP_UNM -> r = -v1;
			case OP_LEN -> {
				return false; /* no constant folding for 'len' */
			}
			default -> throw new AssertionError();
		}

		if (Double.isNaN(r)) return false; /* do not attempt to produce NaN */
		e1.setNval(ValueFactory.valueOf(r));
		return true;
	}

	/**
	 * Emit code for unary expressions that "produce values" (everything but 'not').
	 * <p>
	 * Expression to produce final result will be encoded in 'e'.
	 */
	private void codeUnaryExpression(int opcode, ExpDesc e, long position) throws CompileException {
		int r = exp2AnyReg(e);
		freeExp(e);
		e.info = codeABCAt(opcode, 0, r, 0, position);
		e.kind = ExpKind.VRELOCABLE;
	}

	/**
	 * Emit code for binary expressions that "produce values" (everything but logical operators 'and'/'or' and
	 * comparison operators).
	 * <p>
	 * Expression to produce final result will be encoded in 'e1'. Because 'luaK_exp2RK' can free registers, its calls
	 * must be in "stack order" (that is, first on 'e2', which may have more recent registers to be released).
	 */
	private void codeBinaryExpression(int opcode, ExpDesc e1, ExpDesc e2, long position) throws CompileException {
		int rk2 = exp2RK(e2);
		int rk1 = exp2RK(e1);
		freeExp(e1, e2);
		e1.info = codeABCAt(opcode, 0, rk1, rk2, position);
		e1.kind = ExpKind.VRELOCABLE;
	}

	private void codeArith(int op, ExpDesc e1, ExpDesc e2, long position) throws CompileException {
		if (!constFolding(op, e1, e2)) codeBinaryExpression(op, e1, e2, position);
	}

	private void codeComparison(BinOpr op, ExpDesc e1, ExpDesc e2, long position) throws CompileException {
		int rk1 = switch (e1.kind) {
			case VK -> RKASK(e1.info);
			case VNONRELOC -> e1.info;
			default -> throw new AssertionError();
		};
		int rk2 = exp2RK(e2);
		freeExp(e1, e2);

		e1.info = switch (op) {
			case NE -> condJump(OP_EQ, 0, rk1, rk2, position); // a ~= b => not (a == b)
			case GT -> condJump(OP_LT, 1, rk2, rk1, position); // a > b  => b < a
			case GE -> condJump(OP_LE, 1, rk2, rk1, position); // a >= b => b <= a
			case EQ -> condJump(OP_EQ, 1, rk1, rk2, position);
			case LT -> condJump(OP_LT, 1, rk1, rk2, position);
			case LE -> condJump(OP_LE, 1, rk1, rk2, position);
			default -> throw new AssertionError("not a comparison");
		};
		e1.kind = ExpKind.VJMP;
	}

	void prefix(UnOpr op, ExpDesc e, long position) throws CompileException {
		switch (op) {
			case MINUS -> {
				if (!constFolding(OP_UNM, e, e)) codeUnaryExpression(OP_UNM, e, position);
			}
			case NOT -> codeNot(e);
			case LEN -> codeUnaryExpression(OP_LEN, e, position);
		}
	}

	void infix(BinOpr op, ExpDesc v) throws CompileException {
		switch (op) {
			case AND -> goIfTrue(v);
			case OR -> goIfFalse(v);
			case CONCAT -> exp2NextReg(v); /* operand must be on the `stack' */
			case ADD, SUB, MUL, DIV, MOD, POW -> {
				if (!v.isnumeral()) exp2RK(v);
			}
			default -> exp2RK(v);
		}
	}

	void posfix(BinOpr op, ExpDesc e1, ExpDesc e2, long position) throws CompileException {
		switch (op) {
			case AND -> {
				assert e1.t.value == NO_JUMP; /* list must be closed */
				dischargeVars(e2);
				concat(e2.f, e1.f.value);
				e1.setValue(e2);
			}
			case OR -> {
				assert e1.f.value == NO_JUMP; /* list must be closed */
				dischargeVars(e2);
				concat(e2.t, e1.t.value);
				e1.setValue(e2);
			}
			case CONCAT -> {
				exp2Val(e2);
				if (e2.kind == ExpKind.VRELOCABLE && GET_OPCODE(code[e2.info]) == OP_CONCAT) {
					assert e1.info == GETARG_B(code[e2.info]) - 1;
					freeExp(e1);
					code[e2.info] = SETARG_B(code[e2.info], e1.info);
					e1.kind = ExpKind.VRELOCABLE;
					e1.info = e2.info;
				} else {
					exp2NextReg(e2); /* operand must be on the 'stack' */
					codeBinaryExpression(OP_CONCAT, e1, e2, position);
				}
			}
			case ADD -> codeArith(OP_ADD, e1, e2, position);
			case SUB -> codeArith(OP_SUB, e1, e2, position);
			case MUL -> codeArith(OP_MUL, e1, e2, position);
			case DIV -> codeArith(OP_DIV, e1, e2, position);
			case MOD -> codeArith(OP_MOD, e1, e2, position);
			case POW -> codeArith(OP_POW, e1, e2, position);
			case EQ, NE, LT, LE, GT, GE -> codeComparison(op, e1, e2, position);
			default -> throw new AssertionError("Unknown op " + op);
		}
	}

	void fixPosition(long position) {
		lineInfo[pc - 1] = Lex.unpackLine(position);
		columnInfo[pc - 1] = Lex.unpackColumn(position);
	}


	void setList(int base, int nelems, int tostore) throws CompileException {
		int c = (nelems - 1) / LFIELDS_PER_FLUSH + 1;
		int b = tostore == LUA_MULTRET ? 0 : tostore;
		assert tostore != 0;
		if (c <= MAXARG_C) {
			codeABC(OP_SETLIST, base, b, c);
		} else {
			codeABC(OP_SETLIST, base, b, 0);
			code(CREATE_Ax(OP_EXTRAARG, c), lexer.lastPosition());
		}
		freeReg = base + 1; /* free registers with list values */
	}
}
