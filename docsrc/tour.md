# Tour

## Hello World

    println('Hello World')

## Assignment


## Import, Functions

    import org.bau.Utils

    fun printTime()
        println(Utils.getNanoTime())
  
    printTime()

## Random

    import org.bau.Utils

    println(Utils.random())

## Math

    import org.bau.Math

    println('Pi: ' Math.PI)
    println(Math.sqrt(2))

### Functions

    fun add(x int, y int) int
        return x + y

    println(add(42 1))

### Data Types

    a := 10_000_000
    b := i8(110)
    c := i16(65000)
    d : 'text'
    e := 3.1416
    f := 0..10
    println(a ' ' b)

### Type Conversion

    a := 10_000_000
    b := 3
    println(a / b)
    println(float(a) / b)
    
### Constants

    PI : 3.1415
    println(PI)
    
### Loops

    sum := 1
    loop sum < 10_000
        sum += sum
    println(sum)
    
### For Loops

    sum := 0
    for i := range(0, 10)
        sum += i
    println(sum)

### If

    for i := range(1, 10)
        if i < 5
            println(i)

### If Else

    for i := range(1, 10)
        if i < 5
            println(i)
        else
            println(-i)

### If Elif Else

    for i := range(1, 10)
        if i = 0
            println('zero')
        elif i = 1
            println('one')
        elif i = 2
            println('two')
        else
            println('many')

### Switch

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

### Types

    type point
        x int
        y int
    
    p := point()
    p.x = 10
    p.y = 20
    
### Arrays

    array : i8[10]
    for i := until(array.len)
        array[i] = i

### List

    import org.bau.List
        List
        newList
    
    list := newList(int)
    list.add(100)
    list.add(80)
    println(list.size)
    println(list.array[0])

### Enum

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

### Macros and Ternary Condition

    fun if(cond int, a T, b T) macro T
        if cond
            return a
        else
            return b

    for i := until(3)
        println(i ':', if(i, '>0', 'zero'))

## Custom For Loops

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

## Function Pointer Callback

    fun compareAbsolute(a int, b int) int
        if a < 0
            a = -a
        if b < 0
            b = -b
        return a - b

    fun insertionSort(a int[], comp fun(int, int) int)
        for i := range(1, a.len)
            t := a[i]
            j := i - 1
            while j >= 0 and comp(a[j], t) > 0
                a[j + 1] = a[j]
                j -= 1
            a[j + 1] = t

    fun main()
        array : int[5]
        array[0] = -5
        array[1] = 6
        array[2] = 2
        array[3] = -10
        array[4] = 1
        insertionSort(array, compareAbsolute)
        for i := until(array.len)
            println(i ': ' array[i])

