# Bau

A programming language for everyone.

<a href="https://thomasmueller.github.io/bau-lang/">Try it out in the browser.</a>

* Easy to learn with a concise syntax inspired by Python.
* Memory-safe. Automatic memory management using reference counting by default.
* As fast as Rust where needed, using single ownership and borrowing.
* Low memory usage; no GC pauses.
* Runs everwhere: transpiles to C.
* Null safety: Null pointer errors are prevented at compile time.
* Ability to avoid runtime array bound checks using static analysis.

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

A variable without value requires a type:

    x int

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
`while` without condition is an endless loop.

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
may contain `\n` newline, `\t` tab, `\'` single quote, 
`\\` backslash, `\x00` byte. UTF-8 is used.

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
`/` `%`: integer division by 0 returns max, min, or 0.
`&` `|` `^` `~` `<<` `>>` are bitwise and, or, xor, not, 
shift right, and logical shift right: the leftmost bits become `0`.

### Functions

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

### Macro Functions

`macro` function calls are replaced at compile time
with the implementation,
and so parameter are only evaluated when needed:

    fun if(cond int, a T, b T) macro T
        if cond
            return a
        else
            return b

    text : 'Hello'
    for i := until(10)
        println(if(i < text.len, text[i], 0))

### Types

Types can have fields and functions:

    type Square
        length int

    fun Square area() int
        return length * length

    s : new(Square)
      
If a type has a `close` function, then it is called
before the memory is freed.
`int` and other lowercase types are copied when assigned;
uppercase types are referenced.

### Types

Functions on built-in types are allowed:

    fun int square() int
        return this * this

    println(12.square())

Types can have parameters:

    type List(T)
        array T[]
        size int

    fun newList(T type) List(T)
        ...

    list := newList(Circle)

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

### Arrays Access

To create and access arrays, use:

    data : new(i8[], 3)
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
        
### Tour

#### Hello World

    println('Hello World')

#### Assignment


#### Import, Functions

    import org.bau.Utils

    fun printTime()
        println(Utils.getNanoTime())
  
    printTime()

#### Random

    import org.bau.Utils

    println(Utils.random())

#### Math

    import org.bau.Math

    println('Pi: ' Math.PI)
    println(Math.sqrt(2))

##### Functions

    fun add(x int, y int) int
        return x + y

    println(add(42 1))

##### Data Types

    a := 10_000_000
    b := i8(110)
    c := i16(65000)
    d : 'text'
    e := 3.1416
    f := 0..10
    println(a ' ' b)

##### Type Conversion

    a := 10_000_000
    b := 3
    println(a / b)
    println(float(a) / b)
    
##### Constants

    PI : 3.1415
    println(PI)
    
##### For Loops

    sum := 0
    for i := range(0, 10)
        sum += i
    println(sum)

##### While Loops

    sum := 1
    while sum < 10_000
        sum += sum
    println(sum)
    
##### If

    for i := range(1, 10)
        if i < 5
            println(i)

##### If Else

    for i := range(1, 10)
        if i < 5
            println(i)
        else
            println(-i)

##### If Elif Else

    for i := range(1, 10)
        if i = 0
            println('zero')
        elif i = 1
            println('one')
        elif i = 2
            println('two')
        else
            println('many')

##### Switch

    import org.bau.Utils

    for i := range(1, 10)
        switch Utils.random() & 7
        case 0
            println('zero')
        case 1
            println('one')
        case 2, 3
            println('2 or 3')
        else  
            println('other') 

##### Types

    type point
        x int
        y int
    
    p := new(point)
    p.x = 10
    p.y = 20
    
##### Arrays

    array : new(i8[], 10)
    for i := until(array.len)
        array[i] = i

##### List

    import org.bau.List
        List
        newList
    
    list := newList(int)
    list.add(100)
    list.add(80)
    println(list.size)
    println(list.array[0])

##### Enum

    enum weekday
        sunday
        monday
        tuesday
        wednesday
        thursday
        friday
        saturday

    for a := until(weekday.saturday + 1)
        switch a
        case weekday.sunday
            println('sunday')
        case weekday.monday
            println('monday')
        else
            println('some other day: #' a)

##### Macros and Ternary Condition

    fun if(cond int, a T, b T) macro T
        if cond
            return a
        else
            return b

    for i := until(3)
        println(i ':', if(i, '>0', 'zero'))

#### Custom For Loops

    fun main()
        for x := primesUntil(30)
            println('prime: ' x)
    
    fun primesUntil(until int) int
        _ := 2
        while
            _ += 1 + (_ & 1)
            break _ > until
            if not isPrime(_)
                continue 
            return _
    
    fun isPrime(x int) int
        if (x & 1) = 0
            return 0
        i := 3
        while i * i <= x
            if x % i = 0
                return 0
            i += 2
        return 1

### Comparison

