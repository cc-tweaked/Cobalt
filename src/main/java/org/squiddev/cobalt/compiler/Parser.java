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

import cc.tweaked.cobalt.internal.unwind.AutoUnwind;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.LocalVariable;

import java.util.ArrayList;
import java.util.List;

import static org.squiddev.cobalt.Lua.*;
import static org.squiddev.cobalt.compiler.Lex.*;
import static org.squiddev.cobalt.compiler.LuaC.LUAI_MAXUPVALUES;

/**
 * A parser for Lua code.
 * <p>
 * This largely follows the structure and implementation of lparser.c.
 */
@AutoUnwind
final class Parser {
	private static final int LUAI_MAXCCALLS = 200;
	private static final boolean LUA_COMPAT_VARARG = true;

	private static String LUA_QL(String s) {
		return "'" + s + "'";
	}

	/*
	 ** Marks the end of a patch list. It is an invalid value both as an absolute
	 ** address, and as a list link (would link an element to itself).
	 */
	static final int NO_JUMP = -1;

	final Lex lexer;
	final LuaString envName, gotoName;
	FuncState fs;
	public int nCcalls;

	/**
	 * A stack of active labels in scope. The labels available in the current block are all those at or above index
	 * {@link FuncState.BlockCnt#firstLabel}.
	 */
	private final List<LabelDesc> activeLabels = new ArrayList<>();

	/**
	 * A stack of unsolved gotos in scope. The gotos defined in the current block are all those at or above index
	 * {@link FuncState.BlockCnt#firstLabel}.
	 */
	private final List<LabelDesc> pendingGotos = new ArrayList<>();

	private int activeVariableSize;
	private short[] activeVariables = new short[16];

	public Parser(InputReader stream, int firstByte, LuaString source, LuaString shortSource) {
		lexer = new Lex(source, shortSource, stream, firstByte);
		envName = lexer.newString("_ENV");
		gotoName = lexer.newString("goto");
		fs = null;
	}

	// =============================================================
	// from lparser.c
	// =============================================================

	static class ExpDesc {
		ExpKind kind; // expkind, from enumerated list, above
		long position;

		private LuaNumber nval;
		int info;
		int aux;
		@Nullable ExpKind tableType;

		final IntPtr t = new IntPtr(); /* patch list of `exit when true' */
		final IntPtr f = new IntPtr(); /* patch list of `exit when false' */

		void init(ExpKind kind, int info) {
			this.kind = kind;
			this.info = info;
			f.value = NO_JUMP;
			t.value = NO_JUMP;
		}

		public void setNval(LuaNumber r) {
			nval = r;
		}

		public LuaNumber nval() {
			return nval == null ? LuaInteger.valueOf(info) : nval;
		}

		boolean hasjumps() {
			return t.value != f.value;
		}

		boolean isnumeral() {
			return kind == ExpKind.VKNUM && t.value == NO_JUMP && f.value == NO_JUMP;
		}

		public void setValue(ExpDesc other) {
			kind = other.kind;
			nval = other.nval;
			info = other.info;
			aux = other.aux;
			t.value = other.t.value;
			f.value = other.f.value;
		}

		public void setConstant(int constantIndex) {
			kind = ExpKind.VK;
			info = constantIndex;
		}
	}

	/**
	 * A description for a label.
	 */
	static final class LabelDesc {
		private final LuaString name;
		private final int pc;
		private final int line;
		private short activeVariables;

		/**
		 * @param name            Label identifier
		 * @param pc              Position in code
		 * @param line            Line where it appeared
		 * @param activeVariables Local level where it appears in current block
		 */
		LabelDesc(LuaString name, int pc, int line, short activeVariables) {
			this.name = name;
			this.pc = pc;
			this.line = line;
			this.activeVariables = activeVariables;
		}
	}

	/*----------------------------------------------------------------------
	name		args	description
	------------------------------------------------------------------------*/


	CompileException syntaxError(String msg) {
		return lexer.syntaxError(msg);
	}

	CompileException semError(String msg) {
		return lexer.lexError(msg, 0);
	}

	private void errorExpected(int token) throws CompileException {
		throw syntaxError(token2str(token) + " expected");
	}

	private void errorLimit(FuncState fs, int limit, String what) throws CompileException {
		String msg = fs.lineDefined == 0
			? "main function has more than " + limit + " " + what
			: "function at line " + fs.lineDefined + " has more than " + limit + " " + what;
		throw lexer.lexError(msg, 0);
	}

	private void checkLimit(FuncState fs, int value, int limit, String msg) throws CompileException {
		if (value > limit) errorLimit(fs, limit, msg);
	}

	private boolean testNext(int c) throws CompileException, LuaError, UnwindThrowable {
		if (lexer.token.token() == c) {
			lexer.nextToken();
			return true;
		} else {
			return false;
		}
	}

	void check(int c) throws CompileException {
		if (lexer.token.token() != c) errorExpected(c);
	}

	private void checkNext(int c) throws CompileException, LuaError, UnwindThrowable {
		check(c);
		lexer.nextToken();
	}

	private void checkCondition(boolean c, String msg) throws CompileException {
		if (!c) throw syntaxError(msg);
	}

	private void checkMatch(int what, int who, int where) throws CompileException, LuaError, UnwindThrowable {
		if (testNext(what)) return;

		if (where == lexer.token.line()) {
			errorExpected(what);
		} else {
			throw syntaxError(token2str(what) + " expected " + "(to close " + token2str(who) + " at line " + where + ")");
		}
	}

	private LuaString strCheckName() throws CompileException, LuaError, UnwindThrowable {
		check(TK_NAME);
		LuaString ts = lexer.token.stringContents();
		lexer.nextToken();
		return ts;
	}

	private void codeString(ExpDesc e, LuaString s) {
		e.init(ExpKind.VK, fs.stringK(s));
	}

	private void checkName(ExpDesc e) throws CompileException, LuaError, UnwindThrowable {
		codeString(e, strCheckName());
	}

	private int registerLocal(LuaString name) {
		FuncState fs = this.fs;
		int index = fs.locals.size();
		fs.locals.add(new LocalVariable(name, 0, 0));
		return index;
	}

	private void newLocal(String name) throws CompileException {
		newLocal(lexer.newString(name));
	}

