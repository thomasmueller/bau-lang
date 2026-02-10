package org.bau.at;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**

Javascript Repl

<program>    := <definition> | <expr> ";" ...
<definition> := "fun" <id> "(" <id> ... ")" <block>
<block>      := "{" <expr> ";" ... "}"
<expr>       := <primary> | <expr> <op> <expr>
<primary>    := <literal> | <id>   | <call>
              | <if>      | <loop> | <return>
              | "(" <expr> ")" | "-" <primary>
<if>         := "if" <expr> <block> [ "else" <block> ]
<loop>       := ( "while" | "repeat" ) <expr> <block>
<return>     := "return" <expr>
<call>       := <id> "(" { <expr> [ "," ... ] } ")"
<literal>    := digits [ "." digits ] | "'" characters "'"
<op>         := ":" | "." | "*" | "/"  | "%"  | "+"  | "-"
              | "=" | "<" | ">" | "<>" | "<=" | ">="
              | "&" | "|"
<id>         := letter { letter | digit | "_" }

Notes:
* Assignment uses ":"
* Array indexing uses "." instead of "[]".
* Global variables and constants start with a capital letter.
* Text literals start and end with '. As in SQL, the only escape character is '' for '
* Comments: use text literals.
* "&" and "|" are logical and bitwise "and" and "or"
* The following shortcuts are supported for the keywords:
  ? if, : else, * repeat, @ while, + fun, = return
* There is a built-in function "print" with shortcut ">"




^ is not needed: (x ^ y) = (x | y) - (x & y)
! is not needed: ~x = -(x + 1) = -1 ^ x

 << shift?
do we need >> shift if we can use x / (1 << n)?

>>:
if (x >= 0) {
    result = x / power(2, n);
} else {
    // Floor division logic to match arithmetic shift
    result = (x - (power(2, n) - 1)) / power(2, n);
}


break might not be needed

<comment> ::= "#" <any-char-except-newline>* <newline>

<letter>        ::= "a" | ... | "z" | "A" | ... | "Z"
<digit>         ::= "0" | ... | "9"
<identifier>    ::= <letter> (<letter> | <digit>)*
<globalIdent>   ::= "A" | ... | "Z" (<letter> | <digit>)*
<localIdent>    ::= "a" | ... | "z" (<letter> | <digit>)*
<number>        ::= <digit>+ ("." <digit>+)
<text>          ::= "'" <textChar>* "'"
<textChar>      ::= "''" | <any-char-except-'>
<addOp>     ::= "+" | "-"
<mulOp>     ::= "*" | "/" | "%"
<relOp>     ::= "<" | "<=" | "=" | ">" | ">=" | "<>"
<logicOp>   ::= "&" | "|"
<arrayOp>   ::= "."
<program> ::= <exprList>
<exprList> ::= <expression> ("," <expression>)*
<expression> ::=
      <assignment>
    | <ifExpr>
    | <loopExpr>
    | <breakExpr>
    | <logicExpr>
<assignment> ::= <lvalue> ":" <expression>
<lvalue> ::= <identifier> | <arrayAccess>
<arrayAccess> ::= <identifier> "_" <index>
<index> ::= <integer> | "(" <expression> ")"
<logicExpr> ::= <relExpr> ( <logicOp> <relExpr> )*
<relExpr> ::= <addExpr> ( <relOp> <addExpr> )?
<addExpr> ::= <mulExpr> ( <addOp> <mulExpr> )*
<mulExpr> ::= <unaryExpr> ( <mulOp> <unaryExpr> )*
<unaryExpr> ::= "-" <unaryExpr> | <primary>
<primary> ::= <number>
            | <text>
            | <functionCall>
            | <identifier>
            | "(" <expression> ")"
            | <ifExpr>
            | <loopExpr>
<ifExpr> ::= "?" <expression> "(" <exprList> ")" ( "/" "(" <exprList> ")" )?
<loopExpr> ::= "*" <expression> "(" <exprList> ")"
<functionCall> ::= <identifier> "(" <argList>? ")"
<argList> ::= <expression> ("," <expression>)*




pratt:
expression → logicExpr → relExpr → addExpr → mulExpr → unaryExpr → primary
numbers
text
identifiers
array access
parentheses
unary -
binary ops

not pratt:
assign
control flow
blocks and exression lists
function calls (parameter parsing)


When parsing a primary and you see (:
  Consume (
  Parse one full expression (using top-level parser)
  If next token is:
  ) → grouping
  , → block






* output: use @: @('hello ' name)
*
* loop index is _ and multiple loops are possible, but need to re-assign if needed later: *5(@(_),*5(@(_))
* arrays and numbers only (integers, floating point, bigint)
* data types maybe via naming convention? i integer, b bigint? ia, ba
* out-of-bounds array access return 0; reassign negative index is ignored
* array length: not supported now. possibly a_-1 later

() block
(a:1,b:2)
comma is optional
( a>0 ? (b:a) (b:-a) )

break: !, ! with condition

arrays dot, or _ for index? _ is used as index
i:0,*10(a_i:i,i:i+1)
*10(a._:_)
(out-of-bound reads return 0)

functions:
f(x):(x*x+1)



Alternatively, we could use one-character keywords:

a = 1
? a > 1
  print 'larger'
: a < 1
  print 'smaller'
:
  print 'equal'

+ fact(x #) #
  ? x <= 1
    = 1
  = x * fact(x - 1)

only floating point

if       ? <condition>
elif     :? <condition>
else     :
while    *
for      *<10
assign   :

return   =

decrement  -:1
increment  +:1
break      !
continue   ^

*/
public class AtUtils {

    @Test
    public void xor() {
        for(int a = 0; a <= 0xff; a++) {
            for(int b = 0; b <= 0xff; b++) {
                int c = a ^ b;
                int c1 = (a | b) - (a & b);
                assertEquals(c, c1);
            }
        }
    }

}
