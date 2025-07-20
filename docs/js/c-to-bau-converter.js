/**
 * C to Bau Language Converter
 * Converts C source code to Bau language syntax
 */

// Shared post-processing utility access (no global variables to avoid conflicts)

function convertCToBau(cCode) {
    const lines = cCode.split('\n');
    const result = [];
    let lastLineWasEmpty = false;
    
    for (let i = 0; i < lines.length; i++) {
        const originalLine = lines[i];
        let line = originalLine.trim();
        
        // Preserve empty lines (but only one consecutive empty line)
        if (line === '') {
            if (!lastLineWasEmpty) {
                result.push('');
                lastLineWasEmpty = true;
            }
            continue;
        }
        
        // Get original indentation
        const originalIndent = originalLine.match(/^ */)[0];
        
        // Convert line individually
        line = convertCLine(line);
        
        // Skip if line becomes empty after conversion
        if (line === '') {
            continue;
        }
        
        // Skip braces (they don't appear in Bau)
        if (line === '{' || line === '}') {
            continue;
        }
        
        // Handle lines that start with closing brace
        if (line.startsWith('}')) {
            line = line.slice(1).trim();
            if (line === '') {
                continue;
            }
        }
        
        // Handle lines that end with opening brace
        if (line.endsWith('{')) {
            line = line.slice(0, -1).trim();
        }
        
        // Preserve original indentation (C doesn't have class wrapper like Java)
        const newIndent = originalIndent;
        result.push(newIndent + line);
        lastLineWasEmpty = false;
    }
    
    const rawResult = result.join('\n');
    
    // Apply shared post-processing to clean up the output
    if (typeof window !== 'undefined' && window.cleanupBauCode) {
        // Browser environment
        return window.cleanupBauCode(rawResult);
    } else if (typeof require !== 'undefined') {
        // Node.js environment - require on demand
        try {
            const postProcessor = require('./shared-post-processor.js');
            return postProcessor.cleanupBauCode(rawResult);
        } catch (error) {
            // Fallback if post-processor not available
            return rawResult;
        }
    }
    
    return rawResult;
}

