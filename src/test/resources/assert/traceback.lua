local function f(n)
	if n == 0 then return debug.traceback() end
	if n % 4 == 0 then
		return f(n - 1)
	elseif n % 4 == 1 then
		return select(2, pcall(f, n - 1))
	else
		return (f(n - 1))
	end
end

local bt = f(50)
local expected = [[stack traceback:
	traceback.lua:2: in function <traceback.lua:1>
	[C]: in function 'pcall'
	traceback.lua:6: in upvalue 'f'
	traceback.lua:8: in upvalue 'f'
	traceback.lua:8: in function <traceback.lua:1>
	(...tail calls...)
	[C]: in function 'pcall'
	traceback.lua:6: in upvalue 'f'
	traceback.lua:8: in upvalue 'f'
	traceback.lua:8: in function <traceback.lua:1>
	(...tail calls...)
	[C]: in function 'pcall'
	...
	traceback.lua:6: in upvalue 'f'
	traceback.lua:8: in upvalue 'f'
	traceback.lua:8: in function <traceback.lua:1>
	(...tail calls...)
	[C]: in function 'pcall'
	traceback.lua:6: in upvalue 'f'
	traceback.lua:8: in upvalue 'f'
	traceback.lua:8: in function <traceback.lua:1>
	(...tail calls...)
	[C]: in function 'pcall'
	traceback.lua:6: in upvalue 'f'
	traceback.lua:8: in local 'f'
	traceback.lua:12: in main chunk]]

if bt == expected then return end
local function p(x, y)
	print(("%-50s | %-50s %s"):format(x:gsub("\t", "  "), y:gsub("\t", "  "), x == y and " " or "*"))
end

local bt_f = bt:gmatch("[^\n]+")
local exp_f = expected:gmatch("[^\n]+")
local bt_val, exp_val = bt_f(), exp_f()
while bt_val ~= nil and exp_val ~= nil do p(bt_val, exp_val) bt_val, exp_val = bt_f(), exp_f() end
while bt_val ~= nil do p(bt_val, " ") bt_val = bt_f() end
while exp_val ~= nil do p(" ", exp_val) exp_val = exp_f() end

error("Message mismatch")
