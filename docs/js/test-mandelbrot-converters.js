/**
 * Comprehensive Test Suite for All Language Converters
 * Tests conversion of Mandelbrot benchmark code to Bau
 */

// Import all converters
let converters = {};
if (typeof module !== 'undefined' && module.exports) {
    // Node.js environment
    converters.java = require('./java-to-bau-converter.js');
    converters.c = require('./c-to-bau-converter.js');
    converters.go = require('./go-to-bau-converter.js');
    converters.kotlin = require('./kotlin-to-bau-converter.js');
    converters.python = require('./python-to-bau-converter.js');
    converters.rust = require('./rust-to-bau-converter.js');
    converters.swift = require('./swift-to-bau-converter.js');
}

// Expected Bau output (simplified version for testing key patterns)
const expectedBauOutput = `# The Computer Language Benchmarks Game
# https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

fun main()
    w, h, bitNum := 0
    iter: 50
    limit: 2
    byteAcc := 0
    n := 200
    if args.length > 0
        n = parseInt(args[0])
    w = h = n
    println('P4\\n' w ' ' h '\\n')
    for y := until(h)
        for x := until(w)
            Zr, Zi, Tr, Ti := 0.0
            Cr := 2.0 * x / w - 1.5
            Ci := 2.0 * y / h - 1
            for i := until(iter) and Tr + Ti <= limit * limit
                Zi = 2.0 * Zr * Zi + Ci
                Zr = Tr - Ti + Cr
                Tr = Zr * Zr
                Ti = Zi * Zi
            byteAcc <<= 1
            if Tr + Ti <= limit * limit
                byteAcc |= 0x01
            bitNum += 1
            if bitNum = 8
                print(byteAcc)
                byteAcc = 0
                bitNum = 0
            elif x = w - 1
                byteAcc <<= 8 - (w % 8)
                print(byteAcc)
                byteAcc = 0
                bitNum = 0`;