function convertCLine(line) {
    // Skip include statements
    if (line.match(/^#include/)) {
        return '';
    }
    
    // Convert main function
    if (line.match(/int\s+main\s*\(\s*\)|int\s+main\s*\(\s*void\s*\)/)) {
        return 'fun main()';
    }
    if (line.match(/int\s+main\s*\(\s*int\s+argc\s*,\s*char\s*\*\*argv\s*\)/)) {
        return 'fun main()';
    }
    
    // Convert comments (only first occurrence of // to preserve URLs)
    line = line.replace(/\/\//, '#');
    
    // Remove common C patterns
    line = line.replace(/return\s+0\s*;?\s*$/, ''); // Remove return 0
    
    // Convert printf/scanf functions
    line = line.replace(/printf\s*\(/g, 'print(');
    line = line.replace(/scanf\s*\(/g, 'input(');
    
    // Convert for loops FIRST (before variable declarations)
    // Handle loops starting from 0 with until()
    line = line.replace(/for\s*\(\s*int\s+(\w+)\s*=\s*0\s*;\s*\1\s*<\s*([^;]+)\s*;\s*\1\+\+\s*\)/g, 'for $1 := until($2)');
    line = line.replace(/for\s*\(\s*int\s+(\w+)\s*=\s*0\s*;\s*\1\s*<=\s*([^;]+)\s*;\s*\1\+\+\s*\)/g, 'for $1 := until($2 + 1)');
    // Handle other loops with range()
    line = line.replace(/for\s*\(\s*int\s+(\w+)\s*=\s*([1-9]\d*)\s*;\s*\1\s*<\s*([^;]+)\s*;\s*\1\+\+\s*\)/g, 'for $1 := range($2, $3)');
    line = line.replace(/for\s*\(\s*int\s+(\w+)\s*=\s*([1-9]\d*)\s*;\s*\1\s*<=\s*([^;]+)\s*;\s*\1\+\+\s*\)/g, 'for $1 := range($2, $3 + 1)');
    
    // Convert operators
    line = line.replace(/!=/g, '<>');
    line = line.replace(/==/g, '=');
    line = line.replace(/\s*&&\s*/g, ' and ');
    line = line.replace(/\s*\|\|\s*/g, ' or ');
    line = line.replace(/\s*!\s*([a-zA-Z_]\w*)/g, ' not $1');
    
    // Convert double quotes to single quotes
    line = line.replace(/"/g, "'");
    
    // Convert boolean-like values
    line = line.replace(/\btrue\b/g, '1');
    line = line.replace(/\bfalse\b/g, '0');
    
    // Convert function declarations
    line = line.replace(/(int|float|double|char|void)\s+(\w+)\s*\(([^)]*)\)/g, function(match, returnType, funcName, params) {
        const convertedReturnType = convertBauType(returnType);
        
        let convertedParams = '';
        if (params.trim()) {
            const paramList = params.split(',').map(param => {
                const parts = param.trim().split(/\s+/);
                if (parts.length >= 2) {
                    const type = convertBauType(parts[0]);
                    const name = parts[1];
                    return name + ' ' + type;
                }
                return param.trim();
            });
            convertedParams = paramList.join(', ');
        }
        
        const returnTypePart = convertedReturnType ? ' ' + convertedReturnType : '';
        return 'fun ' + funcName + '(' + convertedParams + ')' + returnTypePart;
    });
    
    // Convert variable declarations (after function declarations)
    // Multi-variable declarations: int w, h, bit_num = 0; -> w, h, bit_num := 0
    line = line.replace(/\b(int|float|double|char)\s+(\w+)\s*,\s*(\w+)\s*,\s*(\w+)\s*=\s*([^;]+);?/g, '$2, $3, $4 := $5');
    line = line.replace(/\b(int|float|double|char)\s+(\w+)\s*,\s*(\w+)\s*=\s*([^;]+);?/g, '$2, $3 := $4');
    // Multi-variable declarations: int i, iter = 50; -> i, iter := 50
    line = line.replace(/\b(int|float|double|char)\s+(\w+)\s*,\s*(\w+)\s*=\s*([^;]+);?/g, '$2, $3 := $4');
    // Multi-variable declarations with multiple types: double Zr, Zi, Cr, Ci, Tr, Ti;
    line = line.replace(/\b(int|float|double|char)\s+(\w+)\s*,\s*(\w+)\s*,\s*(\w+)\s*,\s*(\w+)\s*,\s*(\w+)\s*,\s*(\w+)\s*;?/g, '$2, $3, $4, $5, $6, $7 float');
    // Array declarations: int arr[10]; -> arr : int[10]
    line = line.replace(/\b(int|float|double|char)\s+(\w+)\[([^\]]+)\]\s*;?/g, function(match, cType, varName, size) {
        const bauType = convertBauType(cType);
        return varName + ' : ' + bauType + '[' + size + ']';
    });
    // Const variables (always constants in C)
    line = line.replace(/\bconst\s+(int|float|double|char|long|short|unsigned)\s+(\w+)\s*=\s*([^;]+);?/g, '$2: $3');
    
    // Assignment to multiple variables: Zr = Zi = Tr = Ti = 0; -> Zr, Zi, Tr, Ti := 0
    line = line.replace(/(\w+)\s*=\s*(\w+)\s*=\s*(\w+)\s*=\s*(\w+)\s*=\s*([^;]+);?/g, '$1, $2, $3, $4 := $5');
    
    // Constants (simple literal values only): int x = 50; -> x: 50 (pure literals without operators)
    line = line.replace(/\b(int|float|double|char|long|short|unsigned)\s+(\w+)\s*=\s*(\d+(?:\.\d+)?)\s*;?\s*$/g, '$2: $3');
    
    // Regular variables with initialization (non-literal expressions): int x = getValue(); -> x := getValue()
    line = line.replace(/\b(int|float|double|char|long|short|unsigned)\s+(\w+)\s*=\s*([^;]+);?/g, '$2 := $3');
    
    // Variables without initialization: int x; -> x int (keep type when no initial value)
    // Handle multi-word types first: unsigned long, unsigned int, etc.
    line = line.replace(/\bunsigned\s+(long|int|short|char)\s+(\w+)\s*;?/g, function(match, baseType, varName) {
        const bauType = convertBauType('unsigned');
        return varName + ' ' + bauType;
    });
    line = line.replace(/\b(int|float|double|char|long|short)\s+(\w+)\s*;?/g, function(match, cType, varName) {
        const bauType = convertBauType(cType);
        return varName + ' ' + bauType;
    });
    
    // Convert while loops - remove parentheses
    line = line.replace(/while\s*\(\s*([^)]+)\s*\)/g, 'while $1');
    
    // Convert if statements - remove parentheses
    line = line.replace(/if\s*\(\s*([^)]+)\s*\)/g, 'if $1');
    line = line.replace(/else\s*if\s*\(\s*([^)]+)\s*\)/g, 'elif $1');
    
    // Convert increment/decrement (handles variables and array access)
    line = line.replace(/(\w+(?:\[[^\]]+\])*)\+\+/g, '$1 += 1');
    line = line.replace(/(\w+(?:\[[^\]]+\])*)\-\-/g, '$1 -= 1');
    
    // Remove semicolons
    line = line.replace(/;$/, '');
    
    return line;
}

function convertBauType(cType) {
    switch (cType) {
        case 'int':
        case 'long':
        case 'short':
        case 'unsigned':
            return 'int';
        case 'float':
            return 'f32';
        case 'double':
            return 'float';
        case 'char':
            return 'int';
        case 'void':
            return '';
        default:
            return cType;
    }
}

// Export functions for use in other files
if (typeof module !== 'undefined' && module.exports) {
    module.exports = {
        convertCToBau,
        convertCLine,
        convertBauType
    };
} else if (typeof window !== 'undefined') {
    window.convertCToBau = convertCToBau;
    window.convertCLine = convertCLine;
    window.convertCBauType = convertBauType;
} 