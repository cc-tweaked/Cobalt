local create, ops

create = function(val) return setmetatable({ x = val }, ops) end
ops = {
	__add = function(x, y)
		local a, b = coroutine.yield(x, y)
		return create(a.x + b.x)
	end,
	__div = function(x, y)
		local a, b = coroutine.yield(x, y)
		return create(a.x / b.x)
	end,
	__concat = function(x, y)
		local a, b = coroutine.yield(x, y)
		return create(a.x .. b.x)
	end,
	__eq = function(x, y)
		local a, b = coroutine.yield(x, y)
		return a.x == b.x
	end,
	__lt = function(x, y)
		local a, b = coroutine.yield(x, y)
		return a.x < b.x
	end,
	__len = function(x)
		return coroutine.yield(x).x
	end,
	__index = function(tbl, key)
		local res = coroutine.yield(key)
		return res:upper()
	end,
	__newindex = function(tbl, key, val)
		local rKey, rVal = coroutine.yield(key, val)
		rawset(tbl, rKey, rVal .. "!")
	end,
}

local varA = create(2)
local varB = create(3)

-- Test basic ops
run(function()
	assertEquals(5, (varA + varB).x)
	assertEquals(5, (varB + varA).x)
	assertEquals(4, (varA + varA).x)
	assertEquals(6, (varB + varB).x)

	assertEquals(2 / 3, (varA / varB).x)
	assertEquals(3 / 2, (varB / varA).x)
	assertEquals(1, (varA / varA).x)
	assertEquals(1, (varB / varB).x)

	assertEquals("23", (varA .. varB).x)
	assertEquals("32", (varB .. varA).x)
	assertEquals("22", (varA .. varA).x)
	assertEquals("33", (varB .. varB).x)
	assertEquals("33333", (varB .. varB .. varB .. varB .. varB).x)

	assertEquals(false, varA == varB)
	assertEquals(false, varB == varA)
	assertEquals(true, varA == varA)
	assertEquals(true, varB == varB)

	assertEquals(true, varA < varB)
	assertEquals(false, varB < varA)
	assertEquals(false, varA < varA)
	assertEquals(false, varB < varB)

	assertEquals(true, varA <= varB)
	assertEquals(false, varB <= varA)
	assertEquals(true, varA <= varA)
	assertEquals(true, varB <= varB)

	assertEquals(2, #varA)

	assertEquals("HELLO", varA.hello)
	varA.hello = "bar"
	assertEquals("bar!", varA.hello)
end)

