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
