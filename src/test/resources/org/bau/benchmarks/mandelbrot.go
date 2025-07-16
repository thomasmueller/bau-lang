// The Computer Language Benchmarks Game
// https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

package main

import (
    "bufio"
    "flag"
    "fmt"
    "os"
    "strconv"
)

func main() {
    flag.Parse()
    n := 200
    if flag.NArg() > 0 {
        n, _ = strconv.Atoi(flag.Arg(0))
    }
    out := bufio.NewWriter(os.Stdout)
    defer out.Flush()
    w := n
    h := n
    bitNum := 0
    byteAcc := byte(0)
    iter := 50
    const z = 0.0
    const limit = 2.0
    fmt.Fprintf(out, "P4\n%d %d\n", w, h)
    for y := 0; y < h; y++ {
        for x := 0; x < w; x++ {
            Zr, Zi, Tr, Ti := z, z, z, z
            Cr := 2 * float64(x) / float64(w) - 1.5
            Ci := 2 * float64(y) / float64(h) - 1
            for i := 0; i < iter && Tr + Ti <= limit * limit; i++ {
                Zi = 2 * Zr * Zi + Ci
                Zr = Tr - Ti + Cr
                Tr = Zr * Zr
                Ti = Zi * Zi
            }
            byteAcc <<= 1
            if Tr + Ti <= limit * limit {
                byteAcc |= 0x01
            }
            bitNum++
            if bitNum == 8 {
                out.WriteByte(byteAcc)
                byteAcc = 0
                bitNum = 0
            } else if x == w - 1 {
                byteAcc <<= 8 - w % 8
                out.WriteByte(byteAcc)
                byteAcc = 0
                bitNum = 0
            }
        }
    }
}
