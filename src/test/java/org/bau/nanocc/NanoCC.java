package org.bau.nanocc;

import java.io.ByteArrayOutputStream;

public class NanoCC {

    private long[] memory = new long[64 * 1024];
    private ByteArrayOutputStream out = new ByteArrayOutputStream();
    private StringBuilder outWasm = new StringBuilder();
    private String code;
    private int pos;
    private boolean header = true;

    NanoCC(String code) {
        this.code = code;
    }

    private int getchar() {
        return pos >= code.length() ? 0 : code.charAt(pos++);
    }

    private void putchar(int c) {
        out.write(c & 0xff);
    }

    private void putWasm(int stack, String wasm, String codes) {
        stack = stack % 20;
        for (int i = 0; i < stack; i++) {
            outWasm.append("  ");
        }
        outWasm.append(wasm);
        if (!codes.isEmpty()) {
            for (int i = 20 - 2 * stack - wasm.length(); i > 0; i--) {
                outWasm.append(" ");
            }
            outWasm.append(";; " + codes);
            outWasm.append("  [" + stack + "]");
        }
        outWasm.append("\n");
    }

    public String getOut() {
        return out.toString();
    }

    public void run() {
        long c = getchar();
        int symbols = 16;
        int tokenStart = 256;
        int tokens = tokenStart;
        putWasm(0, "(module", "");
        putWasm(0, "(import \"wasi_snapshot_preview1\" \"fd_write\" (func $fd_write (param i32 i32 i32 i32) (result i32)))", "");
        putWasm(0, "(import \"wasi_snapshot_preview1\" \"fd_read\"  (func $fd_read  (param i32 i32 i32 i32) (result i32)))", "");
        putWasm(0, "(memory (export \"memory\") 1)", "");
        putWasm(0, "(data (i32.const 0) \"\\10\\00\\00\\00\")", "");
        putWasm(0, "(data (i32.const 4) \"\\01\\00\\00\\00\")", "");
        putWasm(0, "(data (i32.const 16) \"\\00\")", "");
        putWasm(0, "(func $getchar (result i64)\n"
                + "  (call $fd_read (i32.const 0) (i32.const 0) (i32.const 1) (i32.const 24))\n"
                + "  drop  i32.const 24  i32.load  i32.eqz  if  i64.const -1  return  end\n"
                + "  i32.const 16  i64.load8_u )", "fd = stdin, iovec*, iovec count, &nread");
        putWasm(0, "(func $putchar (param $c i64)", "");
        putWasm(0, "(i64.store8 (i32.const 16) (local.get $c)) (call $fd_write (i32.const 1) (i32.const 0) (i32.const 1) (i32.const 28) ) drop )", "");
        putWasm(0, "(func $main (export \"_start\")", "");
        for (int s0 = 0; s0 < 16; s0++) {
            putWasm(0, "(local $x" + s0 + " i64)", "dummy");
        }
        // putWasm(0, "(local $c i64)", "");
        int blockStack = 0;
        while (c > 0) {
            if (c >= 127) {
                throw new IllegalStateException("Non-ascii char at " + this.code.substring(this.pos));
            }
            if (c == ' ' || c == '\n') {
                c = getchar();
            } else if (c == '/') {
                c = getchar();
                while (c != 10) {
                    c = getchar();
                }
            } else if (c == '\'') {
                // char literal
                long value = 0;
                c = getchar();
                while (c != '\'') {
                    value = (value << 8) | (c & 0xff);
                    c = getchar();
                }
                c = getchar();
                memory[tokens++] = '0';
                memory[tokens++] = value;
                if (value < 0) {
                    throw new IllegalStateException("negative");
                }
            } else if (c >= '0' && c <= '9') {
                // number
                long value = c - '0';
                c = getchar();
                if (value == 0 && c == 'x') {
                    c = getchar();
                    while ((c >= '0') && (c <= '9') || (c >= 'a' && c <= 'f')) {
                        c = c - (((c - 58) >> 8) & 48) - (((96 - c) >> 8) & 87);
                        value = (value << 4) | c;
                        c = getchar();
                    }
                } else {
                    while (c >= '0' && c <= '9') {
                        value = (value << 3) + (value << 1) + (c - '0');
                        c = getchar();
                    }
                }
                memory[tokens++] = '0';
                memory[tokens++] = value;
                if (value < 0) {
                    throw new IllegalStateException("negative");
                }
            } else if (c == '=' || c == '<' || c == '>' || c == '!' || c == '+') {
                // comparison or operation
                long op = c;
                c = getchar();
                if (c == '=' || c == '<' || c == '>' || c == '+') {
                    op = (op << 8) + c;
                    c = getchar();
                }
                memory[tokens++] = op;
            } else if (c >= 'A' && c <= 'z') {
                // identifier or keyword
                long var = 0;
                StringBuilder buff = new StringBuilder();
                while (c >= 'A' && c <= 'z') {
                    buff.append((char) c);
                    var = (var << 8) + c;
                    c = getchar();
                }
                int s = 16;
                while (s < symbols && memory[s] != var) {
                    s++;
                }
                memory[s] = var;
                if (s == symbols) {
                    putWasm(0, "(local $x" + s + " i64)", buff.toString());
                    symbols++;
                }
                memory[tokens++] = (var << 8) | '.';
                memory[tokens++] = s;
            } else {
                memory[tokens++] = c;
                c = getchar();
            }
            if (tokens > 10000) {
                System.out.println("??? too much code");
            }
        }
        while(symbols < 64) {
            putWasm(0, "(local $x" + symbols + " i64)", "dummy");
            symbols++;
        }
        putWasm(0, "nop", "dummy");
        int i = tokenStart;
        int stackStart = tokens + 10;
        int stack = stackStart;
        int outputStart = stack + 100;
        int output = outputStart;
        memory[output++] = 0x16d73610007L;
        memory[output++] = 0x7f04600414010007L;
        memory[output++] = 0x607f017f7f7f07L;
        memory[output++] = 0x60007e01607e0107L;
        memory[output++] = 0x7716024402000007L;
        memory[output++] = 0x616e735f69736107L;
        memory[output++] = 0x705f746f68737007L;
        memory[output++] = 0x3177656976657207L;
        memory[output++] = 0x6972775f64660807L;
        memory[output++] = 0x6177160000657407L;
        memory[output++] = 0x70616e735f697307L;
        memory[output++] = 0x72705f746f687307L;
        memory[output++] = 0x731776569766507L;
        memory[output++] = 0x646165725f646607L;
        memory[output++] = 0x201030403000007L;
        memory[output++] = 0x701000103050307L;
        memory[output++] = 0x6f6d656d06021307L;
        memory[output++] = 0x735f060002797207L;
        memory[output++] = 0xa04007472617407L;
        memory[output++] = 0x41001e033abb07L;
        memory[output++] = 0x1018410141004107L;
        memory[output++] = 0x22818411a0107L;
        memory[output++] = 0x41057f427e044507L;
        memory[output++] = 0x140b0b0000311007L;
        memory[output++] = 0x3c002010410007L;
        memory[output++] = 0x141004101410007L;
        memory[output++] = 0x840b1a00101c4107L;
        memory[output++] = 0x101017e40013a07L;
        int codeStart = output;

        putWasm(0, "", "0x01      : local decl count");
        putWasm(0, "", symbols + " 0x7e : " + symbols + " locals of type i64 (0x7e)");
        // memory[output++] = 0x0101;
        // memory[output++] = (symbols << 8) + 0x7e0002;
        // System.out.println(debug.toString());
        if (!header) {
            this.outWasm.setLength(0);
        }
        while (i < tokens) {
            // System.out.println(i + ": stack=" + stack + " " + Arrays.toString(Arrays.copyOfRange(memory, stackStart - 1, stackStart + 10)));
            long token = memory[i++];
            int wasValue = 0;
            if (token == '(') {
                memory[stack++] = '(';
            } else if (token == ')') {
                wasValue = 1;
                long op = memory[stack - 1];
                if (op == 0) {
                    throw new IllegalStateException();
                }
                if (op == xchar("putchar.")) {
                    putWasm(blockStack, "call $putchar", "0x10 3");
                    memory[output++] = 0x031002;
                    wasValue = 0;
                }
                stack--;
            } else if (token == '{') {
                long stat = memory[stack - 1];
                if (stat == xchar("if.")) {
                    putWasm(blockStack, "i32.wrap_i64", "0xa7");
                    putWasm(blockStack, "if", "0x04 0x40");
                    memory[output++] = 0x4004a703;
                    blockStack++;
                } else if (stat == xchar("while.")) {
                    putWasm(blockStack, "i32.wrap_i64", "0xa7");
                    putWasm(blockStack, "i32.eqz", "0x45");
                    putWasm(blockStack, "br_if 1", "0x0d 0x01");
                    memory[output++] = 0x010d45a704L;
                } else if (stat == xchar("else.")) {
                    // nothing
                } else {
                    // putWasm(blockStack, "??? stat == " + stat + "\n");
                }
            } else if (token == '}') {
                blockStack--;
                long stat = memory[--stack];
                if (stat == xchar("if.")) {
                    if (memory[i] != xchar("else.")) {
                        putWasm(blockStack, "end", "0x0b [endIf]");
                        memory[output++] = 0x0b01;
                    }
                } else if (stat == xchar("else.")) {
                    putWasm(blockStack, "end", "0x0b [endElse]");
                    memory[output++] = 0x0b01;
                } else if (stat == xchar("while.")) {
                    putWasm(blockStack, "br 0", "0x0c 0x00 [continue loop]");
                    putWasm(blockStack, "end", "0x0b [endLoop1]");
                    putWasm(blockStack, "end", "0x0b [endLoop2]");
                    memory[output++] = 0x0b0b000c04L;
                } else {
                    putWasm(blockStack, "", "??? stat == " + stat);
                }
            } else if (token == xchar("getchar.")) {
                i += 3;
                putWasm(blockStack, "call $getchar", "0x10 2");
                memory[output++] = 0x021002;
                wasValue = 1;
            } else if (token == xchar("putchar.")) {
                i += 2;
                memory[stack++] = token;
            } else if (token == xchar("while.")) {
                // loop
                i++;
                putWasm(blockStack, "block", "0x02 0x40");
                putWasm(blockStack, "loop", "0x03 0x40");
                memory[output++] = 0x4003400204L;
                blockStack++;
                memory[stack++] = token;
            } else if (token == xchar("if.")) {
                // if
                i++;
                memory[stack++] = token;
            } else if (token == xchar("else.")) {
                // else
                i++;
                putWasm(blockStack, "else", "0x05");
                memory[output++] = 0x0501;
                blockStack++;
                memory[stack++] = token;
            } else if (token == '*') {
                // *pointer
                token = memory[i++];
                if ((token & 0xff) != '.') {
                    throw new AssertionError();
                }
                token = memory[i++];
                if (memory[i] == '=') {
                    // memory write
                    putWasm(blockStack, "local.get $x" + token, "0x20 " + token);
                    memory[output++] = 0x2001;
                    memory[output++] = (token << 8) | 0x01;
                    putWasm(blockStack, "i64.const 3", "0x42 0x03");
                    putWasm(blockStack, "i64.shl", "0x86");
                    putWasm(blockStack, "i32.wrap_i64", "0xa7");
                    memory[output++] = 0xa786034204L;
                    memory[stack++] = '*';
                    i++;
                } else {
                    // memory read
                    putWasm(blockStack, "local.get $x" + token, "0x20 " + token);
                    memory[output++] = 0x2001;
                    memory[output++] = (token << 8) | 0x01;
                    putWasm(blockStack, "i64.const 3", "0x42 0x03");
                    putWasm(blockStack, "i64.shl", "0x86");
                    putWasm(blockStack, "i32.wrap_i64", "0xa7");
                    putWasm(blockStack, "i64.load", "0x29 0x03 0x00");
                    memory[output++] = 0x000329a786034207L;
                    wasValue = 1;
                }
            } else if (token == ';') {
                if (stack > stackStart) {
                    long op = memory[--stack];
                    if (op == '*') {
                        // *pointer assign
                        putWasm(blockStack, "i64.store", "0x37 0x03 0x00");
                        memory[output++] = 0x00033703;
                    } else if (op == '.') {
                        long var = memory[--stack];
                        putWasm(blockStack, "local.set $x" + var, "0x21 " + var);
                        memory[output++] = 0x2101;
                        memory[output++] = (var << 8) | 0x01;
                    } else if (op == xchar("if.") || op == xchar("while.") || op == xchar("else.")) {
                        // ok
                        stack++;
                    } else {
                        putWasm(blockStack, "??? " + (char) op, "");
                    }
                }
            } else if (token == '0') {
                token = memory[i++];
                putWasm(blockStack, "i64.const " + token, "0x42 <leb128> of " + token);
                memory[output++] = 0x4201;
                memory[output++] = 0xff;
                memory[output++] = token;
                wasValue = 1;
            } else if ((token & 0xff) == '.') {
                token = memory[i++];
                if (memory[i] == '=') {
                    // variable assign
                    memory[stack++] = token;
                    memory[stack++] = '.';
                    i++;
                } else {
                    // variable read
                    putWasm(blockStack, "local.get $x" + token, "0x20 " + token);
                    memory[output++] = 0x2001;
                    memory[output++] = (token << 8) | 0x01;
                    wasValue = 1;
                }
            } else {
                putWasm(blockStack, "", "operation " + (char) (token >> 8) + (char) (token & 0xff));
                memory[stack++] = token;
            }
            if (wasValue == 1) {
                long st = memory[stack - 1];
                if (st != '(' && st != '=' && st != xchar("if.") && st != xchar("while.")
                        && st != xchar("else.") && st != '.' && st != '*'
                        && st != xchar("putchar.")) {
                    long op = memory[--stack];
                    if (op == '+') {
                        putWasm(blockStack, "i64.add", "0x7c");
                        memory[output++] = 0x7c01;
                    } else if (op == '-') {
                        putWasm(blockStack, "i64.sub", "0x7d");
                        memory[output++] = 0x7d01;
                    } else if (op == '|') {
                        putWasm(blockStack, "i64.or", "0x84");
                        memory[output++] = 0x8401;
                    } else if (op == '&') {
                        putWasm(blockStack, "i64.and", "0x83");
                        memory[output++] = 0x8301;
                    } else if (op == xchar("<<")) {
                        putWasm(blockStack, "i64.shl", "0x86");
                        memory[output++] = 0x8601;
                    } else if (op == xchar(">>")) {
                        // putWasm("i64.shr_s ;; 0x87\n");
                        putWasm(blockStack, "i64.shr_u", "0x88");
                        memory[output++] = 0x8801;
                    } else if (op == xchar("==")) {
                        putWasm(blockStack, "i64.eq", "0x51");
                        putWasm(blockStack, "i64.extend_i32_s", "0xac");
                        memory[output++] = 0xac5102;
                    } else if (op == xchar("!=")) {
                        putWasm(blockStack, "i64.ne", "0x52");
                        putWasm(blockStack, "i64.extend_i32_s", "0xac");
                        memory[output++] = 0xac5202;
                    } else if (op == '<') {
                        putWasm(blockStack, "i64.lt_s", "0x53");
                        putWasm(blockStack, "i64.extend_i32_s", "0xac");
                        memory[output++] = 0xac5302;
                    } else if (op == xchar("<=")) {
                        putWasm(blockStack, "i64.le_s", "0x57");
                        putWasm(blockStack, "i64.extend_i32_s", "0xac");
                        memory[output++] = 0xac5702;
                    // } else if (op == '>') {
                    //     putWasm(blockStack, "i64.gt_s", "0x55");
                    //     putWasm(blockStack, "i64.extend_i32_s", "0xac");
                    //     memory[output++] = 0xac5502;
                    // } else if (op == (('>' << 8) | '=')) {
                    //    putWasm(blockStack, "i64.ge_s", "0x59");
                    //     putWasm(blockStack, "i64.extend_i32_s", "0xac");
                    //     memory[output++] = 0xac5902;
                    } else {
                        putWasm(blockStack, "", "??? unknown operation> " + (char) op);
                        throw new IllegalArgumentException((char) (op >> 8) + "" + (char) (op & 0xff));
                    }
                }
            }
        }
        putWasm(0, ") )", "");
        int codeLength = 7420;
        int footer = output;
        memory[output++] = 0x410003190b0b0107L;
        memory[output++] = 0x10040b0007L;
        memory[output++] = 0x1040b04410007L;
        memory[output++] = 0x10b104100000007L;
        memory[output++] = 0x1L;

        long p = outputStart;
        int writtenCode = 0;
        while(p < output) {
            if (p == codeStart) {
                writtenCode = 0;
            }
            if (p == footer) {
                for (int j = writtenCode; j < codeLength; j++) {
                    putchar(0x01);
                }
            }
            long m = memory[(int) p++];
            long l = m & 0xff;
            if (l == 0xff) {
                long v = memory[(int) p++];
                for (int j = 0; j < 9; j++) {
                    int byteVal = (int) (v & 0x7F);
                    putchar(byteVal | 0x80);
                    writtenCode++;
                    v >>>= 7;
                }
                putchar((int) (v & 0x7F));
                writtenCode++;
            } else {
                while (l > 0) {
                    m = m >>> 8;
                    putchar((int) (m & 0xff));
                    writtenCode++;
                    l--;
                }
            }
        }
    }

    public static long xchar(String s) {
        long result = 0;
        for (int i = 0; i < s.length(); i++) {
            result = (result << 8) | (s.charAt(i) & 0xff);
        }
        return result;
    }

    public String wat() {
        return outWasm.toString();
    }

    public byte[] bytes() {
        return out.toByteArray();
    }

    public void setHeader(boolean header) {
        this.header = header;
    }

}
