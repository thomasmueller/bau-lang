# The Computer Language Benchmarks Game
# https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

import sys

def main():
    global acc, den, num
    n = 10000
    if len(sys.argv) > 1:
        n = int(sys.argv[1])
    acc, den, num = 0, 1, 1
    i, k = 0, 0
    while i < n:
        k += 1
        next_term(k)
        if num > acc:
            continue
        d = extract_digit(3)
        if d != extract_digit(4):
            continue
        eliminate_digit(d)
        print(chr(ord('0') + d), end = "")
        i += 1
        if i % 10 == 0:
            print("\t:%d" % i)

def extract_digit(nth):
    return (num * nth + acc) // den

def eliminate_digit(d):
    global acc, den, num
    acc -= den * d
    acc *= 10
    num *= 10

def next_term(k):
    global acc, den, num
    k2 = k * 2 + 1
    acc += num * 2
    acc *= k2
    den *= k2
    num *= k

main()