// Test cases for each language
const testCases = {
    java: {
        input: `// The Computer Language Benchmarks Game
// https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

package org.bau.benchmarks;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class Mandelbrot {

    public static void main(String[] args) throws IOException {
        int w, h, bitNum = 0;
        int iter = 50;
        double limit = 2;
        byte byteAcc = 0;
        int n = 200;
        if (args.length > 0)
            n = Integer.parseInt(args[0]);
        w = h = n;
        OutputStream out = new BufferedOutputStream(System.out);
        out.write(("P4\\n" + w + " " + h + "\\n").getBytes());
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double Zr = 0, Zi = 0, Tr = 0, Ti = 0;
                double Cr = 2.0 * x / w - 1.5;
                double Ci = 2.0 * y / h - 1;
                for (int i = 0; i < iter && Tr + Ti <= limit * limit; i++) {
                    Zi = 2.0 * Zr * Zi + Ci;
                    Zr = Tr - Ti + Cr;
                    Tr = Zr * Zr;
                    Ti = Zi * Zi;
                }
                byteAcc <<= 1;
                if (Tr + Ti <= limit * limit)
                    byteAcc |= 0x01;
                bitNum++;
                if (bitNum == 8) {
                    out.write(byteAcc);
                    byteAcc = 0;
                    bitNum = 0;
                } else if (x == w - 1) {
                    byteAcc <<= (8 - (w % 8));
                    out.write(byteAcc);
                    byteAcc = 0;
                    bitNum = 0;
                }
            }
        }
        out.flush();
    }
}`,
        patterns: [
            'fun main()',
            'w, h, bitNum := 0',
            'iter: 50',
            'for y := until(h)',
            'for x := until(w)',
            'Zr, Zi, Tr, Ti := 0.0',
            'byteAcc <<= 1',
            'bitNum += 1'
        ]
    },

    c: {
        input: `// The Computer Language Benchmarks Game
// https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

#include <stdio.h>
#include <stdlib.h>

int main(int argc, char **argv) {
    int w, h, bit_num = 0;
    int i, iter = 50;
    char byte_acc = 0;
    double x, y, limit = 2.0;
    double Zr, Zi, Cr, Ci, Tr, Ti;
    int n = 200;
    if (argc > 1)
        n = atoi(argv[1]);
    w = h = n;
    printf("P4\\n%d %d\\n", w, h);
    for (y = 0; y < h; y++) {
        for (x = 0; x < w; x++) {
            Zr = Zi = Tr = Ti = 0;
            Cr = 2.0 * x / w - 1.5;
            Ci = 2.0 * y / h - 1.0;
            for (i = 0; i < iter && Tr + Ti <= limit * limit; i++) {
                Zi = 2.0 * Zr * Zi + Ci;
                Zr = Tr - Ti + Cr;
                Tr = Zr * Zr;
                Ti = Zi * Zi;
            }
            byte_acc <<= 1;
            if (Tr + Ti <= limit * limit)
                byte_acc |= 0x01;
            bit_num++;
            if (bit_num == 8) {
                putc(byte_acc, stdout);
                byte_acc = 0;
                bit_num = 0;
            } else if (x == w - 1) {
                byte_acc <<= 8 - w % 8;
                putc(byte_acc, stdout);
                byte_acc = 0;
                bit_num = 0;
            }
        }
    }
}`,
        patterns: [
            'fun main()',
            'w, h, bit_num := 0',
            'iter: 50',
            'for y := until(h)',
            'Zr = Zi = Tr = Ti := 0',
            'print(',
            'bit_num += 1'
        ]
    },

    kotlin: {
        input: `// The Computer Language Benchmarks Game
// https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

package org.bau.benchmarks

import java.io.BufferedOutputStream
import java.io.OutputStream

fun main(args: Array<String>) {
    val w: Int
    val h: Int
    var bitNum = 0
    val iter = 50
    val limit = 2.0
    var byteAcc: Byte = 0
    var n = 200
    if (args.isNotEmpty())
        n = args[0].toInt()
    h = n
    w = h
    val out: OutputStream = BufferedOutputStream(System.out)
    out.write("P4\\n\$w \$h\\n".toByteArray())
    for (y in 0 until h) {
        for (x in 0 until w) {
            var Zr = 0.0
            var Zi = 0.0
            var Tr = 0.0
            var Ti = 0.0
            val Cr = (2.0 * x / w - 1.5)
            val Ci = (2.0 * y / h - 1.0)
            var i = 0
            while (i < iter && Tr + Ti <= limit * limit) {
                Zi = 2.0 * Zr * Zi + Ci
                Zr = Tr - Ti + Cr
                Tr = Zr * Zr
                Ti = Zi * Zi
                i++
            }
            byteAcc = (byteAcc.toInt() shl 1).toByte()
            if (Tr + Ti <= limit * limit)
                byteAcc = (byteAcc.toInt() or 0x01).toByte()
            bitNum++
            if (bitNum == 8) {
                out.write(byteAcc.toInt())
                byteAcc = 0
                bitNum = 0
            } else if (x == w - 1) {
                byteAcc = (byteAcc.toInt() shl (8 - (w % 8))).toByte()
                out.write(byteAcc.toInt())
                byteAcc = 0
                bitNum = 0
            }
        }
    }
    out.flush()
}`,
        patterns: [
            'fun main()',
            'bitNum := 0',
            'iter: 50',
            'for y := until(h)',
            'for x := until(w)',
            'Zr := 0.0',
            'byteAcc <<= 1'
        ]
    },

    python: {
        input: `# The Computer Language Benchmarks Game
# https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

import sys, struct

def main():
    cout = sys.stdout.buffer.write
    n = 200
    if len(sys.argv) > 1:
        n = int(sys.argv[1])
    w = h = n
    Iter = 50
    Limit = 2.0
    sys.stdout.buffer.write(f'P4\\n{w} {h}\\n'.encode())
    for y in range(h):
        bit_num, byte_acc = 0, 0
        for x in range(w):
            Zr = Zi = Tr = Ti = 0.0
            Cr = 2.0 * x / w - 1.5
            Ci = 2.0 * y / h - 1.0
            i = 0
            while i < Iter and Tr + Ti <= Limit * Limit:
                Zi = 2.0 * Zr * Zi + Ci
                Zr = Tr - Ti + Cr
                Tr = Zr * Zr
                Ti = Zi * Zi
                i += 1
            byte_acc <<= 1
            if Tr + Ti <= Limit * Limit:
                byte_acc |= 0x01
            bit_num += 1
            if bit_num == 8:
                sys.stdout.buffer.write(struct.pack('B', byte_acc))
                bit_num = 0
                byte_acc = 0
            elif x == w - 1:
                byte_acc <<= 8 - (w % 8)
                sys.stdout.buffer.write(struct.pack('B', byte_acc))
                bit_num = 0
                byte_acc = 0

main()`,
        patterns: [
            'fun main()',
            'n := 200',
            'w = h = n',
            'for y := until(h)',
            'Zr, Zi, Tr, Ti := 0.0',
            'println(',
            'byte_acc <<= 1'
        ]
    },

    rust: {
        input: `// The Computer Language Benchmarks Game
// https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

use std::env;
use std::io::{self, Write};

fn main() {
    let args: Vec<String> = env::args().collect();
    let mut n = 200;
    if args.len() > 1 {
        n = args[1].parse().unwrap();
    }
    let w = n;
    let h = n;
    let limit = 2.0;
    let iter = 50;
    let mut stdout = io::stdout().lock();
    writeln!(stdout, "P4\\n{} {}", w, h).unwrap();
    let mut byte_acc: u8 = 0;
    let mut bit_num = 0;
    for y in 0..h {
        for x in 0..w {
            let mut zr = 0.0;
            let mut zi = 0.0;
            let cr = 2.0 * x as f64 / w as f64 - 1.5;
            let ci = 2.0 * y as f64 / h as f64 - 1.0;
            let mut tr = 0.0;
            let mut ti = 0.0;
            let mut i = 0;
            while i < iter && (tr + ti <= limit * limit) {
                zi = 2.0 * zr * zi + ci;
                zr = tr - ti + cr;
                tr = zr * zr;
                ti = zi * zi;
                i += 1;
            }
            byte_acc <<= 1;
            if tr + ti <= limit * limit {
                byte_acc |= 0x01;
            }
            bit_num += 1;
            if bit_num == 8 {
                stdout.write_all(&[byte_acc]).unwrap();
                byte_acc = 0;
                bit_num = 0;
            } else if x == w - 1 {
                byte_acc <<= 8 - (w % 8);
                stdout.write_all(&[byte_acc]).unwrap();
                byte_acc = 0;
                bit_num = 0;
            }
        }
    }
}`,
        patterns: [
            'fun main()',
            'n := 200',
            'for y := until(h)',
            'zr := 0.0',
            'byte_acc <<= 1',
            'println(',
            'bit_num += 1'
        ]
    },

    swift: {
        input: `// The Computer Language Benchmarks Game
// https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

import Foundation

func mandelbrot(n: Int) {
    let w = n
    let h = n
    let iter = 50
    let limit = 2.0
    var bitNum = 0
    var byteAcc: UInt8 = 0
    FileHandle.standardOutput.write("P4\\n\\(w) \\(h)\\n".data(using: .ascii)!)
    for y in 0..<h {
        for x in 0..<w {
            var Zr = 0.0, Zi = 0.0, Tr = 0.0, Ti = 0.0
            let Cr = 2.0 * Double(x) / Double(w) - 1.5
            let Ci = 2.0 * Double(y) / Double(h) - 1.0
            var i = 0
            while i < iter && (Tr + Ti) <= limit * limit {
                Zi = 2.0 * Zr * Zi + Ci
                Zr = Tr - Ti + Cr
                Tr = Zr * Zr
                Ti = Zi * Zi
                i += 1
            }
            byteAcc <<= 1
            if Tr + Ti <= limit * limit {
                byteAcc |= 0x01
            }
            bitNum += 1
            if bitNum == 8 {
                FileHandle.standardOutput.write(Data([byteAcc]))
                bitNum = 0
                byteAcc = 0
            } else if (x == w - 1) {
                byteAcc <<= 8 - (w % 8)
                FileHandle.standardOutput.write(Data([byteAcc]))
                bitNum = 0
                byteAcc = 0
            }
        }
    }
    fflush(stdout)
}

let n = CommandLine.arguments.count > 1 ? Int(CommandLine.arguments[1]) ?? 200 : 200
mandelbrot(n: n)`,
        patterns: [
            'fun mandelbrot(n int)',
            'w: n',
            'iter: 50',
            'for y := until(h)',
            'Zr, Zi, Tr, Ti := 0.0',
            'byteAcc <<= 1',
            'bitNum += 1'
        ]
    },

    go: {
        input: `// The Computer Language Benchmarks Game
// https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

package main

import (
    "bufio"
    "flag"
    "fmt"
    "os"
    "strconv"
)

func main() {
    flag.Parse()
    n := 200
    if flag.NArg() > 0 {
        n, _ = strconv.Atoi(flag.Arg(0))
    }
    out := bufio.NewWriter(os.Stdout)
    defer out.Flush()
    w := n
    h := n
    bitNum := 0
    byteAcc := byte(0)
    iter := 50
    const z = 0.0
    const limit = 2.0
    fmt.Fprintf(out, "P4\\n%d %d\\n", w, h)
    for y := 0; y < h; y++ {
        for x := 0; x < w; x++ {
            Zr, Zi, Tr, Ti := z, z, z, z
            Cr := 2 * float64(x) / float64(w) - 1.5
            Ci := 2 * float64(y) / float64(h) - 1
            for i := 0; i < iter && Tr + Ti <= limit * limit; i++ {
                Zi = 2 * Zr * Zi + Ci
                Zr = Tr - Ti + Cr
                Tr = Zr * Zr
                Ti = Zi * Zi
            }
            byteAcc <<= 1
            if Tr + Ti <= limit * limit {
                byteAcc |= 0x01
            }
            bitNum++
            if bitNum == 8 {
                out.WriteByte(byteAcc)
                byteAcc = 0
                bitNum = 0
            } else if x == w - 1 {
                byteAcc <<= 8 - w % 8
                out.WriteByte(byteAcc)
                byteAcc = 0
                bitNum = 0
            }
        }
    }
}`,
        patterns: [
            'fun main()',
            'n := 200',
            'for y := until(h)',
            'Zr, Zi, Tr, Ti := z, z, z, z',
            'byteAcc <<= 1',
            'bitNum += 1',
            'print('
        ]
    }
};

