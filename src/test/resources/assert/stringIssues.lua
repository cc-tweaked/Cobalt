-- From penlight. They break normally.
local res = string.format("%5.2f %5.2f %5.2f!", 20, 5.2e-2, 52.3)
assert(res == "20.00  0.05 52.30!")

-- %b[] fails if no matches
assert(("foobar"):match("%b[]") == nil)

-- More advanced example
local ptrn = "\n ? ? ?(%b[]):[ \t]*\n?[ \t]*<?([^%s>]+)>?[ \t]*[ \t]+\n?[ \t]*[\"'(]([^\n]+)[\"')][ \t]*"
local str = "\nHello\n"

assert(str:match(ptrn) == nil)

-- More tests
assert(("[foobar]baz"):find("%b[]"))
assert(("baz[foobar]"):find("%b[]"))
assert(("[foobar]"):find("%b[]"))
assert(("[foobar"):find("%b[]") == nil)
assert(("[[[[foobar]]"):find("%b[]"))
assert(("foo"):match("") == "")

do
	-- Empty matches
	local s, e = ("foo"):find("")
	assert(s == 1 and e == 0)

	s, e = ("foo"):find("", 10)
	assert(s == 4 and e == 3)
end

-- GMatch loops forever
local text = [[
foo
bar
baz
]]

local lines = { "foo", "", "bar", "", "baz", "" }
local i = 1
for line in text:gmatch('[^\n]*') do
	assert(line == lines[i], ("%q ~= %q"):format(lines[i], line):gsub("\n", "n"))
	i = i + 1

	if i > 10 then
		error("Too many lines for gmatch")
	end
end

-- Check with matching group
local i = 1
for line in text:gmatch('([^\n]*)') do
	assert(line == lines[i], ("%q ~= %q"):format(lines[i], line):gsub("\n", "n"))
	i = i + 1

	if i > 10 then
		error("Too many lines for gmatch")
	end
end

-- \011 isn't whitespace
local chars = { 9, 10, 11, 12, 13, 32 }
for _, i in ipairs(chars) do
	assert(string.char(i):find("%s"), "Expected whitespace for " .. i)
end

-- string.find
local start, finish = ("--foo=bar"):match("%-%-(.+)"):find("=")
assert(start == 4, "expected 4, got " .. start)
assert(finish == 4, "expected 4, got " .. finish)

-- string.rep allow negative numbers
assert(("foobar"):rep(-1) == "")
assert(("foobar"):rep(-100) == "")
assert(("foobar"):rep(0) == "")

-- Frontier pattern
local out = {}
string.gsub("THE (QUICK) brOWN FOx JUMPS", "%f[%a]%u+%f[%A]", function(x)
	table.insert(out, x)
end)
for k, v in pairs(out) do
	print(k, v)
end
assert(out[1] == "THE")
assert(out[2] == "QUICK")
assert(out[3] == "JUMPS")

-- Malformed patterns
local function assertPtrnError(ptrn, err)
	local ok, msg = pcall(string.find, "", ptrn)
	if ok then
		error(("Expected failure for %q"):format(ptrn), 0)
	end
	if not msg:find(err, 1, true) then
		error(("Expected %q got %q, for %q"):format(err, msg, ptrn), 0)
	end
end

assertPtrnError("[", "malformed pattern (missing ']')")
assertPtrnError("[[", "malformed pattern (missing ']')")
assertPtrnError("[]", "malformed pattern (missing ']')")
assertPtrnError("[^", "malformed pattern (missing ']')")
assertPtrnError("(", "unfinished capture")
assertPtrnError("%", "malformed pattern (ends with '%')")
assertPtrnError("%f", "missing '[' after '%f' in pattern")
assertPtrnError("%b", "unbalanced pattern")

assert(("]"):find("[]]") == 1)

-- Historically rounded to a float, resulting in Infinity
assert(tostring(1e38) == "1e+38", "Got " .. tostring(1e38))
assert(tostring(1e39) == "1e+39", "Got " .. tostring(1e39))

do
	-- Malformed replace
	local s, n = string.gsub("test", "%S", "A%")
	assert(s == "A\0A\0A\0A\0" and n == 4)

	s, n = string.gsub("test", "%S", "%A")
	assert(s == "AAAA" and n == 4)
end

-- Tiny bits of string concatenation
local x, y, z = "foo", "bar", "baz"
assert(x .. y .. z == table.concat { x, y, z })

-- Whitespace on large characters
assert(tonumber(("\128")) == nil)
