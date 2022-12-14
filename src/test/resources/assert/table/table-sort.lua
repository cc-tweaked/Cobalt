-- Ensure table.sort on sparse tables behaves identically to PUC Lua.

local testTable = {[1]="e",[2]="a",[3]="d",[4]="c",[8]="b"}

table.sort(testTable, function(a, b)
  if not a then
    return false
  end
  if not b then
    return true
  end
  return a < b
end)

assert(testTable[1]=="a")
assert(testTable[2]=="b")
assert(testTable[3]=="c")
assert(testTable[4]=="d")
assert(testTable[5]=="e")
