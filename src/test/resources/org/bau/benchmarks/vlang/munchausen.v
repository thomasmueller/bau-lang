// https://github.com/jabbalaci/SpeedTests

import math

const limit = 440_000_000

fn main() {
    cache := get_cache()
    for i in 0 .. limit {
        if is_munchausen(i, cache) {
            println(i)
        }
    }
}

fn is_munchausen(number int, cache []int) bool {
    mut n := number
    mut total := 0
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

fn get_cache() []int {
    mut result := []int{len: 10}
    for i in 1 .. 10 {
        result[i] = int(math.pow(f64(i), f64(i)))
    }
    return result
}
