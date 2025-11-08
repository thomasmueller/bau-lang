package org.bau.parser;

import java.util.Objects;

public class FullName {
    public String module;
    public String name;
    public FullName(String module, String name) {
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

    public String toString() {
        if (module == null) {
            return name;
        }
        return module + "." + name;
    }
}
