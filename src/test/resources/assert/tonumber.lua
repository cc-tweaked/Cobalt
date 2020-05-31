-- tonumber("-") and variants should all be nil
local invalid = { "-", " -", "- ", " - " }
for _, k in pairs(invalid) do
	assert(tonumber(k) == nil, k .. " should not be a number")
end

