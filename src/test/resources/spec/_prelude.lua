--- This provides a "standard" library for our test library. It's largely copied
-- from CC: Tweaked's mcfly testing library, which in turn is derived from
-- busted.


local function serialise(value, seen, indent)
	local ty = type(value)
	if ty == "string" then return (("%q"):format(value):gsub("\\n", "n"))
	elseif ty == "table" then
		if seen[value] then return tostring(value) end
		seen[value] = true

		local items = {}
		local len, contents_len = rawlen(value), 0
		for k, v in pairs(value) do
			local item
			if type(k) == "number" and (k % 1) == 0 and k >= 1 and k <= len then
				item = serialise(v, seen, indent + 1)
			elseif type(k) == "string" and k:match("^[%a_][%w_]*$") then
				item = ("%s = %s"):format(k, serialise(v, seen, indent + 1))
			else
				item = ("[%s] = %s"):format(serialise(k, seen, indent + 1), serialise(v, seen, indent + 1))
			end

			items[#items + 1] = item
			contents_len = contents_len + #item
			if item:find("\n") then contents_len = math.huge end
		end

		if contents_len <= 80 then return "{" .. table.concat(items, ", ") .. "}" end

		for i = 1, #items do items[i] = ("  "):rep(indent + 1) .. items[i] .. ",\n" end
		return "{\n" .. table.concat(items) .. ("  "):rep(indent) .. "}"
	else
		return tostring(value)
	end
end

--- Format an object in order to make it more readable
--
-- @param value The value to format
-- @treturn string The formatted value
local function format(value)
	-- TODO: Look into something like mbs's pretty printer.
	if type(value) == "string" and value:find("\n") then
		return "<<<\n" .. value .. "\n>>>"
	else
		return serialise(value, {}, 0)
	end
end

local expect_mt = {}
expect_mt.__index = expect_mt

function expect_mt:_fail(message)
	message = (self._extra or "Assertion failed") .. "\n" .. message
	fail(message)
end

--- Assert that this expectation has the provided value
--
-- @param value The value to require this expectation to be equal to
-- @throws If the values are not equal
function expect_mt:equals(value)
	if value ~= self.value then
		self:_fail(("Expected %s\n but got %s"):format(format(value), format(self.value)))
	end

	return self
end
expect_mt.equal = expect_mt.equals
expect_mt.eq = expect_mt.equals

--- Assert that this expectation does not equal the provided value
--
-- @param value The value to require this expectation to not be equal to
-- @throws If the values are equal
function expect_mt:not_equals(value)
	if value == self.value then
		self:_fail(("Expected any value but %s"):format(format(value)))
	end

	return self
end
expect_mt.not_equal = expect_mt.not_equals
expect_mt.ne = expect_mt.not_equals

--- Assert that this expectation has something of the provided type
--
-- @tparam string exp_type The type to require this expectation to have
-- @throws If it does not have that thpe
function expect_mt:type(exp_type)
	local actual_type = type(self.value)
	if exp_type ~= actual_type then
		self:_fail(("Expected value of type %s\nbut got %s"):format(exp_type, actual_type))
	end

	return self
end

local function matches(eq, exact, left, right)
	if left == right then return true end

	local ty = type(left)
	if ty ~= type(right) or ty ~= "table" then return false end

	-- If we've already explored/are exploring the left and right then return
	if eq[left] and eq[left][right] then return true end
	if not eq[left]  then eq[left] = { [right] = true } else eq[left][right] = true end
	if not eq[right] then eq[right] = { [left] = true } else eq[right][left] = true end

	-- Verify all pairs in left are equal to those in right
	for k, v in pairs(left) do
		if not matches(eq, exact, v, right[k]) then return false end
	end

	if exact then
		-- And verify all pairs in right are present in left
		for k in pairs(right) do
			if left[k] == nil then return false end
		end
	end

	return true
end

local function pairwise_equal(left, right)
	if left.n ~= right.n then return false end

	for i = 1, left.n do
		if left[i] ~= right[i] then return false end
	end

	return true
end

--- Assert that this expectation is structurally equivalent to
-- the provided object.
--
-- @param value The value to check for structural equivalence
-- @throws If they are not equivalent
function expect_mt:same(value)
	if not matches({}, true, self.value, value) then
		self:_fail(("Expected %s\n but got %s"):format(format(value), format(self.value)))
	end

	return self
end

--- Assert that this expectation contains all fields mentioned
-- in the provided object.
--
-- @param value The value to check against
-- @throws If this does not match the provided value
function expect_mt:matches(value)
	if not matches({}, false, value, self.value) then
		self:_fail(("Expected %s\nto match %s"):format(format(self.value), format(value)))
	end

	return self
end

--- Assert that this expectation matches a Lua pattern
--
-- @tparam string pattern The pattern to match against
-- @throws If it does not match this pattern.
function expect_mt:str_match(pattern)
	local actual_type = type(self.value)
	if actual_type ~= "string" then
		self:_fail(("Expected value of type string\nbut got %s"):format(actual_type))
	end
	if not self.value:find(pattern) then
		self:_fail(("Expected %q\n to match pattern %q"):format(self.value, pattern))
	end

	return self
end

--- Add extra information to this error message.
--
-- @tparam string message Additional message to prepend in the case of failures.
-- @return The current
function expect_mt:describe(message)
	self._extra = tostring(message)
	return self
end

local expect = {}
setmetatable(expect, expect)

--- Construct an expectation on the error message calling this function
-- produces
--
-- @tparam fun The function to call
-- @param ... The function arguments
-- @return The new expectation
function expect.error(fun, ...)
	local ok, res = pcall(fun, ...) local _, line = pcall(error, "", 2)
	if ok then fail("expected function to error") end
	if res:sub(1, #line) == line then
		res = res:sub(#line + 1)
	elseif res:sub(1, 7) == "pcall: " then
		res = res:sub(8)
	end
	return setmetatable({ value = res }, expect_mt)
end

--- Construct a new expectation from the provided value
--
-- @param value The value to apply assertions to
-- @return The new expectation
function expect:__call(value)
	return setmetatable({ value = value }, expect_mt)
end

_G.expect = expect
