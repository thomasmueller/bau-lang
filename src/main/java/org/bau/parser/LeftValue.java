package org.bau.parser;

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

    boolean needToDecrementRefCountOnFree();
    void needToDecrementRefCountOnFree(boolean value);

}
