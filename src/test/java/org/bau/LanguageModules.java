package org.bau;

/*

"module" keyword
- Rust: modules contain modules
- Rust: extern crate foo as the_other_foo;
- Rust: Only the public items of a module can be accessed from outside the module scope.
- Rust: "use deeply::nested::function as other_function"
- Rust: self::function() -- in the same module
- Rust: self::cool::function() -- in different module inside this one
- Rust: super::function() -- parent module
- Rust: Cargo.toml [dependencies] macos-perf = "0.1.1"
- Java: using absolute names
- Java: static import (but this has problems)
- Python: from fibo import fib, fib2; import fibo; from fibo import *; import fibo as fib; from fibo import fib as fibonacci
- Python: dir() does not list the names of built-in functions and variables.
- Python: from ..filters import equalizer
- Javascript: import * as name from "module-name"; import { export1 } from "module-name"; import { export1 as alias1 } from "module-name";
- Javascript: import myDefault, { foo, bar } from "/modules/my-module.js";
- C: include
- C++ include, namespace, use
- C++ modules https://learn.microsoft.com/en-us/cpp/cpp/modules-cpp?view=msvc-170
- C++ export module Example; import Example; import std.core; using namespace std;
- C# "using System.Text;"
- Go "package main" / "import ("fmt" "math/rand" ) "println(rand.Intn(10))"
- Go "import "github.com/google/uuid""
- Go "import ("math/rand" crand "crypto/rand")


 */
public class LanguageModules {

}