	private void newLocal(LuaString name) throws CompileException {
		FuncState fs = this.fs;
		checkLimit(fs, activeVariableSize + 1, LuaC.LUAI_MAXVARS, "local variables");
		if (activeVariableSize >= activeVariables.length) {
			activeVariables = LuaC.realloc(activeVariables, activeVariables.length * 2 + 1);
		}
		activeVariables[activeVariableSize++] = (short) registerLocal(name);
	}

	LocalVariable getLocal(FuncState fs, int i) {
		return fs.locals.get(activeVariables[fs.firstLocal + i]);
	}

	private void adjustLocalVars(int nVars) {
		FuncState fs = this.fs;
		fs.activeVariableCount = (short) (fs.activeVariableCount + nVars);
		for (; nVars > 0; nVars--) {
			getLocal(fs, fs.activeVariableCount - nVars).startpc = fs.pc;
		}
	}

	void removeVars(int toLevel) {
		FuncState fs = this.fs;
		activeVariableSize -= fs.activeVariableCount - toLevel;
		while (fs.activeVariableCount > toLevel) {
			getLocal(fs, --fs.activeVariableCount).endpc = fs.pc;
		}
	}

	private int searchUpvalue(FuncState fs, LuaString name) {
		// Search for an existing upvalue
		for (int i = 0; i < fs.upvalues.size(); i++) {
			if (fs.upvalues.get(i).name() == name) return i;
		}

		return -1;
	}

	private int newUpvalue(FuncState fs, LuaString name, ExpDesc v) throws CompileException {
		// Add a new upvalue
		checkLimit(fs, fs.upvalues.size(), LUAI_MAXUPVALUES, "upvalues");
		assert v.kind == ExpKind.VLOCAL || v.kind == ExpKind.VUPVAL;
		int index = fs.upvalues.size();
		fs.upvalues.add(new Prototype.UpvalueInfo(name, v.kind == ExpKind.VLOCAL, (byte) v.info));
		return index;
	}

	private int searchVar(FuncState fs, LuaString n) {
		for (int i = fs.activeVariableCount - 1; i >= 0; i--) {
			if (n == getLocal(fs, i).name) return i;
		}

		return -1;
	}

	/**
	 * Mark block where variable at given level was defined (to emit close instructions later).
	 */
	private void markUpvalue(FuncState fs, int level) {
		FuncState.BlockCnt block = fs.block;
		while (block != null && block.activeVariableCount > level) block = block.previous;
		if (block != null) block.upval = true;
	}

	/**
	 * Find variable with given name 'n'. If it is an upvalue, add this upvalue into all intermediate functions.
	 */
	private ExpKind singleVarAux(FuncState fs, LuaString n, ExpDesc var, boolean base) throws CompileException {
		if (fs == null) { // No more levels
			return ExpKind.VVOID; // Default is global
		}

		int v = searchVar(fs, n); // look up at current level
		if (v >= 0) {
			var.init(ExpKind.VLOCAL, v);
			if (!base) markUpvalue(fs, v); // local will be used as an upvalue
			return ExpKind.VLOCAL;
		} else {
			// Not found at current level, try an upvalue
			int index = searchUpvalue(fs, n);
			if (index < 0) {
				// No such upvalue, search in the parent.
				if (singleVarAux(fs.prev, n, var, false) == ExpKind.VVOID) return ExpKind.VVOID;
				index = newUpvalue(fs, n, var); // Add the new upvalue.
			}

			var.init(ExpKind.VUPVAL, index);
			return ExpKind.VUPVAL;
		}
	}

	private void singleVar(ExpDesc var) throws CompileException, LuaError, UnwindThrowable {
		var.position = lexer.token.position();

		LuaString varName = strCheckName();
		FuncState fs = this.fs;
		if (singleVarAux(fs, varName, var, true) == ExpKind.VVOID) { // Looking up a global
			var key = new ExpDesc();
			var global = singleVarAux(fs, envName, var, true); // var := _ENV
			assert global == ExpKind.VLOCAL || global == ExpKind.VUPVAL;
			codeString(key, varName); // key := $varName
			fs.indexed(var, key, var.position); // var := var[key]
		}
	}

	private void adjustAssign(int nvars, int nexps, ExpDesc e) throws CompileException {
		FuncState fs = this.fs;
		int extra = nvars - nexps;
		if (e.kind.hasMultiRet()) {
			extra++; // includes call itself
			if (extra < 0) extra = 0;
			fs.setReturns(e, extra); // last exp. provides the difference
			if (extra > 1) fs.reserveRegs(extra - 1);
		} else {
			if (e.kind != ExpKind.VVOID) fs.exp2NextReg(e); // Close last expression
			if (extra > 0) {
				int reg = fs.freeReg;
				fs.reserveRegs(extra);
				fs.nil(reg, extra);
			}
		}

		if (nexps > nvars) fs.freeReg -= nexps - nvars;
	}

	private void enterLevel() throws CompileException {
		nCcalls++;
		checkLimit(fs, nCcalls, LUAI_MAXCCALLS, "syntax levels");
	}

	private void leaveLevel() {
		nCcalls--;
	}

	/**
	 * Solves the goto at index 'g' to given 'label' and removes it from the list of pending gotos.
	 * <p>
	 * If it jumps into the scope of some variable, raises an error.
	 */
	private void closeGoto(int g, LabelDesc label) throws CompileException {
		var gt = pendingGotos.get(g);
		assert gt.name == label.name;

		if (gt.activeVariables < label.activeVariables) {
			var local = getLocal(fs, gt.activeVariables).name;
			throw semError("<goto " + gt.name + "> at line " + gt.line + " jumps into the scope of local '" + local + "'");
		}

		fs.patchList(gt.pc, label.pc);
		pendingGotos.remove(g);
	}

	/**
	 * Try to close a goto with existing labels; this solves backward jumps
	 *
	 * @param g The goto index.
	 * @return If a label was found.
	 */
	private boolean findLabel(int g) throws CompileException {
		var block = fs.block;
		var gt = pendingGotos.get(g);
		for (int i = block.firstLabel; i < activeLabels.size(); i++) {
			var label = activeLabels.get(i);
			if (label.name != gt.name) continue;

			assert block.upval || activeLabels.size() > block.firstLabel;
			if (gt.activeVariables > label.activeVariables) fs.patchClose(gt.pc, label.activeVariables);
			closeGoto(g, label);
			return true;
		}

		return false;
	}

