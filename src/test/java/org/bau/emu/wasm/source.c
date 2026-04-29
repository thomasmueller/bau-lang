c = get();
symbols = 16;
tokenStart = 256;
tokens = tokenStart;
n = 0;
@ ((n < 10000) & (c > 0)) {
    n = n + 1;
    ? ((c == ' ') | (c == 10)) {
        c = get();
    } : { ? (c == '$') {
        value = 0;
        c = get();
        @ (((c >= '0') & (c <= '9')) | ((c >= 'a') & (c <= 'f'))) {
            c = c - (((c - 58) >> 8) & 48) - (((96 - c) >> 8) & 87);
            value = (value << 4) | c;
            c = get();
        }
        *tokens = '0'; tokens=tokens+1;
        *tokens = value; tokens=tokens+1;
    } : { ? (c == 39) {
        value = 0;
        c = get();
        @ (c != 39) {
            value = (value << 8) | (c & 255);
            c = get();
        }
        c = get();
        *tokens = '0'; tokens=tokens+1;
        *tokens = value; tokens=tokens+1;
    } : { ? ((c >= '0') & (c <= '9')) {
        value = c - '0';
        c = get();
        @ ((c >= '0') & (c <= '9')) {
            value = (value << 3) + (value << 1) + (c - '0');
            c = get();
        }
        *tokens = '0'; tokens=tokens+1;
        *tokens = value; tokens=tokens+1;
   } : { ? ((c == '=') | (c == '<') | (c == '>') | (c == '!')) {
       op = c;
       c = get();
       ? ((c == '=') | (c == '<') | (c == '>')) {
           op = (op << 8) + c;
           c = get();
       }
       *tokens = op; tokens=tokens+1;
   } : { ? ((c >= 'A') & (c <= 'z')) {
       var = 0;
       @ ((c >= 'A') & (c <= 'z')) {
           var = (var << 8) + c;
           c = get();
       }
       ? (c == '(') {
           c = get();
           *tokens = 'c'; tokens=tokens+1;
           *tokens = var; tokens=tokens+1;
       } : {
           s = 16;
           @ ((s < symbols) & (*s != var)) {
               s=s+1;
           }
           *s = var;
           ? (s == symbols) {
               symbols = symbols + 1;
           }
           *tokens = 'x'; tokens=tokens+1;
           *tokens = s; tokens=tokens+1;
       }
   } : {
       *tokens = c; tokens=tokens+1;
       c = get();
   } } } } } }
}
i = tokenStart;
stackStart = token + 10;
stack = stackStart;
outputStart = stack + 100;
output = outputStart;
*output = $16d73610007;  output=output+1;
*output = $7f04600414010007;  output=output+1;
*output = $607f017f7f7f07;  output=output+1;
*output = $60007e01607e0107;  output=output+1;
*output = $7716024402000007;  output=output+1;
*output = $616e735f69736107;  output=output+1;
*output = $705f746f68737007;  output=output+1;
*output = $3177656976657207;  output=output+1;
*output = $6972775f64660807;  output=output+1;
*output = $6177160000657407;  output=output+1;
*output = $70616e735f697307;  output=output+1;
*output = $72705f746f687307;  output=output+1;
*output = $731776569766507;  output=output+1;
*output = $646165725f646607;  output=output+1;
*output = $201030403000007;  output=output+1;
*output = $701000103050307;  output=output+1;
*output = $6f6d656d06021307;  output=output+1;
*output = $735f060002797207;  output=output+1;
*output = $a04007472617407;  output=output+1;
*output = $41001e033abb07;  output=output+1;
*output = $1018410141004107;  output=output+1;
*output = $22818411a0107;  output=output+1;
*output = $41057f427e044507;  output=output+1;
*output = $140b0b0000311007;  output=output+1;
*output = $3c002010410007;  output=output+1;
*output = $141004101410007;  output=output+1;
*output = $0b1a00101c4106;  output=output+1;
*output = $8401;  output=output+1;
*output = $101017e40013a07;  output=output+1;

