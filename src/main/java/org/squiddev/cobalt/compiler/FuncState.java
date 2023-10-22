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
	static class UpvalueDesc {
		final LuaString name;
		final ExpKind kind;
		final short info;

		UpvalueDesc(LuaString name, ExpKind kind, short info) {
			this.name = name;
			this.kind = kind;
			this.info = info;
		}
	}

	static class BlockCnt {
		BlockCnt previous;  /* chain */
		IntPtr breaklist = new IntPtr();  /* list of jumps out of this loop */
		short nactvar;  /* # active locals outside the breakable structure */
		boolean upval;  /* true if some variable in the block is an upvalue */
		boolean isbreakable;  /* true if `block' is a loop */
	}

	final FuncState prev;  /* enclosing function */
	final Lex lexer;  /* lexical state */

	final List<LuaValue> constants = new ArrayList<>(0);
	private final Map<LuaValue, Integer> constantLookup = new HashMap<>();  /* table to find (and reuse) elements in `k' */
	final List<LocalVariable> locals = new ArrayList<>(0);
	final List<Prototype> children = new ArrayList<>(0);
	final List<UpvalueDesc> upvalues = new ArrayList<>(0);  /* upvalues */

	int pc;  /* next position to code (equivalent to `ncode') */
	int[] code;
	private int[] lineInfo;
	private int[] columnInfo;

	int lineDefined;
	int lastLineDefined;
	int numParams;
	int varargFlags;
	int maxStackSize = 2;

	BlockCnt block;  /* chain of current blocks */
	int lastTarget = -1;   /* `pc' of last `jump target' */
	final IntPtr jpc = new IntPtr(NO_JUMP);  /* list of pending jumps to `pc' */
	int freeReg;  /* first free register */

	short activeVariableCount;  /* number of active local variables */
	short[] activeVariables = new short[LUAI_MAXVARS];  /* declared-variable stack */

	FuncState(Lex lexer, FuncState prev) {
		this.lexer = lexer;
		this.prev = prev;
	}

	Prototype toPrototype() {
		int i = 0;
		LuaString[] upvalueNames = new LuaString[upvalues.size()];
		for (FuncState.UpvalueDesc upvalue : upvalues) upvalueNames[i++] = upvalue.name;

		return new Prototype(
			lexer.source, lexer.shortSource,
			// Code
			constants.toArray(new LuaValue[0]), LuaC.realloc(code, pc),
			children.toArray(new Prototype[0]),
			numParams, varargFlags, maxStackSize, upvalues.size(),
			// Debug information
			lineDefined, lastLineDefined, LuaC.realloc(lineInfo, pc), LuaC.realloc(columnInfo, pc),
			locals.toArray(new LocalVariable[0]), upvalueNames
		);
	}

	int codeAsBxAt(int o, int A, int sBx, long position) throws CompileException {
		return codeABxAt(o, A, sBx + MAXARG_sBx, position);
	}

	int codeAsBx(int o, int A, int sBx) throws CompileException {
		return codeABx(o, A, sBx + MAXARG_sBx);
	}

	void setMultiRet(ExpDesc e) throws CompileException {
		setReturns(e, LUA_MULTRET);
	}

	LocalVariable getLocal(int i) {
		return locals.get(activeVariables[i]);
	}

	// =============================================================
	// from lcode.c
	// =============================================================

	void nil(int from, int n) throws CompileException {
		if (pc > lastTarget) { /* no jumps to current position? */
			if (pc == 0) { /* function start? */
				if (from >= activeVariableCount) {
					return; /* positions are already clean */
				}
			} else {
				int previous = code[pc - 1];
				if (GET_OPCODE(previous) == OP_LOADNIL) {
					int pfrom = GETARG_A(previous);
					int pto = GETARG_B(previous);
					if (pfrom <= from && from <= pto + 1) { /* can connect both? */
						if (from + n - 1 > pto) code[pc - 1] = SETARG_B(previous, from + n - 1);
						return;
					}
				}
			}
		}
		/* else no optimization */
		codeABC(OP_LOADNIL, from, from + n - 1, 0);
	}

	int jump() throws CompileException {
		int jpc = this.jpc.value; /* save list of jumps to here */
		this.jpc.value = NO_JUMP;
		IntPtr j = new IntPtr(codeAsBx(OP_JMP, 0, NO_JUMP));
		concat(j, jpc); /* keep them on hold */
		return j.value;
	}

	void ret(int first, int nret) throws CompileException {
		codeABC(OP_RETURN, first, nret + 1, 0);
	}

	private int condJump(int op, int A, int B, int C, long position) throws CompileException {
		codeABCAt(op, A, B, C, position);
		return jump();
	}

	private void fixJump(int pc, int dest) throws CompileException {
		int offset = dest - (pc + 1);
		assert dest != NO_JUMP;
		if (Math.abs(offset) > MAXARG_sBx) throw lexer.syntaxError("control structure too long");
		code[pc] = SETARG_sBx(code[pc], offset);
	}

	/*
	 * Returns current `pc' and marks it as a jump target (to avoid wrong
	 * optimizations with consecutive instructions not in the same basic block).
	 */
	int getLabel() {
		lastTarget = pc;
		return pc;
	}

	private int getJump(int pc) {
		int offset = GETARG_sBx(code[pc]);
		if (offset == NO_JUMP) { // point to itself represents end of list
			return NO_JUMP; // end of list
		} else { // turn offset into absolute position
			return pc + 1 + offset;
		}
	}

	private int getJumpControl(int pc) {
		if (pc >= 1 && testTMode(GET_OPCODE(code[pc - 1]))) {
			return pc - 1;
		} else {
			return pc;
		}
	}

	/*
	 * Check whether list has any jump that do not produce a value
	 * (or produce an inverted value).
	 */
	private boolean needValue(int list) {
		for (; list != NO_JUMP; list = getJump(list)) {
			if (GET_OPCODE(code[getJumpControl(list)]) != OP_TESTSET) return true;
		}
		return false; // not found
	}

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

	private void removeValues(int list) {
		for (; list != NO_JUMP; list = getJump(list)) {
			patchTestReg(list, NO_REG);
		}
	}

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

	private void dischargeJumpPc() throws CompileException {
		patchListAux(jpc.value, pc, NO_REG, pc);
		jpc.value = NO_JUMP;
	}

	void patchList(int list, int target) throws CompileException {
		if (target == pc) {
			patchToHere(list);
		} else {
			_assert(target < pc);
			patchListAux(list, target, NO_REG, target);
		}
	}

	void patchToHere(int list) throws CompileException {
		getLabel();
		concat(jpc, list);
	}

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

	void checkStack(int n) throws CompileException {
		int newStack = freeReg + n;
		if (newStack > maxStackSize) {
			if (newStack >= MAXSTACK) throw lexer.syntaxError("function or expression too complex");
			maxStackSize = newStack;
		}
	}

	void reserveRegs(int n) throws CompileException {
		checkStack(n);
		freeReg += n;
	}

	private void freeReg(int reg) throws CompileException {
		if (!ISK(reg) && reg >= activeVariableCount) {
			freeReg--;
			_assert(reg == freeReg);
		}
	}

	private void freeExp(ExpDesc e) throws CompileException {
		if (e.kind == ExpKind.VNONRELOC) freeReg(e.info);
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

	void setReturns(ExpDesc e, int nresults) throws CompileException {
		if (e.kind == ExpKind.VCALL) { /* expression is an open function call? */
			code[e.info] = SETARG_C(code[e.info], nresults + 1);
		} else if (e.kind == ExpKind.VVARARG) {
			int op = SETARG_B(code[e.info], nresults + 1);
			code[e.info] = SETARG_A(op, freeReg);
			reserveRegs(1);
		}
	}

	void setOneRet(ExpDesc e) {
		if (e.kind == ExpKind.VCALL) { /* expression is an open function call? */
			e.kind = ExpKind.VNONRELOC;
			e.info = GETARG_A(code[e.info]);
		} else if (e.kind == ExpKind.VVARARG) {
			code[e.info] = SETARG_B(code[e.info], 2);
			e.kind = ExpKind.VRELOCABLE; /* can relocate its simple result */
		}
	}

	void dischargeVars(ExpDesc e) throws CompileException {
		switch (e.kind) {
			case VLOCAL -> e.kind = ExpKind.VNONRELOC;
			case VUPVAL -> {
				e.info = codeABCAt(OP_GETUPVAL, 0, e.info, 0, e.position);
				e.kind = ExpKind.VRELOCABLE;
			}
			case VGLOBAL -> {
				e.info = codeABxAt(OP_GETGLOBAL, 0, e.info, e.position);
				e.kind = ExpKind.VRELOCABLE;
			}
			case VINDEXED -> {
				freeReg(e.aux);
				freeReg(e.info);
				e.info = codeABCAt(OP_GETTABLE, 0, e.info, e.aux, e.position);
				e.kind = ExpKind.VRELOCABLE;
			}
			case VVARARG, VCALL -> setOneRet(e);
			default -> {
			} /* there is one value available (somewhere) */
		}
	}

	private int codeLabel(int A, int b, int jump) throws CompileException {
		getLabel(); // those instructions may be jump targets
		return codeABC(OP_LOADBOOL, A, b, jump);
	}

	private void discharge2Reg(ExpDesc e, int reg) throws CompileException {
		dischargeVars(e);
		switch (e.kind) {
			case VNIL -> nil(reg, 1);
			case VFALSE, VTRUE -> codeABC(OP_LOADBOOL, reg, e.kind == ExpKind.VTRUE ? 1 : 0, 0);
			case VK -> codeABx(OP_LOADK, reg, e.info);
			case VKNUM -> codeABx(OP_LOADK, reg, numberK(e.nval()));
			case VRELOCABLE -> code[e.info] = SETARG_A(code[e.info], reg);
			case VNONRELOC -> {
				if (reg != e.info) codeABC(OP_MOVE, reg, e.info, 0);
			}
			default -> {
				_assert(e.kind == ExpKind.VVOID || e.kind == ExpKind.VJMP);
				return; /* nothing to do... */
			}
		}
		e.info = reg;
		e.kind = ExpKind.VNONRELOC;
	}

	private void discharge2AnyReg(ExpDesc e) throws CompileException {
		if (e.kind != ExpKind.VNONRELOC) {
			reserveRegs(1);
			discharge2Reg(e, freeReg - 1);
		}
	}

	private void exp2reg(ExpDesc e, int reg) throws CompileException {
		discharge2Reg(e, reg);
		if (e.kind == ExpKind.VJMP) concat(e.t, e.info); /* put this jump in `t' list */
		if (e.hasjumps()) {
			int p_f = NO_JUMP; // position of an eventual LOAD false
			int p_t = NO_JUMP; // position of an eventual LOAD true
			if (needValue(e.t.value) || needValue(e.f.value)) {
				int fj = e.kind == ExpKind.VJMP ? NO_JUMP : jump();
				p_f = codeLabel(reg, 0, 1);
				p_t = codeLabel(reg, 1, 0);
				patchToHere(fj);
			}
			int _final = getLabel(); // position after whole expression
			patchListAux(e.f.value, _final, reg, p_f);
			patchListAux(e.t.value, _final, reg, p_t);
		}
		e.f.value = e.t.value = NO_JUMP;
		e.info = reg;
		e.kind = ExpKind.VNONRELOC;
	}

	void exp2NextReg(ExpDesc e) throws CompileException {
		dischargeVars(e);
		freeExp(e);
		reserveRegs(1);
		exp2reg(e, freeReg - 1);
	}

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

	void exp2Val(ExpDesc e) throws CompileException {
		if (e.hasjumps()) {
			exp2AnyReg(e);
		} else {
			dischargeVars(e);
		}
	}

	int exp2RK(ExpDesc e) throws CompileException {
		exp2Val(e);
		switch (e.kind) {
			case VKNUM, VTRUE, VFALSE, VNIL -> {
				if (constants.size() <= MAXINDEXRK) { /* constant fit in RK operand? */
					e.info = e.kind == ExpKind.VNIL ? nilK()
						: e.kind == ExpKind.VKNUM ? numberK(e.nval())
						: boolK(e.kind == ExpKind.VTRUE);
					e.kind = ExpKind.VK;
					return RKASK(e.info);
				}
			}
			case VK -> {
				if (e.info <= MAXINDEXRK) /* constant fit in argC? */ {
					return RKASK(e.info);
				}
			}
			default -> {
			}
		}
		/* not a constant in the right range: put it in a register */
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
			case VGLOBAL -> {
				int e = exp2AnyReg(ex);
				codeABxAt(OP_SETGLOBAL, e, var.info, var.position);
			}
			case VINDEXED -> {
				int e = exp2RK(ex);
				codeABCAt(OP_SETTABLE, var.info, var.aux, e, var.position);
			}
			default -> _assert(false); /* invalid var kind to store */
		}
		freeExp(ex);
	}

	void self(ExpDesc e, ExpDesc key) throws CompileException {
		int func;
		exp2AnyReg(e);
		freeExp(e);
		func = freeReg;
		reserveRegs(2);
		codeABC(OP_SELF, func, e.info, exp2RK(key));
		freeExp(key);
		e.info = func;
		e.kind = ExpKind.VNONRELOC;
	}

	private void invertJump(ExpDesc e) throws CompileException {
		int pc = getJumpControl(e.info);
		int op = code[pc];
		_assert(testTMode(GET_OPCODE(op)) && GET_OPCODE(op) != OP_TESTSET && GET_OPCODE(op) != OP_TEST);

		int a = GETARG_A(op);
		int nota = a != 0 ? 0 : 1;
		code[pc] = SETARG_A(op, nota);
	}

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

	void goIfTrue(ExpDesc e) throws CompileException {
		dischargeVars(e);
		int pc; /* pc of last jump */
		switch (e.kind) {
			case VK, VKNUM, VTRUE -> pc = NO_JUMP; /* always true; do nothing */
			case VFALSE -> pc = jump(); /* always jump */
			case VJMP -> {
				invertJump(e);
				pc = e.info;
			}
			default -> pc = jumpOnCond(e, 0);
		}
		concat(e.f, pc); /* insert last jump in `f' list */
		patchToHere(e.t.value);
		e.t.value = NO_JUMP;
	}

	private void goIfFalse(ExpDesc e) throws CompileException {
		dischargeVars(e);
		int pc; /* pc of last jump */
		switch (e.kind) {
			case VNIL, VFALSE -> pc = NO_JUMP; /* always false; do nothing */
			case VTRUE -> pc = jump(); /* always jump */
			case VJMP -> pc = e.info;
			default -> pc = jumpOnCond(e, 1);
		}
		concat(e.t, pc); /* insert last jump in `t' list */
		patchToHere(e.f.value);
		e.f.value = NO_JUMP;
	}

	private void codeNot(ExpDesc e) throws CompileException {
		dischargeVars(e);
		switch (e.kind) {
			case VNIL, VFALSE -> e.kind = ExpKind.VTRUE;
			case VK, VKNUM, VTRUE -> e.kind = ExpKind.VFALSE;
			case VJMP -> invertJump(e);
			case VRELOCABLE, VNONRELOC -> {
				discharge2AnyReg(e);
				freeExp(e);
				e.info = codeABC(OP_NOT, 0, e.info, 0);
				e.kind = ExpKind.VRELOCABLE;
			}
			default -> _assert(false); /* cannot happen */
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
		t.aux = exp2RK(k);
		t.kind = ExpKind.VINDEXED;
		t.position = pos;
	}

	private boolean constFolding(int op, ExpDesc e1, ExpDesc e2) throws CompileException {
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
			default -> {
				_assert(false);
				return false;
			}
		}

		if (Double.isNaN(r)) return false; /* do not attempt to produce NaN */
		e1.setNval(ValueFactory.valueOf(r));
		return true;
	}

	private void codeArith(int op, ExpDesc e1, ExpDesc e2, long position) throws CompileException {
		if (constFolding(op, e1, e2)) return;

		int o2 = op != OP_UNM && op != OP_LEN ? exp2RK(e2) : 0;
		int o1 = exp2RK(e1);
		if (o1 > o2) {
			freeExp(e1);
			freeExp(e2);
		} else {
			freeExp(e2);
			freeExp(e1);
		}
		e1.info = codeABCAt(op, 0, o1, o2, position);
		e1.kind = ExpKind.VRELOCABLE;
	}

	private void codeComparison(int op, int cond, ExpDesc e1, ExpDesc e2, long position) throws CompileException {
		int o1 = exp2RK(e1);
		int o2 = exp2RK(e2);
		freeExp(e2);
		freeExp(e1);
		if (cond == 0 && op != OP_EQ) {
			int temp; /* exchange args to replace by `<' or `<=' */
			temp = o1;
			o1 = o2;
			o2 = temp; /* o1 <==> o2 */
			cond = 1;
		}
		e1.info = condJump(op, cond, o1, o2, position);
		e1.kind = ExpKind.VJMP;
	}

	void prefix(UnOpr op, ExpDesc e, long position) throws CompileException {
		ExpDesc e2 = new ExpDesc();
		e2.init(ExpKind.VKNUM, 0);
		switch (op) {
			case MINUS -> {
				if (e.kind == ExpKind.VK) exp2AnyReg(e); /* cannot operate on non-numeric constants */
				codeArith(OP_UNM, e, e2, position);
			}
			case NOT -> codeNot(e);
			case LEN -> {
				exp2AnyReg(e); /* cannot operate on constants */
				codeArith(OP_LEN, e, e2, position);
			}
			default -> _assert(false);
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
				_assert(e1.t.value == NO_JUMP); /* list must be closed */
				dischargeVars(e2);
				concat(e2.f, e1.f.value);
				e1.setValue(e2);
			}
			case OR -> {
				_assert(e1.f.value == NO_JUMP); /* list must be closed */
				dischargeVars(e2);
				concat(e2.t, e1.t.value);
				e1.setValue(e2);
			}
			case CONCAT -> {
				exp2Val(e2);
				if (e2.kind == ExpKind.VRELOCABLE && GET_OPCODE(code[e2.info]) == OP_CONCAT) {
					_assert(e1.info == GETARG_B(code[e2.info]) - 1);
					freeExp(e1);
					code[e2.info] = SETARG_B(code[e2.info], e1.info);
					e1.kind = ExpKind.VRELOCABLE;
					e1.info = e2.info;
				} else {
					exp2NextReg(e2); /* operand must be on the 'stack' */
					codeArith(OP_CONCAT, e1, e2, position);
				}
			}
			case ADD -> codeArith(OP_ADD, e1, e2, position);
			case SUB -> codeArith(OP_SUB, e1, e2, position);
			case MUL -> codeArith(OP_MUL, e1, e2, position);
			case DIV -> codeArith(OP_DIV, e1, e2, position);
			case MOD -> codeArith(OP_MOD, e1, e2, position);
			case POW -> codeArith(OP_POW, e1, e2, position);
			case EQ -> codeComparison(OP_EQ, 1, e1, e2, position);
			case NE -> codeComparison(OP_EQ, 0, e1, e2, position);
			case LT -> codeComparison(OP_LT, 1, e1, e2, position);
			case LE -> codeComparison(OP_LE, 1, e1, e2, position);
			case GT -> codeComparison(OP_LT, 0, e1, e2, position);
			case GE -> codeComparison(OP_LE, 0, e1, e2, position);
			default -> _assert(false);
		}
	}

	void fixPosition(long position) {
		lineInfo[pc - 1] = Lex.unpackLine(position);
		columnInfo[pc - 1] = Lex.unpackColumn(position);
	}

	private int code(int instruction, long position) throws CompileException {
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
		_assert(getOpMode(o) == iABC);
		_assert(getBMode(o) != OpArgN || b == 0);
		_assert(getCMode(o) != OpArgN || c == 0);
		_assert(position > 0);
		return code(CREATE_ABC(o, a, b, c), position);
	}

	int codeABC(int o, int a, int b, int c) throws CompileException {
		_assert(getOpMode(o) == iABC);
		_assert(getBMode(o) != OpArgN || b == 0);
		_assert(getCMode(o) != OpArgN || c == 0);
		return code(CREATE_ABC(o, a, b, c), lexer.lastPosition());
	}

	int codeABxAt(int o, int a, int bc, long position) throws CompileException {
		_assert(getOpMode(o) == iABx || getOpMode(o) == iAsBx);
		_assert(getCMode(o) == OpArgN);
		_assert(position > 0);
		return code(CREATE_ABx(o, a, bc), position);
	}

	int codeABx(int o, int a, int bc) throws CompileException {
		return codeABxAt(o, a, bc, lexer.lastPosition());
	}

	void setList(int base, int nelems, int tostore) throws CompileException {
		int c = (nelems - 1) / LFIELDS_PER_FLUSH + 1;
		int b = tostore == LUA_MULTRET ? 0 : tostore;
		_assert(tostore != 0);
		if (c <= MAXARG_C) {
			codeABC(OP_SETLIST, base, b, c);
		} else {
			codeABC(OP_SETLIST, base, b, 0);
			code(c, lexer.lastPosition());
		}
		freeReg = base + 1; /* free registers with list values */
	}
}
