# Ena

## Example

    fun fact(x int) int
        if x <= 1
            ret 1
        ret x * fact(x - 1)

    for i, 0, 20
        println(fact(i))

## Keywords

Control flow
* `if` `elif` `else` `loop` `exit` `ret`

Assignment
* `:` (re-) assignment

Comparison and other operations
* `=` `<` `>` `<=` `>=` `<>`
* `and` `or` `+` `-` `*` `/` `%`
* `&` `|` `^` `<<` `>>` bitwise

Data types and miscellaneous
* `int` `real` `text`
* `#` comment
* `fun` `type`
* `()` `[]` `,`

### Constants, Variables

Identifiers must start with a letter or `_`,
and may contain letters, digits, and `_`.
`:` assigns a (new) value:

    PI: 3.14159  # assign a real
    a: 10        # assign an integer
    a: a + 1     # increment
    b: 'Hello'   # assign text

Variables need to be defined before they are used.
Variables at the top level (without indentation) are global.

### Built-In Types

The built-in types are

* `int`      (signed integer, 64 bit)
* `real`     (floating point, 64 bit)
* `text`     (text)
* `fun`      (function)
* `<type>[]` (array)

### Conditions

`if` starts a condition.
Spaces group statements into blocks.
`elif` (else if) and `else` are optional.

    if a = 0
        println('zero')
    elif a = 1
        println('one')
    else
        println('many')

### Loops

Loops use `loop`.

    # loop from 0 to 9
    i: 0
    loop i < 10
        println(i)
        i: i + 1

A loop without condition is endless,
unless `exit` exits the loop.

    # loop from 0 to 9
    i: 0
    loop
        if i >= 10
            exit
        println(i)
        i: i + 1


### Comments

`#` starts a line comment.
Line comments can start anywhere.

    # Line comment

### Literals

Numbers start with a digit.
Real literals contain `.`:
`3.0` is real; `3` is int.

Text literals start and end with `'`.
`''` is a single `'`.
UTF-8 is used.

    a: 1000
    b: 3.1415
    c: 3.3e-10
    d: 'Text literal'
    d: 'Joe''s coffee'

### Operators

`=` `<` `>` `<=` `>=` `<>` compare two values and return `1` or `0`.
`and` `or` combine comparisons.
Integer `+` `-` `*` wrap around on overflow/underflow.
Integer `/` by 0 returns max, min, or 0 
for positive, negative, and zero dividends, respectively.
`%` by 0 returns 0.  `%` is only defined for integers.
`&` `|` `^` `<<` `>>` are bitwise and, or, xor, 
shift left, and logical shift right: the leftmost bits become `0`.
They are only allowed for integers.
Real `/` follows IEEE-754 conventions.

### Functions

A program starts with a `main` function.
If `main` is not defined, top-level statements 
(outside any function) are executed instead.

`fun` starts a function. It may `ret` a value.

    fun square(x int) int
        ret x * x

    fun sum(x int[]) int
        sum: 0
        i: 0
        loop i < x.len
            sum: sum + x[i]
            i: i + 1
        ret sum

    a: int[3]
    a[0]: 1
    a[1]: 2
    a[2]: 3
    println('sum: ', sum(a))
    i: 0
    loop i < 5
        println(square(i))
        i: i + 1

### Built-In Functions

The following built-in functions are supported:

*  `println` prints arguments concatenated (numbers and text), followed by a newline. 
   It accepts multiple arguments: `println('a', 1, 'b')`.
*  `puts(out text)` prints its single text argument followed by a newline.
*  `format(args text[])` concatenates its argument 
   and returns the generated text value.

### Function Pointers

Function pointers are variables that point to a function.
Only functions of the shape `f(int, int) int` are supported; 
all other data needs to be passed via global variables.

    fun max(a int, b int) int
        if a > b
            ret a
        ret b

    fun apply(a int, b int, f fun)
        println(f(a, b))

    apply(10, 20, max)

### Array Access

To create a new array and then access it, use:

    a: int[10]   # new integer array with length 10
    a[0]: 10     # update array element 0
    b: real[20]  # new floating point array with length 20
    c: text[30]  # new text array with length 30
    d: c.len     # get the length

Bounds are checked at runtime.
The type is checked at compile time.
The maximum array length is 65536 entries.

### Types

Types are defined as follows:

    type point(x int, y int)

A constructor is automatically created.
You can then use the type as follows:

    p: point(10, 20)
    p.x: 30
    println(p.x)

Types can refer to other types, including to themselves.
Use the literal `0` to mean missing:

    type tree(left tree, right tree)

    a: tree(0, 0)
    b: tree(0, 0)     
    root: tree(a, b)

### Preprocessor

The preprocessor allows defining new syntax via macros. `def` introduces a macro.

Replacement text supports these features:

