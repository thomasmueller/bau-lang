# The Computer Language Benchmarks Game
# https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

import sys, struct

def main():
    cout = sys.stdout.buffer.write
    n = 200
    if len(sys.argv) > 1:
        n = int(sys.argv[1])
    w = h = n
    Iter = 50
    Limit = 2.0
    Zero = 0.0
    sys.stdout.buffer.write(f'P4\n{w} {h}\n'.encode())
    for y in range(h):
        bit_num = 0
        byte_acc = 0
        for x in range(w):
            Zr = Zi = Tr = Ti = Zero
            Cr = 2.0 * x / w - 1.5
            Ci = 2.0 * y / h - 1.0
            i = 0
            while i < Iter and (Tr + Ti) <= Limit * Limit:
                Zi = 2.0 * Zr * Zi + Ci
                Zr = Tr - Ti + Cr
                Tr = Zr * Zr
                Ti = Zi * Zi
                i += 1
            byte_acc <<= 1
            if Tr + Ti <= Limit * Limit:
                byte_acc |= 0x01
            bit_num += 1
            if bit_num == 8:
                sys.stdout.buffer.write(struct.pack('B', byte_acc))
                bit_num = 0
                byte_acc = 0
            elif x == w - 1:
                byte_acc <<= 8 - (w % 8)
                sys.stdout.buffer.write(struct.pack('B', byte_acc))
                bit_num = 0
                byte_acc = 0

main()