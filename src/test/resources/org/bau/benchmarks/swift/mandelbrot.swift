// The Computer Language Benchmarks Game
// https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

import Foundation

func mandelbrot(n: Int) {
    let w = n
    let h = n
    let iter = 50
    let limit = 2.0
    var bitNum = 0
    var byteAcc: UInt8 = 0
    FileHandle.standardOutput.write("P4\n\(w) \(h)\n".data(using: .ascii)!)
    for y in 0..<h {
        for x in 0..<w {
            var Zr = 0.0, Zi = 0.0, Tr = 0.0, Ti = 0.0
            let Cr = 2.0 * Double(x) / Double(w) - 1.5
            let Ci = 2.0 * Double(y) / Double(h) - 1.0
            var i = 0
            while i < iter && (Tr + Ti) <= limit * limit {
                Zi = 2.0 * Zr * Zi + Ci
                Zr = Tr - Ti + Cr
                Tr = Zr * Zr
                Ti = Zi * Zi
                i += 1
            }
            byteAcc <<= 1
            if Tr + Ti <= limit * limit {
                byteAcc |= 0x01
            }
            bitNum += 1
            if bitNum == 8 {
                FileHandle.standardOutput.write(Data([byteAcc]))
                bitNum = 0
                byteAcc = 0
            } else if x == w - 1 {
                byteAcc <<= 8 - (w % 8)
                FileHandle.standardOutput.write(Data([byteAcc]))
                bitNum = 0
                byteAcc = 0
            }
        }
    }
    fflush(stdout)
}

let n = CommandLine.arguments.count > 1 ? Int(CommandLine.arguments[1]) ?? 200 : 200
mandelbrot(n: n)

