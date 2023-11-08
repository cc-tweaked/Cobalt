local x
::L1::
local y -- cannot join this SETNIL with previous one
assert(y == nil)
y = true
if x == nil then
	x = 1
	goto L1
else
	x = x + 1
end
assert(x == 2 and y == true)