// Helper function to check if output contains expected patterns
function checkPatterns(output, patterns, language) {
    const results = [];
    patterns.forEach(pattern => {
        const found = output.includes(pattern);
        results.push({
            pattern,
            found,
            status: found ? 'PASS' : 'FAIL'
        });
        if (!found) {
            console.log(`❌ ${language}: Missing pattern "${pattern}"`);
        }
    });
    return results;
}

// Main test function
function runTests() {
    console.log('🧪 Running Mandelbrot Converter Tests\n');
    
    const testResults = {};
    
    Object.keys(testCases).forEach(language => {
        console.log(`\n🔧 Testing ${language.toUpperCase()} converter:`);
        console.log('=' + '='.repeat(30 + language.length));
        
        const testCase = testCases[language];
        let output = '';
        
        try {
            // Get the appropriate converter function
            const converterName = `convert${language.charAt(0).toUpperCase() + language.slice(1)}ToBau`;
            let converter;
            
            if (typeof window !== 'undefined') {
                // Browser environment
                converter = window[converterName];
            } else {
                // Node.js environment
                converter = converters[language][converterName];
            }
            
            if (!converter) {
                throw new Error(`Converter function ${converterName} not found`);
            }
            
            output = converter(testCase.input);
            
            console.log('✅ Conversion completed successfully');
            
            // Check for expected patterns
            const patternResults = checkPatterns(output, testCase.patterns, language);
            const passedPatterns = patternResults.filter(r => r.found).length;
            const totalPatterns = patternResults.length;
            
            console.log(`📊 Pattern matching: ${passedPatterns}/${totalPatterns} patterns found`);
            
            // Store results
            testResults[language] = {
                success: true,
                patternsFound: passedPatterns,
                totalPatterns: totalPatterns,
                output: output.substring(0, 200) + '...' // First 200 chars
            };
            
            // Show sample output
            console.log('📝 Sample output:');
            console.log(output.split('\n').slice(0, 8).join('\n') + '\n...\n');
            
        } catch (error) {
            console.log(`❌ Error: ${error.message}`);
            testResults[language] = {
                success: false,
                error: error.message
            };
        }
    });
    
    // Summary
    console.log('\n📈 TEST SUMMARY');
    console.log('================');
    
    const totalTests = Object.keys(testResults).length;
    const passedTests = Object.values(testResults).filter(r => r.success).length;
    
    console.log(`Total Languages: ${totalTests}`);
    console.log(`Successful Conversions: ${passedTests}`);
    console.log(`Success Rate: ${(passedTests/totalTests*100).toFixed(1)}%\n`);
    
    Object.keys(testResults).forEach(language => {
        const result = testResults[language];
        if (result.success) {
            const percentage = (result.patternsFound / result.totalPatterns * 100).toFixed(1);
            console.log(`✅ ${language.toUpperCase()}: ${result.patternsFound}/${result.totalPatterns} patterns (${percentage}%)`);
        } else {
            console.log(`❌ ${language.toUpperCase()}: Failed - ${result.error}`);
        }
    });
    
    return testResults;
}

// Export for use in browser or Node.js
if (typeof module !== 'undefined' && module.exports) {
    module.exports = { runTests, testCases, checkPatterns };
} else if (typeof window !== 'undefined') {
    window.runMandelbrotTests = runTests;
    window.mandelbrotTestCases = testCases;
}

// Auto-run tests if this is the main module
if (typeof require !== 'undefined' && require.main === module) {
    runTests();
} 