* `@{id}`         - unique id within a single macro expansion
* `@{left}`       - left-hand side placeholder
* `@{body}`       - inject the block following the macro invocation line
* `@{rest}`       - inserts all tokens to the right of the macro invocation
* `@{param.text}` - parameter converted to a text literal with proper quoting
* `@@`            - escapes `@` inside templates (emits a single `@`)

Notes:
- Placeholders are only recognized outside text literals. Inside `'...'` nothing is substituted.
- Macro heads are detected only outside text literals.
- `@{left}` behavior:
  - At start of a statement: produces `name: ` (assignment form).
  - Immediately before `:`: produces `name:` (no extra space).

Example:

    def +:
        @{left}: (@{left}) + (@{rest})

Usage:

    x +: 10

Result:

    x: (x) + (10)

Example:

    def log
        if logEnabled
            println('Log: ', @{rest})

Usage:

    log 'Hello ', name

Result:

    if logEnabled
        println('Log: ', 'Hello ', name)

Example:

    def for var, from, until
        if 1
            var: from
            loop var < (until)
                @{body}
                var +: 1

Usage:

    for i, 0, 10
        println('hello ', i)
    println('done')
    
Result:

    if 1
        i: 0
        loop i < (10)
            println('hello ', i)
            i +: 1
    println('done')
        
Example:

    def assert condition
        if 0 = (condition)
            println(@{condition.text})
            _x@{id}: int[0]
            _x@{id}[1]: 1

Usage:

    i: -1
    assert i < 0
    println('done')
    
Result:

    i: -1
    if 0 = (i < 0)
        println('i < 0')
        _x0: int[0]
        _x0[1]: 1
    println('done')

Example:

    def when condition, trueExpr, falseExpr
        if condition
            @{left} trueExpr
        else
            @{left} falseExpr
    
    array: int[10]
    x: when array.len > 0, array[0], 0
        
Result:

    array: int[10]
    if array.len > 0
        x: array[0]
    else
        x: 0

Example:

    def max a, b
        _a@{id}: a
        _b@{id}: b
        if _a@{id} > _b@{id}
            @{left} _a@{id}
        else
            @{left} _b@{id}

Usage:

    a: 10
    b: 20
    c: max a, b

Result:
    
    a: 10
    b: 20
    _a0: a
    _b0: b
    if _a0 > _b0
        c: _a0
    else
        c: _b0

Example:

    def break condition
        if condition
            exit

Usage:

    i: 0
    loop
        break i > 10
        i +: 1

Result:
    
    i: 0
    loop
        if i > 10
            exit
        i +: 1


### Possible Panics at Runtime

These conditions cause the program to panic (abort execution) at runtime:

- Array index out of bounds (array bounds are checked at runtime)
- Out of memory
- Stack overflow

Note: Integer division/modulo by zero
returns the documented values and does not panic.
Floating point division/modulo by zero
does not panic either; it follows the IEEE rules.

### Timeouts

To avoid endless programs, the bytecode VMs enforce an operation limit:

- Stack-based VM (SB) and Register-based VM (RB) count executed instructions and panic with `timeout` if they exceed a limit.
- Default limit: 1,000,000 operations; checked approximately every 65,536 operations.
- The limit is configurable in host code:
  - Stack VM: `new StackVM().setMaxOps(maxOps).setCheckEvery(k)`
  - Register VM: `new RegVM().setMaxOps(maxOps).setCheckEvery(k)`
- The interpreter does not enforce an operation timeout by default.

### Memory Management

Objects are reference counted.
Circular references need to be explicitly set to `0` 
where needed to avoid memory leaks.

### Missing Features

* `not` is not supported. Use `0 = (...)` if really needed.
* For bitwise `not`, use `-1 ^ ...`
* Use `1` for `true` and `0` for `false` and `null`.
* Arrays of arrays are not supported. 
  But arrays of user-defined types are supported, 
  and user-defined types may contain arrays.