	private int newLabelEntry(List<LabelDesc> labels, LuaString label, int line, int pc) throws CompileException {
		checkLimit(fs, labels.size() + 1, Short.MAX_VALUE, "labels/gotos");
		int index = labels.size();
		labels.add(new LabelDesc(label, pc, line, fs.activeVariableCount));
		return index;
	}

	/**
	 * Add a label with the given name
	 *
	 * @param label The label whose corresponding gotos should be "closed".
	 */
	private void findGotos(LabelDesc label) throws CompileException {
		int i = fs.block.firstGoto;

		while (i < pendingGotos.size()) {
			if (pendingGotos.get(i).name == label.name) {
				closeGoto(i, label);
			} else {
				i++;
			}
		}
	}

	/**
	 * Export pending gotos to outer level, to check them against outer labels; if the block being exited has upvalues,
	 * and the goto exits the scope of any variable (which can be the upvalue), close those variables being exited.
	 *
	 * @param bl The current block.
	 */
	private void moveGotosOut(FuncState.BlockCnt bl) throws CompileException {
		for (int i = bl.firstGoto; i < pendingGotos.size(); ) {
			var gt = pendingGotos.get(i);
			if (gt.activeVariables > bl.activeVariableCount) {
				if (bl.upval) fs.patchClose(gt.pc, bl.activeVariableCount);
				gt.activeVariables = bl.activeVariableCount;
			}
			if (!findLabel(i)) i++;
		}
	}

	private void enterBlock(FuncState fs, boolean isLoop) {
		enterBlock(fs, new FuncState.BlockCnt(), isLoop);
	}

	private void enterBlock(FuncState fs, FuncState.BlockCnt bl, boolean isLoop) {
		bl.isLoop = isLoop;
		bl.activeVariableCount = fs.activeVariableCount;
		bl.firstLabel = (short) activeLabels.size();
		bl.firstGoto = (short) pendingGotos.size();
		bl.upval = false;
		bl.previous = fs.block;
		fs.block = bl;
		assert fs.freeReg == fs.activeVariableCount;
	}

	/**
	 * Create a label named 'break' to resolve break statements
	 */
	private void breakLabel() throws CompileException {
		var label = newLabelEntry(activeLabels, lexer.newString("break"), 0, fs.pc);
		findGotos(activeLabels.get(label));
	}

	private CompileException undefGoto(LabelDesc gt) {
		// We report this error message on the label's line, rather than the line of the closing "end".
		var buffer = lexer.createErrorMessage(gt.line);
		if (Lex.isReserved(gt.name)) {
			buffer.append(gt.name).append(" outside loop at line ").append(Integer.toString(gt.line));
		} else {
			buffer.append("no visible label '").append(gt.name).append("' for <goto> at line ").append(Integer.toString(gt.line));
		}
		return new CompileException(buffer.toString());
	}

	private void leaveBlock(FuncState fs) throws CompileException {
		FuncState.BlockCnt bl = fs.block;
		if (bl.previous != null && bl.upval) {
			int j = fs.jump();
			fs.patchClose(j, bl.activeVariableCount);
			fs.patchToHere(j);
		}
		if (bl.isLoop) breakLabel();

		fs.block = bl.previous;
		removeVars(bl.activeVariableCount);

		assert bl.activeVariableCount == fs.activeVariableCount;
		fs.freeReg = fs.activeVariableCount; // free registers
		while (activeLabels.size() > bl.firstLabel) activeLabels.remove(activeLabels.size() - 1);
		if (bl.previous != null) {
			moveGotosOut(bl); // Update pending gotos to outer block
		} else if (bl.firstGoto < pendingGotos.size()) {
			// We have an unsolved goto - error.
			throw undefGoto(pendingGotos.get(bl.firstGoto));
		}
	}

	/**
	 * Codes instruction to create new closure in parent function.
	 */
	private void codeClosure(Prototype childPrototype, ExpDesc v) throws CompileException {
		FuncState current = fs;
		int index = current.children.size();
		current.children.add(childPrototype);

		v.init(ExpKind.VRELOCABLE, current.codeABx(OP_CLOSURE, 0, index));
		fs.exp2NextReg(v);
	}

	FuncState openFunc() throws CompileException {
		if (fs != null) checkLimit(fs, fs.children.size(), MAXARG_Bx, "functions");
		FuncState fs = new FuncState(lexer, this.fs, activeVariableSize, activeLabels.size());
		this.fs = fs;
		enterBlock(fs, false);
		return fs;
	}

	Prototype closeFunc() throws CompileException {
		FuncState fs = this.fs;

		fs.ret(0, 0); // final return
		leaveBlock(fs);
		assert fs.block == null;

		this.fs = fs.prev;
		return fs.toPrototype();
	}

	/*============================================================*/
	/* GRAMMAR RULES */
	/*============================================================*/

	/**
	 * Check whether current token is in the follow set of a block.
	 * <p>
	 * {@code until} closes syntactical blocks, but do not close scope, so it is handled separatly.
	 */
	private boolean blockFollow(boolean withUntil) {
		return switch (lexer.token.token()) {
			case TK_ELSE, TK_ELSEIF, TK_END, TK_EOS -> true;
			case TK_UNTIL -> withUntil;
			default -> false;
		};
	}

	private void statementList() throws CompileException, LuaError, UnwindThrowable {
		/* statlist -> { stat [`;'] } */
		while (!blockFollow(true)) {
			if (lexer.token.token() == TK_RETURN) {
				statement();
				return;
			}
			statement();
		}
	}

	private void fieldSelect(ExpDesc v) throws CompileException, LuaError, UnwindThrowable {
		/* field -> ['.' | ':'] NAME */
		ExpDesc key = new ExpDesc();
		fs.exp2AnyRegUp(v);
		long indexPos = lexer.token.position();
		lexer.nextToken(); // skip the dot or colon
		checkName(key);
		fs.indexed(v, key, indexPos);
	}

	private void yindex(ExpDesc v) throws CompileException, LuaError, UnwindThrowable {
		/* index -> '[' expr ']' */
		lexer.nextToken(); // skip the '['
		expression(v);
		fs.exp2Val(v);
		checkNext(']');
	}

