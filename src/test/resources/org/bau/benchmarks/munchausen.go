// https://github.com/jabbalaci/SpeedTests

package main

import (
    "fmt"
    "math"
)

const LIMIT = 440_000_000

func main() {
    cache := getCache()
    for i := 0; i < LIMIT; i++ {
        if isMunchausen(i, &cache) {
            fmt.Println(i)
        }
    }
}

func isMunchausen(number int, cache *[10]int) bool {
    n := number
    total := 0
    for n > 0 {
        digit := n % 10
        total += cache[digit]
        if total > number {
            return false
        }
        n /= 10
    }
    return total == number
}

func getCache() [10]int {
    var result [10]int
    for i := 1; i <= 9; i++ {
        result[i] = int(math.Pow(float64(i), float64(i)))
    }
    return result
}
