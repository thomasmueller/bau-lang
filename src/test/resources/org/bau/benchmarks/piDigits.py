# The Computer Language Benchmarks Game
# https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

import sys

def main():
    global tmp1, tmp2, acc, den, num
    n = 10000
    if len(sys.argv) > 1:
        n = int(sys.argv[1])
    tmp1 = 0
    tmp2 = 0
    acc = 0
    den = 1
    num = 1
    i = 0
    k = 0
    while i < n:
        k += 1
        next_term(k)
        if num > acc:
            continue
        d = extract_digit(3)
        if d != extract_digit(4):
            continue
        print(chr(ord('0') + d), end = "")
        i += 1
        if i % 10 == 0:
            print("\t:%d" % i)
        eliminate_digit(d)

def extract_digit(nth):
    global tmp1, tmp2, acc, den, num
    tmp1 = num * nth
    tmp2 = tmp1 + acc
    tmp1 = tmp2 // den
    return tmp1

def eliminate_digit(d):
    global acc, den, num
    acc = acc - den * d
    acc = acc * 10
    num = num * 10

def next_term(k):
    global acc, den, num
    k2 = k * 2 + 1
    acc = acc + num * 2
    acc = acc * k2
    den = den * k2
    num = num * k

main()