codeStart = output;
@ (i < tokens) {
    token = *i; i = i + 1;
    wasValue = 0;
    ? (token == '(') {
        *stack = '('; stack=stack+1;
    } : { ? (token == ')') {
        wasValue = 1;
        prev = stack - 1;
        op = *prev;
        ? (op != 0) {
            ? (op == 'c') {
                *output = $031002; output=output+1;
                wasValue = 0;
            }
            stack = stack - 1;
        }
    } : { ? (token == '{') {
        prev = stack - 1;
        stat = *prev;
        ? (stat == '?') {
            *output = $4004a703; output=output+1;
        } : { ? (stat == '@') {
            *output = $010d45a704; output=output+1;
        } }
    } : { ? (token == '}') {
        stack = stack - 1;
        stat = *stack;
        ? (stat == '?') {
            ? (*i != ':') {
                *output = $0b01; output=output+1;
            }
        } : { ? (stat == ':') {
            *output = $0b01; output=output+1;
        } : { ? (stat == '@') {
            *output = $0b0b000c04; output=output+1;
        } } }
    } : { ? (token == '@') {
        *output = $4003400204; output=output+1;
        *stack = '@'; stack=stack+1;
    } : { ? (token == '?') {
        *stack = '?'; stack=stack+1;
    } : { ? (token == ':') {
        *output = $0501; output=output+1;
        *stack = ':'; stack=stack+1;
    } : { ? (token == '*') {
        token = *i; i=i+1;
        token = *i; i=i+1;
        ? (*i == '=') {
            *output = $2001; output=output+1;
            *output = (token << 8) | $01; output=output+1;
            *output = $a786034204; output=output+1;
            *stack = 'X'; stack=stack+1;
            i=i+1;
        } : {
            *output = $2001; output=output+1;
            *output = (token << 8) | $01; output=output+1;
            *output = $000329a786034207; output=output+1;
            wasValue = 1;
        }
    } : { ? (token == ';') {
        ? (stack > stackStart) {
            stack = stack - 1;
            op = *stack;
            ? (op == 'X') {
                *output = $00033703; output=output+1;
            } : { ? (op == 'x') {
                stack = stack - 1;
                var = *stack;
                *output = $2101; output=output+1;
                *output = (var << 8) | $01; output=output+1;
            } : { ? ((op == '?') | (op == '@') | (op == ':')) {
                stack=stack+1;
            } } }
        }
    } : { ? (token == '0') {
        token = *i; i=i+1;
        *output = $4201; output=output+1;
        *output = $ff; output=output+1;
        *output = token; output=output+1;
        wasValue = 1;
    } : { ? (token == 'x') {
        token = *i; i=i+1;
        ? (*i == '=') {
            *stack = token; stack=stack+1;
            *stack = 'x'; stack=stack+1;
            i=i+1;
        } : {
            *output = $2001; output=output+1;
            *output = (token << 8) | $01; output=output+1;
            wasValue = 1;
        }
    } : { ? (token == 'c') {
        token = *i; i=i+1;
        ? (token == 'get') {
            *output = $021002; output=output+1;
            wasValue = 1;
            i=i+1;
        } : { ? (token == 'put') {
            *stack = 'c'; stack=stack+1;
        } }
    } : {
        *stack = token; stack=stack+1;
    } } } } } } } } } } } }
    ? (wasValue == 1) {
        prev = stack - 1;
        st = *prev;
        ? ((st != '(') & (st != '=') & (st != '?') & (st != '@') & (st != ':') & (st != 'x') & (st != 'X') & (st != 'c')) {
            stack = stack - 1;
            op = *stack;
            ? (op == '+') {
                *output = $7c01; output=output+1;
            } : { ? (op == '-') {
                *output = $7d01; output=output+1;
            } : { ? (op == '|') {
                *output = $8401; output=output+1;
            } : { ? (op == '&') {
                *output = $8301; output=output+1;
            } : { ? (op == '<<') {
                *output = $8601; output=output+1;
            } : { ? (op == '>>') {
                *output = $8801; output=output+1;
            } : { ? (op == '==') {
                *output = $ac5102; output=output+1;
            } : { ? (op == '!=') {
                *output = $ac5202; output=output+1;
            } : { ? (op == '<') {
                *output = $ac5302; output=output+1;
            } : { ? (op == '>') {
                *output = $ac5502; output=output+1;
            } : { ? (op == '<=') {
                *output = $ac5702; output=output+1;
            } : { ? (op == '>=') {
                *output = $ac5902; output=output+1;
            } } } } } } } } } } } }
        }
    }
}
codeLength = 7420;
footer = output;
*output = $410003190b0b0107; output=output+1;
*output = $10040b0007; output=output+1;
*output = $1040b04410007; output=output+1;
*output = $10b104100000007; output=output+1;
*output = $01; output=output+1;
p = outputStart;
writtenCode = 0;
@(p < output) {
    ? (p == codeStart) {
        writtenCode = 0;
    }
    ? (p == footer) {
        j = writtenCode;
        @ (j < codeLength) {
            put($01);
            j = j + 1;
        }
    }
    m = *p; p=p+1;
    l = m & $ff;
    ? (l == $ff) {
        m = *p; p=p+1;
        j = 0
        @ (j < 9) {
            put($80 | (m & $7f));
            writtenCode=writtenCode+1;
            m = m >> 7;
            j = j + 1;
        }
        put(m & $7f);
        writtenCode=writtenCode+1;
    } : {
        @ (l > 0) {
            m = m >> 8;
            put(m & $ff);
            writtenCode=writtenCode+1;
            l=l-1;
        }
    }
}
