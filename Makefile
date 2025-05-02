# This makefile allows to build and use the transpiler
# without having to install Maven etc.

jar:
	mkdir -p target/build
	javac -d target/build -sourcepath src/main/java src/main/java/org/bau/tools/Transpile.java
	cp -r src/main/resources/* target/build/
	echo "Main-Class: org.bau.tools.Transpile" > target/manifest.txt
	jar cfm target/bau.jar target/manifest.txt -C target/build .

transpile:
	java -jar target/bau.jar demo.bau

compile:
	gcc -O3 demo.c -o demo

run:
	./demo

clean:
	rm -rf target/build
