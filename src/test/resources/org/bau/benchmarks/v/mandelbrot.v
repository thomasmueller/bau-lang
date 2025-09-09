// The Computer Language Benchmarks Game
// https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

import os

fn main() {
    mut n := 200
    if os.args.len > 1 {
        n = os.args[1].int()
    }
    w := n
    h := n
    mut bit_num := 0
    mut byte_acc := u8(0)
    iter := 50
    limit := 2.0
    print('P4\n${w} ${h}\n')
    for y in 0 .. h {
        for x in 0 .. w {
            mut zr, mut zi, mut tr, mut ti := 0.0, 0.0, 0.0, 0.0
            cr := 2.0 * f64(x) / f64(w) - 1.5
            ci := 2.0 * f64(y) / f64(h) - 1.0
            for _ in 0 .. iter {
                if tr + ti > limit * limit {
                    break
                }
                zi = 2.0 * zr * zi + ci
                zr = tr - ti + cr
                tr = zr * zr
                ti = zi * zi
            }
            byte_acc <<= 1
            if tr + ti <= limit * limit {
                byte_acc |= u8(0x01)
            }
            bit_num++
            if bit_num == 8 {
                print(byte_acc.ascii_str())
                byte_acc = 0
                bit_num = 0
            } else if x == w - 1 {
                // pad remaining bits
                byte_acc <<= u8(8 - w % 8)
                print(byte_acc.ascii_str())
                byte_acc = 0
                bit_num = 0
            }
        }
    }
}