-- The Computer Language Benchmarks Game
-- https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

local n = 4
if arg and #arg > 0 then
    n = tonumber(arg[1]) or 4
end

local function fannkuch(n)
    local perm1 = {}
    for i = 1, n do
        perm1[i] = i - 1 
    end
    local perm = {}
    local count = {}
    for i = 1, n do count[i] = 0 end
    local f, k, r, flips, nperm, checksum = 0, 0, 0, 0, 0, 0
    r = n
    while r > 0 do
        while r > 1 do
            count[r] = r
            r = r - 1
        end
        for i = 1, n do 
            perm[i] = perm1[i] 
        end
        -- Count flips and update max and checksum
        f = 0
        k = perm[1]
        while k ~= 0 do
            local jm = math.floor((k + 1) / 2)
            for j = 1, jm do
                local t = perm[j]
                perm[j] = perm[k + 2 - j]
                perm[k + 2 - j] = t
            end
            k = perm[1]
            f = f + 1
        end
        if f > flips then 
            flips = f 
        end
        if (nperm % 2) == 0 then 
            checksum = checksum + f 
        else 
            checksum = checksum - f 
        end
        -- Use incremental change to generate another permutation
        while true do
            if r == n then
                print(checksum)
                return flips
            end
            local p0 = perm1[1]
            for i = 1, r do
                perm1[i] = perm1[i + 1]
            end
            perm1[r + 1] = p0
            count[r + 1] = count[r + 1] - 1
            if count[r + 1] > 0 then
                break
            end
            r = r + 1
        end
        nperm = nperm + 1
    end
    return flips
end

print(string.format("Pfannkuchen(%d) = %d", n, fannkuch(n)))