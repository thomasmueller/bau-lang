package org.bau.nano;

/**

written in Bau

varDecl :=
  <variable> : ints(<number>)
  <variable> := 0

function := fun <name>() <statement> ...

statement := assign | if | loop | print

assign := <variable> | <variable>[<variable>] = <expr>

expr := <variable> | <variable>[<expr>] | <number> +|-|*|/ <expr>

print('text' | <variable>)

if <condition>


primarily interpreted (bytecode), but faster than Lua

https://www.reddit.com/r/programming/comments/badl2/comment/c0lrus0/
* the core of the interpreter fits in 6K
* register-based bytecode
* bytecode is really a word-code (32 bit/ins)
* bytecode is type-specialized
* special calling conventions for built-ins (fast functions).

subset of Bau or assembler? subset of Bau

no ( ) at all? that would be assembler (need goto etc)
function calls without ()?

exception handling:
require a method that does just exception handling and nothing else?
callback? null return? goto?

if else (else if via else + if)
loop, break (always conditional)
return (always needed?)

= += -= = > >= <= !=
and or not + - * / %
& | ^ << >>
'fun' keyword not needed (always needed: main)
[] . , ' `
no "this" but "root"? "root.seed = 1"?
minimal perfect hash for small tables?
root[0]._name
root['id'] = root.id = root[0]
root.id._name = 'id'
no but _name = '' ?
global.doesNotExist = 0 ?

just include -- like in C -- but only once per module?

data types:
- long
- float
- bytearray=string, but no byte type

array = struct = pointer

goto? no, because of labels? that would make it assembler?

*/
public class NanoBauLanguage {

}
