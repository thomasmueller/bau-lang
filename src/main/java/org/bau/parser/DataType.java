package org.bau.parser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

import org.bau.parser.expr.Expression;
import org.bau.parser.expr.New;
import org.bau.parser.expr.NullValue;
import org.bau.parser.expr.NumberValue;
import org.bau.parser.expr.Variable;
import org.bau.runtime.Value;
import org.bau.runtime.Value.ValueNull;

public class DataType {

    public static final String I8 = "i8";
    public static final String I16 = "i16";
    public static final String I32 = "i32";
    public static final String INT = "int";
    public static final String F32 = "f32";
    public static final String FLOAT = "float";
    public static final String TYPE = "type";

    public static final DataType F32_TYPE = newNumberType(DataType.F32, 4);
    public static final DataType FLOAT_TYPE = newNumberType(DataType.FLOAT, 8);
    public static final DataType I8_TYPE = newNumberType(DataType.I8, 1);
    public static final DataType I16_TYPE = newNumberType(DataType.I16, 2);
    public static final DataType I32_TYPE = newNumberType(DataType.I32, 4);
    public static final DataType INT_TYPE = newNumberType(DataType.INT, 8);
    public static final DataType TYPE_TYPE = newNumberType(DataType.TYPE, 8);

    private final FullName fullName;
    private final MemoryType memoryType;

    // number of bytes
    private final int sizeOf;

    // int, float, type, enum
    private final boolean isNumber;
    private final boolean isFloatingPoint;

    // for non-array: the array type
    private final DataType arrayType;
    private final DataType arrayBaseType;

    private final DataType nullableType;
    private DataType notNullType;
    // for array: the base type
    private DataType borrowType;
    private DataType ownerType;

    private final boolean isNullable;

    private Trait traitDefinition;

    public int lineOffset;

    public int fileId, location;

    public List<Variable> fields = new ArrayList<>();

    public LinkedHashMap<String, Long> enumValues;

    public FunctionDefinition autoClose;
    Expression maxValue;

    // templates
    public ArrayList<String> parameters;
    public String template;

    // function pointers
    public boolean isFunctionPointer;
    public ArrayList<DataType> functionPointerArgs;
    public DataType functionPointerReturnType;

    // trait specific
    public ArrayList<FullName> traitNames = new ArrayList<>();
    public ArrayList<DataType> traitTypes = new ArrayList<>();
    public HashSet<DataType> implementingTypes = new HashSet<>();

    public static boolean isGenericTypeName(String token) {
        while (token.endsWith("[]")) {
            token = token.substring(0, token.length() - 2);
        }
        return token != null && !token.isEmpty() &&
                token.length() <= 2 &&
                token.charAt(0) >= 'A' && token.charAt(0) <= 'Z' &&
                token.toUpperCase().equals(token);
    }

    public static DataType newUndefined(FullName fullName) {
        DataType result = newNonArray(fullName, 0, MemoryType.UNDEFINED);
        return result;
    }

    public static DataType newTraitType(FullName fullName, MemoryType memoryType) {
        return new DataType(fullName, 0, false, null, false, memoryType);
    }

    public static DataType newNumberType(String name, int sizeOf) {
        return new DataType(new FullName("", name), sizeOf, true, null, false, MemoryType.COPY);
    }

    public static DataType newBuiltIn(String name, int sizeOf) {
        return newNonArray(new FullName("", name), sizeOf, MemoryType.COPY);
    }

    public static DataType newEnumType(FullName fullName) {
        return new DataType(fullName, 8, true, null, false, MemoryType.COPY);
    }

    public static DataType newRegularType(FullName fullName, int sizeOf, MemoryType memoryType) {
        if (memoryType == MemoryType.BORROW) {
            throw new IllegalArgumentException();
        }
        return newNonArray(fullName, sizeOf, memoryType);
    }

    private static DataType newNonArray(FullName fullName, int sizeOf, MemoryType memoryType) {
        if (memoryType == MemoryType.BORROW) {
            throw new IllegalArgumentException();
        }
        return new DataType(fullName, sizeOf, false, null, false, memoryType);
    }

    public static DataType newFunctionPointer(String module, ArrayList<DataType> params, DataType returnType) {
        DataType r = newNonArray(new FullName(module, "fun"), 0, MemoryType.COPY);
        r.isFunctionPointer = true;
        r.functionPointerArgs = params;
        r.functionPointerReturnType = returnType;
        return r;
    }

    public void setLocation(int fileId, int location) {
        this.fileId = fileId;
        this.location = location;
    }

    public int hashCode() {
        return format().hashCode();
    }

