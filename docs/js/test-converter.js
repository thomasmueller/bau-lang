/**
 * Test cases for Java to Bau converter
 * Run this file with Node.js to test the conversion functions
 */

// Import the converter functions
let convertJavaToBau, convertLine, convertBauType;

if (typeof require !== 'undefined') {
    // Node.js environment
    const converter = require('./java-to-bau-converter.js');
    convertJavaToBau = converter.convertJavaToBau;
    convertLine = converter.convertLine;
    convertBauType = converter.convertBauType;
} else if (typeof window !== 'undefined') {
    // Browser environment - functions are globally available
    convertJavaToBau = window.convertJavaToBau;
    convertLine = window.convertLine;
    convertBauType = window.convertBauType;
}

// Test cases
const testCases = [
    {
        name: "Basic Java class and main method",
        input: `public class Example {
    public static void main(String[] args) {
        System.out.println("Hello World");
    }
}`,
        expected: `fun main()
    println('Hello World')`
    },
    
    {
        name: "Variable declarations",
        input: `    int x = 10;
    final int MAX = 100;`,
        expected: `x := 10
MAX: 100`
    },
    
    {
        name: "For loop with until",
        input: `    for (int i = 0; i < x; i++) {
        System.out.println(i);
    }`,
        expected: `for i := until(x)
    println(i)`
    },
    
    {
        name: "If statement with operators",
        input: `    if (i != 5 && i % 2 == 0) {
        System.out.println("Even: " + i);
    }`,
        expected: `if i <> 5 and i % 2 = 0
    println('Even: ' i)`
    },
    
    {
        name: "Function declaration",
        input: `    static int count(int depth) {`,
        expected: `fun count(depth int) int`
    },
    
    {
        name: "Function with multiple parameters",
        input: `    static double calculate(long x, byte y, float z) {`,
        expected: `fun calculate(x int, y i8, z f32) float`
    },
    
    {
        name: "Package declaration",
        input: `package com.example.myapp;`,
        expected: `module com.example.myapp`
    },
    
    {
        name: "Array variable declaration",
        input: `    var perm1 = new int[n];`,
        expected: `perm1 : int[n]`
    },
    
    {
        name: "Boolean variables and literals",
        input: `    boolean flag = true;
    boolean active = false;
    if (flag == true && active != false) {
        System.out.println("Both conditions");
    }`,
        expected: `flag := 1
active := 0
if flag = 1 and active <> 0
    println('Both conditions')`
    },
    
    {
        name: "Increment and decrement operations",
        input: `    int counter = 0;
    counter++;
    count[r]--;
    data[i][j]++;
    while (counter > 0) {
        counter--;
    }`,
        expected: `counter := 0
counter += 1
count[r] -= 1
data[i][j] += 1
while counter > 0
    counter -= 1`
    },
    
    {
        name: "Comments with URLs",
        input: `    // Simple comment
    // https://github.com/jabbalaci/SpeedTests
    // See: http://example.com//path//to//file
    int x = 10; // Inline comment with URL: https://test.com//api//v1//endpoint`,
        expected: `# Simple comment
# https://github.com/jabbalaci/SpeedTests
# See: http://example.com//path//to//file
x := 10 # Inline comment with URL: https://test.com//api//v1//endpoint`
    },
    
    {
        name: "Empty lines preservation",
        input: `    int x = 10;
    
    
    System.out.println(x);`,
        expected: `x := 10

println(x)`
    }
];

// Test individual functions
function testConvertBauType() {
    console.log('\n=== Testing convertBauType ===');
    const typeTests = [
        ['int', 'int'],
        ['long', 'int'],
        ['double', 'float'],
        ['float', 'f32'],
        ['char', 'int'],
        ['byte', 'i8'],
        ['boolean', 'int'],
        ['void', ''],
        ['String', 'String'] // Unknown type should remain unchanged
    ];
    
    typeTests.forEach(([input, expected]) => {
        const result = convertBauType(input);
        const status = result === expected ? '✓' : '✗';
        console.log(`${status} ${input} → ${result} (expected: ${expected})`);
    });
}

function testConvertLine() {
    console.log('\n=== Testing convertLine ===');
    const lineTests = [
        ['int x = 10;', 'x := 10'],
        ['final int MAX = 100;', 'MAX: 100'],
        ['for (int i = 0; i < 10; i++) {', 'for i := until(10)'],
        ['if (x != 0 && y == 5) {', 'if x <> 0 and y = 5'],
        ['System.out.println("Hello");', 'println(\'Hello\')'],
        ['package com.example;', 'module com.example'],
        ['var perm1 = new int[n];', 'perm1 : int[n]'],
        ['var data = new long[size];', 'data : int[size]'],
        ['boolean flag = true;', 'flag := 1'],
        ['boolean active = false;', 'active := 0'],
        ['if (isValid == true) {', 'if isValid = 1'],
        ['count[r]--;', 'count[r] -= 1'],
        ['data[i]++;', 'data[i] += 1'],
        ['x++;', 'x += 1'],
        ['counter--;', 'counter -= 1'],
        ['// Simple comment', '# Simple comment'],
        ['// https://github.com/jabbalaci/SpeedTests', '# https://github.com/jabbalaci/SpeedTests'],
        ['// See: http://example.com//path//to//file', '# See: http://example.com//path//to//file']
    ];
    
    lineTests.forEach(([input, expected]) => {
        const result = convertLine(input);
        const status = result === expected ? '✓' : '✗';
        console.log(`${status} Input:    ${input}`);
        console.log(`  Result:   ${result}`);
        console.log(`  Expected: ${expected}\n`);
    });
}

function runAllTests() {
    console.log('Running Java to Bau Converter Tests\n');
    console.log('='.repeat(50));
    
    // Test individual functions
    testConvertBauType();
    testConvertLine();
    
    // Test full conversion
    console.log('\n=== Testing Full Conversion ===');
    testCases.forEach((testCase, index) => {
        console.log(`\nTest ${index + 1}: ${testCase.name}`);
        console.log('-'.repeat(30));
        
        const result = convertJavaToBau(testCase.input);
        const passed = result.trim() === testCase.expected.trim();
        
        console.log(`Status: ${passed ? '✓ PASS' : '✗ FAIL'}`);
        console.log(`Input:\n${testCase.input}`);
        console.log(`\nResult:\n${result}`);
        console.log(`\nExpected:\n${testCase.expected}`);
        
        if (!passed) {
            console.log('\n*** MISMATCH ***');
        }
    });
    
    console.log('\n' + '='.repeat(50));
    console.log('Tests completed!');
}

// Run tests if this file is executed directly
if (typeof require !== 'undefined' && require.main === module) {
    runAllTests();
}

// Export for browser usage
if (typeof window !== 'undefined') {
    window.testConverter = {
        runAllTests,
        testConvertBauType,
        testConvertLine,
        testCases
    };
} 