# https://github.com/jabbalaci/SpeedTests
N = 440_000_000

def main():
    cache: list[int] = get_cache()
    for n in range(0, N):
        if is_munchausen(n, cache):
            print(n)

def is_munchausen(number: int, cache: list[int]) -> bool:
    n = number
    total = 0
    while n > 0:
        digit = n % 10
        total += cache[digit]
        if total > number:
            return False
        n = n // 10
    return total == number

def get_cache() -> list[int]:
    return [0] + [i**i for i in range(1, 9 + 1)]

main()
