/**
 * Go to Bau Language Converter
 * Converts Go source code to Bau language syntax
 */

// Shared post-processing utility access (no global variables to avoid conflicts)

function convertGoToBau(goCode) {
    const lines = goCode.split('\n');
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
        
        // Check if this line starts a multi-line block (ends with "(" and not in comment)
        if (line.endsWith('(') && !isInComment(line)) {
            // Collect the entire multi-line block
            const blockResult = collectMultiLineBlock(lines, i);
            const multiLineBlock = blockResult.block;
            i = blockResult.endIndex; // Skip to after the block
            
            // Convert the entire block as one unit
            const convertedBlock = convertGoMultiLineBlock(multiLineBlock, originalIndent);
            if (convertedBlock !== '') {
                result.push(convertedBlock);
            }
            lastLineWasEmpty = false;
            continue;
        }
        
        // Convert line individually
        line = convertGoLine(line);
        
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
        
        // Preserve original indentation (Go doesn't have class wrapper like Java)
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

// Helper function to check if a line is within a comment
function isInComment(line) {
    // Simple check for single-line comments
    const commentIndex = line.indexOf('//');
    const parenIndex = line.lastIndexOf('(');
    return commentIndex !== -1 && commentIndex < parenIndex;
}

// Helper function to collect multi-line blocks starting with "(" until matching ")"
function collectMultiLineBlock(lines, startIndex) {
    const block = [];
    let parenCount = 0;
    let i = startIndex;
    
    // Count opening parentheses in the first line
    const firstLine = lines[i].trim();
    for (let char of firstLine) {
        if (char === '(') parenCount++;
        else if (char === ')') parenCount--;
    }
    block.push(lines[i]);
    i++;
    
    // Continue until we find the matching closing parenthesis
    while (i < lines.length && parenCount > 0) {
        const line = lines[i].trim();
        
        // Skip comment lines completely
        if (line.startsWith('//')) {
            block.push(lines[i]);
            i++;
            continue;
        }
        
        // Count parentheses in this line
        for (let char of line) {
            if (char === '(') parenCount++;
            else if (char === ')') parenCount--;
        }
        
        block.push(lines[i]);
        i++;
    }
    
    return {
        block: block,
        endIndex: i - 1 // Return the last index of the block
    };
}

// Helper function to convert multi-line blocks
function convertGoMultiLineBlock(blockLines, originalIndent) {
    // Join all lines and check what type of block this is
    const fullBlock = blockLines.map(line => line.trim()).join(' ');
    
    // Handle import blocks: import ( ... ) -> skip entirely
    if (fullBlock.startsWith('import (')) {
        return ''; // Skip import blocks completely
    }
    
    // Handle function signatures with parameters
    if (fullBlock.includes('func ')) {
        return convertGoMultiLineFunction(blockLines, originalIndent);
    }
    
    // For other multi-line constructs, convert each line individually and reassemble
    return convertGoGenericMultiLineBlock(blockLines, originalIndent);
}

// Helper function to convert multi-line function signatures
function convertGoMultiLineFunction(blockLines, originalIndent) {
    // Reconstruct the function signature as a single line
    const fullSignature = blockLines.map(line => line.trim()).join(' ');
    
    // Convert the function signature
    let converted = convertGoLine(fullSignature);
    
    if (converted !== '') {
        return originalIndent + converted;
    }
    return '';
}

// Helper function to convert generic multi-line blocks
function convertGoGenericMultiLineBlock(blockLines, originalIndent) {
    const convertedLines = [];
    
    for (let line of blockLines) {
        const trimmed = line.trim();
        if (trimmed === '' || trimmed === '(' || trimmed === ')') {
            continue; // Skip empty lines and bare parentheses
        }
        
        const converted = convertGoLine(trimmed);
        if (converted !== '') {
            // Preserve relative indentation within the block
            const lineIndent = line.match(/^ */)[0];
            convertedLines.push(lineIndent + converted);
        }
    }
    
    return convertedLines.join('\n');
}

function convertGoLine(line) {
    // Skip package declarations
    if (line.match(/^package\s+/)) {
        return '';
    }
    
    // Skip import statements
    if (line.match(/^import\s+/)) {
        return '';
    }
    
    // Convert main function
    if (line.match(/func\s+main\s*\(\s*\)/)) {
        return 'fun main()';
    }
    
    // Convert comments (only first occurrence of // to preserve URLs)
    line = line.replace(/\/\//, '#');
    
    // Convert fmt.Printf/Println
    line = line.replace(/fmt\.Printf\s*\(/g, 'print(');
    line = line.replace(/fmt\.Println\s*\(/g, 'println(');
    line = line.replace(/fmt\.Print\s*\(/g, 'print(');
    
    // Convert for loops FIRST (before variable declarations)
    // Go range loops: for i := 0; i < n; i++ -> for i := until(n)
    line = line.replace(/for\s+(\w+)\s*:=\s*0\s*;\s*\1\s*<\s*([^;]+)\s*;\s*\1\+\+\s*/g, 'for $1 := until($2)');
    line = line.replace(/for\s+(\w+)\s*:=\s*0\s*;\s*\1\s*<=\s*([^;]+)\s*;\s*\1\+\+\s*/g, 'for $1 := until($2 + 1)');
    // Other range loops
    line = line.replace(/for\s+(\w+)\s*:=\s*([1-9]\d*)\s*;\s*\1\s*<\s*([^;]+)\s*;\s*\1\+\+\s*/g, 'for $1 := range($2, $3)');
    line = line.replace(/for\s+(\w+)\s*:=\s*([1-9]\d*)\s*;\s*\1\s*<=\s*([^;]+)\s*;\s*\1\+\+\s*/g, 'for $1 := range($2, $3 + 1)');
    
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
    line = line.replace(/func\s+(\w+)\s*\(([^)]*)\)\s*([^{]*)/g, function(match, funcName, params, returnType) {
        let convertedReturnType = '';
        if (returnType.trim()) {
            convertedReturnType = convertBauType(returnType.trim());
        }
        
        let convertedParams = '';
        if (params.trim()) {
            const paramList = params.split(',').map(param => {
                const parts = param.trim().split(/\s+/);
                if (parts.length >= 2) {
                    const name = parts[0];
                    const type = convertBauType(parts[1]);
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
    // Array/slice declarations: var arr []int -> arr : int[]
    line = line.replace(/var\s+(\w+)\s+\[\](\w+)/g, function(match, varName, goType) {
        const bauType = convertBauType(goType);
        return varName + ' : ' + bauType + '[]';
    });
    // Array with size: var arr [10]int -> arr : int[10]
    line = line.replace(/var\s+(\w+)\s+\[([^\]]+)\](\w+)/g, function(match, varName, size, goType) {
        const bauType = convertBauType(goType);
        return varName + ' : ' + bauType + '[' + size + ']';
    });
    // Constants: const MAX = 100 -> MAX: 100
    line = line.replace(/const\s+(\w+)\s*=\s*([^$]+)/g, '$1: $2');
    // Short variable declarations: x := 10 (already in Bau format)
    // Long variable declarations: var x int = 10 -> x := 10
    line = line.replace(/var\s+(\w+)\s+\w+\s*=\s*([^$]+)/g, '$1 := $2');
    // Variable declarations without initialization: var x int -> x int
    line = line.replace(/var\s+(\w+)\s+(\w+)\s*$/g, function(match, varName, goType) {
        const bauType = convertBauType(goType);
        return varName + ' ' + bauType;
    });
    
    // Convert while-like loops (Go for loops without init/post)
    line = line.replace(/for\s+([^{]+)/g, 'while $1');
    
    // Convert if statements - remove parentheses (Go doesn't require them anyway)
    line = line.replace(/if\s+([^{]+)/g, 'if $1');
    line = line.replace(/else\s+if\s+([^{]+)/g, 'elif $1');
    
    // Convert increment/decrement (handles variables and array access)
    line = line.replace(/(\w+(?:\[[^\]]+\])*)\+\+/g, '$1 += 1');
    line = line.replace(/(\w+(?:\[[^\]]+\])*)\-\-/g, '$1 -= 1');
    
    // Remove semicolons
    line = line.replace(/;$/, '');
    
    return line;
}

function convertBauType(goType) {
    switch (goType) {
        case 'int':
        case 'int8':
        case 'int16':
        case 'int32':
        case 'int64':
            return 'int';
        case 'float32':
            return 'f32';
        case 'float64':
            return 'float';
        case 'string':
            return 'string';
        case 'bool':
            return 'int';
        case 'byte':
            return 'i8';
        case 'rune':
            return 'int';
        default:
            return goType;
    }
}

// Export functions for use in other files
if (typeof module !== 'undefined' && module.exports) {
    module.exports = {
        convertGoToBau,
        convertGoLine,
        convertBauType
    };
} else if (typeof window !== 'undefined') {
    window.convertGoToBau = convertGoToBau;
    window.convertGoLine = convertGoLine;
    window.convertGoBauType = convertBauType;
} 