# The Computer Language Benchmarks Game
# https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

import os, strutils, std/strformat

proc fannkuch(n: int): int =
    var
        perm1 = newSeq[int](n)
        perm = newSeq[int](n)
        count = newSeq[int](n)
        f, k, r, flips, nperm, checksum: int
    for i in 0 ..< n:
        perm1[i] = i
    r = n
    while r > 0:
        while r > 1:
            count[r - 1] = r
            dec r
        for i in 0 ..< n:
            perm[i] = perm1[i]
        # Count flips and update max and checksum
        f = 0
        k = perm[0]
        while k != 0:
            for i in 0 ..< (k div 2) + 1:
                let t = perm[i]
                perm[i] = perm[k - i]
                perm[k - i] = t
            k = perm[0]
            inc f
        if f > flips:
            flips = f
        if (nperm and 1) == 0:
            checksum += f
        else:
            checksum -= f
        # Use incremental change to generate another permutation
        while true:
            if r == n:
                echo checksum
                return flips
            let p0 = perm1[0]
            var i = 0
            while i < r:
                let j = i + 1
                perm1[i] = perm1[j]
                i = j
            perm1[r] = p0
            dec count[r]
            if count[r] > 0:
                break
            inc r
        inc nperm
    result = flips

proc main() =
    let args = commandLineParams()
    var n = 4
    if args.len > 0:
        n = parseInt(args[0])
    echo &"Pfannkuchen({n}) = {fannkuch(n)}"

when isMainModule:
    main()
