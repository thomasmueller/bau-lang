/**
 * Kotlin to Bau Language Converter
 * Converts Kotlin source code to Bau language syntax
 */

// Shared post-processing utility access (no global variables to avoid conflicts)

function convertKotlinToBau(kotlinCode) {
    const lines = kotlinCode.split('\n');
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
        line = convertKotlinLine(line);
        
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
        
        // Preserve original indentation (Kotlin object/class structure varies)
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

function convertKotlinLine(line) {
    // Skip package declarations
    if (line.match(/^package\s+/)) {
        return '';
    }
    
    // Skip import statements
    if (line.match(/^import\s+/)) {
        return '';
    }
    
    // Convert main function
    if (line.match(/fun\s+main\s*\(/)) {
        return 'fun main()';
    }
    
    // Convert comments (only first occurrence of // to preserve URLs)
    line = line.replace(/\/\//, '#');
    
    // Convert println/print
    line = line.replace(/println\s*\(/g, 'println(');
    line = line.replace(/print\s*\(/g, 'print(');
    
    // Convert for loops FIRST (before variable declarations)
    // Kotlin until loops: for (i in 0 until n) -> for i := until(n)
    line = line.replace(/for\s*\(\s*(\w+)\s+in\s+0\s+until\s+([^)]+)\s*\)/g, 'for $1 := until($2)');
    // Kotlin range loops: for (i in 0..n) -> for i := until(n + 1)
    line = line.replace(/for\s*\(\s*(\w+)\s+in\s+0\.\.([^)]+)\s*\)/g, 'for $1 := until($2 + 1)');
    // Kotlin range with start: for (i in 1..n) -> for i := range(1, n + 1)
    line = line.replace(/for\s*\(\s*(\w+)\s+in\s+([1-9]\d*)\.\.([^)]+)\s*\)/g, 'for $1 := range($2, $3 + 1)');
    
    // Convert while loops 
    line = line.replace(/while\s*\(\s*([^)]+)\s*\)/g, 'while $1');
    
    // Convert operators
    line = line.replace(/!=/g, '<>');
    line = line.replace(/==/g, '=');
    line = line.replace(/\s*&&\s*/g, ' and ');
    line = line.replace(/\s*\|\|\s*/g, ' or ');
    line = line.replace(/\s*!\s*([a-zA-Z_]\w*)/g, ' not $1');
    
    // Convert double quotes to single quotes
    line = line.replace(/"/g, "'");
    
    // Convert boolean literals
    line = line.replace(/\btrue\b/g, '1');
    line = line.replace(/\bfalse\b/g, '0');
    
    // Convert function declarations
    line = line.replace(/fun\s+(\w+)\s*\(([^)]*)\)(?:\s*:\s*([^{=]+))?/g, function(match, funcName, params, returnType) {
        let convertedReturnType = '';
        if (returnType && returnType.trim()) {
            convertedReturnType = convertBauType(returnType.trim());
        }
        
        let convertedParams = '';
        if (params.trim()) {
            const paramList = params.split(',').map(param => {
                const parts = param.trim().split(':');
                if (parts.length >= 2) {
                    const name = parts[0].trim();
                    const type = convertBauType(parts[1].trim());
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
    // Multi-variable declarations: val w: Int, val h: Int -> w, h int
    line = line.replace(/val\s+(\w+)(?:\s*:\s*\w+)?\s*(?:,\s*val\s+(\w+)(?:\s*:\s*\w+)?)+/g, function(match) {
        const vars = match.match(/(\w+)(?=\s*:|\s*,|\s*$)/g);
        return vars.join(', ') + ' int';
    });
    // Array declarations: val arr = Array(n) { 0 } -> arr : int[n]
    line = line.replace(/val\s+(\w+)\s*=\s*Array\s*\(\s*([^)]+)\s*\)\s*\{[^}]*\}/g, '$1 : int[$2]');
    // Constants: val MAX = 100 -> MAX: 100
    line = line.replace(/val\s+(\w+)\s*=\s*([^$]+)/g, '$1: $2');
    // Mutable variables: var x: Int = 10 -> x := 10
    line = line.replace(/var\s+(\w+)\s*(?::\s*\w+)?\s*=\s*([^$]+)/g, '$1 := $2');
    // Variables without initialization: var x: Int -> x int
    line = line.replace(/var\s+(\w+)\s*:\s*(\w+)\s*$/g, function(match, varName, kotlinType) {
        const bauType = convertBauType(kotlinType);
        return varName + ' ' + bauType;
    });
    
    // Convert if statements - remove parentheses
    line = line.replace(/if\s*\(\s*([^)]+)\s*\)/g, 'if $1');
    line = line.replace(/else\s*if\s*\(\s*([^)]+)\s*\)/g, 'elif $1');
    
    // Convert string interpolation: "P4\n$w $h\n" -> 'P4\n' w ' ' h '\n'
    line = line.replace(/'([^']*?)\$\{([^}]+)\}([^']*?)'/g, "'$1' $2 '$3'");
    line = line.replace(/'([^']*?)\$(\w+)([^']*?)'/g, "'$1' $2 '$3'");
    
    // Convert increment/decrement (handles variables and array access)
    line = line.replace(/(\w+(?:\[[^\]]+\])*)\+\+/g, '$1 += 1');
    line = line.replace(/(\w+(?:\[[^\]]+\])*)\-\-/g, '$1 -= 1');
    
    // Convert bitwise operations
    line = line.replace(/\.toByte\(\)/g, '');
    line = line.replace(/\.toInt\(\)/g, '');
    line = line.replace(/(\w+)\.toInt\(\)\s+(shl|shr|or|and)\s+/g, '$1 $2 ');
    line = line.replace(/\bshl\b/g, '<<=');
    line = line.replace(/\bshr\b/g, '>>=');
    line = line.replace(/\bor\b/g, '|');
    line = line.replace(/\band\b/g, '&');
    
    // Remove semicolons
    line = line.replace(/;$/, '');
    
    return line;
}

function convertBauType(kotlinType) {
    switch (kotlinType) {
        case 'Int':
        case 'Long':
        case 'Short':
        case 'Byte':
            return 'int';
        case 'Float':
            return 'f32';
        case 'Double':
            return 'float';
        case 'Boolean':
            return 'int';
        case 'Char':
            return 'int';
        case 'String':
            return 'string';
        case 'Unit':
            return '';
        default:
            return kotlinType;
    }
}

// Export functions for use in other files
if (typeof module !== 'undefined' && module.exports) {
    module.exports = {
        convertKotlinToBau,
        convertKotlinLine,
        convertBauType
    };
} else if (typeof window !== 'undefined') {
    window.convertKotlinToBau = convertKotlinToBau;
    window.convertKotlinLine = convertKotlinLine;
    window.convertKotlinBauType = convertBauType;
} 