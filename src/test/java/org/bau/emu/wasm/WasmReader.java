package org.bau.emu.wasm;

import java.io.IOException;
import java.io.RandomAccessFile;

/*

https://notes.kodekloud.com/docs/Exploring-WebAssembly-WASM/Getting-Started-with-WebAssembly/Understanding-the-WebAssembly-Binary-Format/page

00 61 73 6D   ;; Magic number: "\0asm"
01 00 00 00   ;; Version: 1

# Section Layout
Each section has:
1. A one-byte section ID
2. A ULEB128-encoded section length
3. A payload (raw bytes)

https://en.wikipedia.org/wiki/LEB128#Unsigned_LEB128

Section ID  Section Purpose
0   Custom  Custom metadata, debug information
1   Type    Function signatures
2   Import  Imports (functions, memory, tables, globals)
3   Function    Function declarations (type indices)
4   Table   Tables of function references (for call_indirect)
5   Memory  Linear memory definitions (pages and limits)
6   Global  Module-level global variables
7   Export  Exports (functions, memory, tables, globals)
8   Start   Optional start function index
9   Element Table initialization entries
10  Code    Function bodies (locals and opcodes)
11  Data    Data segment initializers

1. Type Section (ID 1)
01           ;; Section ID: Type
0F           ;; Section length: 15 bytes
02           ;; Number of types: 2
60 01 7F     ;; Type 0: (i32) -> ()
60 02 7F     ;; Type 1: (i32, i32) -> ()

2. Import Section (ID 2)
02           ;; Section ID: Import
11           ;; Section length: 17 bytes
01           ;; Imports count: 1
03 65 6E 76  ;; Module name: "env" (length=3 + "env")
08 70 72 69 6E 74 5F 73 74 72  ;; Field name: "print_str" (length=8 + text)
00           ;; Import kind: Function
00           ;; Type index: 0
Module and field names are length-prefixed UTF-8 strings.
Import kind 0x00 refers to a function.

3. Function Section (ID 3)
03           ;; Section ID: Function
05           ;; Section length: 5 bytes
04           ;; Number of functions: 4
00 01 10 02  ;; Type indices for each function
Each byte is a ULEB128-encoded index pointing into the Type Section.

5. Memory Section (ID 5)
Defines the module’s linear memory (in 64 KiB pages).
05           ;; Section ID: Memory
03           ;; Section length: 3 bytes
01           ;; Number of memory blocks: 1
00 11        ;; Flags=0 (initial only), initial=17 pages (0x11)
Flags=0 indicates only an initial limit.
Limits are ULEB128-encoded page counts (1 page = 64 KiB).

6. Global Section (ID 6)
06           ;; Section ID: Global
19           ;; Section length: 25 bytes
03           ;; Number of globals: 3
7F 01        ;; Global entry: i32, mutable
41 0B 0B     ;; Init expr: i32.const 11; end
…            ;; Additional globals…

7. Export Section (ID 7)
07           ;; Section ID: Export
0C           ;; Section length: 12 bytes
01           ;; Number of exports: 1
04 6D 61 69 6E  ;; Name: "main" (length=4)
00              ;; Export kind: Function
03              ;; Function index: 3
Export kind codes: 0x00=Function, 0x02=Memory, etc.

Additional Sections
Start (ID 8): Designates an entrypoint function.
Element (ID 9): Table initialization data.
Code (ID 10): Function bodies (local variables + opcodes).
Data (ID 11): Memory data segments.
Custom (ID 0): Arbitrary metadata and debug info.

 */
public class WasmReader {

    byte[] data;
    int pos;

    public static void main(String... args) throws IOException {
        RandomAccessFile f = new RandomAccessFile("hello-wasm.wasm", "r");
        byte[] data = new byte[(int) f.length()];
        f.readFully(data);
        f.close();
        WasmReader r = new WasmReader();
        r.data = data;
        r.pos = 8;
        r.printSections();
    }

    private void printSections() {
        while (pos < data.length) {
            int sectionId = data[pos++];
            int length = readULEB128();
            System.out.println("sectionId " + sectionId + " len " + length);
            pos += length;
        }
    }

    private int readULEB128() {
        int result = 0;
        int shift = 0;
        int b;
        do {
            b = data[pos++] & 0xff;
            result |= (b & 0x7f) << shift;
            shift += 7;
        } while ((b & 0x80) != 0);
        return result;
    }

//  do {
//  byte = value & 0x7f; /* low-order 7 bits of value */
//  value >>= 7;
//  if (value != 0) /* more bytes to come */
//    byte |= 0x80; /* set high-order bit of byte */
//  emit(byte);
//} while (value != 0);

}
