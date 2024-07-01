package org.bau.runtime;

public class Value {

    public Value get(int index) {
        throw new IllegalStateException("Not an array");
    }
    public Number get() {
        throw new IllegalStateException("Not a value");
    }
    
    public int intValue() {
        return get().intValue();
    }

    public long longValue() {
        return get().longValue();
    }
    
    public Value len() {
        return null;
    }
    
    public void set(int index, Number v) {
        throw new IllegalStateException("Not an array");
    }      
    public void set(Number v) {
        throw new IllegalStateException("Not a value");
    }

    public class ValueI8 extends Value {
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
        public String toString() {
            if (value >= Integer.MAX_VALUE || value <= Long.MIN_VALUE) {
                return String.valueOf(value) + "L";
            }
            return String.valueOf(value);
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
        public void set(Number v) {
            this.value = v.longValue();
        }  
        public String toString() {
            return String.valueOf(value);
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
        public void set(int index, Number b) {
            array[index] = b.byteValue();
        }
        public String toString() {
            StringBuilder buff = new StringBuilder();
            for(int i=0; i<array.length; i++) {
                if(array[i] == 0) {
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

    public static class ValueI32Array extends Value {
        final int[] array;
        ValueI32Array(int size) {
            array = new int[size];
        }
        public Value get(int index) {
            return new ValueI32(array[index]);
        }
        public void set(int index, int v) {
            array[index] = v;
        }  
        public Value len() {
            return new ValueI32(array.length);
        }
        public boolean isArray() {
            return true;
        }
        
    }

    public boolean isArray() {
        return false;
    }

}
