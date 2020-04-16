-- See https://github.com/SquidDev-CC/CC-Tweaked/issues/404
--
-- Tests that our behaviour of % is equivalent to Lua 5.3's.

local large_prime = 48721
local small_prime = 3
local exp_prime = small_prime^math.sqrt(large_prime)
local public_key = exp_prime%large_prime

print(public_key)
assert(public_key == 17511)
