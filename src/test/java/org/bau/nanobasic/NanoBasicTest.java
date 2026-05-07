package org.bau.nanobasic;

import java.util.HashMap;

import org.junit.Test;

/*

https://github.com/TobiasRohner/Bootfuck

while(condition, block)        : while(condition){block}
if(condition, block)           : m=0;while(condition&&m==0){m=1, block}
ifElse(condition, true, false) : m=0;while(condition&&m==0){m=1, true} while(m==1){m=2, false}

if(condition, block)              : "{", condition, "ifFalseGoFwd }", block, "}"
ifElse(condition, true, false)    : "{", condition, "ifFalseGoFwd }", block, "goFwd 2}", "}" "{", false, "}"
while(condition, block)           : "{", condition, "ifFalseGoFwd }", block, "goBack {", "}"
break                             : "goFwd }"
continue                          : "goBack {"
call(method, arg)                 : stack[sp++]=ip, ip=method
return                            : ip=stack[--sp]


00 nul   01 soh   02 stx   03 etx   04 eot   05 enq   06 ack   07 bel
08 bs    09 ht    0a nl    0b vt    0c np    0d cr    0e so    0f si
10 dle   11 dc1   12 dc2   13 dc3   14 dc4   15 nak   16 syn   17 etb
18 can   19 em    1a sub   1b esc   1c fs    1d gs    1e rs    1f us
20 sp    21  !    22  "    23  #    24  $    25  %    26  &    27  '
28  (    29  )    2a  *    2b  +    2c  ,    2d  -    2e  .    2f  /
30  0    31  1    32  2    33  3    34  4    35  5    36  6    37  7
38  8    39  9    3a  :    3b  ;    3c  <    3d  =    3e  >    3f  ?
40  @    41  A    42  B    43  C    44  D    45  E    46  F    47  G
48  H    49  I    4a  J    4b  K    4c  L    4d  M    4e  N    4f  O
50  P    51  Q    52  R    53  S    54  T    55  U    56  V    57  W
58  X    59  Y    5a  Z    5b  [    5c  \    5d  ]    5e  ^    5f  _
60  `    61  a    62  b    63  c    64  d    65  e    66  f    67  g
68  h    69  i    6a  j    6b  k    6c  l    6d  m    6e  n    6f  o
70  p    71  q    72  r    73  s    74  t    75  u    76  v    77  w
78  x    79  y    7a  z    7b  {    7c  |    7d  }    7e  ~    7f del

00   : eof
01-20: space
21-2f: operators1 !"#$%&'()*+,-./
30-39: 0-9
3a-3f: operators2 :;<=>?
40-7f: @ A-Z []\^_ `a-z{|}~del

https://github.com/nanochess/bootBASIC

10 print "Hello" (replace line)
10 (erase line)
print "Hello, world!" (interactive mode)
new (erase program)
run
list
system (or exit)
var=expr assign
print expr      Print expression value, new line
print expr;     Print expression value, continue
print "hello"   Print string, new line
print "hello";  Print string, continue
input var
goto expr (maybe "run" is not needed?)
if expr1 goto expr2
if c-5 goto 20
operators: +, -, *, / ()
rnd // via formula
a = inkey

integer variables a-l (32-bit) (signed)
direct memory access via m(..)

a=b
a=10    // assign a decimal constant to a variable
           a=$10  // assign hexadecimal constant to a variable
           a='Hi!' // assign the number ('H' << 16) | ('i' << 8) | '!'
a=[b]   // read from memory
[a]=c   // write to memory
b+=c    // add a variable to another
b-=d    // subtract
d*=e    // multiply
f>=s    // right shift (or r=, or >>=, or /=)
h&=i    // bitwise and
j^=k    // bitwise xor
?a=b:10 // conditional jump (or if a = b goto 10)
?a<b:20 // conditional jump (or if a < b goto 10)
:30    // unconditional jump
a=get   // read a byte from stdin
put=a   // write a byte to stdout


r = expr
r += expr
r -= expr
r *= expr
r &= expr
r ^= expr
r >>= expr

r could be pc

*/
public class NanoBasicTest {

    @Test
    public void toUpper() {
        for(int i=0; i<0xff; i++) {
            int upper = i & (0xff - 0x30);
            if (upper >= ' ') {
//                System.out.println((char) i + " => " + (char) upper);
            } else {
//                System.out.println((char) i + " => char " + upper);
            }
//            System.out.println((char) i + " => " + (char) (i | (0x30)));
        }
    }

    // @Test
    public void numberHash() {
        HashMap<Integer, String> vars = new HashMap<>();
        for (int a = 'a'; a <= 'z'; a++) {
            if (a > 'Z' && a < 'a') {
                continue;
            }
            for (int b = 'a'; b <= 'z'; b++) {
                if (b > 'Z' && b < 'a') {
                    continue;
                }
                String s = (char) a + "" + (char) b;
                int h10 = a * 32 + b;
                if (vars.get(h10) != null) {
                    System.out.println(s + " <-> " + vars.get(h10));
                }
                vars.put(h10, s);
            }
        }
    }


}
