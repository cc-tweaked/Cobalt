local t = os.time()
T = os.date("*t", t)

loadstring(os.date([[assert(T.year==%Y and T.month==%m and T.day==%d and
  T.hour==%H and T.min==%M and T.sec==%S and
  T.wday==%w+1 and T.yday==%j and type(T.isdst) == 'boolean')]], t))()

assert(os.time(T) == t)

T = os.date("!*t", t)
loadstring(os.date([[!assert(T.year==%Y and T.month==%m and T.day==%d and
  T.hour==%H and T.min==%M and T.sec==%S and
  T.wday==%w+1 and T.yday==%j and type(T.isdst) == 'boolean')]], t))()

do
	local T = os.date("*t")
	local t = os.time(T)
	assert(type(T.isdst) == 'boolean')
	T.isdst = nil
	local t1 = os.time(T)
	assert(t == t1) -- if isdst is absent uses correct default
end

t = os.time(T)
T.year = T.year - 1;
local t1 = os.time(T)
-- allow for leap years
assert(math.abs(os.difftime(t, t1) / (24 * 3600) - 365) < 2)

t = os.time()
t1 = os.time(os.date("*t"))
assert(os.difftime(t1, t) <= 2)

local t1 = os.time { year = 2000, month = 10, day = 1, hour = 23, min = 12, sec = 17 }
local t2 = os.time { year = 2000, month = 10, day = 1, hour = 23, min = 10, sec = 19 }
assert(os.difftime(t1, t2) == 60 * 2 - 2)

io.output(io.stdout)
local d = os.date('%d')
local m = os.date('%m')
local a = os.date('%Y')
local ds = os.date('%w') + 1
local h = os.date('%H')
local min = os.date('%M')
local s = os.date('%S')
io.write(string.format('test done on %2.2d/%2.2d/%d', d, m, a))
io.write(string.format(', at %2.2d:%2.2d:%2.2d\n', h, min, s))

local function assert_eq(exp, act)
	if exp ~= act then
		error(("Assertion error:\n Expected %q,\n      got %q"):format(exp, act), 2)
	end
end

assert_eq("Sun", os.date("%a", t1))
assert_eq("Sunday", os.date("%A", t1))
assert_eq("Oct", os.date("%b", t1))
assert_eq("October", os.date("%B", t1))
assert_eq("Sun Oct  1 23:12:17 2000", os.date("%c", t1))
assert_eq("20", os.date("%C", t1))
assert_eq("01", os.date("%d", t1))
assert_eq("10/01/00", os.date("%D", t1))
assert_eq(" 1", os.date("%e", t1))
assert_eq("2000-10-01", os.date("%F", t1))
assert_eq("00", os.date("%g", t1))
assert_eq("2000", os.date("%G", t1))
assert_eq("Oct", os.date("%h", t1))
assert_eq("23", os.date("%H", t1))
assert_eq("11", os.date("%I", t1))
assert_eq("275", os.date("%j", t1))
assert_eq("10", os.date("%m", t1))
assert_eq("12", os.date("%M", t1))
assert_eq("\n", os.date("%n", t1))
assert_eq("PM", os.date("%p", t1))
assert_eq("11:12:17 PM", os.date("%r", t1))
assert_eq("23:12", os.date("%R", t1))
assert_eq("17", os.date("%S", t1))
assert_eq("\t", os.date("%t", t1))
assert_eq("23:12:17", os.date("%T", t1))
assert_eq("7", os.date("%u", t1))
assert_eq("40", os.date("%U", t1))
assert_eq("39", os.date("%V", t1))
assert_eq("0", os.date("%w", t1))
assert_eq("39", os.date("%W", t1))
assert_eq("10/01/00", os.date("%x", t1))
assert_eq("23:12:17", os.date("%X", t1))
assert_eq("00", os.date("%y", t1))
assert_eq("2000", os.date("%Y", t1))
assert_eq("+0000", os.date("%z", t1))
assert_eq("UTC", os.date("%Z", t1))
assert_eq("%", os.date("%%", t1))

if false then
	local formats = "aAbBcCdDeFgGhHIjmMnprRStTuUVwWxXyYzZ%"
	local h = assert(io.open("time-result.lua", "w"))
	for i = 1, #formats do
		local format = formats:sub(i, i)
		h:write((("assert_eq(%q, os.date(\"%%%s\", t1))\n"):format(os.date('%' .. format, t1), format):gsub("\\\n", "\\n")))
	end
	h:close()
end
