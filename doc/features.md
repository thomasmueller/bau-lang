# Features

## Feature Comparison

|Feature               |Bau    |Python |C      |C++    |Java   |C#     |Go     |Rust   |Swift  |
|----------------------|-------|-------|-------|-------|-------|-------|-------|-------|-------|
|Memory Safety         |&check;|&check;|       |       |&check;|&check;|&check;|&check;|&check;|
|Easy to Learn and Use |&check;|&check;|       |       |&check;|&check;|&check;|       |&check;|
|Concise Syntax        |&check;|&check;|       |       |       |       |       |       |       |
|Vendor Independent    |&check;|&check;|&check;|&check;|       |       |       |&check;|       |
|Strongly Typed        |&check;|       |&check;|&check;|&check;|&check;|&check;|&check;|&check;|
|Fast Execution        |&check;|       |&check;|&check;|&check;|&check;|&check;|&check;|&check;|
|No GC Pauses          |&check;|       |&check;|&check;|       |       |       |&check;|&check;|
|Low Memory Usage      |&check;|       |&check;|&check;|       |       |&check;|&check;|&check;|
|Runs Everywhere       |&check;|       |&check;|&check;|       |       |       |&check;|       |
|Generics / Templates  |&check;|&check;|       |&check;|&check;|&check;|&check;|&check;|&check;|
|Macros                |&check;|       |&check;|&check;|       |       |       |&check;|&check;|
|Exception Support     |&check;|&check;|       |&check;|&check;|&check;|       |&check;|&check;|
|Null Safety           |&check;|       |       |       |       |&check;|&check;|&check;|&check;|
|Array Bounds Checks   |&check;|&check;|       |       |&check;|&check;|&check;|&check;|&check;|
|Compile-Time Execution|&check;|       |       |&check;|       |       |       |&check;|&check;|
|Custom "For" Loops    |&check;|&check;|       |&check;|&check;|&check;|       |&check;|&check;|
|Function Pointers     |&check;|&check;|&check;|&check;|&check;|&check;|&check;|&check;|&check;|
|Indentation-Based     |&check;|&check;|       |       |       |       |       |       |       |
|Interfaces            |  TODO |&check;|&check;|&check;|&check;|&check;|&check;|&check;|&check;|
|Multiple Threads      |  TODO |&check;|&check;|&check;|&check;|&check;|&check;|&check;|&check;|
|Reflection            |       |&check;|       |       |&check;|&check;|       |       |&check;|
|High-Order Functions  |       |&check;|       |&check;|&check;|&check;|&check;|&check;|&check;|
|Map, Filter           |       |&check;|       |&check;|&check;|&check;|       |&check;|&check;|
|Coroutines            |       |&check;|       |&check;|       |&check;|&check;|&check;|&check;|
|String Interpolation  |       |&check;|       |       |       |&check;|       |&check;|&check;|
|Goto                  |       |       |&check;|&check;|       |&check;|&check;|       |       |
|Multiple Inheritance  |       |&check;|       |&check;|       |       |       |       |       |
|Tail-Call Optimization|       |       |       |       |       |       |       |       |       |

## Non-Features

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
* A main method is not needed. However, if there is no main method,
  then the variables before the first function (if any) are global
  variables, and those after a function are local variables.

## Safety 
* There is no way to write unsafe code, except by calling C methods.
* Array bounds are check, except if array access is guaranteed to be 
  inside the bounds. This is implemented using dependent types.
  
## Memory Management
* Reference counting is used for reference types.
* Mark-and-sweep garbage collection is not used to avoid pauses.
* Borrow checking is not used to simplify writing code.
* The plan is to use reference counting only where cycles are not possible.
* The plan is to support weak references.
* The plan is to support unique pointers, and arrays of pre-allocated objects 
  accessed via handlers and a generation

## Exceptions and Panic
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
