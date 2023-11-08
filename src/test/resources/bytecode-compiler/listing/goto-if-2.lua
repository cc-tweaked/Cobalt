local a = ...
if a == 1 then
	goto l1
	error("should never be here!")
elseif a == 2 then goto l2
elseif a == 3 then goto l3
elseif a == 4 then
	goto l1  -- go to inside the block
	error("should never be here!")
	::l1:: a = a + 1   -- must go to 'if' end
else
	goto l4
	::l4a:: a = a * 2; goto l4b
	error("should never be here!")
	::l4:: goto l4a
	error("should never be here!")
	::l4b::
end
do return a end
::l2:: do return "2" end
::l3:: do return "3" end
::l1:: return "1"