	/*
	 ** {======================================================================
	 ** Rules for Constructors
	 ** =======================================================================
	 */

	static class ConsControl {
		final ExpDesc v = new ExpDesc(); /* last list item read */
		final ExpDesc table; /* table descriptor */
		int hashSize; /* total number of `record' elements */
		int arraySize; /* total number of array elements */
		int toStore; /* number of array elements pending to be stored */

		ConsControl(ExpDesc table) {
			this.table = table;
		}
	}

	private void recordField(ConsControl cc) throws CompileException, LuaError, UnwindThrowable {
		/* recfield -> (NAME | `['exp1`]') = exp1 */
		FuncState fs = this.fs;
		int reg = this.fs.freeReg;
		ExpDesc key = new ExpDesc();
		ExpDesc val = new ExpDesc();
		int rkkey;
		if (lexer.token.token() == TK_NAME) {
			checkLimit(fs, cc.hashSize, Lex.MAX_INT, "items in a constructor");
			checkName(key);
		} else { // lexer.token.token() == '['
			yindex(key);
		}
		cc.hashSize++;
		checkNext('=');
		rkkey = fs.exp2RK(key);
		expression(val);
		fs.codeABC(Lua.OP_SETTABLE, cc.table.info, rkkey, fs.exp2RK(val));
		fs.freeReg = reg; // free registers
	}

	void closeListField(FuncState fs, ConsControl cc) throws CompileException {
		if (cc.v.kind == ExpKind.VVOID) return; // there is no list item
		fs.exp2NextReg(cc.v);
		cc.v.kind = ExpKind.VVOID;
		if (cc.toStore == LFIELDS_PER_FLUSH) {
			fs.setList(cc.table.info, cc.arraySize, cc.toStore); // flush
			cc.toStore = 0; // no more items pending
		}
	}

	void lastListField(FuncState fs, ConsControl cc) throws CompileException {
		if (cc.toStore == 0) return;
		if (cc.v.kind.hasMultiRet()) {
			fs.setMultiRet(cc.v);
			fs.setList(cc.table.info, cc.arraySize, LUA_MULTRET);
			cc.arraySize--;  /* do not count last expression (unknown number of elements) */
		} else {
			if (cc.v.kind != ExpKind.VVOID) fs.exp2NextReg(cc.v);
			fs.setList(cc.table.info, cc.arraySize, cc.toStore);
		}
	}

	private void listField(ConsControl cc) throws CompileException, LuaError, UnwindThrowable {
		expression(cc.v);
		checkLimit(fs, cc.arraySize, Lex.MAX_INT, "items in a constructor");
		cc.arraySize++;
		cc.toStore++;
	}

	private void field(ConsControl cc) throws CompileException, LuaError, UnwindThrowable {
		// field -> listfield | recfield
		switch (lexer.token.token()) {
			case TK_NAME -> { // may be listfields or recfields
				if (lexer.lookahead().token() != '=') { // expression?
					listField(cc);
				} else {
					recordField(cc);
				}
			}
			case '[' -> recordField(cc);
			default -> listField(cc);
		}
	}

	private void constructor(ExpDesc t) throws CompileException, LuaError, UnwindThrowable {
		/* constructor -> ?? */
		FuncState fs = this.fs;
		int line = lexer.token.line();
		int pc = fs.codeABC(Lua.OP_NEWTABLE, 0, 0, 0);
		ConsControl cc = new ConsControl(t);
		t.init(ExpKind.VRELOCABLE, pc);
		cc.v.init(ExpKind.VVOID, 0); /* no value (yet) */
		fs.exp2NextReg(t); /* fix it at stack top (for gc) */
		checkNext('{');
		do {
			assert cc.v.kind == ExpKind.VVOID || cc.toStore > 0;
			if (lexer.token.token() == '}') break;
			closeListField(fs, cc);
			field(cc);
		} while (testNext(',') || testNext(';'));
		checkMatch('}', '{', line);
		lastListField(fs, cc);

		int insn = fs.code[pc];
		insn = LuaC.SETARG_B(insn, luaO_int2fb(cc.arraySize)); /* set initial array size */
		insn = LuaC.SETARG_C(insn, luaO_int2fb(cc.hashSize));  /* set initial table size */
		fs.code[pc] = insn;
	}

	/*
	 ** converts an integer to a "floating point byte", represented as
	 ** (eeeeexxx), where the real value is (1xxx) * 2^(eeeee - 1) if
	 ** eeeee != 0 and (xxx) otherwise.
	 */
	private static int luaO_int2fb(int x) {
		int e = 0;  /* expoent */
		while (x >= 16) {
			x = x + 1 >> 1;
			e++;
		}
		if (x < 8) {
			return x;
		} else {
			return e + 1 << 3 | x - 8;
		}
	}

	/* }====================================================================== */

	private void parlist() throws CompileException, LuaError, UnwindThrowable {
		/* parlist -> [ param { `,' param } ] */
		FuncState fs = this.fs;
		int numParams = 0;
		if (lexer.token.token() != ')') {  // is `parlist' not empty?
			do {
				switch (lexer.token.token()) {
					case TK_NAME -> { // param . NAME
						newLocal(strCheckName());
						numParams++;
					}
					case TK_DOTS -> { // param . `...'
						lexer.nextToken();
						fs.isVararg = true;
					}
					default -> throw syntaxError("<name> or " + LUA_QL("...") + " expected");
				}
			} while (!fs.isVararg && testNext(','));
		}
		adjustLocalVars(numParams);
		fs.numParams = fs.activeVariableCount;
		fs.reserveRegs(fs.activeVariableCount);  /* reserve register for parameters */
	}

	private void body(ExpDesc e, boolean needSelf, int line) throws CompileException, LuaError, UnwindThrowable {
		/* body -> `(' parlist `)' chunk END */
		FuncState newFuncState = openFunc();
		newFuncState.lineDefined = line;
		checkNext('(');
		if (needSelf) {
			newLocal("self");
			adjustLocalVars(1);
		}
		parlist();
		checkNext(')');
		statementList();
		newFuncState.lastLineDefined = lexer.token.line();
		checkMatch(TK_END, TK_FUNCTION, line);

		Prototype proto = closeFunc();
		codeClosure(proto, e);
	}

