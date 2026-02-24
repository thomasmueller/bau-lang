# Why

## Vision

Our vision for this new language is
to blend the best of today’s languages,
and combine it into a single, simple package.
We want it to be as easy and concise as Python,
yet as powerful and fast as a C-like systems language.
In practice, this means using Python-style clean syntax
that programmers can pick up quickly,
while compiling to efficient native code
and employing ownership/borrowing like Rust. 
The result is code that reads clearly,
but still runs at C / Rust speeds,
with no boilerplate slowing you down.

## Safety and Robustness

Safety is built in from the ground up.
Our design enforces memory safety at compile time.
Static analysis prevents null-pointer
and out-of-bounds errors at compile time,
before the program ever runs.

The language provides compile-time null-safety (in the spirit of Kotlin)
and Rust-like ownership guarantees out of the box.
The result is a single, unified language that is easy
and expressive to write (think Pythonic brevity),
yet fast, portable, and robust in practice.
Developers can write concise, clear code
and have confidence it will run reliably everywhere,
without the usual performance or safety trade-offs.

We draw inspiration from Python’s emphasis on readability,
Rust’s approach to memory safety and zero-cost abstractions,
and modern features like compile-time null safety,
aiming to unite them into one language.

## Portability and Predictability

The language does not require a runtime,
a just-in-time compiler that is slow to start up,
or a garbage collector.
We use automatic reference counting by default 
(eliminating unpredictable GC pauses).
The generated C code does not require special
libraries, and can be run easily
and on any platform.
