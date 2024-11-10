package org.bau;

/*

https://www.reddit.com/r/ProgrammingLanguages/comments/1gkn7aw/what_else_is_there_besides_borrow_checking_and_gc/

https://verdagon.dev/grimoire/grimoire

https://play.rust-lang.org/?version=stable&mode=debug&edition=2021

- do not increment (and so, also do not decrement) on local variables
- keep references from local variables on a special stack, or use stack walking
- at some point, apply increment for local variables...
  ... but then we also need to decrement later! how?

if a type or object never has a reference, then we don't need ref counting.

"hard" problems:
- hash table
- get a ref
- while holding the ref, delete the entry

------------
use std::collections::HashMap;
fn main() {
    let mut map = HashMap::new();
    map.insert(String::from("key1"), String::from("value1"));
    if let Some(value) = map.get_mut("key1") {
        *value += "x";
        println!("Updated value: {}", value);
    } else {
        println!("Key not found");
    }
    let val = map.get("key1").unwrap();
    println!("val: {}", val);
    map.remove("key1");
    let val2 = map.get("key1").unwrap(); // panic
    println!("val: {}", val2);
}
------------
struct SimpleHashMap {
    key: Option<String>,
    value: Option<String>,
}
impl SimpleHashMap {
    fn new() -> Self {
        SimpleHashMap {
            key: None,
            value: None,
        }
    }
    fn put(&mut self, key: String, value: String) {
        self.key = Some(key);
        self.value = Some(value);
    }
    fn get(&self, key: &str) -> Option<&String> {
        if let Some(ref k) = self.key {
            if k == key {
                return self.value.as_ref();
            }
        }
        None
    }
}
fn main() {
    let mut map = SimpleHashMap::new();
    map.put(String::from("key1"), String::from("value1"));
    if let Some(value) = map.get("key1") {
        println!("Value: {}", value);
    } else {
        println!("Key not found");
    }
}


Data Structure Aware Garbage Collector
you can "hint" that an object can be reclaimed
(speedup for tracing GC)

to avoid the memory and ref-counting overhead,
declare max number of (non-stack) references?
- 1 means single ownership (as in Rust) and means we don't need a counter
- many means ref counted

Paper: A Unified Theory of Garbage Collection
Data Structure Aware Garbage Collector
https://eschew.wordpress.com/2016/09/02/summarizing-gc/

Idea: refcount by default (slow & flexible)
- but option to prevent it, and use (mutable) borrow as option.
Rust:
fn immutable(x: &i64) {
    println!("x: {}", x);
}
fn mutable(x: &mut i64) {
    *x += 1;
    println!("x: {}", x);
}
fn main() {
    let mut value = 42;
    let reference = &mut value; // Mutable borrow
    *reference += 1; // Modifying the value through the mutable reference
    println!("Modified value: {}", reference);
    println!("Modified value: {}", value);
    immutable(&value);
    mutable(&mut value);
}
Rust has 3 options:
A take ownership      - and if it's not returned, and not assigned to something,
                        then memory is freed
B mutable reference   - can change it, but can not assign it to something,
                        and no need to return (like inout parameter)
C immutable reference - can only read...
                        this is so that multiple parameters can use the same reference
                        (allows aliasing; simplifies concurrency)



(only) call close when the ref count falls to 0,
and then check again after calling close because it might have been re-added
(swift panics)
prevent calling close() explicitly (Rust and Swift won't allow it)
If a struct variable has been partially initialized,
only its initialized fields are dropped

Swift Fiddle
https://swiftfiddle.com/?c=H4sIAAAAAAAAA1WQQQuDMAyF%2F0rISWGIO%2ByijDF223XXXjrNXEesYjsPE%2F%2F7WmtlC4XCl%2FdeSCassMCKpTFweRvbtTTAJDS4YrKgZUsF3OygdBOo0somvziNel%2BG%2BJH5LhwXb%2BjM4avJm2GCqO%2Bd3yYChVgSU1BmFSnJ6kO1wLSM4oa7u2SX62eUW7B7oxzWbrEtcQr41T31H%2FTAZUSybiLw6vm575kMLXM3qVYsNO5wdIc6ZPs8y3H%2BAvodtpU3AQAA
https://mikeash.com/pyblog/friday-qa-2017-09-22-swift-4-weak-references.html

Object 0x56202034d060 of class Customer deallocated with non-zero retain count 2.
This object's deinit, or something called from it,
may have created a strong reference to self which outlived deinit,
resulting in a dangling reference.

class Customer {
    let name: String
    init(name: String) {
        self.name = name
    }
    deinit {
      print("\(name) is deinitialized");
      global = self;
    }
}
var global: Customer?
var john: Customer?
john = Customer(name: "John Appleseed")
john = nil



 */
public class LanguageMemory {

}
