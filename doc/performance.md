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

Only a small number of benchmarks are implemented so far, most of them are based on
the microbenchmarks from <a href="https://benchmarksgame-team.pages.debian.net/benchmarksgame/index.html">The Computer Language Benchmarks Game</a>.

Most implementations are simple conversions of the original Java code.
For all languages, a very simple single-threaded implementation is used 
(without inline assembly etc.).
Memory usage  is not currently measured.
The tests are run 3 times, and the best time is used.
Benchmark results in seconds (lower is better; best of 3 runs).
For Java, memory is limited to 128 MB by using `-mx128m`, 
and the just-in-time compiler is pre-warmed 
by running the same test 3 times inside the same JVM 
(which is arguably not quite fair).

| Benchmark              |  Bau |   C  |  Go  | Java |Python| Rust |
|------------------------|------|------|------|------|------|------|
| #1 Binary Trees        |  5.2 |  5.1 |  5.6 |  3.3 | 10.7 |  5.5 |
| #2 Fannkuch Redux      |  1.9 |  1.9 |  2.2 |  2.3 |  9.7 |  1.9 |
| #3 SpeedTest           |  1.8 |  1.8 |  3.2 |  4.4 |270.0 |  1.7 |
| #4 Pi Digits           |  3.0 |   -  |  0.9 |  3.5 |  5.1 |  1.4 |

So in summary, for these benchmarks, Bau has a similar performance
then other popular programming languages, specially C.
Which makes sense, because it is transpiled to C.
It is sometimes slower, and sometimes faster than Java.

```mermaid
---
config:
    xyChart:
        titleFontSize: 16
        width: 500
        height: 200
        plotReservedSpacePercent: 80
        yAxis: { showLabel: false, showTick: false, showAxisLine: false }
        chartOrientation: "horizontal"
    themeVariables:
        xyChart:
            titleColor: "#000000"
            plotColorPalette: "#87CEEB"
---
xychart-beta
    title "Bau"
    x-axis [#1, #2, #3, #4]
    y-axis "Seconds" 0 --> 6
    bar [5.2, 1.9, 1.8, 3.0]
```
```mermaid
---
config:
    xyChart:
        titleFontSize: 16
        width: 500
        height: 200
        plotReservedSpacePercent: 80
        yAxis: { showLabel: false, showTick: false, showAxisLine: false }
        chartOrientation: "horizontal"
    themeVariables:
        xyChart:
            titleColor: "#000000"
            plotColorPalette: "#00599C"
---
xychart-beta
    title "C"
    x-axis [#1, #2, #3, #4]
    y-axis "Seconds" 0 --> 6
    bar [5.1, 1.9, 1.8, 0.0]
```
```mermaid
---
config:
    xyChart:
        titleFontSize: 16
        width: 500
        height: 200
        plotReservedSpacePercent: 80
        yAxis: { showLabel: false, showTick: false, showAxisLine: false }
        chartOrientation: "horizontal"
    themeVariables:
        xyChart:
            titleColor: "#000000"
            plotColorPalette: "#ED8B00"
---
xychart-beta
    title "Java"
    x-axis [#1, #2, #3, #4]
    y-axis "Seconds" 0 --> 6
    bar [5.6, 2.2, 3.2, 0.9]
```
```mermaid
---
config:
    xyChart:
        titleFontSize: 16
        width: 500
        height: 200
        plotReservedSpacePercent: 80
        yAxis: { showLabel: false, showTick: false, showAxisLine: false }
        chartOrientation: "horizontal"
    themeVariables:
        xyChart:
            titleColor: "#000000"
            plotColorPalette: "#FFD43B"
---
xychart-beta
    title "Python"
    x-axis [#1, #2, #3, #4]
    y-axis "Seconds" 0 --> 6
    bar [10.7, 9.7, 99, 5.1]
```
```mermaid
---
config:
    xyChart:
        titleFontSize: 16
        width: 500
        height: 200
        plotReservedSpacePercent: 80
        yAxis: { showLabel: false, showTick: false, showAxisLine: false }
        chartOrientation: "horizontal"
    themeVariables:
        xyChart:
            titleColor: "#000000"
            plotColorPalette: "#5A2A00"
---
xychart-beta
    title "Rust"
    x-axis [#1, #2, #3, #4]
    y-axis "Seconds" 0 --> 6
    bar [5.5, 1.9, 1.7, 1.4]
```

