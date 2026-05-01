    // NanoCC is a compiler for a tiny C-like language to WASM bytecode
    
    c = getchar(); symbols = 16; tokenStart = 256; t = tokenStart;
    while (0 < c) { // tokenizer loop
        if ((c == ' ') | (c == 10)) { // ignore spaces
            c = getchar();
        } else if (c == '/') { // comments
            c = getchar();
            while (c != 10) {
                c = getchar();
            }    
        } else { if (c == 39) { // quoted text, eg. 'hello'
            value = 0;
            c = getchar();
            while (c != 39) {
                value = (value << 8) | (c & 255);
                c = getchar();
            }
            c = getchar();
            *t = '0'; t=t+1;
            *t = value; t=t+1;
        } else { if (('0' <= c) & (c <= '9')) { // numbers
            value = c - '0';
            c = getchar();
            if ((value == 0) & (c == 'x')) { // hex numbers (0x...)
                c = getchar();
                while ((('0' <= c) & (c <= '9')) | (('a' <= c) & (c <= 'f'))) {
                    c = c - (((c - 58) >> 8) & 48) - (((96 - c) >> 8) & 87);
                    value = (value << 4) | c;
                    c = getchar();
                }
            } else { // decimal numbers
                while (('0' <= c) & (c <= '9')) {
                    value = (value << 3) + (value << 1) + (c - '0');
                    c = getchar();
                }
            }
            *t = '0'; t=t+1;
            *t = value; t=t+1;
        } else { if ((c == '=') | (c == '<') | (c == '>') | (c == '!') | (c == '+')) {
            op = c;
            c = getchar();
            if ((c == '=') | (c == '<') | (c == '>') | (c == '+')) {
                op = (op << 8) + c;
                c = getchar();
            }
            *t = op; t=t+1;
        } else { if (('A' <= c) & (c <= 'z')) { // identifiers
            var = 0;
            while (('A' <= c) & (c <= 'z')) {
                var = (var << 8) + c;
                c = getchar();
            }
            s = 16;
            while ((s < symbols) & (*s != var)) {
                s=s+1;
            }
            *s = var;
            if (s == symbols) {
                symbols = symbols+1;
            }
            *t = (var << 8) | '.'; t=t+1;
            *t = s; t=t+1;
        } else { // operators
            *t = c; t=t+1;
            c = getchar();
        } } } } } }
    }
    i = tokenStart; stackStart = token + 10; stack = stackStart;
    outputStart = stack + 100; o = outputStart; // WASM header
    *o = 0x0000016d73610007; o=o+1; *o = 0x7f04600414010007; o=o+1; *o = 0x00607f017f7f7f07; o=o+1;
    *o = 0x60007e01607e0107; o=o+1; *o = 0x7716024402000007; o=o+1; *o = 0x616e735f69736107; o=o+1; 
    *o = 0x705f746f68737007; o=o+1; *o = 0x3177656976657207; o=o+1; *o = 0x6972775f64660807; o=o+1; 
    *o = 0x6177160000657407; o=o+1; *o = 0x70616e735f697307; o=o+1; *o = 0x72705f746f687307; o=o+1; 
    *o = 0x0731776569766507; o=o+1; *o = 0x646165725f646607; o=o+1; *o = 0x0201030403000007; o=o+1; 
    *o = 0x0701000103050307; o=o+1; *o = 0x6f6d656d06021307; o=o+1; *o = 0x735f060002797207; o=o+1; 
    *o = 0x0a04007472617407; o=o+1; *o = 0x0041001e033abb07; o=o+1; *o = 0x1018410141004107; o=o+1; 
    *o = 0x00022818411a0107; o=o+1; *o = 0x41057f427e044507; o=o+1; *o = 0x140b0b0000311007; o=o+1;
    *o = 0x003c002010410007; o=o+1; *o = 0x0141004101410007; o=o+1; *o = 0x000b1a00101c4106; o=o+1; 
    *o = 0x0000000000008401; o=o+1; *o = 0x0101017e40013a07; o=o+1;
    codeStart = o;
    while (i < t) { // stack machine to process tokens and emit variable-size codes
        token = *i; i=i+1;
        wasValue = 0;
        if (token == '(') {
            *stack = '('; stack=stack+1;
        } else { if (token == ')') {
            wasValue = 1;
            prev = stack - 1;
            op = *prev;
            if (op == 'putchar.') {
                *o = 0x031002; o=o+1;
                wasValue = 0;
            }
            stack = stack - 1;
        } else { if (token == '{') {
            prev = stack - 1;
            stat = *prev;
            if (stat == 'if.') {
                *o = 0x4004a703; o=o+1;
            } else { if (stat == 'while.') {
                *o = 0x010d45a704; o=o+1;
            } }
        } else { if (token == '}') {
            stack = stack - 1;
            stat = *stack;
            if (stat == 'if.') {
                if (*i != 'else.') {
                    *o = 0x0b01; o=o+1;
                }
            } else { if (stat == 'else.') {
                *o = 0x0b01; o=o+1;
            } else { if (stat == 'while.') {
                *o = 0x0b0b000c04; o=o+1;
            } } }
        } else { if (token == 'getchar.') {
            i=i+3;
            *o = 0x021002; o=o+1;
            wasValue = 1;
        } else { if (token == 'putchar.') {
            i=i+2;
            *stack = token; stack=stack+1;
        } else { if (token == 'while.') {
            *o = 0x4003400204; o=o+1;
            *stack = token; stack=stack+1; i=i+1;
        } else { if (token == 'if.') {
            *stack = token; stack=stack+1; i=i+1;
        } else { if (token == 'else.') {
            *o = 0x0501; o=o+1; 
            *stack = token; stack=stack+1; i=i+1;
        } else { if (token == '*') { // memory access via pointer
            i=i+1; token = *i; i=i+1;
            if (*i == '=') { // memory write
                *o = 0x2001; o=o+1;
                *o = (token << 8) | 0x01; o=o+1;
                *o = 0xa786034204; o=o+1;
                *stack = '*'; stack=stack+1;
                i=i+1;
            } else { // memory read
                *o = 0x2001; o=o+1;
                *o = (token << 8) | 0x01; o=o+1;
                *o = 0x000329a786034207; o=o+1;
                wasValue = 1;
            }
        } else { if (token == ';') {
            if (stackStart < stack) {
                stack = stack - 1;
                op = *stack;
                if (op == '*') {
                    *o = 0x00033703; o=o+1;
                } else { if (op == '.') {
                    stack = stack - 1;
                    var = *stack;
                    *o = 0x2101; o=o+1;
                    *o = (var << 8) | 0x01; o=o+1;
                } else {
                    stack=stack+1;
                } }
            }
        } else { if (token == '0') { // constants
            token = *i; i=i+1;
            *o = 0x4201; o=o+1;
            *o = 0xff; o=o+1;
            *o = token; o=o+1;
            wasValue = 1;
        } else { if ((token & 0xff) == '.') { // variables
            token = *i; i=i+1;
            if (*i == '=') { // assignment
                *stack = token; stack=stack+1;
                *stack = '.'; stack=stack+1;
                i=i+1;
            } else { // reading a variable
                *o = 0x2001; o=o+1;
                *o = (token << 8) | 0x01; o=o+1;
                wasValue = 1;
            }
        } else { // operators
            *stack = token; stack=stack+1;
        } } } } } } } } } } } } }
        if (wasValue == 1) { // operations like '+' etc
            prev = stack - 1;
            st = *prev;
            if ((st != '(') & (st != '=') & (st != 'if.') & (st != 'while.') 
              & (st != 'else.') & (st != '.') & (st != '*') & (st != 'putchar.')) {
                stack = stack - 1;
                op = *stack;
                if (op == '+') {
                    *o = 0x7c01; o=o+1;
                } else { if (op == '-') { // for negative numbers, use 0 - x
                    *o = 0x7d01; o=o+1;
                } else { if (op == '|') { // bitwise 'and' is also logical
                    *o = 0x8401; o=o+1;
                } else { if (op == '&') { // bitwise 'or' is also logical
                    *o = 0x8301; o=o+1;
                } else { if (op == '<<') {
                    *o = 0x8601; o=o+1;
                } else { if (op == '>>') {
                    *o = 0x8801; o=o+1;
                } else { if (op == '==') {
                    *o = 0xac5102; o=o+1;
                } else { if (op == '!=') {
                    *o = 0xac5202; o=o+1;
                } else { if (op == '<') { // '>' is not supported
                    *o = 0xac5302; o=o+1;
                } else { if (op == '<=') { // '>=' is not supported
                    *o = 0xac5702; o=o+1;
                } } } } } } } } } }
            }
        }
    }
    codeLength = 7420; footer = o;
    *o = 0x410003190b0b0107; o=o+1; *o = 0x00000010040b0007; o=o+1; 
    *o = 0x0001040b04410007; o=o+1; *o = 0x010b104100000007; o=o+1; *o = 0x01; o=o+1;
    p = outputStart;
    written = 0;
    while (p < o) { // convert the variable-size codes to bytes 
        if (p == codeStart) {
            written = 0;
        }
        if (p == footer) {
            j = written;
            while (j < codeLength) {
                putchar(0x01);
                j=j+1;
            }
        }
        m = *p; p=p+1;
        l = m & 0xff;
        if (l == 0xff) {
            m = *p; p=p+1;
            j = 0;
            while (j < 9) {
                putchar(0x80 | (m & 0x7f));
                m = m >> 7;
                j=j+1;
            }
            putchar(m & 0x7f);
            written = written + 10;
        } else {
            while (0 < l) {
                m = m >> 8;
                putchar(m & 0xff);
                written=written+1;
                l=l-1;
            }
        }
    }