	private int expList1(ExpDesc v) throws CompileException, LuaError, UnwindThrowable {
		/* explist1 -> expr { `,' expr } */
		int n = 1; // at least one expression
		expression(v);
		while (testNext(',')) {
			fs.exp2NextReg(v);
			expression(v);
			n++;
		}
		return n;
	}

	private void funcArgs(ExpDesc f) throws CompileException, LuaError, UnwindThrowable {
		FuncState fs = this.fs;
		ExpDesc args = new ExpDesc();
		long position = lexer.token.position();
		int line = lexer.token.line();
		switch (lexer.token.token()) {
			case '(' -> { /* funcargs -> `(' [ explist1 ] `)' */
				lexer.nextToken();
				if (lexer.token.token() == ')') { // arg list is empty?
					args.kind = ExpKind.VVOID;
				} else {
					expList1(args);
					fs.setMultiRet(args);
				}
				checkMatch(')', '(', line);
			}
			case '{' ->  /* funcargs -> constructor */
				constructor(args);
			case TK_STRING -> { /* funcargs -> STRING */
				codeString(args, lexer.token.stringContents());
				/* must use `seminfo' before `next' */
				lexer.nextToken();
			}
			default -> throw syntaxError("function arguments expected");
		}
		assert f.kind == ExpKind.VNONRELOC;

		int base = f.info; /* base register for call */
		int nArgs;
		if (args.kind.hasMultiRet()) {
			nArgs = Lua.LUA_MULTRET; // open call
		} else {
			if (args.kind != ExpKind.VVOID) fs.exp2NextReg(args); // close last argument
			nArgs = fs.freeReg - (base + 1);
		}
		f.init(ExpKind.VCALL, fs.codeABCAt(Lua.OP_CALL, base, nArgs + 1, 2, position));
		// call remove function and arguments and leaves (unless changed) one result
		fs.freeReg = base + 1;
	}

	/*
	 ** {======================================================================
	 ** Expression parsing
	 ** =======================================================================
	 */

	private void primaryExpression(ExpDesc v) throws CompileException, LuaError, UnwindThrowable {
		// primaryexp -> NAME | '(' expr ')'
		switch (lexer.token.token()) {
			case '(' -> {
				int line = lexer.token.line();
				lexer.nextToken();
				expression(v);
				checkMatch(')', '(', line);
				fs.dischargeVars(v);
			}
			case TK_NAME -> {
				singleVar(v);
			}
			default -> throw syntaxError("unexpected symbol");
		}
	}

	private void suffixedExpression(ExpDesc v) throws CompileException, LuaError, UnwindThrowable {
		// suffixedexp -> primaryexp { '.' NAME | '[' exp ']' | ':' NAME funcargs | funcargs }
		FuncState fs = this.fs;
		primaryExpression(v);
		while (true) {
			switch (lexer.token.token()) {
				case '.' -> { // field
					fieldSelect(v);
				}
				case '[' -> { // `[' exp1 `]'
					ExpDesc key = new ExpDesc();
					fs.exp2AnyRegUp(v);
					long indexPos = lexer.token.position();
					yindex(key);
					fs.indexed(v, key, indexPos);
				}
				case ':' -> { // `:' NAME funcargs
					ExpDesc key = new ExpDesc();
					lexer.nextToken();
					checkName(key);
					fs.self(v, key);
					funcArgs(v);
				}
				case '(', TK_STRING, '{' -> { // funcargs
					fs.exp2NextReg(v);
					funcArgs(v);
				}
				default -> {
					return;
				}
			}
		}
	}

	private void simpleExpression(ExpDesc v) throws CompileException, LuaError, UnwindThrowable {
		/*
		 * simpleexp -> NUMBER | STRING | NIL | true | false | ... | constructor |
		 * FUNCTION body | suffixedexp
		 */
		switch (lexer.token.token()) {
			case TK_NUMBER -> {
				v.init(ExpKind.VKNUM, 0);
				v.setNval(lexer.token.numberContents());
			}
			case TK_STRING -> codeString(v, lexer.token.stringContents());
			case TK_NIL -> v.init(ExpKind.VNIL, 0);
			case TK_TRUE -> v.init(ExpKind.VTRUE, 0);
			case TK_FALSE -> v.init(ExpKind.VFALSE, 0);
			case TK_DOTS -> { /* vararg */
				FuncState fs = this.fs;
				checkCondition(fs.isVararg, "cannot use " + LUA_QL("...") + " outside a vararg function");
				v.init(ExpKind.VVARARG, fs.codeABC(Lua.OP_VARARG, 0, 1, 0));
			}
			case '{' -> { /* constructor */
				constructor(v);
				return;
			}
			case TK_FUNCTION -> {
				lexer.nextToken();
				body(v, false, lexer.token.line());
				return;
			}
			default -> {
				suffixedExpression(v);
				return;
			}
		}

		lexer.nextToken();
	}

	/*
	 ** subexpr -> (simpleexp | unop subexpr) { binop subexpr }
	 ** where `binop' is any binary operator with a priority higher than `limit'
	 */
	private BinOpr subExpression(ExpDesc v, int limit) throws CompileException, LuaError, UnwindThrowable {
		enterLevel();
		UnOpr unop = UnOpr.ofToken(lexer.token.token());
		if (unop != null) {
			long opPosition = lexer.token.position();
			lexer.nextToken();
			subExpression(v, UnOpr.PRIORITY);
			fs.prefix(unop, v, opPosition);
		} else {
			simpleExpression(v);
		}
		// expand while operators have priorities higher than `limit'
		BinOpr binop = BinOpr.ofToken(lexer.token.token());
		while (binop != null && binop.left > limit) {
			long position = lexer.token.position();
			lexer.nextToken();

			fs.infix(binop, v);
			// read sub-expression with higher priority
			ExpDesc v2 = new ExpDesc();
			BinOpr nextop = subExpression(v2, binop.right);
			fs.posfix(binop, v, v2, position);
			binop = nextop;
		}
		leaveLevel();
		return binop; // return first untreated operator
	}

	private void expression(ExpDesc v) throws CompileException, LuaError, UnwindThrowable {
		subExpression(v, 0);
	}

