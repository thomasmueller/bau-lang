// https://github.com/jabbalaci/SpeedTests

import Foundation

let LIMIT = 440_000_000

let cache: [Int] = {
    var result = [Int](repeating: 0, count: 10)
    for i in 1...9 {
        result[i] = Int(pow(Double(i), Double(i)))
    }
    return result
}()

func isMunchausen(_ number: Int) -> Bool {
    var n = number
    var total = 0
    while n > 0 {
        let digit = n % 10
        total += cache[digit]
        if total > number {
            return false
        }
        n /= 10
    }
    return total == number
}

func main() {
    for i in 0..<LIMIT {
        if isMunchausen(i) {
            print(i)
        }
    }
}

main()