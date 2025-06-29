// The Computer Language Benchmarks Game
// https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

import Foundation
import BigInt

var acc = BigInt(0)
var den = BigInt(1)
var num = BigInt(1)
let ten = BigInt(10)

@MainActor
func extractDigit(_ nth: Int) -> Int {
    return Int(((num * BigInt(nth)) + acc) / den)
}

@MainActor
func eliminateDigit(_ d: Int) {
    acc -= den * BigInt(d)
    acc *= ten
    num *= ten
}

@MainActor
func nextTerm(_ k: Int) {
    acc += num << 1 // equivalent to num * 2
    let k2p1 = BigInt(k * 2 + 1)
    acc *= k2p1
    den *= k2p1
    num *= BigInt(k)
}

@MainActor
func main() {
    var n = 10_000
    if CommandLine.arguments.count > 1 {
        n = Int(CommandLine.arguments[1]) ?? 10_000
    }
    var i = 0
    var k = 0
    while i < n {
        k += 1
        nextTerm(k)
        if num > acc {
            continue
        }
        let d = extractDigit(3)
        if d != extractDigit(4) {
            continue
        }
        eliminateDigit(d)
        print(Character(UnicodeScalar(48 + d)!), terminator: "")
        i += 1
        if i % 10 == 0 {
            print(" : \(i)")
        }
    }
}

main()