	/* }==================================================================== */

	/*
	 ** {======================================================================
	 ** Rules for Statements
	 ** =======================================================================
	 */

	private void block() throws CompileException, LuaError, UnwindThrowable {
		/* block -> chunk */
		enterBlock(fs, false);
		statementList();
		leaveBlock(fs);
	}

	/*
	 ** Structure to chain all variables in the left-hand side of an assignment
	 */
	static class LhsAssign {
		final LhsAssign prev;
		final ExpDesc v = new ExpDesc(); // variable (global, local, upvalue, or indexed)

		LhsAssign(LhsAssign prev) {
			this.prev = prev;
		}
	}

	/*
	 * Check whether, in an assignment to a local variable, the local variable
	 * is needed in a previous assignment (to a table). If so, save original
	 * local value in a safe place and use this safe copy in the previous
	 * assignment.
	 */
	private void checkConflict(LhsAssign lh, ExpDesc v) throws CompileException {
		FuncState fs = this.fs;
		int extra = fs.freeReg;  // eventual position to save local variable
		boolean conflict = false;
		for (; lh != null; lh = lh.prev) {
			if (lh.v.kind == ExpKind.VINDEXED) {
				// table is the upvalue/local being assigned now?
				if (lh.v.tableType == v.kind && lh.v.info == v.info) {
					conflict = true;
					lh.v.tableType = ExpKind.VLOCAL;
					lh.v.info = extra;  // previous assignment will use safe copy
				}

				// index is the local being assigned? (index cannot be upvalue)
				if (v.kind == ExpKind.VLOCAL && lh.v.aux == v.info) {
					conflict = true;
					lh.v.aux = extra;  // previous assignment will use safe copy
				}
			}
		}

		if (conflict) {
			var opcode = v.kind == ExpKind.VLOCAL ? OP_MOVE : OP_GETUPVAL;
			fs.codeABC(opcode, extra, v.info, 0); /* make copy */
			fs.reserveRegs(1);
		}
	}

	private void assignment(LhsAssign lh, int nvars) throws CompileException, LuaError, UnwindThrowable {
		ExpDesc e = new ExpDesc();
		checkCondition(lh.v.kind.isVar(), "syntax error");
		if (testNext(',')) { // assignment -> `,' primaryexp assignment
			LhsAssign nv = new LhsAssign(lh);
			suffixedExpression(nv.v);
			if (nv.v.kind != ExpKind.VINDEXED) checkConflict(lh, nv.v);
			assignment(nv, nvars + 1);
		} else {  /* assignment . `=' explist1 */
			checkNext('=');
			int nexps = expList1(e);
			if (nexps != nvars) {
				adjustAssign(nvars, nexps, e);
			} else {
				fs.setOneRet(e); // close last expression
				fs.storeVar(lh.v, e);
				return; // avoid default
			}
		}
		e.init(ExpKind.VNONRELOC, fs.freeReg - 1); // default assignment
		fs.storeVar(lh.v, e);
	}

	private int cond() throws CompileException, LuaError, UnwindThrowable {
		/* cond -> exp */
		ExpDesc v = new ExpDesc();
		expression(v); // read condition
		if (v.kind == ExpKind.VNIL) v.kind = ExpKind.VFALSE; // `falses' are all equal here
		fs.goIfTrue(v);
		return v.f.value;
	}

	private void gotoLabel(int pc, int line, LuaString label) throws CompileException {
		int g = newLabelEntry(pendingGotos, label, line, pc);
		findLabel(g); // Link if label already defined.
	}

	private void breakStmt(int pc) throws CompileException, LuaError, UnwindThrowable {
		int line = lexer.token.line();
		lexer.nextToken();
		gotoLabel(pc, line, lexer.newString("break"));
	}

	private void gotoStat(int pc) throws CompileException, LuaError, UnwindThrowable {
		int line = lexer.token.line();
		lexer.nextToken();
		gotoLabel(pc, line, strCheckName());
	}

	/**
	 * Check for repeated labels on the same block.
	 *
	 * @param name The name of the label.
	 */
	private void checkRepeated(LuaString name) throws CompileException {
		for (int i = fs.firstLabel; i < activeLabels.size(); i++) {
			var label = activeLabels.get(i);
			if (label.name == name) throw semError("label '" + name + "' already defined on line " + label.line);
		}
	}

	/**
	 * Skip no-op statements
	 */
	private void skipNoOpStatements() throws CompileException, LuaError, UnwindThrowable {
		while (lexer.token.token() == ';' || lexer.token.token() == TK_DBCOLON) statement();
	}

	private void labelStat(LuaString name, int line) throws CompileException, LuaError, UnwindThrowable {
		// label -> '::' NAME '::'
		checkRepeated(name);
		checkNext(TK_DBCOLON);
		var label = newLabelEntry(activeLabels, name, line, fs.getLabel());
		skipNoOpStatements();
		if (blockFollow(false)) {
			// If label is last statement in the block, assume locals are already out of scope.
			activeLabels.get(label).activeVariables = fs.block.activeVariableCount;
		}
		findGotos(activeLabels.get(label));
	}

	private void whileStmt() throws CompileException, LuaError, UnwindThrowable {
		/* whilestat -> WHILE cond DO block END */
		int line = lexer.token.line();
		lexer.nextToken(); // Skip WHILE

		FuncState fs = this.fs;
		FuncState.BlockCnt bl = new FuncState.BlockCnt();
		int whileInit = fs.getLabel();
		int contExit = cond();
		enterBlock(fs, bl, true);
		checkNext(TK_DO);
		block();
		fs.patchList(fs.jump(), whileInit);
		checkMatch(TK_END, TK_WHILE, line);
		leaveBlock(fs);
		fs.patchToHere(contExit); // false conditions finish the loop
	}

