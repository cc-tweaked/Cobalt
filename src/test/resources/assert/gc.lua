local a = {}; setmetatable(a, { __mode = 'vk' });
local x, y, z = {}, {}, {}
a[1], a[2], a[3] = x, y, z

-- fill a with some `collectable' values
for i = 1, 3 do a[{}] = i end

collectgarbage()
debug.debug(a)
assert(next(a, 2) == 3)
