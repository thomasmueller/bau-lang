// The Computer Language Benchmarks Game
// https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

import os
import math.big

__global (
    tmp1 = big.integer_from_int(0)
    tmp2 = big.integer_from_int(0)
    y2 = big.integer_from_int(1)
    bigk = big.integer_from_int(0)
    acc = big.integer_from_int(0)
    den = big.integer_from_int(1)
    num = big.integer_from_int(1)
    ten = big.integer_from_int(10)
)

fn main() {
    mut n := 10000
    if os.args.len > 1 {
        n = os.args[1].int()
    }
    mut line := []u8{cap: 10}
    mut d := i64(0)
    mut k := i64(0)
    for i in 1 .. n + 1 {
        d, k = next_digit(k)
        line << u8(d) + `0`
        if line.len == 10 {
            println('${line.bytestr()}\t:${i}')
            line.clear()
        }
        eliminate_digit(d)
    }
}

fn next_term(kk i64) i64 {
    mut k := kk
    for {
        k++
        y2 = big.integer_from_i64(k * 2 + 1)
        bigk = big.integer_from_i64(k)
        tmp1 = num.left_shift(1)
        acc = acc + tmp1
        acc = acc * y2
        den = den * y2
        num = num * bigk
        if acc > num {
            return k
        }
    }
    return k
}

fn extract_digit(nth big.Integer) i64 {
    tmp1 = num * nth
    tmp2 = tmp1 + acc
    tmp1 = tmp2 / den
    return tmp1.int()
}

fn next_digit(k0 i64) (i64, i64) {
    mut k := k0
    for {
        k = next_term(k)
        d3 := extract_digit(big.integer_from_int(3))
        d4 := extract_digit(big.integer_from_int(4))
        if d3 == d4 {
            return d3, k
        }
    }
    return 0, k
}

fn eliminate_digit(d i64) {
    tmp1 = den * big.integer_from_i64(d)
    acc = acc - tmp1
    acc = acc * ten
    num = num * ten
}
