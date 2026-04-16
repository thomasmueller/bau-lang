package org.bau.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;

//context for formatting and for line numbers
public class SourceFile {

    private static final int MAX_ERRORS = 50;
    private final int fileId;
    private final String module;
    private String sourceCode;

    // map from module identifier to full module name
    private HashMap<String, String> imports = new HashMap<>();
    // map from type / method / constant identifier to module identifier
    private HashMap<String, String> importedSymbols = new HashMap<>();
    private TreeMap<Integer, Object> elements = new TreeMap<>();
    private TreeMap<Integer, String> errors = new TreeMap<>();
    private boolean imported;
    private int errorCount;

    SourceFile(int fileId, String module, String sourceCode) {
        Utils.assertTrue(module != null);
        this.fileId = fileId;
        this.module = module;
        this.sourceCode = sourceCode;
    }

    public String formatDataType(FullName fullName) {
        if (fullName.module == null || fullName.module.isEmpty()) {
            return fullName.name;
        }
        return fullName.module + "." + fullName.name;
    }

    public String formatFunctionName(FullName fullName) {
        if (fullName.module == null || fullName.module.isEmpty()) {
            return fullName.name;
        }
        return fullName.module + "." + fullName.name;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public int getFileId() {
        return fileId;
    }

    public String format() {
        return sourceCode;
    }

    // eg. import com.acme.collections: acmeCollections { List; sort }
    // modulePath: com.acme.collection
    // moduleAlias: acmeCollections (or collections if not set)
    // importedSymbolList: List, sort
    public void addImport(String modulePath, String moduleAlias, ArrayList<String> symbolList) {
        imports.put(moduleAlias, modulePath);
        for (String e : symbolList) {
            importedSymbols.put(e, modulePath);
        }
    }

    public String getModulePathForSymbol(String identifier) {
        String m = importedSymbols.get(identifier);
        if (m == null) {
            m = module;
        }
        return m;
    }

    public String getModulePath(String moduleAlias) {
        return imports.get(moduleAlias);
    }

    public boolean isImported() {
        return imported;
    }

    public void setImported(boolean imported) {
        this.imported = imported;
    }

    public String toString() {
        return module;
    }

    public void setLocation(int location, Object obj) {
        elements.put(location, obj);
    }

    public Object getLocation(int location) {
        Entry<Integer, Object> e = elements.floorEntry(location);
        return e == null ? null : e.getValue();
    }

    public int getLocation(int line, int character) {
        int pos = 0;
        int l = 0;
        int c = 0;
        while (l < line || (l == line && c < character)) {
            if (pos >= sourceCode.length()) {
                return -1;
            }
            int ch = sourceCode.charAt(pos);
            if (ch == '\n') {
                l++;
                c = 0;
            } else {
                c++;
            }
            pos++;
        }
        return pos;
    }

    public String getLocations() {
        return elements.toString();
    }

    public static int getLine(String source, int pos) {
        int line = 1;
        for (int i = 0; i < pos; i++) {
            if (source.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    public void syntaxError(int location, String message) {
        location = Math.min(location, sourceCode.length() - 1);
        int lineStart = location;
        while (lineStart > 0 && sourceCode.charAt(lineStart - 1) != '\n') {
            lineStart--;
        }
        int line = getLine(sourceCode, location);
        message += " at line " + line + ":\n";
        int lineEnd = sourceCode.indexOf('\n', location);
        if (lineEnd < 0) {
            lineEnd = sourceCode.length();
        }
        message += sourceCode.substring(lineStart, lineEnd) + "\n";
        message += " ".repeat(location - lineStart);
        message += "^";
        errors.put(location, message);
        errorCount++;
        if (errorCount > MAX_ERRORS) {
            // too many errors: throw the first
            throw new IllegalStateException(getErrors());
        }
    }

    public String getErrors() {
        if (errors.isEmpty()) {
            return null;
        }
        StringBuilder buff = new StringBuilder();
        for (String s : errors.values()) {
            buff.append(s);
            buff.append("\n");
        }
        return buff.toString();
    }

}
