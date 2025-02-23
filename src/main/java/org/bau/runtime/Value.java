package org.bau.runtime;

import java.util.HashMap;

import org.bau.parser.NumberValue;

public class Value {

    public Value get(int index) {
        throw new IllegalStateException("Not an array");
    }

    public Number get() {
        throw new IllegalStateException("Not a number");
    }

    public int intValue() {
        return get().intValue();
    }

    public long longValue() {
        return get().longValue();
    }

    public double doubleValue() {
        return get().doubleValue();
    }

    public Value len() {
        return null;
    }

    public void set(int index, Value val) {
        throw new IllegalStateException("Not an array");
    }

    public boolean isArray() {
        return false;
    }

    public String print() {
        return toString();
    }

    public static class ValueI8 extends Value {
        private byte value;
        public ValueI8(byte value) {
            this.value = value;
        }
        public Number get() {
            return value;
        }
        public void set(Number v) {
            this.value = v.byteValue();
        }
        public String toString() {
            return String.valueOf(value);
        }
    }

    public static class ValueI32 extends Value {
        private int value;

        public ValueI32(int value) {
            this.value = value;
        }

        public Number get() {
            return value;
        }
        public void set(Number v) {
            this.value = v.intValue();
        }
        public String toString() {
            return String.valueOf(value);
        }

    }

    public static class ValueI16 extends Value {
        private short value;

        public ValueI16(short value) {
            this.value = value;
        }

        public Number get() {
            return value;
        }
        public void set(Number v) {
            this.value = v.shortValue();
        }
        public String toString() {
            return String.valueOf(value);
        }

    }

    public static class ValueNull extends Value {
        public final static ValueNull INSTANCE = new ValueNull();
        private ValueNull() {
        }
        public Number get() {
            return 0;
        }
    }

    public static class ValueException extends Value {
        public String message;
        public ValueException(String message) {
            this.message = message;
        }
        public String toString() {
            return "Exception: " + message;
        }
    }

    public static class ValuePanic extends Value {
        public String message;
        public ValuePanic(String message) {
            this.message = message;
        }
        public String toString() {
            return "Panic: " + message;
        }
    }

    public static class ValueRef extends Value {
        private long heapId;

        public static final ValueRef NULL = new ValueRef(0);

        public ValueRef(long heapId) {
            this.heapId = heapId;
        }
        public Number get() {
            return heapId;
        }
        public void set(Number v) {
            this.heapId = v.longValue();
        }
        public String print() {
            return String.valueOf("*" + heapId);
        }
        public String toString() {
            return String.valueOf("*" + heapId);
        }
    }

    public static class ValueInt extends Value {
        private long value;

        public static final ValueInt ZERO = new ValueInt(0);
        public static final ValueInt ONE = new ValueInt(1);

        public ValueInt(long value) {
            this.value = value;
        }
        public Number get() {
            return value;
        }
        public void set(Number v) {
            this.value = v.longValue();
        }
        public String print() {
            return String.valueOf(value);
        }
        public String toString() {
            return NumberValue.toC(value);
        }
    }

    public static class ValueFloat extends Value {
        private double value;

        public ValueFloat(double value) {
            this.value = value;
        }
        public Number get() {
            return value;
        }
        public int intValue() {
            if (Double.isNaN(value)) {
                return 0;
            } else if (value == Double.POSITIVE_INFINITY) {
                return Integer.MAX_VALUE;
            } else if (value == Double.NEGATIVE_INFINITY) {
                return Integer.MIN_VALUE;
            }
            return get().intValue();
        }

        public long longValue() {
            if (Double.isNaN(value)) {
                return 0;
            } else if (value == Double.POSITIVE_INFINITY) {
                return Long.MAX_VALUE;
            } else if (value == Double.NEGATIVE_INFINITY) {
                return Long.MIN_VALUE;
            }
            return get().longValue();
        }
        public void set(Number v) {
            this.value = v.floatValue();
        }
        public String toString() {
            return NumberValue.toC(value);
        }
        public double doubleValue() {
            return value;
        }
    }

    public static class ValueArray extends Value {
        final Value[] array;
        public ValueArray(int len, Value init) {
            this.array = new Value[len];
            for(int i=0; i<len; i++) {
                array[i] = init;
            }
        }
        public Value get(int index) {
            return array[index];
        }
        public void set(int index, Value x) {
            array[index] = x;
        }
        public Value len() {
            return new ValueI32(array.length);
        }

        public boolean isArray() {
            return true;
        }

    }

    public static class ValueI8Array extends Value {
        final byte[] array;
        public ValueI8Array(byte[] array) {
            this.array = array;
        }
        public Value get(int index) {
            return new ValueI8(array[index]);
        }
        public void set(int index, Value b) {
            array[index] = (byte) b.intValue();
        }

        public String toString() {
            StringBuilder buff = new StringBuilder();
            for (int i = 0; i < array.length; i++) {
                if (array[i] == 0) {
                    break;
                }
                buff.append((char) array[i]);
            }
            return buff.toString();
        }

        public Value len() {
            return new ValueI32(array.length);
        }
        public boolean isArray() {
            return true;
        }

    }

    public static class ValueStruct extends Value {
        private final HashMap<String, Value> map = new HashMap<>();

        public Value get(String fieldName) {
            return map.get(fieldName);
        }
        public void set(String fieldName, Value v) {
            map.put(fieldName, v);
        }
        public String toString() {
            return map.toString();
        }
    }

    public static class ValueI32Array extends Value {
        final int[] array;
        public ValueI32Array(int size) {
            array = new int[size];
        }
        public Value get(int index) {
            return new ValueI32(array[index]);
        }
        public void set(int index, Value v) {
            array[index] = v.intValue();
        }
        public Value len() {
            return new ValueI32(array.length);
        }
        public boolean isArray() {
            return true;
        }

    }

}
