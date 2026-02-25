import std/time/chrono
import std/time/duration
import std/time/instant
import std/time/utc

type node
  con Nil
  con Cons( value : int, next : node )

fun head( n : node) : int
  match n
    Cons( value, _ ) -> value
    Nil -> 0

fun create-linked-list( limit : int ) : div node
  fun build( i : int ) : div node
    if i >= limit then Nil
    else Cons( i, build( i + 1 ) )
  build( 0 )

fun main()
  println( "Starting stress test..." )
  val x     = create-linked-list( 50_000_000 )
  val count = 1_000_000
  val len   = 1000

  var worst := 0
  var last  := now().duration.nano-seconds
  repeat(count)
    val _ = create-linked-list(len)
    val now = now().duration.nano-seconds
    worst := max(now - last, worst)
    last  := now

  println(
    "Max delay: " ++ show( worst ) ++
    " ns; dummy value: " ++ show( x.head )
  )