	private void repeatStmt() throws CompileException, LuaError, UnwindThrowable {
		/* repeatstat -> REPEAT block UNTIL cond */
		int line = lexer.token.line();
		lexer.nextToken(); // Skip REPEAT

		FuncState fs = this.fs;
		int repeatInit = fs.getLabel();
		FuncState.BlockCnt bl1 = new FuncState.BlockCnt();
		FuncState.BlockCnt bl2 = new FuncState.BlockCnt();
		enterBlock(fs, bl1, true); /* loop block */
		enterBlock(fs, bl2, false); /* scope block */
		statementList();
		checkMatch(TK_UNTIL, TK_REPEAT, line);
		int condexit = cond(); // read condition (inside scope block)
		if (bl2.upval) fs.patchClose(condexit, bl2.activeVariableCount); // Close upvalues
		leaveBlock(fs); // finish scope */
		fs.patchList(condexit, repeatInit); /* close the loop */
		leaveBlock(fs); /* finish loop */
	}

	private void exp1() throws CompileException, LuaError, UnwindThrowable {
		ExpDesc e = new ExpDesc();
		expression(e);
		fs.exp2NextReg(e);
	}

	private void forBody(int base, long position, int nvars, boolean isNum) throws CompileException, LuaError, UnwindThrowable {
		/* forbody -> DO block */
		FuncState.BlockCnt bl = new FuncState.BlockCnt();
		FuncState fs = this.fs;
		adjustLocalVars(3); /* control variables */
		checkNext(TK_DO);
		int prep = isNum ? fs.codeAsBx(Lua.OP_FORPREP, base, NO_JUMP) : fs.jump();
		enterBlock(fs, bl, false); // scope for declared variables
		adjustLocalVars(nvars);
		fs.reserveRegs(nvars);
		block();
		leaveBlock(fs); /* end of scope for declared variables */
		fs.patchToHere(prep);
		int endFor;
		if (isNum) {
			endFor = fs.codeAsBxAt(OP_FORLOOP, base, NO_JUMP, position);
		} else {
			fs.codeABCAt(OP_TFORCALL, base, 0, nvars, position);
			endFor = fs.codeAsBxAt(OP_TFORLOOP, base + 2, NO_JUMP, position);
		}
		fs.patchList(endFor, prep + 1);
	}

	private void forNum(LuaString varName, long position) throws CompileException, LuaError, UnwindThrowable {
		/* fornum -> NAME = exp1,exp1[,exp1] forbody */
		FuncState fs = this.fs;
		int base = fs.freeReg;
		newLocal("(for index)");
		newLocal("(for limit)");
		newLocal("(for step)");
		newLocal(varName);
		checkNext('=');
		exp1(); /* initial value */
		checkNext(',');
		exp1(); /* limit */
		if (testNext(',')) {
			exp1(); /* optional step */
		} else { /* default step = 1 */
			fs.codeK(fs.freeReg, fs.numberK(LuaInteger.valueOf(1)));
			fs.reserveRegs(1);
		}
		forBody(base, position, 1, true);
	}

	private void forList(LuaString indexName) throws CompileException, LuaError, UnwindThrowable {
		/* forlist -> NAME {,NAME} IN explist1 forbody */
		FuncState fs = this.fs;
		ExpDesc e = new ExpDesc();
		int nvars = 4;
		int base = fs.freeReg;
		/* create control variables */
		newLocal("(for generator)");
		newLocal("(for state)");
		newLocal("(for control)");
		/* create declared variables */
		newLocal(indexName);
		while (testNext(',')) {
			newLocal(strCheckName());
			nvars++;
		}
		checkNext(TK_IN);
		long position = lexer.token.position();
		adjustAssign(3, expList1(e), e);
		fs.checkStack(3); // extra space to call generator
		forBody(base, position, nvars - 3, false);
	}

	private void forStmt() throws CompileException, LuaError, UnwindThrowable {
		/* forstat -> FOR (fornum | forlist) END */
		long position = lexer.token.position();
		lexer.nextToken(); /* skip `for' */

		FuncState fs = this.fs;
		FuncState.BlockCnt bl = new FuncState.BlockCnt();
		enterBlock(fs, bl, true); // scope for loop and control variables
		LuaString varName = strCheckName(); // first variable name
		switch (lexer.token.token()) {
			case '=' -> forNum(varName, position);
			case ',', TK_IN -> forList(varName);
			default -> throw syntaxError(LUA_QL("=") + " or " + LUA_QL("in") + " expected");
		}
		checkMatch(TK_END, TK_FOR, Lex.unpackLine(position));
		leaveBlock(fs); // loop scope (`break' jumps to this point)
	}

	private void testThenBlock(IntPtr escapeList) throws CompileException, LuaError, UnwindThrowable {
		// test_then_block -> [IF | ELSEIF] cond THEN block
		lexer.nextToken(); // skip IF or ELSEIF

		// Read condition;
		ExpDesc v = new ExpDesc();
		expression(v);

		checkNext(TK_THEN);

		int jumpFalse;
		if (lexer.token.token() == TK_BREAK || (lexer.token.token() == TK_NAME && tryGoto())) {
			fs.goIfFalse(v); // Jump to label if condition is true
			enterBlock(fs, false);
			if (lexer.token.token() == TK_BREAK) {
				breakStmt(v.t.value);
			} else {
				gotoStat(v.t.value);
			}

			while (testNext(';')) ; // Skip semicolons

			// If the block is just some goto, we can bail immediately
			if (blockFollow(false)) {
				leaveBlock(fs);
				return;
			} else {
				// Otherwise we need to skip over the rest of the body.
				jumpFalse = fs.jump();
			}
		} else {
			fs.goIfTrue(v); // Skip over block if condition is false
			enterBlock(fs, false);
			jumpFalse = v.f.value;
		}

		statementList(); // `then' part
		leaveBlock(fs);

		// If followed by an else/elseif, emit something to jump over that block
		if (lexer.token.token() == TK_ELSE || lexer.token.token() == TK_ELSEIF) {
			fs.concat(escapeList, fs.jump());
		}

		fs.patchToHere(jumpFalse);
	}

	private void ifStat() throws CompileException, LuaError, UnwindThrowable {
		// ifstat -> IF cond THEN block {ELSEIF cond THEN block} [ELSE block] END
		int line = lexer.token.line();

		FuncState fs = this.fs;
		IntPtr escapeList = new IntPtr(NO_JUMP);
		testThenBlock(escapeList); /* IF cond THEN block */
		while (lexer.token.token() == TK_ELSEIF) testThenBlock(escapeList); // ELSEIF cond THEN block */
		if (testNext(TK_ELSE)) block(); // `else' part

		checkMatch(TK_END, TK_IF, line);
		fs.patchToHere(escapeList.value); // Patch list to "if" end.
	}

