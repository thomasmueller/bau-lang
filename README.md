# The Bau Language

Simplicity, speed, and safety - redefined.

## Highlights

* <a href="doc/conciseSyntax.md">Easy to learn. Concise syntax inspired by Python.</a>
* <a href="doc/performance.md">High performance</a>. As fast as Rust where needed, using single ownership and borrowing.
* Memory-safe. Automatic memory management using reference counting by default.
* Low memory usage; no GC pauses.
* Runs everywhere: transpiles to C. No runtime library needed.
* Avoid runtime array bound checks using static analysis.
* Null safety: null pointer errors are prevented at compile time.

## Tools and Documentation

* <a href="https://thomasmueller.github.io/bau-lang/">Playground</a>
* <a href="https://thomasmueller.github.io/bau-lang/convert-to-bau.html">Convert to Bau</a>
* <a href="doc/transpile.md">Transpiler</a>
* <a href="doc/features.md">Features and Non-Features</a>.
* <a href="doc/tour.md">Tour</a>.

## Example

    fun fact(x int) int
        if x <= 1
            return 1
        return x * fact(x - 1)

    for i := range(0, 20)
        println(fact(i))

## Keywords

Control flow
* `if` `elif` `else` `for` `while`
* `break` `continue` `return`
* `throw` `catch` `switch` `case`

Assignment, comparison, operations
* `:` constant, `:=`  variable
* `=` `+=` `-=` `*=` `/=` etc. update
* `=` `<` `>` `<=` `>=` `<>`
* `and` `or` `not` `+` `-` `*` `/` `%`
* `&` `|` `^` `~` `<<` `>>` bitwise

Data types and miscellaneous
* `int` `i32` `i16` `i8`, `float` `f32`
* `#` comment, `##` block comment
* `fun` `type` `enum` `const` `macro`
* `import` `module` `null`
* `()` `[]` `.` `..` `,` `'` `` ` `` `?`

### Constants, Variables

Identifiers contain letters, digits, and `_`.
`:` defines a constant.
`:=` defines a variable. `=` `+=` `-=` `*=`
`/=` `&=` `|=` `^=` `<<=` `>>=` updates it:

    PI : 3.14159
    x := 10
    x += 1    # shortcut for x = x + 1

Variables need to be defined before they are used.
Variables are at the top level (without indentation) are global.
Variables without value require a type:

    x int

A list of variables can be declared and initialized:

    x, y := 0

### Built-In Types

The built-in types are `int` `i32` `i16` `i8` (signed integer),
and `float` `f32` (floating point).
`int` can be restricted to a range using `0..`.
Defaults are `int` and `float`; both are 64 bit.
Conversion functions change the type, and may truncate.

    c := i8(10)
    
### Conditions

`if` starts a condition.
Spaces group statements into blocks.
`elif` (else if) and `else` are optional.

    if a = 0
        println('zero')
    elif a = 1
        println('one')
    else
        println('many')

### Loops

There are `for` and `while` loops.
`while` without a condition is endless.

    # loop from 0 to 9
    for i := range(0, 10)
        println(i)

`for` is internally converted to `while`:

    i := 0
    while i < 10
        println(i)
        i += 1

`break` exits a loop. `continue` restarts.
They may have a condition:

    # prints 1 to 4
    for i := range(1, 10)
        break i = 5
        println(i)

### Comments

`#` starts a line comments; 
two or more start and end a block comment.

    # Line comment
    
    ##
    Block comment
    ##

Comments before types and functions are
converted to documentation.

### Literals

Numbers start with a digit. `_` is ignored.
`.` is floating point, `0x` hexadecimal. 

Strings starting with `'` 
may contain `\n` newline, `\r` return, `\t` tab, 
`\'` single quote,  `\\` backslash, `\x00` byte. 
UTF-8 is used.

Raw strings don't have escapes 
and start and end with one or more `` ` ``.
Multi-line ones begin on the next line 
and may be indented.

    a : 1_000_000
    b : 3.1415
    c : 0xcafe
    d : 'String literal'
    e : `Raw string`
    f : ``
        Two-line
        raw string with `
        ``

### Operators

`=` `<` `>` `<=` `>=` `<>` compare two values and return `1` or `0`.
`not` inverses a comparison. `and` `or` combines comparisons;
the right side is only evaluated when needed.
Integer `+` `-` `*` wrap around on over- / underflow.
`/` division by 0 returns max, min, or 0; `%` by 0 return 0.
`&` `|` `^` `~` `<<` `>>` are bitwise and, or, xor, not, 
shift right, and logical shift right: the leftmost bits become `0`.

### Functions

A program starts with a `main` function.
If `main` is not defined, top-level statements 
(outside any function) are executed instead.

`fun` starts a function. It may `return` a value.
`..` means variable number of arguments.
Functions can share a name if the number of arguments is different.
They can be declared first and implemented later.
`const` functions are executed at compile time
if the arguments are constants.

    fun square(x int) int
        return x * x

    fun sum(x int..) const int
        sum := 0
        for i := until(x.len)
            sum += x[i]
        return sum

    println('sum: ' sum(1 2 3))
    for i := until(5)
        println(square(i))

