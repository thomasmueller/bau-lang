# Transpile

Download and build the latest version:

    git clone git@github.com:thomasmueller/bau-lang.git
    cd bau-lang

Using Maven:

    mvn -DskipTests clean install

Alternatively, using the Make:

    make jar

Create a `demo.bau` file, transpile it, compile, and run:

    echo "println('Hello World')" > hello.bau
    java -jar target/*.jar hello.bau
    gcc -O3 hello.c -o hello
    ./hello

## C Compiler and Toolchain

The generated source code is relatively simple and readable C code.
The following features are needed:

* `goto` is used for `continue` and exception handling
* `varargs` are used for functions with a variable number of arguments.
* Functions that may throw an exception return a struct.
* The standard library is used by default.

The regular C tools may be used. As an exception, to analyze performance
on Mac OS, use the following command:

    xctrace record --template 'Time Profiler' --launch a.out