	private void localFunc() throws CompileException, LuaError, UnwindThrowable {
		FuncState fs = this.fs;
		newLocal(strCheckName());
		adjustLocalVars(1);

		ExpDesc b = new ExpDesc();
		body(b, false, lexer.token.line());
		getLocal(fs, b.info).startpc = fs.pc;
	}

	private void localStmt() throws CompileException, LuaError, UnwindThrowable {
		/* stat -> LOCAL NAME {`,' NAME} [`=' explist1] */
		int nvars = 0;
		ExpDesc e = new ExpDesc();
		do {
			newLocal(strCheckName());
			nvars++;
		} while (testNext(','));

		int nexps;
		if (testNext('=')) {
			nexps = expList1(e);
		} else {
			e.kind = ExpKind.VVOID;
			nexps = 0;
		}
		adjustAssign(nvars, nexps, e);
		adjustLocalVars(nvars);
	}

	private boolean funcName(ExpDesc v) throws CompileException, LuaError, UnwindThrowable {
		// funcname -> NAME {field} [`:' NAME]
		singleVar(v);
		while (lexer.token.token() == '.') fieldSelect(v);

		boolean needSelf = false;
		if (lexer.token.token() == ':') {
			needSelf = true;
			fieldSelect(v);
		}
		return needSelf;
	}

	private void funcStmt() throws CompileException, LuaError, UnwindThrowable {
		// funcstat -> FUNCTION funcname body
		long position = lexer.token.position();
		lexer.nextToken(); // skip FUNCTION
		ExpDesc v = new ExpDesc();
		ExpDesc b = new ExpDesc();
		boolean needSelf = funcName(v);
		body(b, needSelf, Lex.unpackLine(position));
		fs.storeVar(v, b);
		fs.fixPosition(position); // definition `happens' in the first line
	}

	private void exprStmt() throws CompileException, LuaError, UnwindThrowable {
		// stat -> func | assignment
		FuncState fs = this.fs;
		LhsAssign v = new LhsAssign(null);
		suffixedExpression(v.v);
		if (lexer.token.token() == '=' || lexer.token.token() == ',') { // stat -> assignment
			assignment(v, 1);
		} else if (v.v.kind == ExpKind.VCALL) {  // stat -> func
			fs.code[v.v.info] = LuaC.SETARG_C(fs.code[v.v.info], 1); // call statement uses no results
		} else {
			throw syntaxError("syntax error");
		}
	}

	private void returnStmt() throws CompileException, LuaError, UnwindThrowable {
		// stat -> RETURN explist
		FuncState fs = this.fs;
		int first, nret; // registers with returned values
		lexer.nextToken(); // skip RETURN
		if (blockFollow(true) || lexer.token.token() == ';') {
			first = nret = 0; // return no values
		} else {
			ExpDesc e = new ExpDesc();
			nret = expList1(e); /* optional return values */
			if (e.kind.hasMultiRet()) {
				fs.setMultiRet(e);
				if (e.kind == ExpKind.VCALL && nret == 1) { /* tail call? */
					int op = fs.code[e.info] = LuaC.SET_OPCODE(fs.code[e.info], OP_TAILCALL);
					assert GETARG_A(op) == fs.activeVariableCount;
				}
				first = fs.activeVariableCount;
				nret = Lua.LUA_MULTRET; /* return all values */
			} else {
				if (nret == 1) { // only one single value?
					first = fs.exp2AnyReg(e);
				} else {
					fs.exp2NextReg(e); // values must go to the `stack'
					first = fs.activeVariableCount; // return all `active' values
					assert nret == fs.freeReg - first;
				}
			}
		}
		fs.ret(first, nret);
		testNext(';');
	}

	private boolean tryGoto() throws CompileException, LuaError, UnwindThrowable {
		assert lexer.token.token() == TK_NAME;
		if (lexer.token.stringContents() != gotoName) return false;

		return lexer.lookahead().token() == TK_NAME;
	}

	private void statement() throws CompileException, LuaError, UnwindThrowable {
		enterLevel();

		switch (lexer.token.token()) {
			case ';' -> lexer.nextToken();
			case TK_IF -> ifStat(); // stat -> ifstat
			case TK_WHILE -> whileStmt(); // stat -> whiles-tat
			case TK_DO -> { /* stat -> DO block END */
				int line = lexer.token.line(); // may be needed for error messages
				lexer.nextToken(); // skip DO
				block();
				checkMatch(TK_END, TK_DO, line);
			}
			case TK_FOR -> forStmt(); // stat -> forstat
			case TK_REPEAT -> repeatStmt(); // stat -> repeatstat
			case TK_FUNCTION -> funcStmt(); // stat -> funcstat
			case TK_LOCAL -> { /* stat -> localstat */
				lexer.nextToken(); // skip LOCAL
				if (testNext(TK_FUNCTION)) { // local function?
					localFunc();
				} else {
					localStmt();
				}
			}
			case TK_DBCOLON -> {
				int line = lexer.token.line();
				lexer.nextToken(); // skip ::
				labelStat(strCheckName(), line);
			}
			case TK_RETURN -> returnStmt();  /* stat -> retstat */

			case TK_BREAK -> breakStmt(fs.jump()); /* stat -> breakstat */

			case TK_NAME -> {
				if (tryGoto()) {
					gotoStat(fs.jump());
				} else {
					exprStmt();
				}
			}

			default -> exprStmt();
		}

		assert fs.maxStackSize >= fs.freeReg && fs.freeReg >= fs.activeVariableCount;
		fs.freeReg = fs.activeVariableCount; /* free registers */

		leaveLevel();
	}

	/* }====================================================================== */

	public Prototype mainFunction() throws CompileException, LuaError, UnwindThrowable {
		FuncState funcstate = openFunc();
		funcstate.isVararg = true; /* main func. is always vararg */

		var v = new ExpDesc();
		v.init(ExpKind.VLOCAL, 0);
		newUpvalue(funcstate, envName, v);
		lexer.nextToken(); // read first token
		statementList();
		check(Lex.TK_EOS);

		Prototype prototype = closeFunc();
		assert funcstate.upvalues.size() == 1;
		assert fs == null;
		return prototype;
	}
}
