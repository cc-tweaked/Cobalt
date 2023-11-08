local t = {}
do
	local i = 1
	local a, b, c, d
	t[1] = function () return a, b, c, d end
	::l1::
	local b
	do
		local c
		t[#t + 1] = function () return a, b, c, d end    -- t[2], t[4], t[6]
		if i > 2 then goto l2 end
		do
		local d
		t[#t + 1] = function () return a, b, c, d end   -- t[3], t[5]
		i = i + 1
		local a
		goto l1
		end
	end
end
::l2:: return t
