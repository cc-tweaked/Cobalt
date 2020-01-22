local a, b, c = unpack({ 1, 2, 3, 4, 5 }, nil, 2)
assert(a == 1)
assert(b == 2)
assert(c == nil)

local a, b, c = unpack({ 1, 2 }, nil, nil)
assert(a == 1)
assert(b == 2)
assert(c == nil)

local ok, err = pcall(rawset, {}, nil, 1)
assert(not ok and err:find("table index is nil"))

local ok, err = pcall(rawset, {}, 0 / 0, 1)
assert(not ok and err:find("table index is NaN"))