### BNF

    <program>       ::= { <statement> | <function_def> | <type_def> }
    <statement>     ::= <assignment>  | <if_stmt>     
                      | <loop_stmt>   | "exit" 
                      | <expr>        | <return_stmt>
                      | <comment>
    <assignment>    ::= <lvalue> ":" <expr>
    <lvalue>        ::= <identifier> { ("[" <expr> "]") | ("." <identifier>) }
    <if_stmt>       ::= "if" [ <expr> ] <block> { "elif" <expr> <block> } 
                      [ "else" <block> ]
    <loop_stmt>     ::= "loop" [ <expr> ] <block>
    <return_stmt>   ::= "ret" [ <expr> ]
    <block>         ::= INDENT { <statement> } DEDENT    
    <function_def>  ::= "fun" <identifier> "(" [<param_list>] ")" [ <type> ] <block>
    <param_list>    ::= <param> { "," <param> }
    <param>         ::= <identifier> <type>
    <type>          ::= <base_type> [ "[]" ]
    <base_type>     ::= "int" | "real" | "text" | "fun" | <identifier>    
    <type_def>      ::= "type" <identifier> "(" [<param_list>] ")"
    <expr>          ::= <and_expr> { "or" <and_expr> }
    <and_expr>      ::= <cmp_expr> { "and" <cmp_expr> }
    <cmp_expr>      ::= <add_expr> [ ("=" | "<>" | "<" | "<=" | ">" | ">=") <add_expr> ]
    <add_expr>      ::= <mul_expr> { ("+" | "-" | "|" | "^") <mul_expr> }
    <mul_expr>      ::= <unary_expr> { ("*" | "/" | "%" | "<<" | ">>" | "&") <unary_expr> }
    <unary_expr>    ::= "-" <unary_expr> | <postfix_expr>
    <postfix_expr>  ::= <primary> { <postfix_op> }
    <primary>       ::= <literal> | <identifier> | <paren_expr> | <new_array>
    <new_array>     ::= <base_type> "[" <expr> "]"
    <postfix_op>    ::= <func_call> | <array_access> | <member_access>
    <func_call>     ::= "(" [<arg_list>] ")"
    <array_access>  ::= "[" <expr> "]"
    <member_access> ::= "." <identifier>
    <arg_list>      ::= <expr> { "," <expr> }
    <paren_expr>    ::= "(" <expr> ")"
    <literal>       ::= [0-9]+ | <real> | <text>
    <real>          ::= [0-9]+ "." [0-9]+ [ "e" [-] [0-9]+ ]
    <text>          ::= "'" { <char> | "''" } "'"
    <char>          ::= any UTF-8 character except "'"
    <identifier>    ::= [_a-zA-Z] { [_a-zA-Z0-9] }
    <comment>       ::= "#" { any character except newline }

### Bytecode Formats

This language can be lowered to two bytecode formats, chosen by the backend:

- Stack-based bytecode: optimized for super-fast generation and simple execution (potentially slower at runtime).
- Register-based bytecode: optimized for fast execution (harder to generate).

Both formats are semantically equivalent.

#### Stack-based Bytecode

- Design goals
    - No registers; a single operand stack.
    - Instructions are 0/1-operand where possible to simplify generation.
    - Locals/globals addressed by small indices.

- Conventions
    - TOS means top-of-stack.
    - Most binary ops pop right then left, and push the result.

- Instructions (selected)

    ; Constants / literals
    PUSHI   imm            ; push integer literal
    PUSHR   imm            ; push real literal
    PUSHT   id             ; push text literal (const pool index)

    ; Variables (locals/globals chosen by compiler)
    LLOAD   idx            ; push local[idx]
    LSTORE  idx            ; pop,  local[idx]
    GLOAD   idx            ; push global[idx]
    GSTORE  idx            ; pop,  global[idx]

    ; Arrays / text / objects
    ALOAD                  ; pop idx, pop arr,  push arr[idx]
    ASTORE                 ; pop val, pop idx, pop arr,  arr[idx]=val
    NEWARR   type          ; pop len,  push new array[type](len)
    LEN                    ; pop x,  push length(x)   (array or text)
    NEWOBJ  type, arity    ; pop args (right-to-left),  push object map
    GETF    nameIdx        ; pop obj,  push obj[name]
    SETF    nameIdx        ; pop obj, pop val,  obj[name]=val

    ; Arithmetic / bitwise / comparison (integers unless real detected)
    ADD SUB MUL DIV MOD    ; pop b, pop a,  push a op b
    SHL SHR BAND BOR BXOR  ; shifts/bitwise
    AND OR                 ; boolean ops (1/0)
    CMPEQ CMPNE CMPLT CMPLE CMPGT CMPGE ; push 1/0
    NEG                    ; pop x,  push -x
    I2R I2S R2S            ; pop x,  push converted value (int→real/text, real→text)

    ; Control flow
    JMP      target        ; unconditional jump
    JMPIF    target        ; pop cond, if cond != 0 jump
    CALL     f, argc       ; pop args …,  push result (or nothing for void)
    RET                    ; ret with optional TOS (if present)

    ; Misc
    PRINT                  ; pop x,  print x (debug/builtin)

SB is trivial to emit from an AST: perform a postorder walk and emit PUSH for leaves, then the operator.

#### Register-based Bytecode

- Design goals
  - Minimize memory traffic and dispatch; keep values in registers.
  - Harder to allocate registers, but faster to execute.

- Register file
  - General purpose registers: R0..R255 (implementation currently uses a fixed budget per function)

