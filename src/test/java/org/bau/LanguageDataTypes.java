package org.bau;

/*


Character literal

Bau        i8('a')     byte (compile time error if more than one byte)
           i16('a')
           int('a')    unicode code point
Java       'a'         int
Rust       'a'         Character 
           b'H'        Byte
C++        'c'         char
C          'c'         int (c-char)
           u8'c'
Swift      let ch: Character = "a"
           let chr = UInt8(ascii:"A")
           'x' + 'y' == "xy", which was not intended
           let myChar = 'f'
Javascript -- only string --
Python     -- only string --
           ord('A') / chr(65)



# Data Types

Lua: Number. SQLite: Integer, Real

Rust Java     C       Python C#      Swift      Go          SQLite  Fortran
bool boolean  bool    bool   bool    Bool       bool                integer
i8   byte     char           sbyte   Int8       int8
i16  short    short          short   Int16      int16
i32  int      int     int    int     Int32/Int  int32/int
i64  long     long           long    Int64/Int  int64/int

u8   -                       byte               uint8/byte
u16  char                    ushort             uint16
u32  -                       uint         UInt  uint32/uint
u64  -                       ulong              uint64/uint

f32  float    float   float  float   Float      float32
f64  double   double  float  double  Double     float64      real


# Boolean Data Type

Advantage: require boolean in conditions... does it have less typos?
it's also a disadvantage: javascript doesn't have it.



# Data Types

i8, i16, i32, i64, f32, f64, ref; arrays of something

- no "boolean". instead, use "1" for true and "0" for false.
  (those could be constants)

- no distinction between "signed" and "unsigned", to reduce complexity.
  (e.g. LLVM doesn't have a distinction any more)
  Instead, have two "shift right", "div", "modulo" operations, 
  cast with/without sign extend

https://doc.rust-lang.org/book/ch03-02-data-types.html
Number literals can also use _ as a visual separator to make the number easier to read, such as 1_000,
Note that number literals that can be multiple numeric types allow a type suffix, such as 57u8, to designate the type. 
integer types default to i32

Values and Types
Lua: nil, boolean, number, string, function, userdata, thread, and table


# Classes

type Address
  id int
  name string
  
fun Address
  hash() int
    return hash(id)
  compare(o Address) int
    return compare(o.id, this.id)
    
fun Address hash() int

fun
  compare(a int, b int)
    if a < b
      return -1
    elif a > b
      return 1
    else
      return 0
  hash(x int)
    x ^= x >> 16
    x *= 23454
    x ^= x >> 16
    x *= 23454
    x ^= x >> 16
    return x

type Address
  id int
  
fun buildAddress(id int) Address
  Address a := Address
  a.id = id
  return a

fun Address.close()
  print('close ' id)

fun Address.print()
  print('#' id)
  
- unusual syntax (a bit like Go?)
- still repeat the "Address"

--------------------

type Address
  id int
  
buildAddress(id int) Address
  Address a := Address
  a.id = id
  return a

.close()
  print('close ' id)

.print()
  print('#' id)

- unusual syntax
- quite easy to read and understand?
- requires definition of a "Module"
- doesn't strictly requires that methods are in same file... but isn't that what we want?
- could add more methods in new file, using Address.additional()

Go

package vin_test
import(
  "vin-stages/2"
  "testing"
)
const (
  validVIN   = vin.VIN("W0L000051T2123456")
  invalidVIN = vin.VIN("W0")
)
type VIN string
func NewVIN(code string)(VIN, error) {
  if len(code) != 17 {
    return "", fmt.Errorf("invalid VIN %s: more or less than 17 characters", code)
  }
  // ... check for disallowed characters ...
  return VIN(code), nil
}


--------------------

type Address
  id int
  
buildAddress(id int) Address
  Address a := Address
  a.id = id
  return a

close(Address this)
  print('close ' this.id)

print(Address this)
  print('#' this.id)

- repeat the 'Address this' many times
- harder to see if it's member or not?

- for someone new it is a bit hard to understand?
- more 'flat' (functions and types on the same level)
  this might be a disadvantage or an advantage
- allow to add new methods later on

-------------------

type Address
  id int
  
  static build(id int) Address

  close()
    print('close ' id)

  print()
    print('#' id)


Address a := Address.build(1)
a.print()

- shorter
- we don't need 'fun' and 'type', technically...
- closely matches Java
- problem: constructor need special syntax, or needs to be after other functions... one can get used to it
- problem: to add new methods later on, we need special syntax

Address
  id int
  close()
    print('closing')
  print()
    print('hello')
    
build() Address
  return Address


-------------------
  
fun buildAddress(id int) Address
  Address a := Address
  a.id = id
  return a

fun Address close()
  print('close ' a.id)

fun Address print()
  print('#' a.id)

fun Address setName(name string) Address throw
  this.name = name;
  return this





 */
public class LanguageDataTypes {

}
