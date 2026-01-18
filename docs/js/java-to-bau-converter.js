/**
 * Java to Bau Language Converter
 * Converts Java source code to Bau language syntax
 */

// Shared post-processing utility access (no global variables to avoid conflicts)

function convertJavaToBau(javaCode) {
    const lines = javaCode.split('\n');
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
        line = convertJavaLine(line);
        
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
        
        // Reduce indentation by 4 spaces
        const newIndent = originalIndent.length >= 4 ? originalIndent.slice(4) : '';
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

function convertJavaLine(line) {
    // Skip class declarations
    if (line.match(/^public\s+class\s+\w+/)) {
        return '';
    }
    
    // Convert main method
    if (line.match(/public\s+static\s+void\s+main/)) {
        return 'fun main()';
    }
    
    // Convert package to module
    line = line.replace(/\bpackage\s+/g, 'module ');
    
    // Convert comments (only first occurrence of // to preserve URLs)
    line = line.replace(/\/\//, '#');
    
    // Remove access modifiers
    line = line.replace(/\b(public|private|protected|static)\s+/g, '');
    
    // Convert System.out.println/print
    line = line.replace(/System\.out\.println\s*\(/g, 'println(');
    line = line.replace(/System\.out\.print\s*\(/g, 'print(');
    
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
    line = line.replace(/>>>/g, '>>');
    line = line.replace(/\s*&&\s*/g, ' and ');
    line = line.replace(/\s*\|\|\s*/g, ' or ');
    line = line.replace(/\s*!\s*([a-zA-Z_]\w*)/g, ' not $1');
    
    // Convert double quotes to single quotes
    line = line.replace(/"/g, "'");
    
    // Convert boolean literals (Bau uses int instead of boolean)
    line = line.replace(/\btrue\b/g, '1');
    line = line.replace(/\bfalse\b/g, '0');
    
    // Convert method declarations with parameters FIRST (before variable declarations)
    line = line.replace(/(?:static\s+|final\s+)*(\w+)\s+(\w+)\s*\(([^)]*)\)/g, function(match, returnType, methodName, params) {
        // Convert return type
        const convertedReturnType = convertJavaBauType(returnType);
        
        // Convert parameters
        let convertedParams = '';
        if (params.trim()) {
            const paramList = params.split(',').map(param => {
                const parts = param.trim().split(/\s+/);
                if (parts.length >= 2) {
                    const type = convertJavaBauType(parts[0]);
                    const name = parts[1];
                    return name + ' ' + type;
                }
                return param.trim();
            });
            convertedParams = paramList.join(', ');
        }
        
        // Add return type only if not void
        const returnTypePart = convertedReturnType ? ' ' + convertedReturnType : '';
        return 'fun ' + methodName + '(' + convertedParams + ')' + returnTypePart;
    });
    
    // Convert variable declarations (after function declarations)
    // Multi-variable declarations: int w, h, bitNum = 0; -> w, h, bitNum := 0
    line = line.replace(/\b(int|long|float|double|boolean|char|byte|short|String)\s+(\w+)\s*,\s*(\w+)\s*,\s*(\w+)\s*=\s*([^;]+);?/g, '$2, $3, $4 := $5');
    line = line.replace(/\b(int|long|float|double|boolean|char|byte|short|String)\s+(\w+)\s*,\s*(\w+)\s*=\s*([^;]+);?/g, '$2, $3 := $4');
    // Multi-variable assignment: w = h = n; -> w = h = n (keep as is)
    // Multi-variable declarations: double Zr = 0, Zi = 0, Tr = 0, Ti = 0;
    line = line.replace(/\b(int|long|float|double|boolean|char|byte|short|String)\s+(\w+)\s*=\s*([^,;]+)\s*,\s*(\w+)\s*=\s*([^,;]+)\s*,\s*(\w+)\s*=\s*([^,;]+)\s*,\s*(\w+)\s*=\s*([^;]+);?/g, '$2, $4, $6, $8 := $3');
    // Handle case where all variables have same value
    line = line.replace(/\b(int|long|float|double|boolean|char|byte|short|String)\s+(\w+)\s*=\s*(0|0\.0)\s*,\s*(\w+)\s*=\s*(0|0\.0)\s*,\s*(\w+)\s*=\s*(0|0\.0)\s*,\s*(\w+)\s*=\s*(0|0\.0);?/g, '$2, $4, $6, $8 := $3');
    // Array declarations: var perm1 = new int[n]; -> perm1 : int[n]
    line = line.replace(/\bvar\s+(\w+)\s*=\s*new\s+(\w+)\[([^\]]+)\]\s*;?/g, function(match, varName, javaType, size) {
        const bauType = convertJavaBauType(javaType);
        return varName + ' : ' + bauType + '[' + size + ']';
    });
    // Handle Java 'var' keyword (type inferred): var x = 10; -> x := 10
    line = line.replace(/\bvar\s+(\w+)\s*=\s*([^;]+);?/g, '$1 := $2');
    
    // Final variables (constants) - handle both with and without type
    line = line.replace(/\bfinal\s+\w+\s+(\w+)\s*=\s*([^;]+);?/g, '$1: $2');
    
    // Constants (simple literal values only): int x = 50; -> x: 50 (pure literals without operators)
    line = line.replace(/\b(int|long|float|double|boolean|char|byte|short|String)\s+(\w+)\s*=\s*(\d+(?:\.\d+)?|true|false|'[^']*'|"[^"]*")\s*;?\s*$/g, '$2: $3');
    
    // Regular variables with initialization (non-literal expressions): int x = getValue(); -> x := getValue()
    line = line.replace(/\b(int|long|float|double|boolean|char|byte|short|String)\s+(\w+)\s*=\s*([^;]+);?/g, '$2 := $3');
    
    // Variables without initialization: int x; -> x int (keep type when no initial value)
    line = line.replace(/\b(int|long|float|double|boolean|char|byte|short|String)\s+(\w+)\s*;?/g, function(match, javaType, varName) {
        const bauType = convertJavaBauType(javaType);
        return varName + ' ' + bauType;
    });
    
    // Convert while loops - remove parentheses
    line = line.replace(/while\s*\(\s*([^)]+)\s*\)/g, 'while $1');
    
    // Convert if statements - remove parentheses
    line = line.replace(/if\s*\(\s*([^)]+)\s*\)/g, 'if $1');
    line = line.replace(/else\s*if\s*\(\s*([^)]+)\s*\)/g, 'elif $1');
    
    // Fix string concatenation - convert to space-separated args
    line = line.replace(/'([^']*)'\s*\+\s*([^,);]+)/g, "'$1' $2");
    line = line.replace(/([^,);(]+)\s*\+\s*'([^']*)'/g, "$1 '$2'");
    
    // Convert increment/decrement (handles variables and array access)
    line = line.replace(/(\w+(?:\[[^\]]+\])*)\+\+/g, '$1 += 1');
    line = line.replace(/(\w+(?:\[[^\]]+\])*)\-\-/g, '$1 -= 1');
    
    // Remove semicolons
    line = line.replace(/;$/, '');
    
    return line;
}

function convertJavaBauType(javaType) {
    switch (javaType) {
        case 'long':
        case 'int':
            return 'int';
        case 'double':
            return 'float';
        case 'float':
            return 'f32';
        case 'char':
            return 'int';
        case 'byte':
            return 'i8';
        case 'boolean':
            return 'int';
        case 'String':
            return 'string';
        case 'void':
            return '';
        default:
            return javaType; // Keep unknown types as-is
    }
}

// Export functions for use in other files
if (typeof module !== 'undefined' && module.exports) {
    // Node.js environment
    module.exports = {
        convertJavaToBau,
        convertJavaLine,
        convertJavaBauType
    };
} else if (typeof window !== 'undefined') {
    // Browser environment - make functions globally available
    window.convertJavaToBau = convertJavaToBau;
    window.convertJavaLine = convertJavaLine;
    window.convertJavaBauType = convertJavaBauType;
} 