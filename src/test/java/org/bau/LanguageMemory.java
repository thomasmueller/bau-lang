package org.bau;

/*

Idea: refcount by default (slow & flexible); preventing that and use (mutable) borrow as option.
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
