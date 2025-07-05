# The Computer Language Benchmarks Game
# https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

import sys

def main():
    n = 4
    if len(sys.argv) > 1:
        n = int(sys.argv[1])
    print(f"Pfannkuchen({n}) = {fannkuch(n)}")

def fannkuch(n):
    perm1 = list(range(n))
    count = [0] * n
    perm = [0] * n
    flips, flips, checksum, nperm = 0, 0, 0, 0
    r = n
    while r > 0:
        while r > 1:
            count[r - 1] = r
            r -= 1
        perm[:] = perm1[:]
        # Count flips and update max and checksum
        f = 0
        k = perm[0]
        while k != 0:
            for i in range(k // 2 + 1):
                perm[i], perm[k - i] = perm[k - i], perm[i]
                i += 1
            k = perm[0]
            f += 1
        if f > flips:
            flips = f
        if (nperm & 1) == 0:
            checksum += f
        else:
            checksum -= f
        # Use incremental change to generate another permutation
        while True:
            if r == n:
                print(checksum)
                return flips
            p0 = perm1[0]
            i = 0
            while i < r:
                j = i + 1
                perm1[i] = perm1[j]
                i = j
            perm1[r] = p0
            count[r] -= 1
            if count[r] > 0:
                break
            r += 1
        nperm += 1
    return flips

main()
