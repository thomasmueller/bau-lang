/**
 * Swift to Bau Language Converter
 * Converts Swift source code to Bau language syntax
 */

// Shared post-processing utility access (no global variables to avoid conflicts)

function convertSwiftToBau(swiftCode) {
    const lines = swiftCode.split('\n');
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
        line = convertSwiftLine(line);
        
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
        
        // Preserve original indentation (Swift doesn't have class wrapper like Java)
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

function convertSwiftLine(line) {
    // Skip import statements
    if (line.match(/^import\s+/)) {
        return '';
    }
    
    // Convert main function calls
    if (line.match(/^main\(\s*\)/)) {
        return '';
    }
    
    // Convert function definitions
    if (line.match(/func\s+main\s*\(\s*\)/)) {
        return 'fun main()';
    }
    
    // Convert comments (only first occurrence of // to preserve URLs)
    line = line.replace(/\/\//, '#');
    
    // Convert print statements
    line = line.replace(/print\s*\(/g, 'println(');
    
    // Convert for loops FIRST (before variable declarations)
    // Swift range loops: for i in 0..<n -> for i := until(n)
    line = line.replace(/for\s+(\w+)\s+in\s+0\.\.<?(\w+)/g, 'for $1 := until($2)');
    // Swift closed range: for i in 0...n -> for i := until(n + 1)
    line = line.replace(/for\s+(\w+)\s+in\s+0\.\.\.(\w+)/g, 'for $1 := until($2 + 1)');
    // Other range loops: for i in 1..<n -> for i := range(1, n)
    line = line.replace(/for\s+(\w+)\s+in\s+([1-9]\d*)\.\.<?(\w+)/g, 'for $1 := range($2, $3)');
    line = line.replace(/for\s+(\w+)\s+in\s+([1-9]\d*)\.\.\.(\w+)/g, 'for $1 := range($2, $3 + 1)');
    
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
    line = line.replace(/func\s+(\w+)\s*\(([^)]*)\)\s*(?:->\s*([^{]+))?/g, function(match, funcName, params, returnType) {
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
    // Array declarations: var arr = Array(repeating: 0, count: 10) -> arr : int[10]
    line = line.replace(/var\s+(\w+)\s*=\s*Array\(repeating:\s*[^,]+,\s*count:\s*([^)]+)\)/g, '$1 : int[$2]');
    // Array literals: var arr: [Int] = [1, 2, 3] -> arr : int[] := [1, 2, 3]
    line = line.replace(/var\s+(\w+)\s*:\s*\[(\w+)\]\s*=\s*([^$]+)/g, function(match, varName, swiftType, value) {
        const bauType = convertBauType(swiftType);
        return varName + ' : ' + bauType + '[] := ' + value;
    });
    // Constants: let MAX = 100 -> MAX: 100
    line = line.replace(/let\s+(\w+)\s*=\s*([^$]+)/g, '$1: $2');
    // Mutable variables: var x: Int = 10 -> x := 10
    line = line.replace(/var\s+(\w+)\s*(?::\s*\w+)?\s*=\s*([^$]+)/g, '$1 := $2');
    // Variables without initialization: var x: Int -> x int
    line = line.replace(/var\s+(\w+)\s*:\s*(\w+)\s*$/g, function(match, varName, swiftType) {
        const bauType = convertBauType(swiftType);
        return varName + ' ' + bauType;
    });
    
    // Convert while loops - remove braces
    line = line.replace(/while\s+([^{]+)/g, 'while $1');
    
    // Convert if statements - remove braces
    line = line.replace(/if\s+([^{]+)/g, 'if $1');
    line = line.replace(/else\s+if\s+([^{]+)/g, 'elif $1');
    
    // Convert string interpolation: "Even: \(i)" -> 'Even: ' i
    line = line.replace(/'([^']*?)\\?\\\(([^)]+)\)([^']*?)'/g, "'$1' $2 '$3'");
    
    // Convert increment/decrement (handles variables and array access)
    line = line.replace(/(\w+(?:\[[^\]]+\])*)\s*\+=\s*1/g, '$1 += 1');
    line = line.replace(/(\w+(?:\[[^\]]+\])*)\s*-=\s*1/g, '$1 -= 1');
    
    // Remove semicolons
    line = line.replace(/;$/, '');
    
    return line;
}

function convertBauType(swiftType) {
    switch (swiftType) {
        case 'Int':
        case 'Int8':
        case 'Int16':
        case 'Int32':
        case 'Int64':
        case 'UInt':
        case 'UInt8':
        case 'UInt16':
        case 'UInt32':
        case 'UInt64':
            return 'int';
        case 'Float':
            return 'f32';
        case 'Double':
            return 'float';
        case 'Bool':
            return 'int';
        case 'Character':
            return 'int';
        case 'String':
            return 'string';
        case 'Void':
            return '';
        default:
            return swiftType;
    }
}

// Export functions for use in other files
if (typeof module !== 'undefined' && module.exports) {
    module.exports = {
        convertSwiftToBau,
        convertSwiftLine,
        convertBauType
    };
} else if (typeof window !== 'undefined') {
    window.convertSwiftToBau = convertSwiftToBau;
    window.convertSwiftLine = convertSwiftLine;
    window.convertSwiftBauType = convertBauType;
} 