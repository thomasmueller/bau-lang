# Bau

Bau is a simple, concise, safe, powerful and fast programming language. Features:

* Easy to learn. Concise syntax.
* Memory-safe. Statically typed.
* Fast compilation and execution (transpiles to C).

<a href="https://thomasmueller.github.io/bau-lang/">Try it out in the browser.</a>

It addresses other languages' issues:

* Memory safety (C, C++)
* Hard to use and master (C++, Rust)
* Vendor lock-in (Java, Swift, C#)
* GC pauses (Python, Java,...)
* Verbose syntax (C, Go, Java,...)
* Slow execution (Python)
* Null pointer errors (Java, C,...)
* Array bound checks (Java, Rust,...)

## Example

    fun fact(x int) int
      if x <= 1
        return 1
      return x * fact(x - 1)

    for i:= range(0 20)
      println(fact(i))

## Keywords

Control flow
* `if` `elif` `else` `for` `while` 
* `break` `return` `throw` `catch`
* `switch` `case`

Assignment, comparison, operations
* `:` constant, `:=`  variable
* `=` `+=` `-=` `*=` `/=` etc. update
* `=` `<` `>` `<=` `>=` `!=`
* `and` `or` `not` `+` `-` `*` `/` `%`
* `&` `|` `^` `~` `<<` `>>` bitwise

Data types and miscellaneous
* `int` `i32` `i16` `i8`, `f64` `f32`
* `#` comment, `##` block comment
* `fun` `type` `enum` definitions
* `()` `[]` `.` `..` `,` `'` `` ` `` `?`
* `import` `module` `null` `const`

### Contants, Variables

Identifiers contain letters, digits, and `_`.
`:` defines a constant.
`:=` defines a variable. `=` `+=` `-=` `*=`
`/=` `&=` `|=` `^=` `<<=` `>>=` updates it:

    PI : 3.14159
    x := 10
    x = x + 1
    x += 1      # shortcut

### Built-In Types

The built-in types are `int` `i32` `i16` `i8` (signed integer),
and `f64` `f32` (floating point).
`int` can be restricted to a range using `0..`.
Defaults are `int` and `f64`; both are 64 bit.
Conversion functions change the type, and may truncate.

    c := i8(10)
    
### Conditions

`if` starts a condition.
Spaces group statements into blocks.
`elif` ("else if") and `else` are optional.

    if a = 0
        println('zero')
    elif a = 1
        println('one')
    else
        println('many')

### Loops

There are `for` and `while` loops.
`,` is optional if the arguments are simple:

    # loop from 0 to 9
    for i := range(0 10)
        println(i)

`for` is internally converted to `while`:

    i := 0
    while i < 10
        println(i)
        i += 1

`break` exits a loop. It may have a condition:

    # prints 1 to 4
    for i := range(0 10)
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

`=` `<` `>` `<=` `>=` `!=` compare two values and return `1` or `0`.
`and` `or` `not` combine comparisons. 
`and` `or` only evaluate the right side when needed.
Integer `+` `-` `*` wrap around on over- / underflow.
`/` `%`: integer division by 0 returns max, min, or 0.
`&` `|` `^` `~` `<<` `>>` are bitwise and, or, xor, not, 
shift right, and logical shift right: the leftmost bits become `0`.

### Functions

`fun` starts a function. It may `return` a value.
`..` means variable number of arguments.
`const` functions are executed at compile time
if the arguments are constants.
Functions can share a name if the number of arguments is different.
They can be declared first and implemented later.
Types can be passed as parameters 
(internally, for each type a new function is used - like C++ templates).

    fun square(x int) int
      return x * x

    fun sum(x int..) const int
    
    fun sort(T type, T[] data)


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

Value types (eg. `int`) can't be `null`.

### Arrays Access

To create and access arrays, use:

    data : new(i8[], 1)
    data[0] = 10

Bounds are checked where needed.
Access without runtime checks require that the compiler verifies correctness.
Index variables with range restrictions allow this.
For performance-critical code, use `[` `]!` to ensure
no runtime checks are done.
The conditional `break` guarantees that `i` is within the bounds.

    if data.len
      i := 0..data.len
      while 1
        data[i]! = i
        break i >= data.len - 1
        i += 1

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
The last part of the module name is the identifier,
unless it is renamed using `as`.
The module identifier can be omitted
if the type, function, or constant is listed after `import`:

    import org.bau.Utils
      random
    import org.bau.Math as M
    println(random())
    println(Utils.getNanoTime())
    println(M.PI)

`module` defines a module. 
The name needs to match the file path, here `org/bau/Math.bau`:

    module org.bau.Math
    PI : 3.14159265358979323846

### Tour

#### Hello World

    println('Hello World')

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
    println(sqrt(2))

##### Functions

    fun add(x int, y int) int
      return x + y

    println(add(42 1))

##### Data Types

    a := 10_000_000
    b := u8(110)
    c := u16(65000)
    d := 'text'
    e := 3.1416
    f := 0..10
    println(a ' ' b)

##### Type Conversion

    a := 10_000_000
    b := 3
    println(a / b)
    println(f64(a) / b)
    
##### Constants

    PI : 3.1415
    println(PI)
    
##### For Loops

    sum := 0
    for i := range(0 10)
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
        switch Utils.random() & 3
        case 0
            println('zero')
        case 1
            println('one')
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

### Comparison

|Feature               |Bau    |Python |C      |C++    |Java   |C#     |Go     |Rust   |Swift  |
|----------------------|-------|-------|-------|-------|-------|-------|-------|-------|-------|
|Memory Safety         |&check;|&check;|       |       |&check;|&check;|&check;|&check;|&check;|
|Easy to Learn and Use |&check;|&check;|&check;|       |&check;|&check;|&check;|       |&check;|
|Concise Syntax        |&check;|&check;|       |       |       |       |       |       |       |
|Vendor Independent    |&check;|&check;|&check;|&check;|       |       |       |&check;|       |
|Strongly Typed        |&check;|       |&check;|&check;|&check;|&check;|&check;|&check;|&check;|
|Fast Execution        |&check;|       |&check;|&check;|&check;|&check;|&check;|&check;|&check;|
|No GC Pauses          |&check;|       |&check;|&check;|       |       |       |&check;|&check;|
|Runs Everywhere       |&check;|       |&check;|       |       |       |       |       |       |
|Generics / Templates  |&check;|       |       |&check;|&check;|&check;|&check;|&check;|&check;|
|Exception Handling    |&check;|&check;|       |&check;|&check;|&check;|&check;|&check;|&check;|
|Null Safety           |&check;|       |       |       |       |&check;|       |&check;|&check;|
|Array Bounds Checks   |&check;|&check;|       |       |&check;|&check;|&check;|&check;|&check;|
|Compile-Time Execution|&check;|       |       |&check;|       |       |       |       |       |

### Non-Features

* Many concepts of object-oriented programming languages are not supported,
  for example inheritance, method overloading, polymorphism,
  and more complex encapsulation.
* Many concepts of functional programming languages are not supported, 
  for example high-order functions, functional composition,
  closures.
* Reflection is not supported.
* Tail calls are only optimized by the C compiler.
* Multi-threading support is limited to what C supports.
* Coroutines are not supported.
* `goto` and labels are not supported.
* `continue` is not currently supported (it is implemented, but might be removed later).
* `switch` is not supported, to simplify the language. The compiler is supposed to 
  internally uses the same performance optimisations as if there was a switch statement.
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
* `continue` is implemented but it may not be needed, so let's not use it for now.
  Removing it simplifies the language.
* `break` and `continue` can have a condition, to avoid a separate line with `if`.
* Labels for `break` and `continue` are not supported to simplify the language.
  If needed, the function can return from inside the loop; 
  exception handling could also be used.
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
  The same goes for modulo operations.
