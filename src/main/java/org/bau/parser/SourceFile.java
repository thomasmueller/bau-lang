package org.bau.parser;

import java.util.ArrayList;
import java.util.HashMap;

//context for formatting and for line numbers
public class SourceFile {

    private final int fileId;
    private final String module;
    private String sourceCode;

    // map from module identifier to full module name
    private HashMap<String, String> imports = new HashMap<>();
    // map from type / method / constant identifier to module identifier
    private HashMap<String, String> importEntries = new HashMap<>();
    private boolean imported;

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

    public void addImport(String name, String as, ArrayList<String> entries) {
        imports.put(as, name);
        for (String e : entries) {
            importEntries.put(e, name);
        }
    }

    public String getImportModule(String identifier) {
        String m = importEntries.get(identifier);
        if (m == null) {
            m = "";
        }
        return m;
    }

    public String getImport(String as) {
        return imports.get(as);
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

}
