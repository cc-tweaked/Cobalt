local endians = { ">", "<" }
local paddings = { "", "1", "2", "4", "8", "16" }

local function is_eq(a, b)
	return a == b or (a ~= a and b ~= b)
end

local function dump(tbl)
	local out = {}
	for i = 1, #tbl do
		out[i] = (type(tbl[i]) == "string" and "%q" or "%s"):format(tbl[i]):gsub("\\000", "\\0")
	end
	return table.concat(out, ", ")
end

local function pack(fmt, ...)
	local ok, packed = pcall(string.pack, fmt, ...)
	if not ok then return packed end

	local unpacked = { pcall(string.unpack, fmt, packed) }
	local ok = table.remove(unpacked, 1)

	local hex = ("%02x"):rep(#packed):format(packed:byte(1, -1))
	if not ok then return ("%s[%s]"):format(hex, unpacked[1]) end

	local len = table.remove(unpacked)
	if len ~= #packed + 1 then return ("%s[%d ~= %d]"):format(hex, len, #packed + 1) end

	local input = { ... }
	local eq = #input == #unpacked
	for i = 1, #input do if not is_eq(unpacked[i], input[i]) then eq = false break end end

	if not eq then return ("%s[%s]"):format(hex, dump(unpacked)) end

	return hex
end

local function pack_all(fmt, ...)
	io.write(("string.pack(%q, %s) ="):format(fmt, dump({ ... })))
	for _, endian in pairs(endians) do
		for _, padding in pairs(paddings) do
			local p = "!" .. padding .. endian
			io.write(("\t%s%s"):format(p, pack(p .. fmt, ...)))
		end
	end
	io.write("\n")
end

local bytes = { 0x7f, -0x80, 0x01 }
local ubytes = { 0xff, 0x01 }

local shorts = { 0x7fff, -0x8000, 0x01 }
local ushorts = { 0xffff, 0x0001 }

local ints = { 0x7fffffff, 0x01, -0x80000000 }
local uints = { 0x0000000001, 0xffffffffff, 0xdeadbeef }

local floats = { math.huge, -math.huge, math.abs(0/0), 1, 2, 3 }

local strings = { "a", "abc", "abcde" }

print("= Integers =")
for _, x in pairs(bytes) do pack_all("b", x) end
for _, x in pairs(ubytes) do pack_all("B", x) end

for _, x in pairs(shorts) do pack_all("h", x) end
for _, x in pairs(ushorts) do pack_all("H", x) end

for _, x in pairs(ints) do pack_all("l", x) end
for _, x in pairs(ints) do pack_all("j", x) end

for _, x in pairs(uints) do pack_all("L", x) end
for _, x in pairs(uints) do pack_all("J", x) end

print("\n= Variable width Integers =")
for i = 0, 4 do for _, x in pairs(bytes) do pack_all("i" .. math.floor(2^i), x) end end

print("\n= Floats =")
for _, x in pairs(floats) do pack_all("f", x) end
for _, x in pairs(floats) do pack_all("d", x) end
for _, x in pairs(floats) do pack_all("n", x) end

print("\n= Strings =")
for _, x in pairs(strings) do pack_all("c5", x) end
for _, x in pairs(strings) do pack_all("z", x) end
for _, x in pairs(strings) do pack_all("s4", x) end
for _, x in pairs(strings) do pack_all("s1", x) end

print("\n= Padding =")
pack_all("BXjB", 0xff, 0x7f)
pack_all("BxB", 0xff, 0x7f)
