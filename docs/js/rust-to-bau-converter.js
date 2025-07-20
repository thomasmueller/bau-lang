/**
 * Rust to Bau Language Converter
 * Converts Rust source code to Bau language syntax
 */

// Shared post-processing utility access (no global variables to avoid conflicts)

function convertRustToBau(rustCode) {
    const lines = rustCode.split('\n');
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
        line = convertRustLine(line);
        
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
        
        // Preserve original indentation (Rust doesn't have class wrapper like Java)
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

function convertRustLine(line) {
    // Skip use statements
    if (line.match(/^use\s+/)) {
        return '';
    }
    
    // Convert main function
    if (line.match(/fn\s+main\s*\(\s*\)/)) {
        return 'fun main()';
    }
    
    // Convert comments (only first occurrence of // to preserve URLs)
    line = line.replace(/\/\//, '#');
    
    // Convert println! and print! macros
    line = line.replace(/println!\s*\(/g, 'println(');
    line = line.replace(/print!\s*\(/g, 'print(');
    
    // Convert for loops FIRST (before variable declarations)
    // Rust range loops: for i in 0..n -> for i := until(n)
    line = line.replace(/for\s+(\w+)\s+in\s+0\.\.(\w+)/g, 'for $1 := until($2)');
    line = line.replace(/for\s+(\w+)\s+in\s+0\.\.=(\w+)/g, 'for $1 := until($2 + 1)');
    // Other range loops: for i in 1..n -> for i := range(1, n)
    line = line.replace(/for\s+(\w+)\s+in\s+([1-9]\d*)\.\.(\w+)/g, 'for $1 := range($2, $3)');
    line = line.replace(/for\s+(\w+)\s+in\s+([1-9]\d*)\.\.=(\w+)/g, 'for $1 := range($2, $3 + 1)');
    
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
    line = line.replace(/fn\s+(\w+)\s*\(([^)]*)\)\s*(?:->\s*([^{]+))?/g, function(match, funcName, params, returnType) {
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
    // Constants: const MAX: i32 = 100; -> MAX: 100
    line = line.replace(/const\s+(\w+)\s*:\s*\w+\s*=\s*([^;]+);?/g, '$1: $2');
    
    // Array declarations with explicit mutable: let mut arr = [0; 10]; -> arr : int[10]
    line = line.replace(/let\s+mut\s+(\w+)\s*=\s*\[[^;]*;\s*([^\]]+)\]\s*;?/g, '$1 : int[$2]');
    // Array declarations immutable: let arr = [0; 10]; -> arr : int[10]  
    line = line.replace(/let\s+(\w+)\s*=\s*\[[^;]*;\s*([^\]]+)\]\s*;?/g, '$1 : int[$2]');
    
    // Mutable variables: let mut x: i32 = 10; -> x := 10 (order matters - this must come before general let)
    line = line.replace(/let\s+mut\s+(\w+)\s*(?::\s*[^=]+)?\s*=\s*([^;]+);?/g, '$1 := $2');
    
    // Immutable variables that are clearly constants (literal values only): let x = 50; -> x: 50
    line = line.replace(/let\s+(\w+)\s*(?::\s*[^=]+)?\s*=\s*(\d+(?:\.\d+)?)\s*;?\s*$/g, '$1: $2');
    
    // Other immutable variables (could be derived values): let x: i32 = value; -> x := value
    line = line.replace(/let\s+(\w+)\s*(?::\s*[^=]+)?\s*=\s*([^;]+);?/g, '$1 := $2');
    
    // Variables without initialization: let mut x: i32; -> x int
    line = line.replace(/let\s+(?:mut\s+)?(\w+)\s*:\s*(\w+)\s*;?/g, function(match, varName, rustType) {
        const bauType = convertBauType(rustType);
        return varName + ' ' + bauType;
    });
    
    // Convert while loops - remove braces
    line = line.replace(/while\s+([^{]+)/g, 'while $1');
    
    // Convert if statements - remove braces
    line = line.replace(/if\s+([^{]+)/g, 'if $1');
    line = line.replace(/else\s+if\s+([^{]+)/g, 'elif $1');
    
    // Convert increment/decrement (handles variables and array access)
    line = line.replace(/(\w+(?:\[[^\]]+\])*)\s*\+=\s*1/g, '$1 += 1');
    line = line.replace(/(\w+(?:\[[^\]]+\])*)\s*-=\s*1/g, '$1 -= 1');
    
    // Remove semicolons
    line = line.replace(/;$/, '');
    
    return line;
}

function convertBauType(rustType) {
    switch (rustType) {
        case 'i8':
            return 'i8';
        case 'i16':
        case 'i32':
        case 'i64':
        case 'isize':
        case 'u8':
        case 'u16':
        case 'u32':
        case 'u64':
        case 'usize':
            return 'int';
        case 'f32':
            return 'f32';
        case 'f64':
            return 'float';
        case 'bool':
            return 'int';
        case 'char':
            return 'int';
        case 'String':
        case '&str':
            return 'string';
        case '()':
            return '';
        default:
            return rustType;
    }
}

// Export functions for use in other files
if (typeof module !== 'undefined' && module.exports) {
    module.exports = {
        convertRustToBau,
        convertRustLine,
        convertBauType
    };
} else if (typeof window !== 'undefined') {
    window.convertRustToBau = convertRustToBau;
    window.convertRustLine = convertRustLine;
    window.convertRustBauType = convertBauType;
} 