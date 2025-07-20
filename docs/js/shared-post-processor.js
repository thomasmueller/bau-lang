/**
 * Shared Post-Processing Utility for Bau Code Generation
 * 
 * This module provides common post-processing functions that can be applied
 * to generated Bau code from any source language converter.
 */

/**
 * Post-processes generated Bau code to clean up formatting issues
 * @param {string} bauCode - The generated Bau code to clean up
 * @returns {string} - The cleaned up Bau code
 */
function postProcessBauCode(bauCode) {
    if (!bauCode || typeof bauCode !== 'string') {
        return '';
    }
    
    let cleaned = bauCode;
    
    // 1. Remove multiple consecutive newlines (keep at most one empty line)
    // Replace 3+ newlines with exactly 2 newlines (one empty line)
    cleaned = cleaned.replace(/\n{3,}/g, '\n\n');
    
    // 2. Trim leading and trailing whitespace
    cleaned = cleaned.trim();
    
    // 3. Ensure file ends with exactly one newline (if not empty)
    if (cleaned.length > 0 && !cleaned.endsWith('\n')) {
        cleaned += '\n';
    }
    
    return cleaned;
}

/**
 * Removes excessive consecutive newlines specifically
 * @param {string} text - Text to clean
 * @returns {string} - Text with normalized newlines
 */
function normalizeNewlines(text) {
    if (!text) return '';
    
    // Replace multiple consecutive newlines with at most 2 newlines (one empty line)
    return text.replace(/\n{3,}/g, '\n\n');
}

/**
 * Removes leading and trailing empty lines while preserving internal structure
 * @param {string} text - Text to trim
 * @returns {string} - Trimmed text
 */
function trimEmptyLines(text) {
    if (!text) return '';
    
    const lines = text.split('\n');
    
    // Remove leading empty lines
    let startIndex = 0;
    while (startIndex < lines.length && lines[startIndex].trim() === '') {
        startIndex++;
    }
    
    // Remove trailing empty lines
    let endIndex = lines.length - 1;
    while (endIndex >= 0 && lines[endIndex].trim() === '') {
        endIndex--;
    }
    
    // Return the trimmed lines
    if (startIndex > endIndex) {
        return ''; // All lines were empty
    }
    
    return lines.slice(startIndex, endIndex + 1).join('\n');
}

/**
 * Main entry point for post-processing - applies all cleanup rules
 * @param {string} bauCode - Generated Bau code
 * @param {Object} options - Optional configuration
 * @param {boolean} options.preserveTrailingNewline - Whether to ensure trailing newline (default: true)
 * @param {boolean} options.normalizeNewlines - Whether to normalize consecutive newlines (default: true)
 * @returns {string} - Clean Bau code
 */
function cleanupBauCode(bauCode, options = {}) {
    const {
        preserveTrailingNewline = true,
        normalizeNewlines: shouldNormalizeNewlines = true
    } = options;
    
    if (!bauCode || typeof bauCode !== 'string') {
        return '';
    }
    
    let result = bauCode;
    
    // Apply normalizations
    if (shouldNormalizeNewlines) {
        result = normalizeNewlines(result);
    }
    
    // Trim excessive whitespace
    result = trimEmptyLines(result);
    
    // Ensure proper ending
    if (preserveTrailingNewline && result.length > 0 && !result.endsWith('\n')) {
        result += '\n';
    }
    
    return result;
}

// Export functions for use in other modules
if (typeof module !== 'undefined' && module.exports) {
    // Node.js environment
    module.exports = {
        postProcessBauCode,
        normalizeNewlines,
        trimEmptyLines,
        cleanupBauCode
    };
} else if (typeof window !== 'undefined') {
    // Browser environment
    window.postProcessBauCode = postProcessBauCode;
    window.normalizeNewlines = normalizeNewlines;
    window.trimEmptyLines = trimEmptyLines;
    window.cleanupBauCode = cleanupBauCode;
} 