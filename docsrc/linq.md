# Language Integrated Query

Simple and typesafe language integrated queries (LINQ)
for both database and collection backends are supported.
The LINQ implementation internally uses macros and templates.

## SQL Backend

    import org.bau.db.Sqlite3
        Record
        text
    
    type Customer
        id int
        name text
    
    fun main()
        db : Sqlite3.open('demo.db')
        if db 
            db.dropTable(Customer)
            db.createTable(Customer)
            db.insert(newCustomer(0, 'James'))
            db.insert(newCustomer(1, 'Sarah'))
            db.insert(newCustomer(2, 'David'))
            list : db.from(Customer).where(it.id > 0).
                    orderBy(it.name).select()
            for i := until(list.size)
                u : list.get(i)
                break u == null
                println('id: ' u.id '; name: ' u.name.data)
    
    fun newCustomer(id int, name text) Customer
        u : Customer(name)
        u.id = id
        return u
    
    # helper function for de-serialization
    fun convertRecordToCustomer(r Record) Customer
        return Customer(text())

## Collection Backend

The collection backend has similar features:

    import org.bau.List
        newList
    import org.bau.FilterMap
        from
    
    type Point
        x int
        y int
    
    fun main()
        list : newList(Point)
        list.add(newPoint(1, 10))
        list.add(newPoint(3, 300))
        # for each entry that has y > 100
        # calculate x * y, and store
        # the result in a list of integers
        l2 : from(Point).in(list).where(it.y > 100).
            map(it.x * it.y).select()
        for i := until(l2.len())
            println(l2.get(i))
    
    fun newPoint(x int, y int) Point
        p : Point()
        p.x = x
        p.y = y
        return p

## Advanced Features: Sorting

Here is an example implementation that shows how to capture values
printing the AST of the parameter expression, and sort entries:

    import org.bau.Int
    
    type Point
        x int
        y float

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

## Advanced Features: Folding

The following syntax might be useful for folding / aggregation:

    fun main()
        sum : for(rangeArray(1, 20)).
            init(0).aggregate(it.agg + it.value)
        println('sum: ' sum)
        prod : for(rangeArray(1, 20)).
            init(1).aggregate(it.agg * it.value)
        println('product: ' prod)
    
    fun rangeArray(from T, to T) T[]
        data : T[to - from]
        for i := range(from, to)
            data[i - from] = i
        return data
    
    type intPair
        agg int
        value int
        
    type intAggregator
        data int[]
        init int
    
    fun for(data int[]) intAggregator
        x : intAggregator()
        x.data = data
        return x
    
    fun intAggregator init(value int) intAggregator
        init = value
        return this
    
    fun intAggregator aggregate(operation(intPair) int) macro int
        i := 0
        agg := init
        loop i < data.len
            it : intPair()
            it.agg = agg
            it.value = data[i]
            agg = operation
            i += 1
        return agg
