/**
 * Python to Bau Language Converter
 * Converts Python source code to Bau language syntax
 */

// Shared post-processing utility access (no global variables to avoid conflicts)

function convertPythonToBau(pythonCode) {
    const lines = pythonCode.split('\n');
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
        line = convertPythonLine(line);
        
        // Skip if line becomes empty after conversion
        if (line === '') {
            continue;
        }
        
        // Python doesn't use braces, so no need to handle them
        
        // Preserve original indentation (Python doesn't have class wrapper like Java)
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

function convertPythonLine(line) {
    // Skip import statements
    if (line.match(/^(import|from)\s+/)) {
        return '';
    }
    
    // Skip if __name__ == "__main__": pattern
    if (line.match(/if\s+__name__\s*==\s*["']__main__["']\s*:/)) {
        return '';
    }
    
    // Convert function definitions
    if (line.match(/def\s+main\s*\(\s*\)\s*:/)) {
        return 'fun main()';
    }
    
    // Comments are already in # format, no conversion needed
    
    // Convert print statements
    line = line.replace(/print\s*\(/g, 'println(');
    
    // Convert for loops FIRST (before variable declarations)
    // Python range loops: for i in range(n): -> for i := until(n)
    line = line.replace(/for\s+(\w+)\s+in\s+range\s*\(\s*([^,)]+)\s*\)\s*:/g, 'for $1 := until($2)');
    // Python range loops with start: for i in range(1, n): -> for i := range(1, n)
    line = line.replace(/for\s+(\w+)\s+in\s+range\s*\(\s*([^,]+)\s*,\s*([^)]+)\s*\)\s*:/g, 'for $1 := range($2, $3)');
    
    // Convert operators
    line = line.replace(/!=/g, '<>');
    line = line.replace(/==/g, '=');
    line = line.replace(/\s+and\s+/g, ' and ');
    line = line.replace(/\s+or\s+/g, ' or ');
    line = line.replace(/\s+not\s+([a-zA-Z_]\w*)/g, ' not $1');
    
    // Convert double quotes to single quotes
    line = line.replace(/"/g, "'");
    
    // Convert boolean literals
    line = line.replace(/\bTrue\b/g, '1');
    line = line.replace(/\bFalse\b/g, '0');
    
    // Convert function declarations
    line = line.replace(/def\s+(\w+)\s*\(([^)]*)\)\s*:/g, function(match, funcName, params) {
        // Python doesn't have explicit types, so we can't convert them
        let convertedParams = '';
        if (params.trim()) {
            // Just keep parameter names as-is since Python is dynamically typed
            convertedParams = params.trim();
        }
        
        return 'fun ' + funcName + '(' + convertedParams + ')';
    });
    
    // Convert variable declarations (Python uses dynamic typing)
    // Python assignments are already close to Bau: x = 10 -> x := 10
    line = line.replace(/(\w+)\s*=\s*([^=][^$]*)/g, '$1 := $2');
    
    // Convert while loops - remove colon
    line = line.replace(/while\s+([^:]+)\s*:/g, 'while $1');
    
    // Convert if statements - remove colon and parentheses if present
    line = line.replace(/if\s+([^:]+)\s*:/g, 'if $1');
    line = line.replace(/elif\s+([^:]+)\s*:/g, 'elif $1');
    line = line.replace(/else\s*:/g, 'else');
    
    // Convert increment/decrement (Python uses += and -=)
    line = line.replace(/(\w+(?:\[[^\]]+\])*)\s*\+=\s*1/g, '$1 += 1');
    line = line.replace(/(\w+(?:\[[^\]]+\])*)\s*-=\s*1/g, '$1 -= 1');
    
    // Handle f-strings and format strings
    line = line.replace(/f['"]([^'"]*)\{([^}]+)\}([^'"]*)['"]/g, "'$1' $2 '$3'");
    line = line.replace(/['"]([^'"]*)['"]\.format\(([^)]+)\)/g, "'$1' $2");
    
    // Remove semicolons (Python rarely uses them, but just in case)
    line = line.replace(/;$/, '');
    
    return line;
}

function convertBauType(pythonType) {
    // Python is dynamically typed, but we can handle some common type hints
    switch (pythonType) {
        case 'int':
            return 'int';
        case 'float':
            return 'float';
        case 'str':
            return 'string';
        case 'bool':
            return 'int';
        case 'list':
            return 'array';
        default:
            return pythonType;
    }
}

// Export functions for use in other files
if (typeof module !== 'undefined' && module.exports) {
    module.exports = {
        convertPythonToBau,
        convertPythonLine,
        convertBauType
    };
} else if (typeof window !== 'undefined') {
    window.convertPythonToBau = convertPythonToBau;
    window.convertPythonLine = convertPythonLine;
    window.convertPythonBauType = convertBauType;
} 