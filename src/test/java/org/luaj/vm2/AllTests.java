/*******************************************************************************
 * Copyright (c) 2009 Luaj.org. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 ******************************************************************************/
package org.luaj.vm2;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.luaj.vm2.WeakTableTest.WeakKeyTableTest;
import org.luaj.vm2.WeakTableTest.WeakKeyValueTableTest;
import org.luaj.vm2.WeakTableTest.WeakValueTableTest;
import org.luaj.vm2.compiler.CompilerUnitTests;
import org.luaj.vm2.compiler.DumpLoadEndianIntTest;
import org.luaj.vm2.compiler.RegressionTests;
import org.luaj.vm2.compiler.SimpleTests;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	AllTests.VmTests.class,
	AllTests.TableTests.class,
	AllTests.CompilerTests.class,
	RequireClassTest.class,
	ErrorsTest.class,
})
public class AllTests {

	@RunWith(Suite.class)
	@Suite.SuiteClasses({
		TypeTest.class,
		UnaryBinaryOperatorsTest.class,
		MetatableTest.class,
		LuaOperationsTest.class,
		StringTest.class,
		OrphanedThreadTest.class,
	})
	public class VmTests {
	}

	@RunWith(Suite.class)
	@Suite.SuiteClasses({
		TableTest.class,
		TableArrayTest.class,
		TableHashTest.class,
		WeakValueTableTest.class,
		WeakKeyTableTest.class,
		WeakKeyValueTableTest.class,
	})
	public class TableTests {
	}

	@RunWith(Suite.class)
	@Suite.SuiteClasses({
		CompilerUnitTests.class,
		DumpLoadEndianIntTest.class,
		RegressionTests.class,
		SimpleTests.class,
	})
	public class CompilerTests {
	}
}