### Template Functions

Types can be passed as parameters, or implicitly.
Internally, this functions are templates.

    fun main()
        a : arrayOf(i8, 0, 1, 2, 3)
        b : array(0, 1, 2, 3)

    fun arrayOf(T type, entries T..) T[]
        return entries

    fun array(entries T..) T[]
        return entries

The name of the type is available using `T.name`.

### Macro Functions

`macro` function calls are replaced at compile time
with the implementation,
and so parameters are only evaluated when needed:

    fun if(cond int, a T, b T) macro T
        if cond
            return a
        else
            return b

    text : 'Hello'
    for i := until(10)
        println(if(i < text.len, text[i], 0))

The source code of the parameter is available
using `.source`:

    fun assert(x int) macro
        if not x
            println('assertion failed: ' x.source)

    for i := until(10)
        assert(i < 5)

### Arrays Access

To create a new array and then access it, use:

    data : i8[3]
    data[0] = 10

Bounds are checked where needed.
Access without runtime checks require that the compiler verifies correctness.
Index variables with range restrictions allow this.
For performance-critical code, use `[` `]!` to ensure
no runtime checks are done.
The conditional `break` guarantees that `i` is within the bounds.

    if data.len
        i := 0 .. data.len
        while
            data[i]! = i
            next : i + 1
            break next >= data.len
            i = next

### Types

Types can have fields and functions:

    type Square
        length int

    fun Square area() int
        return length * length

    s : Square()

`int` and other types that start with lowercase are copied when assigned;
types that start with uppercase are referenced.

If a type has a `close` function, then it is called
before the memory is freed.

For each type, a constructor is automatically added,
which has the non-nullable fields as arguments.

Functions on built-in types, and arrays, are allowed.

    fun int square() int
        return this * this

    println(12.square())

### Template Types

Types can have parameters. Such types are called templates:

    type List(T)
        array T[]
        size int

    fun newList(T type) List(T)
        return List(T)(T[0])

    intList := newList(int)
    squareList := newList(Square)

### Null

`?` means it may be `null`. 
An explicit check is required before using the value.
There are no null pointer errors at runtime.

    fun get(key int) Circle?
        # may return null

    v : get(key) 
    if v
        print(v.area())

For value types and numbers, `null` means zero.

### Memory Management

Objects are reference counted by default.
To avoid cycles, explicitly set fields to `null`.

    type Tree
        left Tree?
        right Tree?

    fun Tree nodeCount() int
        result := 1
        l : left
        if l
            result += l.nodeCount()
        r : right
        if r
            result += r.nodeCount()
        return result

Where speed is critical, use single ownership,
by adding `+` to the type, and borrow with `&`.

    type Tree
        left Tree+?
        right Tree+?

    fun Tree+ nodeCount() int
        result := 1
        l : &left
        if l
            result += l.nodeCount()
        r : &right
        if r
            result += r.nodeCount()
        return result

### Exceptions

`throw` throws an exception. `catch` is needed,
or the method needs `throws`.
Custom exception types are allowed.

    import org.bau.Exception
        exception

    fun square(x int) int throws exception
        if x > 3_000_000_000
            throw exception('Too big')
        return x * x
    
    x := square(3_000_000_001)
    println(x)
    catch e
        println(e.message)

### Modules and Import

`import` allows using types and functions from a module.
The last part of the module name is the identifier.
The module identifier can be omitted
if the type, function, or constant is listed after `import`.
The full module name can be used as well.

    import org.bau.Utils
        random
    import org.bau.Math
    println(random())
    println(Utils.getNanoTime())
    println(Math.PI)
    println(org.bau.Math.PI)

`module` defines a module. 
The name needs to match the file path, here `org/bau/Math.bau`:

    module org.bau.Math
    PI : 3.14159265358979323846

### Custom Loops

Libraries and users can define their own `for` loops using user-defined functions.
Such functions work like macros, as they are expanded at compile time.
The loop is replaced during compilation with the function body.
The variable `_` represents the current iteration value.
The `return _` statement is replaced during compilation with the loop body.

    fun main()
        for x := evenRange(0, 30)
            println('even: ' x)

    fun evenRange(from int, to int) int
        _ := from
        while _ < to
            return _
            _ += 2

is equivalent to:

    fun main()
        x := 0
        while x < 30
            println('even: ' x)
            x += 2

### Function Pointers

Function pointers are variables that point to a function.
They allow to call a function indirectly,
which is useful for callbacks. Example:

    fun log(x int)
        println('value is ' x)

    fun count(to int, callback fun(int))
        for i := until(to)
            callback(i)

    count(5, log)

### Traits

Note: The trait implementation is work-in-progress.

Traits (in other languages called interfaces, mixin, or prototype)
contain a list of functions.

    trait Reader
        read() int
        size() int

If a type implements the trait, it needs to declares these function,
unless if the trait already has a default implementation.

    fun Reader size()
        return -1

    type Memory : Reader
        array int[]
        pos int
    
    fun Memory read()
    
Traits can require other traits; such traits inherit the functions
of the required traits.

    trait Writer : Reader
        write(x int)

Types can implement multiple traits, 
and multiple types can implement the same trait.

