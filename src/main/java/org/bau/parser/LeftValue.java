package org.bau.parser;

import org.bau.runtime.Memory;
import org.bau.runtime.Value;

/**
 * An assignment target
 */
public interface LeftValue extends Expression {

    String assignmentC();

    Expression replace(Variable old, Expression with);

    void setBoundValue(Expression scope, String modify, Expression value);

    void addBoundCondition(Expression scope, String operation, Expression right);

    String decrementRefCountC();
    String incrementRefCountC();

    Value setValue(Memory memory, Value val, boolean incRefCount);

}
