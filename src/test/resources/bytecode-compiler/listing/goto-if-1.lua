local first = true
local a = false
if true then
	goto LBL
	::loop::
	a = true
	::LBL::
	if first then
		first = false
		goto loop -- On Lua 5.2 this would jump to the assert
	end
end
assert(a)
