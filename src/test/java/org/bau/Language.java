package org.bau;

/**

Name: Lei, Kuona, Mya, Pha, Tau (Anouk), Atlas, Soma (Anouk2), Twelve, Ro
https://github.com/NicoNex/tau


https://lemire.me/blog/2026/02/02/converting-data-to-hexadecimal-outputs-quickly/
char nibble(uint8_t x) { return x + '0' + ((x > 9) * 39); }
for (size_t i = 0, k = 0; k < dlen; i += 1, k += 2) {
    uint8_t val = src[i];
    dst[k + 0] = nibble(val >> 4);
    dst[k + 1] = nibble(val & 15);
}

add test that tries to re-assign "this"

verify if / how array bound checks are removed in other languages and Bau,
eg. reversing the entries of an array.

yn: input('Is the number larger than ' x ' ?')

* Weird syntax
At the same type a data type _and_ a value, which is weird
i := 0..x.len

* Bound check:
fun reverse_i8_array_i8(buff i8[], first 0..buff.len, last 0..buff.len)
    while first < last
        temp : buff[first]![*]
        buff[first]! = buff[last]!
        buff[last]! = temp
        f := first + 1
        break f >= buff.len
        first = f
        l := last - 1
        break l < 0
        break l >= buff.len
        last = l

* Bound check:
fun ord1(s i8[]) const int
    if s.len
        return s[0]!
    return 0

* Bound check:
fun getCache() const int[]
    result : int[10]
    result[0] = 0
    for i := range(1, 10)
        result[i] = i
    return result


* Bound check:
fun ord1(s i8[]) const int
    if s.len <= 0
        return 0
    return s[0]!

* Solver
add rules for len >= 0.
that means len <> 0 means len > 0

* Bound check (should pass)
fun sum(x int[]) int
    sum := 0
    if x.len = 0
        return 0
    i := 0..x.len
    while i < x.len
        sum += x[i]!
    return sum

* Bound check (should pass)
fun test()
  x : i8[4]
  x[0]! = 0x12


  x[1]! = 0x34
  x[2]! = 0x56
  x[3]! = 0x78
  println(readI32Le(x, i32(0)))


* Bound check (should pass)
fun truncate(data i32[]) int
    newLen := data.len
    while newLen > 0 and data[newLen - 1]! = 0
        newLen -= 1
    return newLen

* Bound check (should pass)
fun matchChar(text i8[], pos int) int
    if pos < 0
        return 0
    if pos >= text.len
        return 0
    c : text[pos]!
    return c - 1

* Bound check (should fail; passed before)
fun matchChar(text i8[], pos int) int
    if pos >= text.len
        return 0
    c : text[pos]!
    return c - 1

* Bound checks (should pass; didn't pass before)
fun test(value i8[]) int
    if value.len
        x : value[0]!
        return x
    return 0

fun test(value i8[]) int
    if value.len > 0
        x : value[0]!
        return x
    return 0

* Bound checks (should pass)
cache : int[10]
fun isMunchausen(number int) int
    n := number
    total := 0
    while n > 0
        digit : n % 10
        total += cache[digit]!
        n /= 10
    return total

* Bound checks test case (should pass):
fun readInt(d i8[], pos 0 .. d.len - 4) int
    return d[pos + 3]!

fun readInt(d i8[], pos 0 .. d.len - 4) int
    return (d[pos]! & 0xff) |
           ((d[pos + 1]! & 0xff) << 8) |
           ((d[pos + 2]! & 0xff) << 16) |
           ((d[pos + 3]! & 0xff) << 24)

* Bound checks test case (should fail):
fun test()
    data : i8[10]
    i := 0
    while i < 20
        println(data[i]!)
        i += 1
test()

* Bound checks test case (should pass)
fun test()
    data : i8[10]
    for i := until(data.len)
        println(data[i]!)
test()



* verify whitespace count (eg warn if whitespace doesn't match the expectation)

* simplify throw / catch by limiting catch to top level?
* per-module compiler flags: panicOnNullPointer, panicOnDivByZero,...
* option (just 'throws divByZero'?) to throw a checked exception for NPE and division by zero

* test setjmp / longjmp

* we have a doc and a docs directory, rename one

* array bound checks
fun test()
    data : i8[10]
    for i:= until(data.len)
        data[i - 1]! = i

fun test()
    x : int[10]
    i := 0..x.len
    while
        println(x[i]!)
        next : i + 1
        break next >= x.len
        i = next
test()

bitwise not: ~ => 0^
Operators: currently we use ~ for negation... what about ^ like in go, or 0 ^ ... ?

* better error message than "type main not found"
type Test
    value int
x := 10
fun main()
    println('test')
fun test() int
    return 1
y := test()

is it allow to edit constants (string literals) in a C program?

div by zero docs

numberOf -> leadingZeros

Javascript backend, like Nim:
nim js -d:nodejs fannkuch.nim

Demo apps:
- block game,
- text editor,
- data compression,
- sudoku solver,
- chess
- ...

diff

shell
split, cat, tar

text-based browser
https://cssence.com/2026/text-based-web-browsers/

webserver

- SQLite database reader

array-backed Treap

hash function with seed parameter

- hash table benchmark
  https://bluuewhale.github.io/posts/building-a-fast-and-memory-efficient-hash-table-in-java-by-borrowing-the-best-ideas/
  https://github.com/bluuewhale/hash-smith
- hash map with tombstones
- array-backed or doubly-linked hash map

Simplify AVL tree

all calculator functions

sound / mp3 (afplay, aplay, miniaud.io, minimp3)

matrix
- Addition and Subtraction: A + B and A - B. (Requires to have the same dimensions).
- Scalar Multiplication c . A
- Element-wise Multiplication (Hadamard Product) A o B
- Element-wise Division A (/) B
- Matrix Multiplication A x B Requires the number of columns in A to equal the number of rows in B
- Transpose: A^T (Flips rows and columns)
- Identity Matrix Creation: Creates an n * n matrix with ones on the main diagonal and zeros elsewhere
- Determinant det(A) (Calculated for square matrices; key for determining invertibility)
- Inverse A^-1 For solving systems of linear equations and other advanced tasks
- Eigenvalues and Eigenvectors Essential for principal component analysis (PCA) and dimensionality reduction
- Creation/Initialization - Zeros/Ones: Creating matrices filled entirely with 0 or 1
- Random: Creating matrices with elements drawn from a uniform or normal distribution.
- Reshaping/Flattening: Changing the dimensions of a matrix (e.g., turning a 2x3 into a 6x1).
- Slicing/Indexing: Efficiently accessing and modifying sub-matrices, rows, or columns.
- Concatenation: Combining two matrices horizontally or vertically.
(Apache Commons Math, EJML) C# System.Numerics; Math.NET Numerics; Python/NumPy

tinyStats (histogram,...)

logging

assertions

unit tests

processes

more graphics methods (color images; blur etc)

threads: thread creation, locking, mutex, semaphore,...

choices(seq, k, weights=None): Weighted sampling with replacement.

prob/
    distributions/  # Normal, Binomial, etc.
    secure/         # cryptographic RNG

- fractions
- probability functions

- Go math/big.Rat

- division by zero: 0 (or -1: Risc-V)
- modulo: return dividend

- JSON object

- html parser test

- tiny stats

- difflib (python)

- logging
- unit testing
- assertions

- cordic?

- url

SciPy
- numerical root-finding methods (Bisection, Secant)

NumPy

- Computer Algebra System (SymPy)
  - Combining Like Terms
  - Constant Folding
  - Applying Identities (+0, *1, x/x where x/0)
  - Expansion/Factoring

- minesweeper

--------------

I'm writing a new systems programming language.
I'm trying to write an article about "panic".
Could you review the article and provide feedback?

# Preventing and Handling Panic Situations

I want to build a memory-safe systems language
that reduces panic situations that stops program execution,
such as null pointer access, integer division by zero,
array-out-of-bounds, errors on unwrap, and similar.

For my language, I would like to prevent such cases where possible,
and provide a good framework to handle them when needed.
I'm writing a memory-safe language; I do not want to compromise of the memory safety.
My language does not have undefined behavior, and even in such cases,
I want behavior to be well defined.

In Java and similar languages,
these result in unchecked exceptions that can be caught.
My language does not support unchecked exceptions, so this is not an option.

In Rust, these usually result in panic which stops the process
or the thread, if unwinding is enabled.
I don't think unwinding is easy to implement in C
(my language is transpiled to C).
There is libunwind, but I would prefer to not depend on it,
as it is not available everywhere.

Why I'm trying to find a better solution:

(a) To prevent things like the Cloudflare outage on November 2025 (usage of Rust "unwrap");
    the Ariane 5 rocket explosion, where an overflow caused a hardware trap;
    divide by zero causing operating systems to crash (eg. find_busiest_group, get_dirty_limits).
(b) Be able to use the language for embedded systems,
    where there are are no panics.
(c) Simplify analysis of the program.

For Ariane, according to Wikipedia
https://en.wikipedia.org/wiki/Ariane_flight_V88
"in the event of any detected exception the processor was to be stopped".
I'm not trying to say that my proposal would have saved this flight,
but I think there is more and more agreement now that unexpected state / bugs
should not just stop the process, operating system, and
cause eg. a rocket to explode.

# Prevention

## Null Pointer Access

My language supports nullable, and non-nullable references.
Nullable references need to be checked using "if x == null",
So that null pointer access at runtime is not possible.

## Division by Zero

My language prevents prevented possible division by zero at compile
time, similar to how it prevents null pointer access.
That means, before dividing (or modulo) by a variable,
the variable needs to be checked for zero.
(Division by constants can be checked easily.)
As far as I'm aware, no popular language works like this.
I know some languages can prevent division by zero,
by using the type system, but this feels complicated to me.

Library functions (for example divUnsigned) could be guarded
with a special data type that does not allow zero:
Rust supports std::num::NonZeroI32 for a similar purpose.
However this would complicate usage quite a bit;
I find it simpler to change the contract: divUnsignedOrZero,
so that zero divisor returns zero in a well-documented way
(this is then purely op-in).

## Error on Unwrap

My language does not support unwrap.

## Illegal Cast

My language does not allow unchecked casts (similar to null pointer).

## Re-link in Destructor

My language support a callback method ('close') if an object is freed.
In Swift, if this callback re-links the object, the program panics.
In my language, right now, my language also panics for this case currently,
but I'm considering to change the semantics.
In other languages (eg. Java), the object will not be garbage collected in this case.
(in Java, "finalize" is kind of deprecated now AFAIK.)

## Array Index Out Of Bounds

My language support value-dependent types for array indexes.
By using a  as follows:

    for i := until(data.len)
        data[i]! = i    <<== i is guaranteed to be inside the bound

That means, similar to null checks, the array index is guaranteed
to be within the bound when using the "!" syntax like above.
I read that this is similar to what ATS, Agda, and SPARK Ada support.
So for these cases, array-index-out-of-bounds is impossible.

However, in practise, this syntax is not convenient to use:
unlike possible null pointers, array access is relatively common.
_requiring_ an explicit bound check for each array access would not be practical in my view.
Sure, the compiled code is faster if array-bound checks are not needed,
and there are no panics.
But it is inconvenient: not all code needs to be fast.

I'm considering a special syntax such that a zero value is returned for out-of-bounds.
Example:

    x = buffer[index]?   // zero or null on out-of-bounds

The "?" syntax is well known in other languages like Kotlin.
It is opt-in and visually marks lossy semantics.

    val length = user?.name?.length            // null if user or name is null
    val length: Int = user?.name?.length ?: 0  // zero if null

Similarly, when trying to update, this syntax would mean "ignore":

    index := -1
    valueOrNull = buffer[index]?  // zero or null on out-of-bounds
    buffer[index]? = 20           // ignored on out-of-bounds

## Out of Memory

Memory allocation for embedded systems and operating systems
is often implemented in a special way, for example,
using pre-defined buffers, allocate only at start.
So this leaves regular applications. For 64-bit operating systems,
if there is a memory leak, typically the process will just
use more and more memory, and there is often no panic;
it just gets slower.

## Stack Overflow

This is similar to out-of-memory.
Static analysis can help here a bit, but not completely.
GCC -fsplit-stack allows to increase the stack size automatically if needed,
which then means it "just" uses more memory.
This would be ideal for my language,
but it seems to be only available in GCC, and Go.

## Panic Callback

So many panic situations can be prevented, but not all.
For most use cases, "stop the process" might be the best option.
But maybe there _are_ cases where logging (similar to WARN_ONCE in Linux)
and continuing might be better,
if this is possible in a controlled way,
and memory safety can be preserved.
These cases would be op-in.
For these cases, a possible solution might be to have a (configurable) callback,
which can either: stop the process;
log an error (like printk_ratelimit in the Linux kernel) and continue;
or just continue.
Logging is useful, because just silently ignoring can hide bugs.
A user-defined callback could be used, but which decides
what to do, depending on problem.
There are some limitations on what the callback can do,
these would need to be defined.


----------------



## Division by Zero

Arguably, if integer division by zero (and remainder by zero) needs to be prevented,
then integer overflow (for addition, multiplication) also need to be prevented.
My language does not do that for performance reasons.
I know Rust does it in the debug builds,
but having different behavior in debug and release builds doesn't feel right to me.
For regular integers, currently in my language, integer overflow wraps
(in a well-defined way), just like in Java.
And integer division by zero works like floating point division by zero:
division by 0 returns max, min, or 0, for positive, negative,
and 0 divisors, respectively. Remainder by 0 returns 0.
If I implement the panic callback (above), then this can be used.

An option might be to support a new data type "safe integer" that does not allow
overflow, division by zero, and remainder by zero. This data type is then slower.


--------------

https://news.ycombinator.com/item?id=44672003   Go Segvault: There is no memory safety without thread safety

Sj.h: A tiny little JSON parsing library in ~150 lines of C99
https://news.ycombinator.com/item?id=45324349
https://github.com/rxi/sj.h

Rust: panic unwinding... consider making a "non-panic" mode? what about endless loops?

JSON Test Suite
https://github.com/nst/JSONTestSuite


Mini-Bau
x: [0] # array length 1, one integer 0? or empty array?
x[100]: 0 # array size 100 with value 0?
fun sum(x: [0])
fun factorial(x: 0)
    if x <= 1
        return 1
    return x * factorial(x - 1)


int value = 0
b = a.get("x")
if (b != null)
  c = b.get("y")
  if (c != null)
    d = c.get("z")
    if (d != null)
      value = d.get()

int value = 0
loop
  b = a.get("x")
  break not b
  c = b.get("y")
  break not c
  d = c.get("z")
  break not d
  value = d.get()
  break
value

Stack Traces
Rust: https://news.ycombinator.com/item?id=45032898

CBOR = Concise Binary Object Representation (binary json)

documentation on how to run the playground by downloading the files and run using a browser.

https://news.ycombinator.com/item?id=44767508
Efficiently Generating a Number in a Range
uint32_t bounded_rand(rng_t& rng, uint32_t range) {
    uint32_t t = (-range) % range;
    do {
        uint32_t x = rng();
        uint64_t m = uint64_t(x) * uint64_t(range);
        uint32_t l = uint32_t(m);
    } while (l < t);
    return m >> 32;
}
uint32_t bounded_rand(rng_t& rng, uint32_t range) {
    uint32_t r = rng();
    if (r < range) {
        uint32_t t = (-range) % range;
        while (r < t)
            r = rng();
    }
    return r % range;
}
uint32_t bounded_rand(rng_t& rng, uint32_t range) {
    uint32_t mask = ~uint32_t(0);
    --range;
    mask >>= __builtin_clz(range|1);
    uint32_t x;
    do {
        x = rng() & mask;
    } while (x > range);
    return x;
}




https://benbridle.com/projects/bedrock.html
Bedrock is a compact and portable 8-bit computer system that runs anywhere.

== Markdown: Pandoc

brew install pandoc
pandoc conciseSyntax.md -f gfm -t html -s -o conciseSyntax.html --css=github-styles.css

== XCC docs / Wasm docs
wasm
make wcc
./wcc -o hello.wasm hello.c
wasmtime hello.wasm
wasm2wat hello.wasm
wasm-objdump -d hello.wasm

# Go
Go allows variables with the same name as well;
they shadow the function
package main
import (
    "cmp"
    "fmt"
    "slices"
)
func customCompare(a, b int) int {
    return cmp.Compare(a, b)
}
func main() {
    ints := []int{7, 2, 4}
    slices.SortFunc(ints, customCompare)
    slices.SortFunc(ints, func(a, b int) int {
        return cmp.Compare(a, b)
    })
    fmt.Println("Ints: ", ints)
}
##

Performance:
https://github.com/smarr/are-we-fast-yet

Document
auto-conversion:
convert<type>To<type>
vs
toString

https://github.com/kostya/benchmarks

print and println without help of C
- each type has a "toStr()" method

short string optimization

maybe support slices

arrays may not be null,
but can only be empty (meaning: have zero elements).
- empty array should map to null in C, so that no memory is used.
- and so, no inc/dec ref count
- now it is so for value types, e.g. string that has a pointer;
  this is needed to support string arrays (which do not have constructors)

re-arrange fields for arrays?
combine array length, refcount, array data

maybe support something line __LINE__ and __FILE__ -- see https://stackoverflow.com/questions/2849832/c-c-line-number

type.name, id, size,... (fields or functions)

faster itoa

maybe support LINQ via ast -> string, plus compile time evaluation
type Book
    id column(int)
    title column(str)
    published column(int)
b : Book
db.from(b).where(b.published > 2010).select(b.id, b.title)
fun macro database from(table T) query
    return query(#table)
fun macro query where(condition bool) query
    this.setCondition(#condition)
    return this
fun  macro query select(x column..) query
    for i := range(x.len)
        this.addColumns(#x[i])
    return this
this kind of works:
fun from(T type) i8[]
    println('from ' T.name)
    return T.name
fun where(condition int) macro
    if condition
        println('select * from where ' condition.source)
    else
        println('select * from where ' condition.source)
fun select(x T..) macro
    println('len ' x.len)
type address
    id int
    name i8[]
fun main()
    a : address('me')
    from(address)
    where(a.id > 10 and a.name.len < 2)


Text editor
https://news.ycombinator.com/item?id=44034459
https://github.com/antirez/kilo

'is' function instead of 'equals'?

support slices as an alternative / addition to range tracking?

in templates, allow iterating over fields of types?
compile-time reflection

Stdlib:
- unit testing
- accessing file systems (directories, file listing)
- networking
- array copy / move
- starting / managing processes
- threads
- file I/O
- speed up hash table and increase load factor
- memory management (retrieve free space)
- memory mapped files
- system calls
- logging
- set (hash set, sorted set)
- btree
- sqlite
- statistics (avg, median, approximate, hyperloglog, etc)
- radix sort

TODO integrate C compiler in the browser, once 'goto' is available:
     https://github.com/tyfkda/xcc
     cd ~/data/xcc
     npm up
     npm run build-assets && gulp
     open http://192.168.45.88:3000/

TODO reduce number of lines, maybe by supporting "a, b, c := 0"

TODO No panic mode: https://blog.reverberate.org/2025/02/03/no-panic-rust.html

TODO test fully qualified package access (types,...)

TODO converter from Java or C or Rust to Bau

TODO 100% code coverage, for parser at least

TODO mark loop functions as "loop macro" or so (make sure they can not be called)

TODO REPL, similar to Scala
REPL; local vs global variables: see Scala
https://github.com/thomasmueller/bau-lang/issues/3
https://onecompiler.com/scala/43n4ew98z
println("Hello, World!")
if (true) {
  var x = 10
  x = 20
  println("Hello, World! " + x)
  sayHi()
}
def sayHi(): Unit = {
   val name = "Scala"
   println(s"Hi, $name " + x)
}


org.bau.Memory
  copy
  offset (of field)
  location (pointer)

more string formatting (and move code there from Math)

datetime library (see Rust)

cache implementation

alias: for global variables only? to void inc/dec of reference counting gc

disallow widening conversation sometimes?
x := <some int>
x += i32 * i32 (if i32 * i32 results in a i32, then it's a problem)

ARM neon intrisics like c# ?

work on playground
https://go.dev/play/

https://news.ycombinator.com/item?id=40940225
WebVM is a server-less virtual Linux environment running client-side

https://thomasmueller.github.io/bau-lang/

https://rosettacode.org/wiki/Rosetta_Code

https://langdev.stackexchange.com/

https://github.com/google/j2cl
https://www.teavm.org/docs/intro/getting-started.html

Memory management
https://nullprogram.com/blog/2024/05/25/

Lobster
https://strlen.com/lobster/

https://github.com/bazelbuild/starlark/blob/master/design.md#booleans-are-not-integers

better document, test, implement constant expression functions
- warning if it takes long to compile

debugger:
line number mapping

input
keyboard character input
goto home or up

Multi-threading
https://www.geeksforgeeks.org/multithreading-in-c/
pthread.h

---------------------------------

https://github.com/bazelbuild/starlark
- randomize the iteration order of a dictionary: optional? to simplify unit tests
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

https://en.wikipedia.org/wiki/Language_Server_Protocol

https://langserver.org/

# Documentation

A Tour of Go
https://go.dev/tour/welcome/1
https://ryjo.codes/tour-of-clips.html

# IDE, Debugger

# Online tool

https://emscripten.org/
https://tryclojure.org/
WASM
https://stackoverflow.com/questions/61925125/is-there-a-way-to-run-c-program-locally-in-a-browser

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

# Arrays

- Option to use power of 2 sizes

a := i64[1024]
a := ring(i64[1024])

circular buffer / circular array / ring buffer
https://en.wikipedia.org/wiki/Circular_buffer

# Syntax

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
  while
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
  while
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
  while
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

if x = 1
  print "one"
elif x = 2
  print "two"


 */
public class Language {

}
