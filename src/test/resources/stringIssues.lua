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
