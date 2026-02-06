// The Computer Language Benchmarks Game
// https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

import Foundation

func fannkuch(_ n: Int) -> Int {
    var perm1 = Array(0..<n)
    var perm = Array(repeating: 0, count: n)
    var count = Array(repeating: 0, count: n)
    var flips = 0
    var nperm = 0
    var checksum = 0
    var r = n
    while r > 0 {
        var i = 0
        while r != 1 {
            count[r - 1] = r
            r -= 1
        }
        for i in 0..<n {
            perm[i] = perm1[i]
        }
        // Count flips and update max and checksum
        var f = 0
        var k = perm[0]
        while k != 0 {
            i = 0
            while 2 * i < k {
                let t = perm[i]
                perm[i] = perm[k - i]
                perm[k - i] = t
                i += 1
            }
            k = perm[0]
            f += 1
        }
        flips = max(flips, f)
        checksum += (nperm % 2 == 0) ? f : -f
        // Use incremental change to generate another permutation
        var more = true
        while more {
            if r == n {
                print(checksum)
                return flips
            }
            let p0 = perm1[0]
            for i in 0..<r {
                perm1[i] = perm1[i + 1]
            }
            perm1[r] = p0
            count[r] -= 1
            if count[r] > 0 {
                more = false
            } else {
                r += 1
            }
        }
        nperm += 1
    }
    return flips
}

func main() {
    let n = CommandLine.arguments.count > 1 ? Int(CommandLine.arguments[1]) ?? 4 : 4
    let result = fannkuch(n)
    print("Pfannkuchen(\(n)) = \(result)")
}

main()
