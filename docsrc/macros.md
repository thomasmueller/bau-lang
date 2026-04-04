# Macros

Macros can reduce repetitive code,
and can help improve the readability.
Some use cases are:

* `for` loops to iterate over entries and / or indexes,
* `when` (ternary expressions),
* `assert`,
* (debug) logging,
* sorting, 
* filter and map (list comprehension),
* serialization,
* language integrated query,
* compile-time reflection.

## Using Macros

Macros are used like regular functions.
However, macros do not always evaluate all parameters:
Macros are replaced at compile time
with the macro implementation, and parameters are only evaluated when needed.
Macros have access to the source code of the arguments.

### For Loops

There are built-in macros `range` and `until` to iterate
from `0` or from some other value up to, but excluding, an upper bound.
The following will print `0`, `1`, `2`, `3`, and `4`:

    for i := until(5)
        println('i: ' i)
    for i := range(0, 5)
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

### Language Integrated Query

Macros can be used to generate SQL queries,
and access databases in a type-safe way. 

    import org.bau.db.Sqlite3
        Sqlite
        Record
        text
        printText
    import org.bau.Int
    import org.bau.String
    
    type User
        id int
        name text
    
    fun main()
        db : Sqlite3.open('demo.db')
        if db == null
            return
        db.dropTable(User)
        db.createTable(User)
        db.insert(newUser(0, 'This'))
        db.insert(newUser(1, 'Is'))
        db.insert(newUser(2, 'Better'))
        list : db.from(User).where(it.id > 0).orderBy(it.name).select()
        for i := until(list.size)
            u : list.get(i)
            break u == null
            printText('id: ' u.id '; name: ' u.name)
    
    # help function for inserting
    fun newUser(id int, name text) User
        u : User(name)
        u.id = id
        return u
    
    # helper function for de-serialization
    fun convertRecordToUser(r Record) User
        return User(text())

## Implementing For Loop Functions

Custom `for` loop functions can be implemented.
The variable `_` marks the result, and `return _` is replaced with the loop body. 
A `continue` statement jumps to the statement just after the `return`.

As an example, the following allows to iterate over the elements of an array:

    fun elements(array T[]) T
        if array.len
            i := 0..array.len
            loop
                _ : array[i]
                return _
                break i + 1 >= array.len
                i += 1

Sometimes it is useful to have the index and the value,
or (for a hash table) the key and the value.
For this purpose, a `pair` type can be used:

    type pair(T)
        i int
        value T
    
    fun newPair(value T, index int) pair(T)
        result : pair(T)()
        result.value = value
        result.i = index
        return result
    
    fun pairs(array T[]) pair(T)
        if array.len
            i := 0..array.len
            loop
                _ : newPair(array[i], i)
                return _
                break i + 1 >= array.len
                i += 1
    
    fun main()
        array : int[5]
        for i := until2(array.len)
            array[i] = i * 10        
        for e := elements(array)
            println('element ' e)        
        for p := pairs(array)
            println('index-value-pairs #' p.i ' = ' p.value)

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

