package org.bau.emu.x86;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Emu86 {

    int ax, bx, cx, dx;
    int si, di;
    int ip, sp;
    boolean zf, cf, of, sf;
    // direction flag
    boolean df;
    byte[] mem = new byte[65536];
    ByteBuffer memBuff = ByteBuffer.wrap(mem);

    Emu86() {
        memBuff.order(ByteOrder.LITTLE_ENDIAN);
        sp = 0xffff;
    }

    int addHigh(int x, int y) {
        return (x & 0xff00) | (add8((x >> 8) & 0xff, y & 0xff) << 8);
    }

    int addLow(int x, int y) {
        return (x & 0xff) | add8(x & 0xff, y & 0xff);
    }

    int add8(int a, int b) {
        int res = a + b;
        int r = res & 0xff;
        cf = res > 0xff;
        zf = r == 0;
        of = ((a ^ r) & (b ^ r) & 0x80) != 0;
        sf = (r & 0x80) != 0;
        return r;
    }

    int add16(int a, int b) {
        int res = a + b;
        int r = res & 0xffff;
        cf = res > 0xffff;
        zf = r == 0;
        of = ((a ^ r) & (b ^ r) & 0x8000) != 0;
        sf = (r & 0x8000) != 0;
        return r;
    }

    int sub16(int a, int b) {
        int res = a - b;
        int r = res & 0xffff;
        cf = (a & 0xffff) < (b & 0xffff);
        zf = r == 0;
        of = ((a ^ b) & (a ^ r) & 0x8000) != 0;
        sf = (r & 0x8000) != 0;
        return r;
    }

    int sub8(int a, int b) {
        int res = a - b;
        int r = res & 0xff;
        cf = (a & 0xff) < (b & 0xff);
        zf = r == 0;
        of = ((a ^ b) & (a ^ r) & 0x80) != 0;
        sf = (r & 0x80) != 0;
        return r;
    }

    void cmp8(int a, int b) {
        sub8(a, b);
    }

    void cmp16(int a, int b) {
        sub16(a, b);
    }

    int andLow(int x, int y) {
        return (x & 0xff) | and8(x & 0xff, y & 0xff);
    }

    int mem8(int x) {
        return mem[x & 0xffff] & 0xff;
    }

    int mem16(int x) {
        return memBuff.getShort(x) & 0xffff;
    }

    int and8(int a, int b) {
        int res = a & b;
        int r = res & 0xff;
        cf = false;
        zf = r == 0;
        of = false;
        sf = (r & 0x80) != 0;
        return r;
    }

    int or8(int a, int b) {
        int res = a | b;
        int r = res & 0xff;
        cf = false;
        zf = r == 0;
        of = false;
        sf = (r & 0x80) != 0;
        return r;
    }

    void push(int value) {
        sp -= 2;
        memBuff.putShort(sp, (short) value);
    }

    int pop() {
        int result = mem16(sp);
        sp += 2;
        return result;
    }

    void cbw() {
        // convert byte to word
        int al = ax & 0xff;
        if ((al & 0x80) != 0) {
            ax = (al | 0xff00);
        } else {
            ax = al;
        }
    }

    void cld() {
        // clear direction flag
        df = false;
    }

    void std() {
        // set direction flag
        df = true;
    }

    void lodsb() {
        int val = mem8(si);
        ax = (ax & 0xff00) | val;
        if (df) {
            si = (si - 1) & 0xffff;
        } else {
            si = (si + 1) & 0xffff;
        }
    }

    void lodsw() {
        int val = mem16(si);
        ax = val;
        if (df) {
            si = (si - 1) & 0xffff;
        } else {
            si = (si + 1) & 0xffff;
        }
    }

    int dec16(int x) {
        boolean oldCf = cf;
        int result = sub16(x, 1);
        cf = oldCf;
        return result;
    }

    int dec8(int x) {
        boolean oldCf = cf;
        int result = sub8(x, 1);
        cf = oldCf;
        return result;
    }


    int inc16(int x) {
        boolean oldCf = cf;
        int result = add16(x, 1);
        cf = oldCf;
        return result;
    }

    int inc8(int x) {
        boolean oldCf = cf;
        int result = add8(x, 1);
        cf = oldCf;
        return result;
    }

    int neg16(int x) {
        int old = x & 0xffff;
        x = (-x) & 0xffff;
        zf = (x == 0);
        sf = (x & 0x8000) != 0;
        of = (old == 0x8000);
        return x;
    }

    void je(int target) {
        if (zf) {
            ip = target;
        }
    }

    void jne(int target) {
        if (!zf) {
            ip = target;
        }
    }

    void jnc(int target) {
        if (!cf) {
            ip = target;
        }
    }

    void jmp(int target) {
        ip = target;
    }

    void mul(int x) {
        long a = ax & 0xffff;
        long b = x & 0xffff;
        long result = a * b;
        int low = (int) (result & 0xffff);
        int high = (int) ((result >> 16) & 0xffff);
        ax = low;
        dx = high;
        cf = (high != 0);
        of = (high != 0);
        zf = (low == 0);
        sf = (low & 0x8000) != 0;
    }

    void imul(int x) {
        short a = (short) (ax & 0xffff);
        short b = (short) (x & 0xffff);
        long result = (long) a * (long) b;
        int low = (int) (result & 0xffff);
        int high = (int) ((result >> 16) & 0xffff);
        ax = low;
        dx = high;
        boolean isSignExtended = (low & 0x8000) == 0 ? (high == 0) : (high == 0xffff);
        cf = of = !isSignExtended;
        zf = (low == 0);
        sf = (low & 0x8000) != 0;
    }

    int xor16(int a, int b) {
        int av = a & 0xffff;
        int bv = b & 0xffff;
        int x = (av ^ bv) & 0xffff;
        zf = (x == 0);
        sf = (x & 0x8000) != 0;
        cf = false;
        of = false;
        return x;
    }

    void call(int address) {
        int returnAddress = ip + 1;
        push(returnAddress);
        ip = address;
    }

    void ret() {
        ip = pop();
    }

    int xchgAx(int other) {
        int old = ax;
        ax = other;
        return old;
    }

    void repCmpsb() {
        while (cx != 0) {
            cmpsb();
            cx = (cx - 1) & 0xffff;
        }
    }

    void movsb() {
        int src = si & 0xffff;
        int dest = di & 0xffff;
        mem[dest] = mem[src];
        if (df) {
            si = (si - 1) & 0xffff;
            di = (di - 1) & 0xffff;
        } else {
            si = (si + 1) & 0xffff;
            di = (di + 1) & 0xffff;
        }
    }

    void repMovsb() {
        while (cx != 0) {
            movsb();
            if (!zf) {
                break;
            }
            cx = (cx - 1) & 0xffff;
        }
    }

//    private int mov16(int old, int newValue) {
//        return newValue;
//    }
//
//    private int movLow(int old, int newValue) {
//        return (old & 0xff00) | (newValue & 0xff);
//    }
//
//    private void movByte(int pos, int newValue) {
//        mem[pos] = (byte) newValue;
//    }
//
//    private void movWord(int pos, int newValue) {
//        memBuff.putShort(pos, (short) newValue);
//    }

    private void cmpsb() {
        int src = si & 0xffff;
        int dest = di & 0xffff;
        int val1 = mem[src] & 0xff;
        int val2 = mem[dest] & 0xff;
        cmp8(val1, val2);
        if (df) {
            si = (si - 1) & 0xffff;
            di = (di - 1) & 0xffff;
        } else {
            si = (si + 1) & 0xffff;
            di = (di + 1) & 0xffff;
        }
    }

    void int0x10() {
        // BIOS Video Services
        int ah = (ax >>> 8) & 0xff;
        int al = ax & 0xff;
        if (ah == 0x0e) {
            System.out.print((char) al);
        } else if (ah == 0x00) {
            // Moves the blinking cursor to a specific (row, column)
        } else if (ah == 0x0f) {
            // Write Character at Cursor (ah = 0x0F): Writes a character to a specific coordinate.
        }
    }

    void int0x16() {
        // BIOS Keyboard Services
        int ah = (ax >>> 8) & 0xff;
        if (ah == 0x00) {
            // read a character
        } else if (ah == 0x01) {
            // Checks if a key is currently waiting in the buffer without stopping the program (useful for non-blocking input).
        }
    }

    void int0x20() {
        // DOS Terminate Program

    }

    void run() {
        bx = add16(bx, ax); // add bx,ax
        di = add16(di, cx); // add di,cx
        ax = andLow(ax, 0x1f); // and al,0x1f
    }

}
