package org.bau.ena.types;

import java.util.List;
import java.util.Map;

public sealed interface Type
        permits Type.Int, Type.Real, Type.Text, Type.Array, Type.Struct, Type.Fun, Type.Void, Type.Unknown {
    record Int() implements Type {
    }

    record Real() implements Type {
    }

    record Text() implements Type {
    }

    record Array(Type elem) implements Type {
    }

    record Struct(String name, Map<String, Type> fields) implements Type {
    }

    record Fun(List<Type> params, Type ret) implements Type {
    }

    record Void() implements Type {
    }

    record Unknown() implements Type {
    }

    static final Int INT = new Int();
    static final Real REAL = new Real();
    static final Text TEXT = new Text();
    static final Void VOID = new Void();
    static final Unknown UNKNOWN = new Unknown();
}
