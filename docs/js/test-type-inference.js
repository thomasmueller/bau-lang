/**
 * Unit Tests for Java and C Type Inference Improvements
 * Tests the enhanced type conversion rules for better Bau output
 * Uses the same converter functions as the web application (convertJavaToBau, convertCToBau)
 */

// Import converters
let converters = {};
if (typeof module !== 'undefined' && module.exports) {
    // Node.js environment
    converters.java = require('./java-to-bau-converter.js');
    converters.c = require('./c-to-bau-converter.js');
}

// Test cases for Java type inference
const javaTestCases = [
    // Constants (literals should use ':')
    {
        input: 'int iter = 50;',
        expected: 'iter: 50',
        description: 'Integer literal should become constant'
    },
    {
        input: 'double limit = 2.0;',
        expected: 'limit: 2.0',
        description: 'Double literal should become constant'
    },
    {
        input: 'boolean flag = true;',
        expected: 'flag: 1',
        description: 'Boolean literal should become constant with conversion'
    },
    {
        input: 'String name = "test";',
        expected: 'name: \'test\'',
        description: 'String literal should become constant with quote conversion'
    },
    {
        input: 'byte byteVal = 0;',
        expected: 'byteVal: 0',
        description: 'Byte literal should become constant'
    },
    
    // Variables (expressions should use ':=')
    {
        input: 'int w = Integer.parseInt(args[0]);',
        expected: 'w := Integer.parseInt(args[0])',
        description: 'Method call should become variable'
    },
    {
        input: 'double calc = 2.0 * x / w - 1.5;',
        expected: 'calc := 2.0 * x / w - 1.5',
        description: 'Expression should become variable'
    },
    {
        input: 'int result = getValue() + 10;',
        expected: 'result := getValue() + 10',
        description: 'Complex expression should become variable'
    },
    
    // Java-specific features
    {
        input: 'var x = 10;',
        expected: 'x := 10',
        description: 'var keyword should become variable'
    },
    {
        input: 'var name = getString();',
        expected: 'name := getString()',
        description: 'var with expression should become variable'
    },
    {
        input: 'final int MAX = 100;',
        expected: 'MAX: 100',
        description: 'final keyword should become constant'
    },
    
    // Type declarations without initialization
    {
        input: 'int count;',
        expected: 'count int',
        description: 'Uninitialized variable should keep type'
    },
    {
        input: 'String text;',
        expected: 'text string',
        description: 'Uninitialized String should convert type'
    }
];

// Test cases for C type inference
const cTestCases = [
    // Constants (literals should use ':')
    {
        input: 'int iter = 50;',
        expected: 'iter: 50',
        description: 'Integer literal should become constant'
    },
    {
        input: 'double limit = 2.0;',
        expected: 'limit: 2.0',
        description: 'Double literal should become constant'
    },
    {
        input: 'char ch = 0;',
        expected: 'ch: 0',
        description: 'Char literal should become constant'
    },
    {
        input: 'float val = 3.14;',
        expected: 'val: 3.14',
        description: 'Float literal should become constant'
    },
    
    // Variables (expressions should use ':=')
    {
        input: 'int n = atoi(argv[1]);',
        expected: 'n := atoi(argv[1])',
        description: 'Function call should become variable'
    },
    {
        input: 'double calc = 2.0 * x / w - 1.5;',
        expected: 'calc := 2.0 * x / w - 1.5',
        description: 'Expression should become variable'
    },
    {
        input: 'int result = getValue() + offset;',
        expected: 'result := getValue() + offset',
        description: 'Complex expression should become variable'
    },
    
    // C-specific const keyword
    {
        input: 'const int MAX = 100;',
        expected: 'MAX: 100',
        description: 'const keyword should become constant'
    },
    {
        input: 'const double PI = 3.14159;',
        expected: 'PI: 3.14159',
        description: 'const double should become constant'
    },
    
    // Type declarations without initialization
    {
        input: 'int count;',
        expected: 'count int',
        description: 'Uninitialized variable should keep type'
    },
    {
        input: 'unsigned long value;',
        expected: 'value int',
        description: 'Uninitialized unsigned long should convert type'
    }
];

// Helper function to run tests for a language
function runLanguageTests(language, testCases, converter) {
    console.log(`\n🧪 Testing ${language.toUpperCase()} Type Inference`);
    console.log('=' + '='.repeat(35 + language.length));
    
    let passed = 0;
    let failed = 0;
    
    testCases.forEach((testCase, index) => {
        const actual = converter.convertToBau(testCase.input).trim();
        const success = actual === testCase.expected;
        
        if (success) {
            console.log(`✅ Test ${index + 1}: ${testCase.description}`);
            passed++;
        } else {
            console.log(`❌ Test ${index + 1}: ${testCase.description}`);
            console.log(`   Input:    ${testCase.input}`);
            console.log(`   Expected: ${testCase.expected}`);
            console.log(`   Actual:   ${actual}`);
            failed++;
        }
    });
    
    console.log(`\n📊 Results: ${passed} passed, ${failed} failed`);
    return { passed, failed, total: testCases.length };
}

// Main test function
function runTypeInferenceTests() {
    console.log('🔬 Type Inference Conversion Tests');
    console.log('==================================');
    
    const results = {};
    
    // Test Java
    if (typeof window !== 'undefined') {
        // Browser environment
        results.java = runLanguageTests('Java', javaTestCases, { convertToBau: window.convertJavaToBau });
    } else {
        // Node.js environment
        results.java = runLanguageTests('Java', javaTestCases, { convertToBau: converters.java.convertJavaToBau });
    }
    
    // Test C
    if (typeof window !== 'undefined') {
        // Browser environment
        results.c = runLanguageTests('C', cTestCases, { convertToBau: window.convertCToBau });
    } else {
        // Node.js environment
        results.c = runLanguageTests('C', cTestCases, { convertToBau: converters.c.convertCToBau });
    }
    
    // Overall summary
    console.log('\n🎯 OVERALL SUMMARY');
    console.log('==================');
    
    const totalPassed = results.java.passed + results.c.passed;
    const totalFailed = results.java.failed + results.c.failed;
    const totalTests = results.java.total + results.c.total;
    const successRate = (totalPassed / totalTests * 100).toFixed(1);
    
    console.log(`Total Tests: ${totalTests}`);
    console.log(`Passed: ${totalPassed}`);
    console.log(`Failed: ${totalFailed}`);
    console.log(`Success Rate: ${successRate}%`);
    
    Object.keys(results).forEach(lang => {
        const result = results[lang];
        const rate = (result.passed / result.total * 100).toFixed(1);
        console.log(`${lang.toUpperCase()}: ${result.passed}/${result.total} (${rate}%)`);
    });
    
    return results;
}

// Export for use in browser or Node.js
if (typeof module !== 'undefined' && module.exports) {
    module.exports = { 
        runTypeInferenceTests, 
        javaTestCases, 
        cTestCases,
        runLanguageTests 
    };
} else if (typeof window !== 'undefined') {
    window.runTypeInferenceTests = runTypeInferenceTests;
    window.javaTypeTestCases = javaTestCases;
    window.cTypeTestCases = cTestCases;
}

// Auto-run tests if this is the main module
if (typeof require !== 'undefined' && require.main === module) {
    runTypeInferenceTests();
} 