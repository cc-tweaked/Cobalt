local exchange = { base = 11, primeMod = 625210769 }
local function modexp(base, exponent, modulo)
	local remainder = base
	for _ = 1, exponent-1 do
		-- print(remainder)
		remainder = remainder * remainder
		if remainder >= modulo then
			remainder = remainder % modulo
		end
	end
	return remainder
end

local secretKey = 800000
modexp(exchange.base, secretKey, exchange.primeMod)
