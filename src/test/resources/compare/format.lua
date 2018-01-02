local modifiers = { '+', '-', ' ', '#', '0' }
local all_modifiers = {}

local function gen_modifiers(idx, buffer)
	if idx == 0 then
		all_modifiers[#all_modifiers + 1] = buffer
	else
		gen_modifiers(idx - 1, buffer)
		gen_modifiers(idx - 1, buffer .. modifiers[idx])
	end
end

gen_modifiers(#modifiers, "")

local precisions = { "", ".0", ".1", ".6" }
local widths = { "", "1", "2", "6", "20" }

local function do_format(code, value)
	local ok, res = pcall(string.format, code, value)
	if not ok then
		return res:gsub("format.lua:%d+: ", "")
	else
		return res
	end
end

local function format(convs, values)
	for _, conv in ipairs(convs) do
		print("= " .. conv .. " =")
		for _, mod in ipairs(all_modifiers) do
			for _, width in ipairs(widths) do
				for _, precision in ipairs(precisions) do
					local default_code = "%" .. mod .. conv
					local code = "%" .. mod .. width .. precision .. conv

					for i = 1, #values do
						local value = values[i]
						local default_res = do_format(default_code, value)
						local res = do_format(code, value)

						if default_res ~= res or default_code == code then
							local value_code = type(value) == "string" and "q" or "s"
							print(string.format("string.format(%q, %" .. value_code .. ") == %q", code, value, res))
						end
					end
				end
			end
		end
	end
end

print("== Integers ==")
format({ 'i', 'd', 'o', 'u', 'x', 'X', }, {
	0, 1, -1, 2, -2, 3, -3,
	1e2, 1e4, 1e5, 1e6, 1e7,
})

print("== Floats ==")
format({ 'e', 'E', 'f', 'F', 'g', 'G', }, {
	0, 1, -1, 2, -2, 3, -3,
	math.abs(0 / 0), 1 / 0, -1 / 0,
	1e2, 1e4, 1e5, 1e6, 1e7,
	1e-2, 1e-4, 1e-5, 1e-6, 1e-7,
	123.456, 123.456e7, 123.456e-7,
	123.456789123, 123.456789123e7, 123.456789123e-7,
})

print("== Strings ==")
format({ 's' }, {
	"", "a", "aa", ("a"):rep(10), ("a"):rep(20), ("a"):rep(101)
})


