# Transpile

Download and build the latest version:

    git clone git@github.com:thomasmueller/bau-lang.git
    cd bau-lang
    mvn -DskipTests clean install

Create a `demo.bau` file, transpile it, compile, and run:

    echo "println('Hello World')" > hello.bau
    java -jar target/*.jar hello.bau
    gcc -O3 hello.c -o hello.bin
    ./hello.bin
