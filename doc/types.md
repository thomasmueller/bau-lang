## Values Types and Reference Types

Value types are for example `int` and other types that start with lowercase.
Value types directly contain their data, and each variable
(or array element, or field) holds a unique copy.
Value types can not be null.

    type point
        x int
        y int
        
    a : point()
    a.x = 10
    a.y = 20
    b : a     # the data is copied
    b.x = 30
    b.y = 40
    println('a: ' a.x ',' a.y) # a: 10,20
    println('b: ' b.x ',' b.y) # b: 30,40

Reference types start with uppercase.
Variables store an address (a reference) to the actual object in memory,
usually in the heap.
Assigning a reference type from one variable to another
copies the address only, so that both variables
point to the same memory location.
Reference types can optionally be null,
which means they do not point to a valid object in memory.

    type Point
        x int
        y int
        
    a : Point()
    a.x = 10
    a.y = 20
    b : a     # the object is referenced
    b.x = 30
    b.y = 40
    println('a: ' a.x ',' a.y) # a: 30,40
    println('b: ' b.x ',' b.y) # b: 30,40
