# Language Integrated Query

Support for language integrated query is work in progress.
Here the implementation and usage of what is currently working:

    import org.bau.Int
    import org.bau.String
    
    type Any
    
    type Query(T)
        name i8[]
        condition i8[]
        orderBy i8[]
        orderBy2 i8[]
    
    fun newQuery(T type) Query(T)
        result : Query(T)(T.name, i8[0], i8[0], i8[0])
        return result
    
    fun convertIntToI8Array(x int) i8[]
        return Int.intToString(x)
    
    fun Query(T) where(condition(T) int) macro Query(T)
        this.condition = condition.source
        if condition.paramCount > 0
            p0 : condition.param0
            # TODO support parameters
        return this
    
    fun Query(T) orderBy(column(T) Any) macro Query(T)
        this.orderBy = column.source
        return this
    
    fun Query(T) then(column(T) Any) macro Query(T)
        this.orderBy2 = column.source
        return this
    
    fun Query(T) execute() macro List(T)
        result := newList(T)
        return result
    
    type Address
        id int
        name i8[]
    
    fun main()
        for i := until(3)
            query : newQuery(Address).
                where(it.id = i and it.name.len < 20).
                orderBy(it.name).then(it.id)
            println(query.name)
            println(query.condition)
            println(query.orderBy)
            println(query.orderBy2)
            result : query.execute()
        