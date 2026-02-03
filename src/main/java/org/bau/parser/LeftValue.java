package org.bau.parser;

import org.bau.runtime.Memory;
import org.bau.runtime.Value;

/**
 * An assignment target
 */
public interface LeftValue extends Expression {

    String assignmentC();

    Expression replace(Variable old, Expression with);

    String decrementRefCountC();
    String incrementRefCountC();

    Value setValue(Memory memory, Value val, boolean incRefCount, boolean initial);

    boolean isContant();

    void incrementReassignCount();

}
