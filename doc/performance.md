# Performance and Efficiency

Performance and efficiency have multiple aspects: ease of learning and usage
(developer productivity), compilation speed, startup time, runtime performance, and memory usage.

This language transpiles to C, which has a highly optimized toolchain, and is available
for embedded systems and server usage. This added step does slow down compilation a bit,
compared to pure C.

Startup time is significantly faster than that of virtual machine-based languages like Java or C#, 
as there is no VM or runtime to initialize.

Runtime performance: this language aims to compete with high-performance languages
such as C, Rust, Go, Java, and Swift.
It avoids a tracing garbage collector, which results in low memory usage and eliminates GC pauses.

Memory safety results in runtime overhead from reference counting and array bounds checking.
However, for performance-critical sections, this overhead can be mitigated:
The language supports ownership semantics for references, and range-restricted index variables,
so that the compiler can eliminate these checks where applicable.
The complexity of these features is however not needed in the majority of the cases,
which results in simple code and high productivity.

## Benchmarks

Only a small number of benchmarks are implemented so far, most of them are from
<a href="https://benchmarksgame-team.pages.debian.net/benchmarksgame/index.html">The Computer Language Benchmarks Game</a>
The implementations are simple conversions of the original Java code.
For all languages, a very simple single-threaded implementation is used (without inline assembly etc.).

Benchmark results in seconds (lower is better; best of 3 runs):

|Benchmark               |  C |Java| Bau|
|------------------------|----|----|----|
|Binary Trees            | 5.0| 3.4| 3.4|
|Fannkuch Redux          | 2.2| 2.4| 2.2|
|SpeedTest               | 1.8| 4.4| 1.8|


### Binary Trees

This test generates a number of binary trees and counts the nodes.
The Java version is very fast if given enough memory, because it doesn't collect garbage;
when limiting memory to 128 MB, it i similar in speed than C.
In Bau, a faster malloc implementation is used, which is faster than the default one.
If C uses the same malloc implementation, it is as fast as Bau.
For Bau, the ownership variant is used; the reference counted variant is 3.7 seconds.

<a href="https://benchmarksgame-team.pages.debian.net/benchmarksgame/performance/binarytrees.html">Original</a>
 - <a href="../blob/src/test/java/org/bau/perf/benchmarksgame/BinaryTrees.java">Java</a>
 - <a href="../blob/src/test/resources/org/bau/benchmarksgame/binaryTrees.c">C</a>
 - <a href="../blob/src/test/resources/org/bau/perf/benchmarksgame/binaryTrees.bau">Bau (reference counting)</a>
 - <a href="../blob/src/test/resources/org/bau/perf/benchmarksgame/binaryTreesOwned.bau">Bau (ownership)</a>
 
### Fannkuch Redux

This test uses many array accesses. For Bau, no attempt was made to eliminate them.

* <a href="https://benchmarksgame-team.pages.debian.net/benchmarksgame/performance/fannkuchredux.html">Original</a>
* <a href="../blob/src/test/java/org/bau/perf/benchmarksgame/FannkuchRedux.java">Java</a>
* <a href="../blob/blob/src/test/resources/org/bau/benchmarksgame/fannkuch.c">C</a>
* <a href="../blob/src/test/resources/org/bau/perf/benchmarksgame/fannkuch.bau">Bau</a>

### SpeedTest

This test is about the <a href="https://github.com/jabbalaci/SpeedTests">Münchausen numbers problem</a>.

* <a href="https://github.com/jabbalaci/SpeedTests/blob/master/c/main.c">Original</a>
* <a href="../blob/src/test/java/org/bau/perf/speedtest/Munchausen.java">Java</a>
* <a href="../blob/src/test/java/org/bau/perf/speedtest/munchausen.c">C</a>
* <a href="../blob/src/test/java/org/bau/perf/speedtest/munchausen.bau">Bau</a>

### Running the Test

Download and build the latest version:

    git clone git@github.com:thomasmueller/bau-lang.git
    cd bau-lang

Using Maven:

    mvn -DskipTests clean install

Using Make:

    make jar

Compiling and Running the C, Java, and Bau versions:

    mkdir -p target/benchmark

    cp src/test/resources/org/bau/benchmarksgame/*.c target/benchmark
    cp src/test/java/org/bau/perf/speedtest/*.c target/benchmark
    gcc -O3 target/benchmark/binaryTrees.c -o target/benchmark/binaryTreesC
    gcc -O3 target/benchmark/fannkuch.c -o target/benchmark/fannkuchC
    gcc -O3 target/benchmark/munchausen.c -o target/benchmark/munchausenC
    for i in {1..3}; do time target/benchmark/binaryTreesC 20; done
    for i in {1..3}; do time target/benchmark/fannkuchC 11; done    
    for i in {1..3}; do time target/benchmark/munchausenC; done

    javac src/test/java/org/bau/perf/benchmarksgame/BinaryTrees.java -d target/benchmark
    javac src/test/java/org/bau/perf/benchmarksgame/FannkuchRedux.java -d target/benchmark
    javac src/test/java/org/bau/perf/speedtest/Munchausen.java -d target/benchmark
    for i in {1..3}; do time java -cp target/benchmark org.bau.perf.benchmarksgame.BinaryTrees 20; done
    for i in {1..3}; do time java -mx128m -cp target/benchmark org.bau.perf.benchmarksgame.BinaryTrees 20; done
    for i in {1..3}; do time java -cp target/benchmark org.bau.perf.benchmarksgame.FannkuchRedux 11; done
    for i in {1..3}; do time java -cp target/benchmark org.bau.perf.speedtest.Munchausen; done
    
    cp src/test/resources/org/bau/benchmarksgame/*.bau target/benchmark
    cp src/test/java/org/bau/perf/speedtest/*.bau target/benchmark
    java -jar target/bau.jar target/benchmark/binaryTreesRefCount.bau
    java -jar target/bau.jar target/benchmark/binaryTreesOwned.bau
    java -jar target/bau.jar target/benchmark/fannkuch.bau
    java -jar target/bau.jar target/benchmark/munchausen.bau
    gcc -O3 target/benchmark/binaryTreesRefCount.c -o target/benchmark/binaryTreesRefCountBau
    gcc -O3 target/benchmark/binaryTreesOwned.c -o target/benchmark/binaryTreesOwnedBau
    gcc -O3 target/benchmark/fannkuch.c -o target/benchmark/fannkuchBau
    gcc -O3 target/benchmark/munchausen.c -o target/benchmark/munchausenBau
    for i in {1..3}; do time target/benchmark/binaryTreesRefCountBau 20; done
    for i in {1..3}; do time target/benchmark/binaryTreesOwnedBau 20; done
    for i in {1..3}; do time target/benchmark/fannkuchBau 11; done
    for i in {1..3}; do time target/benchmark/munchausenBau; done

