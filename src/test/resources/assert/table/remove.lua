-- Ensures correct behaviour of table.remove on out of bounds lengths
local a = { 1, 2, 3 }

assert(select('#', table.remove(a, 0)) == 0)
assert(a[1] == 1)
assert(a[3] == 3)

assert(select('#', table.remove(a, 4)) == 0)
assert(a[1] == 1)
assert(a[3] == 3)
