# The Computer Language Benchmarks Game
# https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

import std/[os, strutils, options]
import bigints

var
    acc = 0.initBigInt
    den = 1.initBigInt
    num = 1.initBigInt
    ten = 10.initBigInt

proc extractDigit(nth: int): int =
    let n = (num * nth.initBigInt + acc) div den
    result = toInt[int](n).get()

proc eliminateDigit(d: int) =
    acc -= den * d.initBigInt
    acc *= ten
    num *= ten

proc nextTerm(k: int) =
    let k2 = initBigInt(k * 2 + 1)
    acc += num.shl(1)
    acc *= k2
    den *= k2
    num *= k.initBigInt

proc main() =
    var n = 10000
    let args = commandLineParams()
    if args.len > 0:
        n = parseInt(args[0])
    var i = 0
    var k = 0
    while i < n:
        inc k
        nextTerm(k)
        if num > acc:
            continue
        let d = extractDigit(3)
        if d != extractDigit(4):
            continue
        eliminateDigit(d)
        stdout.write(chr(ord('0') + d))
        inc i
        if i mod 10 == 0:
            stdout.write("\t:", $i, "\n")
    stdout.write("\n")

when isMainModule:
    main()
