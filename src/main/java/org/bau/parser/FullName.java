package org.bau.parser;

import java.util.Locale;
import java.util.Objects;

public class FullName {

    public final String module;
    public final String name;

    public FullName(String module, String name) {
        Utils.assertTrue(module != null);
        if (DataType.isGenericTypeName(name)) {
            Utils.assertTrue(module.isEmpty());
        }
        this.module = module;
        this.name = name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(module, name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FullName other = (FullName) obj;
        return Objects.equals(module, other.module) && Objects.equals(name, other.name);
    }

    public String getFullName() {
        if (module.isEmpty()) {
            return name;
        }
        return module + "." + name;
    }

    public String getFullTypeId() {
        return getFullName().replace('.', '_').replace("[]", "_array");
    }

    public String getEscapedModuleId() {
        return FullName.esc(module).replace(".", "_");
    }

    public static String esc(String identifier) {
        if (identifier.length() == 1) {
            return identifier;
        }
        if (identifier.startsWith("0")) {
            return "_" + identifier.substring(1);
        }
        int underscoreIndex = identifier.indexOf('_', 1);
        if (underscoreIndex > 0) {
            if (identifier.toUpperCase(Locale.ENGLISH).equals(identifier)) {
                // constants
                return identifier;
            }
            identifier = identifier.replace("_", "__");
        }
        if (identifier.startsWith("_")) {
            if (identifier.charAt(1) <= 'Z') {
                // uppercase
                return identifier;
            }
            if (identifier.equals("_next")) {
                return identifier;
            }
            // user defined
            return "_u" + identifier;
        }
        if (identifier.indexOf('_') > 0) {
            return identifier;
        }
        return identifier;
        // TODO this needs some more work
        /*
        switch (identifier) {
        case "code":
        case "copyLen":
        case "data":
        case "dateTime":
        case "day":
        case "exceptionType":
        case "File":
        case "filePointer":
        case "hour":
        case "idx":
        case "index":
        case "i32":
        case "i32_array":
        case "len":
        case "mode":
        case "month":
        case "minute":
        case "millis":
        case "newLen":
        case "name":
        case "pos":
        case "result":
        case "second":
        case "shiftLeft":
        case "shiftRight":
        case "this":
        case "year":
            return identifier;
        }
        return "b_" + identifier;
        */
    }
}
