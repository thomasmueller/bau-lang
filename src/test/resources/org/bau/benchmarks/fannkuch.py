# The Computer Language Benchmarks Game
# https://salsa.debian.org/benchmarksgame-team/benchmarksgame/
import sys

def main():
    n = 7
    if len(sys.argv) > 1:
        n = int(sys.argv[1])
    print(f"Pfannkuchen({n}) = {fannkuch(n)}")

def fannkuch(n):
    perm1 = list(range(n))
    count = [0] * n
    perm = [0] * n
    flips, max_flips, checksum, nperm = 0, 0, 0, 0
    r = n

    while True:
        while r != 1:
            count[r - 1] = r
            r -= 1

        perm[:] = perm1[:]

        # Count flips and update max and checksum
        f = 0
        k = perm[0]
        while k != 0:
            i = 0
            while 2 * i < k:
                perm[i], perm[k - i] = perm[k - i], perm[i]
                i += 1
            k = perm[0]
            f += 1

        if f > max_flips:
            max_flips = f
        if nperm % 2 == 0:
            checksum += f
        else:
            checksum -= f

        # Generate next permutation
        while True:
            if r == n:
                print(checksum)
                return max_flips
            p0 = perm1[0]
            for i in range(r - 1):
                perm1[i] = perm1[i + 1]
            perm1[r - 1] = p0
            count[r] -= 1
            if count[r] > 0:
                break
            r += 1

        nperm += 1

main()
