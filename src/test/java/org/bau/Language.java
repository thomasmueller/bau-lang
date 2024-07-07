package org.bau;

/**

TODO only call close when the ref count falls to 0, and then check again after calling close because it might have been re-added

TODO some examples don't work, eg sqrt, in browser

work on playground
https://go.dev/play/

https://codemirror.net/

https://thomasmueller.github.io/bau-lang/

https://rosettacode.org/wiki/Rosetta_Code

https://langdev.stackexchange.com/

https://github.com/google/j2cl
https://www.teavm.org/docs/intro/getting-started.html

Memory management
Arena Allocator
https://nullprogram.com/blog/2024/05/25/

Lobster
https://strlen.com/lobster/

https://github.com/bazelbuild/starlark/blob/master/design.md#booleans-are-not-integers

better document, test, implement constant expression functions
- warning if it takes long to compile

debugger:
line number mapping

pub

input
keyboard character input
goto home or up

Multi-threading
https://www.geeksforgeeks.org/multithreading-in-c/
pthread.h

---------------------------------

https://github.com/bazelbuild/starlark
- randomize the iteration order of a dictionary: optional?
- No mutation during iteration
- x < y > z (no chained operators)

C++
https://en.cppreference.com/w/cpp/language/constant_expression%23Core_constant_expressions

const int cn = 2;
constexpr int n = std::numeric_limits<int>::max(); // OK: max() is constexpr
function needs to be defined, not just declared:
a function call to a constexpr function which is declared, but not defined

throwing exceptions or executing the assembly is 
disallowed in a constant expression.

no observable side-effect is permitted.
recursion depth (the standard suggests a minimum of 512)

c++14 relaxed rules for constexpr

c++11:
the function body cannot declare variables or define new types.
the body may only contain declarations, null statements and a single return statement


Rust
const fn
Constant evaluating a complex constant, this might take some time
exceeded interpreter step limit (see `#[const_eval_limit]`)

Swift
@compilerEvaluable

Go and C#: do not have it



ord('a') function


main function with array of strings

chess program
♖ ♘ ♗ ♔ ♕ ♗ ♘ ♖ 8
♙ ♙ ♙ ♙ ♙ ♙ ♙ ♙ 7
. . . . . . . . 6
. . . . . . . . 5
. . . . . . . . 4
. . . . . . . . 3
♟ ♟ ♟ ♟ ♟ ♟ ♟ ♟ 2
♜ ♞ ♝ ♚ ♛ ♝ ♞ ♜ 1
A B C D E F G H

tetris program



String data type

Built-in data types:

Do we just need int, f64, f32, and range?
u8: 0..0xff
u16: 0..0xffff
u32: 0..0xffff_ffff


Convert to Javascript as well?

https://en.wikipedia.org/wiki/Language_Server_Protocol

https://langserver.org/


# Documentation

A Tour of Go
https://go.dev/tour/welcome/1
https://ryjo.codes/tour-of-clips.html

# IDE, Debugger



# This

Get rid of "this" as a keyword?

A) ensure that fields can not be hidden by parameters and variables?
B) use ".": .radius * .radius; parameters setColor(color int) { .color = color }


# Online tool

https://emscripten.org/
https://tryclojure.org/
WASM
https://stackoverflow.com/questions/61925125/is-there-a-way-to-run-c-program-locally-in-a-browser

https://bellard.org/tcc/


Name:
Tau (Anouk)
https://github.com/NicoNex/tau

Bau
Atlas
Soma

https://docs.python.org/3/reference/index.html

Lua
Basic
Python
Java
Rust
Forth
C

https://github.com/google/wuffs


# Stack Size

how large is the stack size in C, C++, Rust, Swift, Go?
8 MB

# Heap vs Stack Allocated Arrays

Most languages seem to allocate array data on the heap, e.g. Swift
https://stackoverflow.com/questions/38298961/possible-to-create-statically-allocated-array-in-swift

# Delete

-> close (Java: resources), free (C, C++: memory), drop (only rust)
(deleteFile sounds like we want to delete the file itself...)

# Syntax from Scripting Languages

https://news.ycombinator.com/item?id=40316010
https://elv.sh/

# Memory Management

type address
  name i8[20]

type Address
  name i8[200]

a : int        -- value type, 64 bit
a : address    -- value type, user defined
a : Address    -- unique_ptr (the memory is released once the loop repeats, 
                              the loop exits, or the method returns; 
                              unless it is assigned to another pointer)
                              The drop function in Rust is a special function 
                              that is automatically called when an 
                              owned value goes out of scope.
a : Address?   -- reference to a value that might might not exist or was deleted


# C++

The C++ Iceberg
https://fouronnes.github.io/cppiceberg/

# RAII

https://doc.rust-lang.org/nomicon/obrm.html

make_unique
unique_prt
call constructor
call deconstructor


If a type has new/delete methods, then the object is automatically
deleted.
The new() method is called after initialization, and delete() when
it is no longer assigned (e.g. goes out of scope). 
It can only be assigned to one variable. 

type File
  fp i64
  File(fp i64)
    this.fp = fp
  new()
    print('opening ', fp)
  delete()
    print'closing ', fp)

for i := range(0, 10)
  f := File(i)
  print('opened ', i)

# Non-Features

- operator overloading
- boolean data type (use 0 / 1 instead; or use const)
- else if ? no: https://www.php-fig.org/psr/psr-12/
- become a functional language
- allow unsafe code

# Built-in functions

range
conversion

# Documentation

Direct source code comparison between different languages

# Generics

types as function parameters?

Use templating (like in C++)
example: sort(list, compare)

# Private vs Public

(isn't the question actually: final or not final? why can't final properties be public? 
C# supports readonly. could we make properties read-only public?)
Java: yes
C++: yes (friend)
C: no
Python: no
C#: yes
Ruby: yes but not fully (just more complicated)
Javascript: yes (public by default)
Rust: yes (private by default, public; within module)
Go: public if uppercase, private if lowercase
“Private” instance variables that cannot be accessed except from inside an object, 
don’t exist in Python. However, there is a convention that is followed by most Python code: 
a name prefixed with an underscore (e.g. _spam) should be treated 
as a non-public part of the API (whether it is a function, a method or a data member). 
It should be considered an implementation detail and subject to change without notice.

# Annotations

no need to support? or support via comments?

# Macros

https://www.quora.com/What-programming-languages-allow-you-to-reimplement-their-control-structures-in-the-language-itself

can we support macros in a generic but safe way?

Python:
https://www.w3schools.com/python/ref_func_range.asp
range(start, stop, step)
start   Optional. An integer number specifying at which position to start. Default is 0
stop    Required. An integer number specifying at which position to stop (not included).
step    Optional. An integer number specifying the incrementation. Default is 1

# Performance

https://github.com/jabbalaci/SpeedTests/tree/master/java

# List Syntax

Commas could be optional - see Lisp, Clojure
https://discourse.julialang.org/t/optional-use-of-commas/29757

# Tabs vs Spaces

Spaces are more popular
https://hoffa.medium.com/400-000-github-repositories-1-billion-files-14-terabytes-of-code-spaces-or-tabs-7cfe0b5dd7fd

# Arrays

- Option to use power of 2 sizes

a := i64[1024]
a := ring(i64[1024])

circular buffer / circular array / ring buffer
https://en.wikipedia.org/wiki/Circular_buffer

# Syntax

Do we also want conditional return? return 123 if ... ?

"while 1" or just "while", or "while true"?

    while exp do block end
    repeat block until exp
    if exp then block {elseif exp then block} [else block] end
    
    goto Name
    break
    return [explist] [‘;’]
    
    for Name ‘=’ exp ‘,’ exp [‘,’ exp] do block end
    for namelist in explist do block end
    functioncall
    local attnamelist [‘=’ explist]
    To-be-closed Variables

# Keywords and Reserved Words
"goto": reserved keyword? so we can write really low-level code, and can convert "C" or assembler
"label <abc>" keyword?
"assert" reserved keyword?
"switch" reserved keyword?

switch vs if else if

for i to list.len(), a = list[i]
for a in list

C#
https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/statements/iteration-statements
List<int> fibNumbers = [0, 1, 1, 2, 3, 5, 8, 13];
foreach (int element in fibNumbers) {
    Console.Write($"{element} ");
}
A type has the public parameterless GetEnumerator method. 
  The GetEnumerator method can be a type's extension method.
The return type of the GetEnumerator method has the public 
  Current property and the public parameterless 
  MoveNext method whose return type is bool.

# Keywords

Keywords
Lua:
and          break        do           else         elseif       end
false        for          function     goto         if           in
local        nil          not          or           repeat       return
then         true         until        while

Go:
break        default      func         interface    select
case         defer        go           map          struct
chan         else         goto         package      switch
const        fallthrough  if           range        type
continue     for          import       return       var

MyLang
if           elif         else         while        break
not          and          or           return       
catch        throw 
continue?   


# Functions

annotations
- side effect free functions? (also for the reader)
- ensure that return value is consumed? seems we don't need, as we have exceptions. 
  but e.g. read(file) returns an integer... we could use "tryRead" and "readFully"

# Null

https://news.ycombinator.com/item?id=40178325 -- What if null was an Object in Java?
https://openjdk.org/jeps/8316779 -- JEP draft: Null-Restricted Value Class Types (Preview)
 -> Zero instances

null vs default object vs optional vs exception

HashMap get() T?
- ? instead of "option<T>"
- there is no null, but it would be "absent / none"

a = map.get(abc)
- type of a is "string?"
- can cast to string as follows:
- do we allow a allow map of "i64?" or not? could be done via exception mechanism but only when returning

val l = b?.length ?: -1

struct ScientificName {
    var genus: String
    var species: String
    var subspecies: String?
    var description: String {
        var text = "\(genus) \(species)"
        if let subspecies {
            // subspecies guaranteed to be non-nil
            text += "subsp. \(subspecies)"
        }
        return text
    }
}

type ScientificName
    genus string
    species string
    subspecies string?
    description() string
        text: format(genus, ' ', species)
        if subspecies
            text: format(text, ' subsp. ', subspecies)
        return text

what if:
- obj can not be null
  -> how to distinguish from "not found"?
- support for zero object (hash map with default return value for "not found")
  -> risk of someone putting zero object in map... find(key) returns false
- if it can be null then it's an "exception" (hash map that returns an exception)
  -> weird

# Value Types

pass an out-of-range int
https://openjdk.org/jeps/8316779 
  "It's easy to, say, interpret a double with the wrong units, 
  pass an out-of-range int to a library method, 
  or fail to keep two boolean flags together in the right order."

# Operators

`,` separates parameters

https://en.wikipedia.org/wiki/Operator-precedence_parser
https://journal.stuffwithstuff.com/2011/03/19/pratt-parsers-expression-parsing-made-easy/

## Operator Precedence

https://stackoverflow.com/questions/28256/equation-expression-parser-with-precedence

// For example, in Rust x <= y == z is illegal
// a or b and c => a or (b and c)

'=' (do we need ==? Python has ==)
is '=' for strings / references possible?

+, -, *
/ never throws or panics
% or "mod" ?

>> is unsigned shift; for signed shift, use "x / (1 << n)"

and, or, not (we do not use &&, ||, !, because they are not easy to understand for non-programmers, plus ! is sometimes hard to see)
"is"? we only have one reference?
"in"? python has

# Bit Operations

& and
| or
^ xor (same as python) -- or do we use "xor" ?
~ not (same as python) -- or do we use "bnot" ?
<< -- or do we use "shl"
>>, >>> (same as Java, C#), or "shr" / "shru"

# Operator Overloading

We could do operator overloading for the following?
=, !=, >=, >, <=

but not
+ - * 

precedence

Operators
- operator overloading
- Lua: +, -, *, /, %, ^ (exponent), - (unary), // (floor div),
  &, |, ~ (xor, not), <<, >>, .. (concat), ==, <, <=, [key]
     +     -     *     /     %     ^     #
     &     ~     |     <<    >>    //
     ==    ~=    <=    >=    <     >     =
     (     )     {     }     [     ]     ::
     ;     :     ,     .     ..    ...

if !empty()
if not empty()

fun u1(x u8) u1
  return x
  
let eFloat: Float = 2.71828
eFloat: f8(2.71828)

let eFloat: Float
eFloat: f8            ??

# Type Cast

(automatic cast upwards)
10u1      -- not a solution, as operations still require cast
u1(10)    -- simple, short; clutters namespace?
(u1) 10   -- works as well

/: floating point? avoid division by zero
"div", "divUnsigned", "mod", "modUnsigned"
%: modulo (0: same as div)
div(a, b)?


# Exception / Error Handling


readFile(name string) string throws IOException
  fp = fopen(name, 'r')
  catch e IOException
    logger.log('Could not open file ', name)
    throw e
  while 1
    c : fgetc(fp)
    break feof(fp)
    print(c)
  fclose(fp)
  catch e IOException
    logger.log(e)
    throw e


fun openFile(fileName string) File or throw
  throw('File ', fileName, ' not found')

openFile(fileName)
catch e
  print(e)

https://www.amazingcto.com/best-way-to-handle-errors-for-a-programming-language/#:~:text=Java%20was%20one%20of%20the,its%20caller%20with%20the%20exception.
Zig has a short notion of Result<A,E> by annotating a type with !.

Python: raise / try: / except ValueError. no definition in method (it is not strictly needed)
Javascript: function func1() { throw new Error('oops'); }

exception class could be a map?

Swift: func processFile(filename: String) throws { ...

Go:    

body, err := iotuil.ReadFile("file.txt")
if err != nil {
  log.Fatalf("unable to read file: %v", err)
}

body = iotuil.ReadFile("file.txt")
catch err
  log.Fatalf("unable to read file: %v", err)

func check(e error) {
    if e != nil {
        panic(e)
    }
}

dat, err := os.ReadFile("/tmp/dat")
check(err)

dat = os.readFile("/tmp/dat")
catch e panic(e)

read() : i32
  do
    fp = fopen('file.txt', 'r')
  catch e
    panic(e)
  else

read() : i32, throws
  fp = fopen('file.txt', 'r')
  while 1
    c : fgetc(fp)
    break feof(fp)
    print(c)
  fclose(fp)
  
  catch e
    log(e)
    throw e

read() i32?
  fp = fopen('file.txt', 'r')
  catch e
    throw e+1
  while 1
    c : fgetc(fp)?
    break feof(fp)?
    print(c)
  fclose(fp)?

// implicit exception variable name 'e'
fp = fopen('file.txt', 'r')
or throw e+1

fp = fopen('file.txt', 'r')
or panic(e)

fp = fopen('file.txt', 'r') or panic(e)
fp = fopen('file.txt', 'r') or throw


int main () {
   FILE *fp;
   int c; 
   fp = fopen("file.txt","r");
   while(1) {
      c = fgetc(fp);
      if( feof(fp) ) { 
         break ;
      }
      printf("%c", c);
   }
   fclose(fp);
   return(0);
}


- error handler
- stack traceback
- Int32.TryParse()
try catch finally
panic: how to write tests?

# Constants

- preprocessor?
Math.PI
true
false

compile-time evaluation of user-defined functions

# Enum
- just constants? define? data types? 
- user defined ranges, or conditions?
Pascal:
  type
    cardsuit = (clubs, diamonds, hearts, spades);
    suit: cardsuit;
             value: 1 .. 13;
enum cardsuit { Clubs, Diamonds, Hearts, Spades };

# Python-like REPL (Read–eval–print loop)

# Memory Management

The parser needs to know what type of object it is

## Objects with one Reference

Unique pointer (like std::unique_ptr) to an object.
Use for temporary objects (eg. StringBuilder).
We could automatically call a function when it goes out of scope (RAII).
We can not share it or store more than one reference to it.
But we can pass it to a function, and the function can use it.
Or a function can return it.

a : new StringBuilder()
fun appendValue(a StringBuilder)

## Manually Managed Objects
Index & generation for global pool object (aka in-memory db).
Needs to be explicitly removed.
Unlike regular pointers, use-after-free is not possible,
because the runtime detects if the object was removed.
For debugging, we could enumerate all live objects.

root : new Node()
...
if root.key = key
  delete root
...
if not root
  print('Already deleted')
root.value # would results in "object deleted"
  -> can we use Node?

## Reference Counted Objects
Objects that are shared a lot.
Only possible for value objects, like string,
that do not have pointers to other objects.
Ref-countered pointer (like std::shared_ptr).
Constant (e.g. string constant) could be implemented 
with a special counter value (integer.max, or 0).

## Value Type
For short strings.
b : StringShort

## String

- string constants (any constants) don't need ref counters
- string could be be ref counted 
  because they don't have pointers
  *and* they are immutable
- we _could_ support interned strings (stored in hash table)
  but (unless if we always do that, and that would be slow), 
  this would require a "s = s.intern()" call (like in Java)
  but this would still be ref counted

## GC

- we could allocate objects on the stack, but don't do that except for variables
- constants, where we just have pointers (never free up)
- objects where we have one pointer per object, and "move" logic
- ref counted objects (value objects) if they don't have out references
  .. make sure to increment / decrement only when _storing_ the reference,
  and not in function call parameters
- global array(s) of objects
- index into this array, plus generation
- bump the generation on each delete
- check the generation on each access
- may use ref counter
each object can only have one reference / pointer
... local variables count!
f() {
  a = <obj 1>
  g()
  a.x = 1
}
g() {
  delete a  // must not be allowed
}
... each access(!) must check the generation, 
    _except_ if the generation may not have been was changed

preference of value-based datatypes allocated on the stack

https://news.ycombinator.com/item?id=40146615
Borrow Checking, RC, GC, and the Eleven () Other Memory Safety Approaches (verdagon.dev)

https://verdagon.dev/grimoire/grimoire

The Stack

special memory management code using arrays and generations and check if generation matches: 
is it complicated? is it worth it?

- index into global array
- generation counter, incremented on delete
- pointer is index+generation
- delete is: call deconstructor, increment generation
- follow pointer is: global table, check if generation matches

ref (pointer)
https://v8.dev/blog/pointer-compression
https://en.wikipedia.org/wiki/Tagged_pointer
- locked
- purgeable
- strong / weak pointer
- smi (small integer)
malloc() provides 8-byte aligned memory addresses for 32-bit platforms, and 16-byte alignment for 64-bit platforms.

initializer, or memset

drop / delete

# Assert Macro

how often is assert used in practise? answer: not often

assert as a statement?
assert 
assert(...)
if it's a function call, then the expression is always evaluated, but we do not want this
if assertTrue <statement>

# Namespaces

C++    namespace; class::
Java   nesting
Rust   impl Person

must use return value
https://stackoverflow.com/questions/36938104/can-i-apply-must-use-to-a-function-result
Swift's behavior is reversed: we must explicitly annotate the function with @discardableResult

RAII ?
https://stackoverflow.com/questions/368385/implementing-raii-in-pure-c
void scoped(int * pvariable) {
int watched __attribute__((cleanup (scoped)));


https://borretti.me/article/introducing-austral
https://eev.ee/blog/2016/12/01/lets-stop-copying-c/

interfaces Traits
structs

# Comparison to Other Languages

https://www.lua.org/manual/5.4/
https://ziglang.org/learn/overview/ - no memory safety

Garbage Collection
Weak References
Coroutines

String Literals
- escape

struct

function

function pointers, e.g. in sort
- use a compare "interface" / "class" / "type"?
- function pointer?
- use macro / template / preprocessor?

sort(a, b, compare)

fun sort(a u8[], compare fun(a u8, b u8))
fun sort(a u8[8], compare fun(a u8, b u8))
fun sort(a [u8], compare fun(a u8, b u8))
fun sort(a [type], compare fun(a type, b type))
  
if x = 1
  print('one')
else x = 2
  print('two')
else
  print('tree')


array and map init

kirk: u8[100]
kirk u8: 10
kirk: 10
kirk: u8[1, 2, 3]
kirk: person(1, 2, 3)
kirk: [1, 2, 3]


Variables
- global

Statements

Assignment
varlist ‘=’ explist
x, y = y, x

Logical Operators

    and, or, and not
    
Precedence


Table Constructors

Function Definitions

Visibility Rules
     x = 10                -- global variable
     do                    -- new block
       local x = x         -- new 'x', with value 10
       print(x)            --> 10
       x = x+1
       do                  -- another block
         local x = x+1     -- another 'x'
         print(x)          --> 12
       end
       print(x)            --> 11
     end
     print(x)              --> 10  (the global one)
     
     a = {}
     local x = 20
     for i = 1, 10 do
       local y = 0
       a[i] = function () y = y + 1; return x + y end
     end

Debugging
- we can ignore this for now, because we can convert to C / Rust / Java / Lua / ...


Python:
def factorial(x):
    if x <= 1:
        return 1
    return x * factorial(x - 1)
    
def factorial(x):
    return 1 if x <= 1 else x * factorial(x - 1)
    
Rust:
let x: Typ = Wert;
let mut v: Typ;
v = Wert;

fn fakultaet(i: u64) -> u64 {
    let mut acc = 1;
    for num in 2..=i {
        acc *= num;
    }
    acc
}
  
https://en.wikipedia.org/wiki/Foreach_loop
Lua:    for index, value in pairs(array) do
Python: for i, item in enumerate(seq):  


for a, :1, <10, +:1

if x = 1
  print "one"
elif x = 2
  print "two"


macro for A, B, C
  D
  






// gcc -O3 test.c; ls -al; ./a.out
#include <stdio.h>
#include <stdlib.h>
#include <time.h>
int main() {
    size_t num_bytes = 16 * 1024 * 1024;
    size_t* buffer = (size_t*)malloc(num_bytes * sizeof(size_t));
    if (buffer == NULL) {
        perror("Error allocating memory");
        return 1;
    }
    for (size_t i = 0; i < num_bytes; ++i) {
        buffer[i] = rand() & (num_bytes - 1);
    }
    for (size_t test = 0; test < 10; test++) {
    unsigned long long sum = 0;
    for (size_t i = 0; i < num_bytes; ++i) {
        sum += buffer[i];
    }
    struct timespec start, end;
    clock_gettime(CLOCK_MONOTONIC, &start);
    size_t j = 0;
    for (size_t repeat = 0; repeat < 10; repeat++) {
        for (size_t i = 0; i < num_bytes; ++i) {
            j = buffer[i];
            // size_t z = j & (num_bytes - 1);
            size_t z = j;
            // size_t z = j < num_bytes ? j : 0;
            if (z >= num_bytes) {
                printf("z too large \n");
                exit(1);
            }
            
            sum += buffer[z];
        }
    }
    // 33656 634021000 100%  z = j
    // 33656 659459000 104%  z = j & (num_bytes - 1)
    // 33736 671090000 106%  "if"
    // 33656 699629000 110%  j < num_bytes ? j : 0
    clock_gettime(CLOCK_MONOTONIC, &end);
    unsigned long long elapsed_ns = (end.tv_sec - start.tv_sec) * 1000000000 + (end.tv_nsec - start.tv_nsec);
    printf("Elapsed time: %llu nanoseconds Sum of %zu bytes: %llu\n", elapsed_ns, num_bytes, sum);
    
    }
    free(buffer);
    return 0;
}





grep -R --include="*.java" "<<" . | wc -l
grep -E -R --include="*.java" " \(int\) " . 


56138 "^ + [A-Za-z<>]+ [a-zA-Z0-9]+ = " (initial assignment)
17627 "^ + [a-zA-Z0-9]+ = "             (re-assign)
3081  "\+\+"                            (increment by one)
2020  "  for.*\+\+"                     (increment by one in 'for' loop)
660   "^ + [a-zA-Z0-9]+ \+= "           (increment)
1570  "\-\-"
772   "\+="
53    "\-="
49    "\*="
8     ">>="
116   "\|="
11    "\&="
11    "\/="

87832 "\""
2197  "'.'"
268   "'\\\\.'"

      
5708  " == "
5073  " != " 
2869  " < "
1679  " > "
575   " >= "
463   " <= "
4095  "\![^=]"
2556  " && "
1304  " || "
325   " & "
413   " | "
191   " << "
58    " >> "
50    " >>> "
45    " ^ "
55    " ~" / 34:  grep -R --include="*.java" "~" . | grep -v "//" | grep -v "lucene" | grep -v "/test/" | wc -l
200   " \(int\) "

886   "//.*\-\-" (comments)

29820 "  return "
18966 "  if ?\("
4009  " else \{"
1723  " else if"
1207  " \? .* : "
264   "  switch"
1241  "  case"
263   "  default"

7313  "  for ?\("
5114  "  for ?\(.*:"
2188  "  for ?\(.*;.*;" 
2019  "  for ?\(.*;.*;.*\+\+"

2005  "  for ?\( ?String.*:"
2098  "  for ?\( ?int"
264   "  for ?\( ?Map.*:"
250   "forEach"
1158  "  while ?\("
126   " while \(true\)"
54    "  do {"
52    "} while ?\("
972   "  break"
2     "  break " (with goto)
368   "  continue"
3     "  continue " (with goto)

4181  "!= null"
2969  "== null"
1786  " = null"

1900  "log\."
620   "log\.info"
373   "log\.warn"

6468  "class "
838   "interface "
133   "enum "

4928  "static final "
108   "final static "

3243  "toString"
2785  "equals"

9635  "  String"
3299  "  int"
3127  "  int "
2726  "  long"
2675  "  long "
2146  "  boolean"
633   "  byte"
40    "  byte "
112   "  char"
4     "  short"
174   "  double "
2     "  BigDecimal "
4     "  BigInteger "
53    "  ByteBuffer"
5502  "\[\]"
2989  "ArrayList"
1752  "HashSet"
1447  "HashMap"

74485 "^import "
5218  "^package "

64    "  assert "

4346  '\\'    (total escapes)
2613  '\\"'   ( \" )
 345  '\\\\'  ( \\ )
 150  "\\\\'" ( \' )
1543  '\\n'  
 161  '\\t'
  83  '\\u'
  43  '\\r'
  10  '\\f'
  20  '\\b'

 */
public class Language {

}
