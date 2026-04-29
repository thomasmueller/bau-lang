package org.bau.emu.wasm;

import java.io.FileOutputStream;
import java.io.IOException;

public class WasmBuilder {

    public static void main(String... args) throws IOException {
        FileOutputStream out = new FileOutputStream("test.wasm");
        out.close();

/*

https://github.com/sunfishcode/wasm-reference-manual/blob/master/WebAssembly.md

if (x)
20 00   local.get 0
04 7f   if i32
0b  end

while (x)
02 7f   block
03 7f   loop
20 00   local.get 0
45      i32.eqz
0d 01   br_if 1
0c 00   br 0
0b      end
0b      end

*/


    }
}
