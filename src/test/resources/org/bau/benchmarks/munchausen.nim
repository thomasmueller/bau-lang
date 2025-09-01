# https://github.com/jabbalaci/SpeedTests

import math

func get_cache(): array[10, int] =
    result[0] = 0
    for i in 1 .. 9:
        result[i] = i ^ i

const
    MAX = 440_000_000
    cache: array[10, int] = get_cache()

func is_munchausen(number: int): bool =
    var
        n = number
        total = 0
    while n > 0:
        let digit = n mod 10
        total += cache[digit]
        if total > number:
            return false
        n = n div 10
    total == number

proc main() =
    for i in 0 ..< MAX:
        if is_munchausen(i):
            echo i

when isMainModule:
    main()