- Instructions (selected)

    MOV     Rd, Ra         ; Rd = Ra (copy)
    LOADI   Rd, imm        ; Rd = integer literal
    LOADR   Rd, k          ; Rd = real from const pool index
    LOADS   Rd, k          ; Rd = text from const pool index
    ALUI    Rd, Ra, Rb, op ; integer ALU (ADD,SUB,MUL,DIV,MOD, BAND,BOR,XOR, SHL,SHR, EQ,NE,LT,LE,GT,GE, NEG)
    ALUR    Rd, Ra, Rb, op ; real ALU    (ADD,SUB,MUL,DIV,MOD,           EQ,NE,LT,LE,GT,GE, NEG)
    CONCAT  Rd, Ra, Rb     ; text concatenation
    LOAD    Rd, addr       ; Rd = Mem[addr]      (locals/globals resolved by compiler)
    STORE   addr, Rs       ; Mem[addr] = Rs
    ALOADI  Rd, Ra, Rb     ; Rd = intArray[Rb]
    ALOADR  Rd, Ra, Rb     ; Rd = realArray[Rb]
    ALOADT  Rd, Ra, Rb     ; Rd = textArray[Rb] (returns handle)
    ALOADS  Rd, Ra, Rb     ; Rd = structArray[Rb]
    ASTOREI Ra, Rb, Rc     ; intArray[Rb] = Rc
    ASTORER Ra, Rb, Rc     ; realArray[Rb] = Rc
    ASTORET Ra, Rb, Rc     ; textArray[Rb] = Rc/handle
    ASTORES Ra, Rb, Rc     ; structArray[Rb] = Rc (object)
    JMP     target         ; unconditional jump
    JMPIF   Ra, target     ; if Ra != 0 jump
    CALL    Rd, f, argN    ; Rd = f(Rargs)       (f is function index/register, argN = arg count)
    RET     Ra             ; return Ra (or omit Ra for void)
    NEWARR  Rd, type, Rlen ; Rd = new array[type] with length Rlen
    LEN     Rd, Ra         ; Rd = length(Ra)     (arrays or text)
    PRINT   Ra             ; builtin println/debug
    I2R     Rd, Ra         ; int→real
    I2S     Rd, Ra         ; int→text
    R2S     Rd, Ra         ; real→text
    NEWOBJ  Rd, type, n    ; construct object with n fields from R1..Rn
    GETF    Rd, Robj, k    ; Rd = obj[ textPool[k] ]
    SETF    Robj, k, Rv    ; obj[ textPool[k] ] = Rv

Notes:
- `+` uses CONCAT for text only; arrays cannot be concatenated.
- Short basic blocks benefit from keeping temporaries in registers and fusing ALU chains.
- Common lowering: translate SB to RB with a simple virtual-stack to register allocator, 
  then apply basic copy/constant folding.

### Cross-language VM and Tooling

To run Ena programs on a C implementation of the Register VM, we provide:

- A bytecode serializer in Java:
  - `RegBytecode.writeTo(OutputStream)` writes a portable binary format (magic `RBVM`, version 1), including text/real pools, types, and all functions and instructions.
  - `RegCompiler.writeToFile(RegBytecode, Path)` convenience helper.

- A Java CLI tool to compile Ena to RB bytecode:
  - Class: `org.bau.ena.RegCompileTool`
  - Usage:

    javac -d target/build -sourcepath src/test/java src/test/java/org/bau/ena/RegCompileTool.java
    java -cp target/build org.bau.ena.RegCompileTool program.ena program.rbvm

- A reference C VM that loads and executes the RB bytecode: `src/test/resources/org/bau/ena/regvm.c`.

Supported features in the C VM:
- Integer/real arithmetic and comparisons, F-prefixed real ops.
- Globals (`LOAD`/`STORE`), `MOV`, `JMP`/`JMPIF`, `CALL`/`RET`, `PRINT`, and `CONCAT` for text.
- Typed arrays for int and real: `NEWARR`, `ALOADI`/`ASTOREI`, `ALOADR`/`ASTORER`, `LEN`.

Build and run the C VM (macOS/Linux):

    # 1) Build Java and produce the compiler tool
    mvn -q -DskipTests=false test

    # 2) Compile your Ena program to RBVM bytecode
    java -cp target/classes org.bau.ena.tools.RegCompileTool src/test/resources/org/bau/ena/demo.ena out.rbvm

    # 3) Build the C VM
    gcc -O3 -std=c11 -DREGVM_MAIN -o regvm src/test/resources/org/bau/ena/regvm.c

    # 4) Run
    ./regvm out.rbvm

Notes and limitations:
- The on-disk opcode numbers mirror Java’s enum order. Changing the Java enum order breaks compatibility; freeze it for stable builds.
- The C VM currently skips user-defined structs/objects and text arrays; extend similarly to Java’s typed handles if needed.
- Strings are loaded from the Java text pool as raw UTF-8 and printed as-is in C.