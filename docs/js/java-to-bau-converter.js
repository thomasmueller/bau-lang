/**
 * Java to Bau Language Converter
 * Converts Java source code to Bau language syntax
 */

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
        line = convertLine(line);
        
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
    
    return result.join('\n');
}

function convertLine(line) {
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
        const convertedReturnType = convertBauType(returnType);
        
        // Convert parameters
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
        
        // Add return type only if not void
        const returnTypePart = convertedReturnType ? ' ' + convertedReturnType : '';
        return 'fun ' + methodName + '(' + convertedParams + ')' + returnTypePart;
    });
    
    // Convert variable declarations (after function declarations)
    // Array declarations: var perm1 = new int[n]; -> perm1 : int[n]
    line = line.replace(/\bvar\s+(\w+)\s*=\s*new\s+(\w+)\[([^\]]+)\]\s*;?/g, function(match, varName, javaType, size) {
        const bauType = convertBauType(javaType);
        return varName + ' : ' + bauType + '[' + size + ']';
    });
    // Final variables (constants)
    line = line.replace(/\bfinal\s+\w+\s+(\w+)\s*=\s*([^;]+);?/g, '$1: $2');
    // Regular variables with initialization
    line = line.replace(/\b(int|long|float|double|boolean|char|byte|short|String)\s+(\w+)\s*=\s*([^;]+);?/g, '$2 := $3');
    // Variables without initialization
    line = line.replace(/\b(int|long|float|double|boolean|char|byte|short|String)\s+(\w+)\s*;?/g, '$2 $1');
    
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

function convertBauType(javaType) {
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
        convertLine,
        convertBauType
    };
} else if (typeof window !== 'undefined') {
    // Browser environment - make functions globally available
    window.convertJavaToBau = convertJavaToBau;
    window.convertLine = convertLine;
    window.convertBauType = convertBauType;
} 