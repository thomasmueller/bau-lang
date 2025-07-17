# Playground

The <a href="https://thomasmueller.github.io/bau-lang/">Playground</a> allows to transpile and run Bau programs in the browser.

## Technical Implementation Details

The playground is a static web app. 
All processing (transpilation, compilation, running the generated program) is done in the browser.
It consists of:

* Source code editor: <a href="https://codemirror.net/">CodeMirror</a>, implemented in JavaScript. For mobile clients, a HTML "textarea" is used.
* Transpiler: <a href="https://teavm.org/">TeaVM</a>, ahead-of-time compiler for Java bytecode. JavaScript is used.
* C Compiler and runtime: <a href="https://github.com/tyfkda/xcc">XCC</a>, a very small C compiler. The compiler uses WASM.

