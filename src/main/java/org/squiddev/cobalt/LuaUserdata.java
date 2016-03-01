/**
 * ****************************************************************************
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
 * ****************************************************************************
 */
package org.squiddev.cobalt;

public class LuaUserdata extends LuaValue {

	public final Object m_instance;
	public LuaValue m_metatable;

	public LuaUserdata(Object obj) {
		m_instance = obj;
	}

	public LuaUserdata(Object obj, LuaValue metatable) {
		m_instance = obj;
		m_metatable = metatable;
	}

	@Override
	public String tojstring() {
		return String.valueOf(m_instance);
	}

	@Override
	public int type() {
		return Constants.TUSERDATA;
	}

	@Override
	public String typeName() {
		return "userdata";
	}

	public int hashCode() {
		return m_instance.hashCode();
	}

	public Object userdata() {
		return m_instance;
	}

	@Override
	public boolean isuserdata() {
		return true;
	}

	@Override
	public boolean isuserdata(Class<?> c) {
		return c.isAssignableFrom(m_instance.getClass());
	}

	@Override
	public Object touserdata() {
		return m_instance;
	}

	@Override
	public Object touserdata(Class<?> c) {
		return c.isAssignableFrom(m_instance.getClass()) ? m_instance : null;
	}

	@Override
	public Object optuserdata(Object defval) {
		return m_instance;
	}

	@Override
	public Object optuserdata(Class<?> c, Object defval) {
		if (!c.isAssignableFrom(m_instance.getClass())) {
			typeError(c.getName());
		}
		return m_instance;
	}

	@Override
	public LuaValue getMetatable(LuaState state) {
		return m_metatable;
	}

	@Override
	public LuaValue setMetatable(LuaState state, LuaValue metatable) {
		this.m_metatable = metatable;
		return this;
	}

	@Override
	public Object checkuserdata() {
		return m_instance;
	}

	@Override
	public Object checkuserdata(Class<?> c) {
		if (c.isAssignableFrom(m_instance.getClass())) {
			return m_instance;
		}
		return typeError(c.getName());
	}

	@Override
	public LuaValue get(LuaState state, LuaValue key) {
		return m_metatable != null ? gettable(state, this, key) : Constants.NIL;
	}

	@Override
	public void set(LuaState state, LuaValue key, LuaValue value) {
		if (m_metatable == null || !settable(state, this, key, value)) {
			throw new LuaError("cannot set " + key + " for userdata");
		}
	}

	public boolean equals(Object val) {
		if (this == val) {
			return true;
		}
		if (!(val instanceof LuaUserdata)) {
			return false;
		}
		LuaUserdata u = (LuaUserdata) val;
		return m_instance.equals(u.m_instance);
	}

	// equality w/o metatable processing
	@Override
	public boolean raweq(LuaValue val) {
		return val.raweq(this);
	}

	@Override
	public boolean raweq(LuaUserdata val) {
		return this == val || (m_metatable == val.m_metatable && m_instance.equals(val.m_instance));
	}

	// __eq metatag processing
	public boolean eqmt(LuaState state, LuaValue val) {
		return m_metatable != null && val.isuserdata() && eqmtcall(state, this, m_metatable, val, val.getMetatable(state));
	}
}
