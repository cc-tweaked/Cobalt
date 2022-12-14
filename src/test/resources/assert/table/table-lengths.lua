-- Ensure the length operator on sparse tables behaves identically to PUC Lua.
assert(#{ 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, nil, 17, 18, [33] = {} } == 18)
assert(#{ 1, 2, 3, nil, 5, nil, nil, 8, 9 } == 9)
assert(#{ 1, 2, 3, nil, 5, nil, 7, 8 } == 8)
assert(#{ 1, 2, 3, nil, 5, nil, 7, 8, 9 } == 9)
assert(#{ 1, nil, [2] = 2, 3 } == 3)
