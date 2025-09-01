# The Computer Language Benchmarks Game
# https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

import strutils, os

proc main() =
    var n = 200
    let args = commandLineParams()
    if args.len > 0:
        n = parseInt(args[0])
    let w = n
    let h = n
    var bitNum = 0
    var byteAcc: uint8 = 0
    let iter = 50
    const z = 0.0
    const limit = 2.0
    stdout.write("P4\n", $w, " ", $h, "\n")
    for y in 0 ..< h:
        for x in 0 ..< w:
            var Zr = z
            var Zi = z
            var Tr = z
            var Ti = z
            let Cr = 2.0 * float64(x) / float64(w) - 1.5
            let Ci = 2.0 * float64(y) / float64(h) - 1.0
            var i = 0
            while i < iter and Tr + Ti <= limit * limit:
                Zi = 2.0 * Zr * Zi + Ci
                Zr = Tr - Ti + Cr
                Tr = Zr * Zr
                Ti = Zi * Zi
                inc i
            byteAcc = byteAcc shl 1
            if Tr + Ti <= limit * limit:
                byteAcc = byteAcc or 0x01'u8
            inc bitNum
            if bitNum == 8:
                stdout.write(char(byteAcc))
                byteAcc = 0
                bitNum = 0
            elif x == w - 1:
                byteAcc = byteAcc shl (8 - w mod 8)
                stdout.write(char(byteAcc))
                byteAcc = 0
                bitNum = 0

when isMainModule:
    main()
