// The Computer Language Benchmarks Game
// https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

package main

import (
    "bufio"
    "flag"
    "fmt"
    "math/big"
    "os"
    "runtime"
    "strconv"
)

func main() {
    runtime.GOMAXPROCS(1)
    w := bufio.NewWriter(os.Stdout)
    defer w.Flush()
    line := make([]byte, 0, 10)
    var d, k int64
    var n = 10000
    flag.Parse()
    if flag.NArg() > 0 {
        n, _ = strconv.Atoi(flag.Arg(0))
    }
    for i := 1; i <= n; i++ {
        d, k = next_digit(k)
        line = append(line, byte(d) + '0')
        if len(line) == 10 {
            fmt.Fprintf(w, "%s\t:%d\n", string(line), i)
            line = line[:0]
        }
        eliminate_digit(d)
    }
}

var (
    tmp1  = big.NewInt(0)
    tmp2  = big.NewInt(0)
    y2    = big.NewInt(1)
    bigk  = big.NewInt(0)
    acc = big.NewInt(0)
    den = big.NewInt(1)
    num = big.NewInt(1)
    ten   = big.NewInt(10)
)

func next_term(k int64) int64 {
    for {
        k++
        y2.SetInt64(k * 2 + 1)
        bigk.SetInt64(k)
        tmp1.Lsh(num, 1)
        acc.Add(acc, tmp1)
        acc.Mul(acc, y2)
        den.Mul(den, y2)
        num.Mul(num, bigk)
        if acc.Cmp(num) > 0 {
            return k
        }
    }
}

func extract_digit(nth *big.Int) int64 {
    tmp1.Mul(nth, num)
    tmp2.Add(tmp1, acc)
    tmp1.Div(tmp2, den)
    return tmp1.Int64()
}

func next_digit(k int64) (int64, int64) {
    for {
        k = next_term(k)
        d3 := extract_digit(big.NewInt(3))
        d4 := extract_digit(big.NewInt(4))
        if d3 == d4 {
            return d3, k
        }
    }
}

func eliminate_digit(d int64) {
    tmp1.SetInt64(d)
    acc.Sub(acc, tmp1.Mul(den, tmp1))
    acc.Mul(acc, ten)
    num.Mul(num, ten)
}
