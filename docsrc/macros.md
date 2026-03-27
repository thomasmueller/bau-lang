# Macros

Macros can reduce repetitive code,
and can help improve the readability.
Some use cases are:

* `for` loops,
* `when` (ternary expressions),
* `assert`,
* (debug) logging,
* sorting, 
* filter and map (list comprehension),
* serialization,
* language integrated query.

## Using Macros

Macros are used like regular functions.
However, macros do not always evaluate all parameters:
Macros are replaced at compile time
with the macro implementation, and parameters are only evaluated when needed.
Macros have access to the source code of the arguments.

### For Loops

There are built-in macros `range` and `until`:

    for i := range(0, 5)
        println('i: ' i)
    for i := until(5)
        println('i: ' i)

### When (Ternary Expression)

The macro `when(condition, ifTrue, ifFalse)` evaluates `ifTrue` and `ifFalse` only when needed.
It is used like the ternary expression `condition ? ifTrue : ifFalse` in other languages.

    import org.bau.Utils
        when
    
    number := 10
    group : when(number >= 10, 'large', 'small')
    println(group)

### Assert

The `assert` macro verifies that expression is true,
and stops execution if not, printing the line number, module (if any),
source code of the expression, and variable values (sometimes called "power assert"):

    import org.bau.Assert
        assert

    age := -10
    assert(age >= 0)
    
    > assertion failed at line 423: age >= 0
    > for values "age","-10"

### Debug Logging

The `debug` macro prints the expression value, source code, and variable values (if any).

    import org.bau.Debug
        debug

    x := -10
    y := debug(x * x)
    
    > debug line 2: x * x == 100 for "x","-10"

## Implementing Macros

Macros are implemented like regular functions, by adding `macro`.
Generic types are supported. Example:

    fun when(condition int, a T, b T) const macro T
        if condition
            return a
        else
            return b

However, unlike regular functions, macros are expanded when used.
Because of that, the parameters `a` and `b` are only evaluated when needed.

In a macro, additional fields are available for each parameter:

* `module` contains the module of the source code (if any)
* `line` contains the line number in this module
* `source` contains the source code of the expression
* `ast` contains the AST (abstract syntax tree)
* `values` contains the variable names and values of the expression

Example:

    fun assert(x int) macro
        if not x
            source : x.source
            line : x.line
            values : x.values
            module : x.module
            println('assertion failed at line ' line ': ' source)
            if values.len
                println('for values ' values)
            if module.len
                println('in module ' module)
            Env.exit(1)

## The `it` Variable

When calling a macro, a variable named `it` is available, if the macro defines this variable.
This is useful to eg. iterate over elements. 
As an example, if a function or operation should be applied to each element of an array:

    fun int[] mapInt(value int) macro int[]
        result : int[this.len]
        i := 0
        while i < this.len
            it : this[i]
            result[i] = value
            i += 1
        return result

    array : int[10]
    for i := until(10)
        array[i] = i
    squares : array.mapInt(it * it)
    for i := until(10)
        println(i ': ' squares[i])

The type of the `it` variable can be specified in the parameter
if it is different from the type of the variable (`value` for the case above):

    import org.bau.Math

    fun int[] mapIntToFloat(value(int) float) macro float[]
        result : float[this.len]
        i := 0
        while i < this.len
            it : this[i]
            result[i] = value
            i += 1
        return result

    array : int[10]
    for i := until(10)
        array[i] = i
    squares : array.mapIntToFloat(Math.sqrt(it))
    for i := until(10)
        println(i ': ' squares[i])

