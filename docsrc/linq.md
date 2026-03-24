# Language Integrated Query

Support for language integrated query is work in progress.
Here the implementation and usage of what is currently working:

    import org.bau.Int
    import org.bau.String
    
    type Query(T)
        name i8[]
        condition i8[]
        orderBy i8[]
        orderBy2 i8[]
    
    type Address
        id int
        name i8[]
    
    fun newQuery(T type) Query(T)
        result : Query(T)(T.name, i8[0], i8[0], i8[0])
        return result
    
    fun convertIntToI8Array(x int) i8[]
        return Int.intToString(x)
    
    fun Query(T) where(condition(T) int) macro Query(T)
        this.condition = condition.source
        return this
    
    fun Query(T) orderBy(column(T) U) macro Query(T)
        this.orderBy = column.source
        return this
    
    fun Query(T) thenBy(column(T) U) macro Query(T)
        this.orderBy2 = column.source
        return this
    
    fun Query(T) execute() macro List(T)
        result := newList(T)
        return result
    
    fun main()
        println('one ')
        aa : Address[0]
        bb : newList(Address)
        for i := until(3)
            println('two ' i)
            query := newQuery(Address)
            println('4 ' i)
            query = query.where(i == 1)
            println('5 ' i)
            query = query.orderBy(it.name)
            println('6 ' i)
            query = query.thenBy(it.id)
            println('7 ' i)
            println(query.name)
            println(query.condition)
            println(query.orderBy)
            println(query.orderBy2)
            result : query.execute()

Sorting an array by an expression, printing the AST, capturing values:

    import org.bau.Int
    
    type Point
        x int
        y float
        
    fun append(a i8[], b i8[]) i8[]
        result : i8[a.len + b.len]
        for i := until(a.len)
            result[i] = a[i]
        for i := until(b.len)
            result[i + a.len] = b[i]
        return result
    
    fun convertIntToI8Array(x int) i8[]
        return Int.intToString(x)
    
    fun T[] sort(by(T) U) macro
        a : this
        println('sorting by: ' by.source)
        println('AST form  : ' by.ast)
        println('values    : ' by.values)
        h := 16
        loop a.len > h / 16
            h = h + h + h / 4 + 16
        loop h > 15
            h = (h - 16) / 9 * 4
            g : (h + 15) / 16
            i := g
            loop i < a.len
                old := a[i]
                it := a[i]
                t : by
                j := i - g
                loop j >= 0
                    it = a[j]
                    o : by
                    break o <= t
                    a[j + g] = a[j]
                    j -= g
                a[j + g] = old
                i += 1
    
    fun main()
        println('c is: ' append(-10, -2))
        list : Point[2]
        list[0] = Point()
        list[1] = Point()
        list[0].x = 10
        list[0].y = 1
        list[1].x = 20
        list[1].y = 2
        println('testing')
        for j := until(3)
            capturedValue : j
            list.sort(-it.y + it.x + capturedValue)
            for i := until(list.len)
                println(i ': x=' list[i].x ' y=' list[i].y)
        println('done')

Serialization of values:

    import org.bau.Int
    import org.bau.Math
    import org.bau.String
    
    type point
        x int
        y float
    
    fun convertIntToI8Array(x int) i8[]
        return Int.intToString(x)
    
    fun convertFloatToI8Array(x int) i8[]
        return Math.floatToString(x)
    
    fun convertPointToI8Array(x point) i8[]
        return 'I am a Point!'
    
    fun printField(field int, name i8[], x T) macro
        if T.fieldCount > 2
            if field > 2
                printFieldMany(field, name, x)
            else
                printFieldFew(field, name, x)
        else
            printFieldFew(field, name, x)
    
    fun printFieldFew(field int, name i8[], x T)
        if field == 0
            z : x.0
            println('  ' name ' = ' z)
            printMe(z)
        elif field == 1
            z : x.1
            println('  ' name ' = ' z)
            printMe(z)
        elif field == 2
            z : x.2
            println('  ' name ' = ' z)
            printMe(z)
    
    fun printFieldMany(field int, name i8[], x T)
        if field == 3
            z : x.0
            println('  ' name ' = ' z)
            printMe(z)
        elif field == 4
            z : x.1
            println('  ' name ' = ' z)
            printMe(z)
        elif field == 5
            z : x.2
            println('  ' name ' = ' z)
            printMe(z)
    
    fun printMe(x T) macro
        if T.fieldCount == 0
            println('Type :' T.name)
            text i8[] := x
            println(T.name ' = ' text)
            println()
        else
            println('Type :' T.name)
            println(T.name ' = {')
            j := 0
            fieldNames : T.fieldNames
            i := 0
            while i < T.fieldCount
                start : j
                loop j < fieldNames.len and fieldNames[j] <> ord(',')
                    j += 1
                len : j - start
                fn : i8[len]
                k := 0
                loop k < len
                    fn[k] = fieldNames[k + start]
                    k += 1
                j += 1
                # println('  field ' fn)
                printField(i, fn, x)
                i += 1
            println('}')
            println()
    
    fun main()
        y : 101
        printMe(y)
        p : point()
        p.x = 10
        p.y = -22.4
        printMe(p)
