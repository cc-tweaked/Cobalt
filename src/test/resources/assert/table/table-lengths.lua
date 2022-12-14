-- Ensure the length operator on sparse tables behaves identically to PUC Lua.
assert(#{ 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, nil, 17, 18, [33] = {} } == 18)
assert(#{ 1, 2, 3, nil, 5, nil, nil, 8, 9 } == 9)
assert(#{ 1, 2, 3, nil, 5, nil, 7, 8 } == 8)
assert(#{ 1, 2, 3, nil, 5, nil, 7, 8, 9 } == 9)
assert(#{ 1, nil, [2] = 2, 3 } == 3)

-- Ensure table.getn on sparse tables behaves identically to PUC Lua
print(table.getn({[1]="e",[2]="a",[3]="b",[4]="c"})==4)
print(table.getn({[1]="e",[2]="a",[3]="b",[4]="c",[8]="f"})==8)

-- Ensure table.maxn on sparse tables behaves identically to PUC Lua
print(table.maxn({[1]="e",[2]="a",[3]="b",[4]="c"})==4)
print(table.maxn({[1]="e",[2]="a",[3]="b",[4]="c",[8]="f"})==8)

-- Ensure rawlen on sparse tables behaves identically to PUC Lua
print(rawlen({[1]="e",[2]="a",[3]="b",[4]="c"})==4)
print(rawlen({[1]="e",[2]="a",[3]="b",[4]="c",[8]="f"})==8)
