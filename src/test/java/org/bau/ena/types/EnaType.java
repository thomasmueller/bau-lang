package org.bau.ena.types;

import java.util.List;
import java.util.Map;

public sealed interface EnaType
        permits EnaType.Int, EnaType.Real, EnaType.Text, EnaType.Array, EnaType.Struct, EnaType.Fun, EnaType.Void, EnaType.Unknown {
    record Int() implements EnaType {
    }

    record Real() implements EnaType {
    }

    record Text() implements EnaType {
    }

    record Array(EnaType elem) implements EnaType {
    }

    record Struct(String name, Map<String, EnaType> fields) implements EnaType {
    }

    record Fun(List<EnaType> params, EnaType ret) implements EnaType {
    }

    record Void() implements EnaType {
    }

    record Unknown() implements EnaType {
    }

    static final Int INT = new Int();
    static final Real REAL = new Real();
    static final Text TEXT = new Text();
    static final Void VOID = new Void();
    static final Unknown UNKNOWN = new Unknown();
}