|Feature               |Bau    |Python |C      |C++    |Java   |C#     |Go     |Rust   |Swift  |
|----------------------|-------|-------|-------|-------|-------|-------|-------|-------|-------|
|Memory Safety         |&check;|&check;|       |       |&check;|&check;|&check;|&check;|&check;|
|Easy to Learn and Use |&check;|&check;|&check;|       |&check;|&check;|&check;|       |&check;|
|Concise Syntax        |&check;|&check;|       |       |       |       |&check;|       |&check;|
|Vendor Independent    |&check;|&check;|&check;|&check;|       |       |       |&check;|       |
|Strongly Typed        |&check;|       |&check;|&check;|&check;|&check;|&check;|&check;|&check;|
|Fast Execution        |&check;|       |&check;|&check;|&check;|&check;|&check;|&check;|&check;|
|No GC Pauses          |&check;|       |&check;|&check;|       |       |       |&check;|&check;|
|Runs Everywhere       |&check;|       |&check;|       |       |       |       |       |       |
|Generics / Templates  |&check;|       |       |&check;|&check;|&check;|&check;|&check;|&check;|
|Macros                |&check;|       |&check;|&check;|       |       |       |&check;|       |
|Exception Handling    |&check;|&check;|       |&check;|&check;|&check;|&check;|&check;|&check;|
|Null Safety           |&check;|       |       |       |       |&check;|       |&check;|&check;|
|Array Bounds Checks   |&check;|&check;|       |       |&check;|&check;|&check;|&check;|&check;|
|Compile-Time Execution|&check;|       |       |&check;|       |       |       |&check;|       |

### Non-Features

* Many concepts of object-oriented programming languages are not supported,
  for example inheritance, method overloading, polymorphism,
  and more complex encapsulation.
* Many concepts of functional programming languages are not supported, 
  for example high-order functions, functional composition,
  closures.
* `map`, `filter`, etc are not supported. One problem here is exception handling.
* Reflection is not supported.
* Tail calls are only optimized by the C compiler.
* Multi-threading support is limited to what C supports.
* Coroutines are not supported; however,
  custom `for` loops are supported that work like macros.
* `goto` and labels are not supported.
* String interpolation is not supported to simplify the language. 
  Instead, use an arrays of strings. As commas are optional, this is short.
* Dynamic dispatch is not supported.

### Syntax

* Spaces (indentation) is used to group statements. This reduces the number of lines.
  Tabs are not supported. The reason is that spaces are more common,
  and tabs do not mix well with spaces. By disallowing tabs, problems are detected early.
* Commas in parameter lists are optional, if parameters are simple values.
  The same as in shell scripts or Lisp.
  This also makes 'print' statements more readable (without string interpolation).
* There is no `boolean` data type to simplify the syntax. 
  Instead, `true` is `1` and `false` is `0`.
  The common pitfalls, e.g. comparing the result of a comparison,
  requires parenthesis (eg. `a > b < c` is not allowed).
* Constants and variables are defined in a different way (`:` vs `:=`)
  so that it's easier to see for a reader if it may change later.
  But there is no keyword like "var", "val", "const", or "final" to shorten the code.
* Definition of a variables is distinct from updating it (`:=` vs `=`) to quickly
  detect if a variable was already defined, and to detect typos.
* `break` and `continue` can have a condition, to avoid a separate line with `if`.
* Labels for `break` and `continue` are not supported to simplify the language.
  If needed, the function can return from inside the loop, or throw an exception
  (such exceptions are very fast).
* Comments are only a single character (`#`) to save some typing.
  Block comments (`##`) are useful if the editor doesn't support commenting a block.
  To support eg. Markdown inside of block comments, the delimiters can be variable length.
* Raw strings are useful, to avoid escaping problems: https://xkcd.com/1638/
* Multi-line strings are always raw strings, 
  as escape sequences don't seem useful for this (tabs are supported here).
* Dangling `,` are supported to e.g. simplify re-ordering entries.
* Bit operations `|`, `&`, `^`, `~` have a higher order of precedence than comparison.
  This is different from other programming languages.
  It seems the reason why it is different in other languages is historical reasons only.
* Instead of `&&` `||` `!` we use the keywords `and`, `or` and `not`, 
  to make these common cases easier to understand for new developers,
  and in case of `not` to make it easier to read.
* There are no separate unsigned data types, to simplify the language.
* Bitwise shift to the right (`>>`) is a logical shift, that means for negative values,
  a number of zeros are added to the left. The arithmetic shift is not supported
  by the language itself, but can be supported by a library function
  (same as eg. rotation). The reason is that logical shifts are more common.

### Safety 
* There is no way to write unsafe code, except by calling C methods.
* Array bounds are check, except if array access is guaranteed to be 
  inside the bounds. This is implemented using dependent types.
  
### Memory Management
* Reference counting is used for reference types.
* Mark-and-sweep garbage collection is not used to avoid pauses.
* Borrow checking is not used to simplify writing code.
* The plan is to use reference counting only where cycles are not possible.
* The plan is to support weak references.
* The plan is to support unique pointers, and arrays of pre-allocated objects 
  accessed via handlers and a generation

### Exceptions and Panic
* Exceptions need to be handled using `catch`, or re-thrown.
* There is no `try` keyword: `catch` will catch all exceptions in the same scope.
  This is to simplify the code, and reduce the need of indentation.
  Ruby supports a similar syntax: `begin` is not needed.
* Custom exception types are allowed, with some restrictions:
  Exception types need to have an integer field `exceptionType`
  that may not have a negative value (because internally, this field
  is used to to flag whether the method was successful or not,
  and a negative value is used to indicate success).
* Possible null references need to be handled. 
  There is no way that null references can throw an exception or panic.
* Integer division (`/`) by zero, the same as floating point division by zero,
  doesn't throw an exception. Instead, it returns the highest / lowest value
  (if dividing positive or negative numbers), or zero (for zero by zero).
  This is to be more consistent with the floating point division, and
  to avoid panic for cases were it was used for "unimportant" operations
  such as calculating the number of instructions per second, for zero seconds.
  Modulo by zero always returns zero.
* Where array bound checks are needed, and the index is out of bounds,
  the program panics.
* If a type has a  `close()` function, it is called when the memory
  is freed. If this function re-adds a reference to the object,
  then the program panics.