    public boolean equals(DataType other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }
        // function pointer types are not equal
        return this.format().equals(other.format());
    }

    public Expression nullExpression() {
        if (isNumber) {
            return NumberValue.ZERO;
        } else if (!isPointer()) {
            return new New(this, null);
        } else if (isArray()) {
            return new New(this, NumberValue.ZERO);
        } else {
            return new NullValue(this);
        }
    }

    private DataType(FullName fullName, int sizeOf, boolean isNumber, DataType arrayBaseType, boolean isNullable, MemoryType memoryType) {
        this.isNullable = isNullable;
        this.memoryType = memoryType;
        this.fullName = fullName;
        this.sizeOf = sizeOf;
        this.isNumber = isNumber;
        this.arrayBaseType = arrayBaseType;
        if (isNumber) {
            isFloatingPoint = fullName.name.charAt(0) == 'f';
        } else {
            isFloatingPoint = false;
        }
        if (!isArray() && memoryType != MemoryType.COPY && !isNullable) {
            nullableType = new DataType(fullName, sizeOf, false, null, true, memoryType);
            nullableType.fields = fields;
            nullableType.notNullType = this;
        } else {
            nullableType = null;
        }
        if (!isArray()) {
            arrayType = new DataType(new FullName(fullName.module, fullName.name + "[]"), sizeOf, false, this, isNullable, MemoryType.REF_COUNT);
        } else {
            arrayType = this;
        }
    }

    void addFields(List<Variable> fields) {
        this.fields.addAll(fields);
    }

    public void used(Program program) {
        if (DataType.isGenericTypeName(fullName.name)) {
            throw new IllegalStateException();
        }
        program.usedTypes.add(this);
        if (!traitNames.isEmpty()) {
            if (traitTypes.isEmpty()) {
                for (FullName n : traitNames) {
                    DataType tm = program.getType(n.module, n.name);
                    tm.implementingTypes.add(this);
                    traitTypes.add(tm);
                }
            }
        }
        if (isArray()) {
            arrayBaseType.used(program);
        }
        if (autoClose != null) {
            program.getFunctionById(autoClose.getFunctionId()).used(program);
        }
    }

    public boolean isNumber() {
        return isNumber;
    }

    public boolean isFloatingPoint() {
        return isFloatingPoint;
    }

    public boolean isCopyType() {
        return memoryType == MemoryType.COPY;
    }

    public FullName getFullName() {
        return fullName;
    }

    public String idC() {
        String n = fullName.name;
        if (isArray()) {
            n = arrayBaseType.idC() + "_array";
        }
        if (memoryType == MemoryType.OWNER || memoryType == MemoryType.BORROW) {
            n += "_owned";
        }
        return n;
    }

    public String getCamelCaseName() {
        String result = fullName.name;
        char firstChar = fullName.name.charAt(0);
        if (firstChar >= 'a' && firstChar <= 'z') {
            result = (char)(firstChar - 'a' + 'A') + result.substring(1);
        }
        if (result.endsWith("[]")) {
            result = result.substring(0, result.length() - 2) + "Array";
        }
        return result;
    }

    public String module() {
        return fullName.module;
    }

    public String name() {
        return fullName.name;
    }

    public int sizeOf() {
        return sizeOf;
    }

    public DataType baseType() {
        if (!isArray()) {
            throw new IllegalStateException();
        }
        return arrayBaseType;
    }

    public DataType arrayType() {
        if (isArray()) {
            throw new IllegalStateException();
        }
        return arrayType;
    }

    public String format() {
        StringBuilder buff = new StringBuilder();
        if (traitDefinition != null) {
            buff.append(fullName.getFullName());
            return buff.toString();
        }
        if (isFunctionPointer) {
            buff.append("fun(");
            for (int i = 0; i < functionPointerArgs.size(); i++) {
                if (i > 0) {
                    buff.append(", ");
                }
                buff.append(functionPointerArgs.get(i).format());
            }
            buff.append(")");
            if (functionPointerReturnType != null) {
                buff.append(' ').append(functionPointerReturnType.format());
            }
            return buff.toString();
        }
        buff.append(fullName.name);
        if (parameters != null) {
            buff.append('(');
            int i = 0;
            for (String p : parameters) {
                if (i > 0) {
                    buff.append(", ");
                }
                i++;
                buff.append(p);
            }
            buff.append(')');
        }
        if (isNullable) {
            buff.append("?");
        }
        return buff.toString();
    }

    public String nameC() {
        String s;
        if ("i8".equals(fullName.name)) {
            s = "int8_t";
        } else if ("i16".equals(fullName.name)) {
            s = "int16_t";
        } else if ("i32".equals(fullName.name)) {
            s = "int32_t";
        } else if ("int".equals(fullName.name)) {
            s = "int64_t";
        } else if ("f32".equals(fullName.name)) {
            s = "float";
        } else if ("float".equals(fullName.name)) {
            s = "double";
        } else if (fullName.name.startsWith("0..")) {
            s = "int64_t";
        } else if (enumValues != null) {
            s = "int64_t";
        } else {
            String n = fullName.name;
            if (!fullName.module.isEmpty()) {
                s = fullName.getEscapedModuleId() + "_" + n;
            } else {
                s = n;
            }
        }
        if (isArray()) {
            s = s.replace("[]", "_array");
        }
        if (memoryType == MemoryType.OWNER || memoryType == MemoryType.BORROW) {
            s += "_owned";
        }
        return s;
    }

    public String toC() {
        Utils.assertTrue(memoryType != MemoryType.UNDEFINED);
        if (isFunctionPointer) {
            throw new IllegalStateException();
        }
        String s = nameC();
        return (isPointer() || isArray()) ? s + "*" : s;
    }

    public DataType getFieldDataType(String f) {
        // TODO use a map, not a loop
        for (Variable v : fields) {
            if (v.name().equals(f)) {
                return v.type();
            }
        }
        return null;
    }

    public boolean needFree() {
        if (isFunctionPointer) {
            return false;
        }
        return !isNumber;
    }

    public boolean needIncDec() {
        return isPointer() | isArray();
    }

    public boolean isPointer() {
        return memoryType != MemoryType.COPY;
    }

    public boolean isArray() {
        return arrayBaseType != null;
    }

    public DataType orNull() {
        if (isNullable) {
            return this;
        }
        return nullableType;
    }

    public DataType notNullType() {
        if (!isNullable) {
            return this;
        }
        return notNullType;
    }

    public MemoryType memoryType() {
        return memoryType;
    }

    public boolean isRange() {
        return maxValue != null;
    }

    public DataType ownerType() {
        if (memoryType == MemoryType.OWNER) {
            return this;
        } else if (memoryType == MemoryType.BORROW) {
            if (ownerType == null) {
                throw new IllegalStateException();
            }
            return ownerType;
        } else {
            throw new IllegalStateException();
        }
    }

    public DataType borrowType() {
        if (memoryType == MemoryType.BORROW) {
            return this;
        } else if (memoryType == MemoryType.OWNER) {
            if (borrowType == null) {
                borrowType = new DataType(fullName, sizeOf, false, null, false, MemoryType.BORROW);
                borrowType.ownerType = this;
                borrowType.fields = fields;
            }
            return borrowType;
        } else {
            throw new IllegalStateException();
        }

    }

    public boolean isNullable() {
        return isNullable;
    }

    public void setTraitDefinition(Trait trait) {
        this.traitDefinition = trait;
    }

    public Trait getTraitDefinition() {
        if (traitDefinition != null) {
            return traitDefinition;
        }
        if (notNullType != null && notNullType.getTraitDefinition() != null) {
            return notNullType.getTraitDefinition();
        }
        if (ownerType != null) {
            return ownerType.getTraitDefinition();
        }
        return null;
    }

    public static String getId(String name, ArrayList<DataType> params) {
        StringBuilder buff = new StringBuilder();
        buff.append(name);
        for (DataType t : params) {
            buff.append('_');
            buff.append(t.getFullName().getFullTypeId());
        }
        return buff.toString();
    }

    public DataType resolveEnumType() {
        if (enumValues != null) {
            return DataType.INT_TYPE;
        }
        return this;
    }

    public Value getZeroValue() {
        switch(fullName.name) {
        case I8:
            return new Value.ValueI8((byte) 0);
        case I16:
            return new Value.ValueI16((short) 0);
        case I32:
            return new Value.ValueI32(0);
        case INT:
            return new Value.ValueInt(0);
        case F32:
            return new Value.ValueFloat(0);
        case FLOAT:
            return new Value.ValueFloat(0);
        }
        return ValueNull.INSTANCE;
    }

    public int getTraitSlot() {
        return traitDefinition.getSlot();
    }

    public boolean isTrait() {
        if (traitDefinition != null) {
            return true;
        }
        if (ownerType != null && ownerType.isTrait()) {
            return true;
        }
        if (notNullType != null && notNullType.isTrait()) {
            return true;
        }
        if (isArray()) {
            return arrayBaseType.isTrait();
        }
        return false;
    }

    public boolean implementsTrait(DataType traitType) {
        for (DataType t : traitTypes) {
            if (t.equals(traitType)) {
                return true;
            }
        }
        for (FullName t : traitNames) {
            if (t.equals(traitType.getFullName())) {
                return true;
            }
        }
        return false;
    }

    public String[] getFieldNames() {
        String[] result = new String[fields.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = fields.get(i).name();
        }
        return result;
    }

    public String[] getFieldTypes() {
        String[] result = new String[fields.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = fields.get(i).type().getFullName().getFullName();
        }
        return result;
    }

    public void resolveTypes(Program program) {
        for (int i = 0; i < fields.size(); i++) {
            Variable v = fields.get(i);
            v.resolveTypes(program);
        }
    }

    public DataType resolve(Program program) {
        if (memoryType == MemoryType.UNDEFINED) {
            DataType t2 = program.getType(module(), name());
            if (isNullable) {
                return t2.orNull();
            } else if (isArray()) {
                return t2.arrayType();
            }
            return t2;
        }
        return this;
    }

}
