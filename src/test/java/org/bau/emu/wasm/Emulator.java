package org.bau.emu.wasm;

public class Emulator {
/*

https://notes.kodekloud.com/docs/Exploring-WebAssembly-WASM/Compiling-to-WebAssembly/Demo-Compiling-from-WAT-to-WASM/page
brew install wabt
brew install wasmtime

(module
    ;; Import the required fd_write WASI function which will write the given io vectors to stdout
    ;; The function signature for fd_write is:
    ;; (File Descriptor, *iovs, iovs_len, *nwritten) -> Returns 0 on success, nonzero on error
    (import "wasi_snapshot_preview1" "fd_write" (func $fd_write (param i32 i32 i32 i32) (result i32)))
    (import "wasi_snapshot_preview1" "fd_read"  (func $fd_read  (param i32 i32 i32 i32) (result i32)))
    (memory 1)
    (export "memory" (memory 0))
    ;; Write 'hello world\n' to memory at an offset of 8 bytes
    ;; Note the trailing newline which is required for the text to appear
        (data (i32.const 8) "hello world\n")
    (func $main (export "_start")
        ;; Creating a new io vector within linear memory
        (i32.store (i32.const 0) (i32.const 8))  ;; iov.iov_base - This is a pointer to the start of the 'hello world\n' string
        (i32.store (i32.const 4) (i32.const 12))  ;; iov.iov_len - The length of the 'hello world\n' string
        (call $fd_read
            (i32.const 0) ;; file_descriptor - 1 for stdout
            (i32.const 0) ;; *iovs - The pointer to the iov array, which is stored at memory location 0
            (i32.const 1) ;; iovs_len - We're printing 1 string stored in an iov - so one.
            (i32.const 20) ;; nwritten - A place in memory to store the number of bytes written
        )
        drop ;; Discard the number of bytes read from the top of the stack
        (call $fd_write
            (i32.const 1) ;; file_descriptor - 1 for stdout
            (i32.const 0) ;; *iovs - The pointer to the iov array, which is stored at memory location 0
            (i32.const 1) ;; iovs_len - We're printing 1 string stored in an iov - so one.
            (i32.const 20) ;; nwritten - A place in memory to store the number of bytes written
        )
        drop ;; Discard the number of bytes written from the top of the stack
    )
)

(module
    ;; Import the required fd_write WASI function which will write the given io vectors to stdout
    ;; The function signature for fd_write is:
    ;; (File Descriptor, *iovs, iovs_len, *nwritten) -> Returns 0 on success, nonzero on error
    (import "wasi_snapshot_preview1" "fd_write" (func $fd_write (param i32 i32 i32 i32) (result i32)))
    (import "wasi_snapshot_preview1" "fd_read"  (func $fd_read  (param i32 i32 i32 i32) (result i32)))
    (memory 1)
    (export "memory" (memory 0))
    (data (i32.const 0) "\08\00\00\00") ;;  position
    (data (i32.const 4) "\0c\00\00\00") ;;  length, as 4 bytes
    (data (i32.const 8) "hello world\n") ;; data
    (func $main (export "_start")
        (call $fd_write
            (i32.const 1) ;; file_descriptor - 1 for stdout
            (i32.const 0) ;; *iovs - The pointer to the iov array, which is stored at memory location 0
            (i32.const 1) ;; iovs_len - We're printing 1 string stored in an iov - so one.
            (i32.const 20) ;; nwritten - A place in memory to store the number of bytes written
        )
        drop ;; Discard the number of bytes written from the top of the stack
    )
)

(module
  ;; import the browser console object, you'll need to pass this in from JavaScript
  (import "console" "log" (func $log (param i32)))
  (func
    i32.const 0 ;; change to positive number (true) if you want to run the if block
    (if
      (then
        i32.const 1
        call $log ;; should log '1'
      )
      (else
        i32.const 0
        call $log ;; should log '0'
      )
    )
  )
  (start 1) ;; run the first function automatically
)

(module
    (import "wasi_snapshot_preview1" "fd_write" (func $fd_write (param i32 i32 i32 i32) (result i32)))
    (import "wasi_snapshot_preview1" "fd_read"  (func $fd_read  (param i32 i32 i32 i32) (result i32)))
    (memory 1)
    (export "memory" (memory 0))
    (data (i32.const 0) "\08\00\00\00") ;;  position
    (data (i32.const 4) "\0c\00\00\00") ;;  length, as 4 bytes
    (data (i32.const 8) "hello world\n") ;; data
    (func $main (export "_start")
        (local $i i32)        ;; declare local variable i
        i32.const 0           ;; i = 0
        local.set $i
        (block $exit          ;; outer block for breaking out
            (loop $loop       ;; loop label
                local.get $i
                i32.const 10
                i32.ge_s
                br_if $exit  ;; if (i >= 10) break
                (call $fd_write
                    (i32.const 1)
                    (i32.const 0)
                    (i32.const 1)
                    (i32.const 20)
                )
                drop
                local.get $i
                i32.const 1
                i32.add
                local.set $i   ;; i++
                br $loop       ;; repeat loop
            )
        )
    )
)

wat2wasm hello-wasm.wat -o hello-wasm.wasm
wasmtime hello-wasm.wasm



(import "wasi_snapshot_preview1" "fd_read"
  (func $fd_read (param i32 i32 i32 i32) (result i32)))
(import "wasi_snapshot_preview1" "fd_write"
  (func $fd_write (param i32 i32 i32 i32) (result i32)))
(memory 1)
(export "memory" (memory 0))
;; iovec at 0:
(data (i32.const 0) "\10\00\00\00") ;; buf = 16
(data (i32.const 4) "\01\00\00\00") ;; len = 1
;; 1-byte buffer at address 16
(data (i32.const 16) "\00")

(func $getchar (result i32)
  ;; call fd_read(stdin=0, iov=0, iovcnt=1, nread=24)
  (call $fd_read
    (i32.const 0) ;; stdin
    (i32.const 0) ;; iovec array
    (i32.const 1) ;; 1 entry
    (i32.const 24) ;; where to store bytes read
  )
  drop
  ;; load the byte from buffer[16]
  (i32.load8_u (i32.const 16))
)
(func $putchar (param $c i32)
  ;; store byte into buffer
  (i32.store8 (i32.const 16) (local.get $c))
  ;; call fd_write(stdout=1, ...)
  (call $fd_write
    (i32.const 1) ;; stdout
    (i32.const 0) ;; iovec
    (i32.const 1)
    (i32.const 28) ;; bytes written
  )
  drop
)

  (local.set $c (call $getchar))
  (call $putchar (local.get $c))

02 block
03 loop
04 if
05 else
0b end
0c br
0f return
10 call
1a drop
20 local.get
21 local.set
2a i32.load
36 i32.store
46 i32.eq
47 i32.ne
41 i32.const
48 i32.lt_s
6a i32.add
6b i32.sub
71 i32.and
72 i32.or
74 i32.shl
75 i32.shr_s

if(a>0) {
} else {
}

local.get a
local.get b
i32.le_s
if
  ...
else
  ...
end

while (cond) { body }
block
  loop
    local.get a
    local.get b
    i32.le_s
    if
      br 1
    end
    ... body ...
  end
end

x = y + 1;
local.get y
i32.const 1
i32.add
local.set x

not needed:

01 nop
0d br_if ? branch_if
23 global.get ?
24 global.set ?
4c i32.le_s
76 i32.shr_u
73 i32.xor

 */
}
