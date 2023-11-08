-- if x back goto out of scope of upvalue
local X
goto L1

::L2:: goto L3

::L1:: do
  local a = {}
  if a then goto L2 end   -- jumping back out of scope of 'a'
end

::L3:: assert(X == true)   -- checks that 'a' was correctly closed
