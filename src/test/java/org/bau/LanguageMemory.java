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


I wonder if the following memory management strategy is already used in a programming language.
strategy is as follows:
memory is managed manually, using malloc and free.
however, pointer arithmetic beyond this is not allowed.
when allocating an object, a magic number is generated at random
and stored just before the object.
the reference to the object consists of the pointer to the object,
and the magic number that was generated.
when an object is freed, the object is put in a queue.
from time to time, for example each 10000 "free" calls,
or on low memory, the queue is processed,
in which case the memory of all objects in the queue are deallocated,
but first all magic numbers before the objects are cleared.
afterwards, for all references on the on the stack and in registers,
the magic number is compared against the one just before the object.
when accessing an object using a pointer in the stack or in a register,
there is no need to verify the magic number.
only when loading a reference from another object,
the magic number is verified.
is this technique already known?

=> canaries in stack frames to detect stack overflows.

assuming only standard C features are used, how
is it possible to retrieve the list of all references that
are on the stack?

-------------------

like reference counting, but instead of a count,
maintain a doubly-linked list of references.

-------------------

reference counting, but keep the count in the owning reference instead of the object
(secondary references point to the owning reference & the object)

-------------------

compile-time reference counting:
- types have a ref-count, e.g. 3
- when creating an object, there are 3 variables, each one has one ref
- can join a ref with another
- when trying to remove an object, need to remove all 3

a, b, c = new(Node3)
-> a, b, and c have type Node3_1
d = Node3.join(a, b)
-> d has type Node3_2; a and b are deleted
Node3.remove(d, c)
-> the node is removed

Static Reference Counting
What I mean with compile-time reference counting is the following:
when creating an object, the number of references is encoded in the type,
e.g. Node<3> means there are 3 references.
Then the Node<3> can be split into three references of type Node<1 of 3>.
To delete the object, three references to the same object are needed of type Node<1 of 3>.
It is also possible to join two references of type Node<1 of 3> to form a reference of type Node<2 of 3>.
That means, reference counting is done at compile time.

-------------------------

For a new programming language, I'm trying to find a high-performance memory management strategy
similar to Rust and C.
I am thinking about a hybrid:
Where performance is not very important, use reference counting.
Where performance is critical, use restricted references.
When creating an object, a restricted reference is created that
owns the object 100%. But the reference can be split into
two references that each own 50%.
The "100% owning" reference is a slightly different type than the "50% owning" reference:
100% = String:1, 50% = String:2.
If necessary, these can be further split, eg. 25% = String:4.
And references can be merged.
When the last reference (that owns 100%) is removed, the memory is freed.

Is this memory management strategy already known in the literature?

-------------------------

Memory Safety needs Approximate Reference Counting

Todays modern popular programming languages are memory-safe.
There are 3 main flavors currently, all have some advantages and disadvantages:

* Tracing GC (garbage collection): Java, C#, JavaScript.
  Advantages: fast, convenient.
  Disadvantages: Needs more memory. Pauses.
* Reference counting: Python, Swift.
  Advantages: convenient, needs little memory, no pauses.
  Disadvantage: slow.
* Borrow checking: Rust.
  Advantage: fast, needs little memory, no pauses.
  Disadvantages: hard to use (manual), no circular references.

I want to design a new language, but I'm not convinced
any of the above is a good strategy.
The perfect strategy would need less memory than tracing GC,
be more convenient than borrow checking,
and faster than reference counting.
But how to do this? To find out,
we need to find out what is really needed.
What seems to be required, and what all strategies have,
is some form of approximate reference counting:

* Tracing GC uses one bit (flag) per entry:
  no references, or at least one reference.
  Entries are periodically marked.

* Reference counting is more exact, but not fully:
  counters can overflow.
  Also, cycles need to be detected or prevented.

* Borrow checking is approximate reference counting as well,
  just at compile time.
  Here, the compiler keeps track of the approximate reference count,
  which consists of owner, at most one mutable borrow,
  and a number of shared (read-only) borrows.

There are more than the 3 described above.
Here are some more: https://verdagon.dev/grimoire/grimoire

What if we have some hybrid strategy:
mostly reference counting, but where possible at compile time.
Or put in another way, use borrow checking where possible,
but switch to reference counting if needed?

-------------------------

For a new programming language, I consider adding a tag to functions (maybe automatically) called "free".
This is for functions that, directly, or indirectly, free memory. How many functions
would likely have this tag, in percent?

Estimate: ~5-15% of functions.

-------------------------

https://play.rust-lang.org/?version=stable&mode=debug&edition=2024

use std::collections::HashMap;
fn main() {
    let mut map: HashMap<String, String> = HashMap::new();
    map.insert("name".to_string(), "Alice".to_string());
    println!("Before removal: {:?}", map);
    let v = map.get("name");
    println!("Get: {:?}", v);
    map.remove("name");
    println!("After removal: {:?}", map);
    println!("Get: {:?}", v);
}

fn main() {
    let string1 = String::from("abcd");
    let string2 = "xyz";
    let result = longest(string1.as_str(), string2);
    println!("The longest string is {result}");
}
fn longest<'a>(x: &'a str, y: &'a str) -> &'a str {
    if x.len() > y.len() { x } else { y }
}

annotations in languages:
[[nodiscard]]   : C++ : do not discard the return value please
constexpr       : C++ : evaluated at compile time
Pure            : Ada : free from side effects
comptime        : Zig : evaluated at compile-time
static          : D   : static assert(x == 5) // Compile-time assertion
{.compileTime.} : Nim :

-----------------------------

Fractional references / static reference counting / compile-time reference counting
https://docs.rs/static-rc/latest/static_rc/

 */
public class LanguageMemory {

}