### Binary Trees

This test generates a number of binary trees and counts the nodes.
The Java version is very fast if given enough memory, because it doesn't collect garbage;
when limiting memory to 128 MB, it i similar in speed than C.
For Bau, the ownership variant is used; the reference counted variant is a bit slower.

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
This is a very fast loop with a lot of array access. 
(Standard) Python is particularly slow here because it is interpreted and doesn't use a JIT compiler.

* <a href="https://github.com/jabbalaci/SpeedTests/blob/master/c/main.c">Original</a>
* <a href="../blob/src/test/java/org/bau/perf/speedtest/Munchausen.java">Java</a>
* <a href="../blob/src/test/java/org/bau/perf/speedtest/munchausen.c">C</a>
* <a href="../blob/src/test/java/org/bau/perf/speedtest/munchausen.bau">Bau</a>

### Pi Digits

This uses a big integer library that computes .
<a href="https://benchmarksgame-team.pages.debian.net/benchmarksgame/description/pidigits.html#pidigits">10'000 digits of Pi</a>.
Performance depends mostly on the big integer library.
The big integer library of Go, for example, is highly optimized, and using platform-specific assembly.
The Rust library is highly optimized as well.
The Bau library is around 400 lines of code, modelled after the Java library,
without platform-specific code.

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
    mkdir -p target/benchmark/org/src/bau

    # Bau
    cp src/main/resources/org/bau/*.bau target/benchmark
    cp src/test/resources/org/bau/benchmarksgame/*.bau target/benchmark
    cp src/test/java/org/bau/perf/speedtest/*.bau target/benchmark
    java -jar target/bau.jar target/benchmark/binaryTreesRefCount.bau
    java -jar target/bau.jar target/benchmark/binaryTreesOwned.bau
    java -jar target/bau.jar target/benchmark/fannkuch.bau
    java -jar target/bau.jar target/benchmark/munchausen.bau
    java -jar target/bau.jar target/benchmark/piDigits.bau
    gcc -O3 target/benchmark/binaryTreesRefCount.c -o target/benchmark/binaryTreesRefCountBau
    gcc -O3 target/benchmark/binaryTreesOwned.c -o target/benchmark/binaryTreesOwnedBau
    gcc -O3 target/benchmark/fannkuch.c -o target/benchmark/fannkuchBau
    gcc -O3 target/benchmark/munchausen.c -o target/benchmark/munchausenBau
    gcc -O3 target/benchmark/piDigits.c -o target/benchmark/piDigitsBau
    for i in {1..3}; do time target/benchmark/binaryTreesRefCountBau 20; done
    for i in {1..3}; do time target/benchmark/binaryTreesOwnedBau 20; done
    for i in {1..3}; do time target/benchmark/fannkuchBau 11; done
    for i in {1..3}; do time target/benchmark/munchausenBau; done
    for i in {1..3}; do time target/benchmark/piDigitsBau > out.txt; done

    # C
    cp src/test/resources/org/bau/benchmarksgame/*.c target/benchmark
    cp src/test/java/org/bau/perf/speedtest/*.c target/benchmark
    gcc -O3 target/benchmark/binaryTrees.c -o target/benchmark/binaryTreesC
    gcc -O3 target/benchmark/fannkuch.c -o target/benchmark/fannkuchC
    gcc -O3 target/benchmark/munchausen.c -o target/benchmark/munchausenC
    for i in {1..3}; do time target/benchmark/binaryTreesC 20; done
    for i in {1..3}; do time target/benchmark/fannkuchC 11; done    
    for i in {1..3}; do time target/benchmark/munchausenC; done

    # Go
    go build -ldflags="-s -w" src/test/resources/org/bau/benchmarksgame/piDigits.go
    for i in {1..3}; do time ./piDigits > out.txt; done
    go build -ldflags="-s -w" src/test/resources/org/bau/benchmarksgame/binaryTrees.go
    for i in {1..3}; do time ./binaryTrees 20; done
    go build -ldflags="-s -w" src/test/resources/org/bau/speedtest/munchausen.go
    for i in {1..3}; do time ./munchausen; done
    go build -ldflags="-s -w" src/test/resources/org/bau/benchmarksgame/fannkuch.go
    for i in {1..3}; do time ./fannkuch 11; done

    # Java
    javac src/test/java/org/bau/perf/benchmarksgame/Loop.java -d target/benchmark    
    javac src/test/java/org/bau/perf/benchmarksgame/BinaryTrees.java -d target/benchmark
    javac src/test/java/org/bau/perf/benchmarksgame/FannkuchRedux.java -d target/benchmark
    javac src/test/java/org/bau/perf/speedtest/Munchausen.java -d target/benchmark
    javac src/test/java/org/bau/perf/benchmarksgame/PiDigits.java -d target/benchmark
    java -cp target/benchmark -mx128m org.bau.perf.benchmarksgame.Loop org.bau.perf.benchmarksgame.BinaryTrees 20
    java -cp target/benchmark -mx128m org.bau.perf.benchmarksgame.Loop org.bau.perf.benchmarksgame.FannkuchRedux 11
    java -cp target/benchmark -mx128m org.bau.perf.benchmarksgame.Loop org.bau.perf.speedtest.Munchausen
    java -cp target/benchmark -mx128m org.bau.perf.benchmarksgame.Loop org.bau.perf.benchmarksgame.PiDigits | grep time
    for i in {1..3}; do time java -mx128m -cp target/benchmark org.bau.perf.benchmarksgame.BinaryTrees 20; done
    for i in {1..3}; do time java -mx128m -cp target/benchmark org.bau.perf.benchmarksgame.FannkuchRedux 11; done
    for i in {1..3}; do time java -mx128m -cp target/benchmark org.bau.perf.speedtest.Munchausen; done
    for i in {1..3}; do time java -mx128m -cp target/benchmark org.bau.perf.benchmarksgame.PiDigits; done
    
    # Python
    for i in {1..3}; do time python src/test/resources/org/bau/benchmarksgame/binaryTrees.py 20; done
    for i in {1..3}; do time python src/test/resources/org/bau/benchmarksgame/fannkuch.py 11; done
    for i in {1..3}; do time python src/test/resources/org/bau/benchmarksgame/piDigits.py 10000 > out.txt; done
    for i in {1..3}; do time python src/test/resources/org/bau/speedtest/munchausen.py; done
    
    # Rust
    mkdir -p target/benchmark/rust
    cp -R src/test/resources/org/bau/benchmarksgame/rust target/benchmark/rust
    cd target/benchmark/rust
    cargo build --release
    for i in {1..3}; do time target/release/piDigits > out.txt; done
    cd ../../..
    cp src/test/resources/org/bau/speedtest/*.rs target/benchmark
    rustc -C opt-level=3 -C lto=yes -C codegen-units=1 -C strip=symbols target/benchmark/munchausen.rs 
    for i in {1..3}; do time ./munchausen; done
    cp src/test/resources/org/bau/benchmarksgame/*.rs target/benchmark
    rustc -C opt-level=3 -C lto=yes -C codegen-units=1 -C strip=symbols target/benchmark/fannkuch.rs 
    for i in {1..3}; do time ./fannkuch 11; done
    rustc -C opt-level=3 -C lto=yes -C codegen-units=1 -C strip=symbols target/benchmark/mandelbrot.rs 
    for i in {1..3}; do time ./mandelbrot > test.tiff; done
    cp src/test/resources/org/bau/benchmarksgame/*.rs target/benchmark
    rustc -C opt-level=3 -C lto=yes -C codegen-units=1 -C strip=symbols target/benchmark/binaryTrees.rs 
    for i in {1..3}; do time ./binaryTrees 20; done
    
