package org.bau.parser;

import java.util.ArrayList;

import org.bau.runtime.Memory;
import org.bau.runtime.Value;

public interface Expression {

    Value eval(Memory memory);

    DataType type();

    DataType canThrowException();

    Expression replace(Variable old, Expression with);

    Expression simplify();

    String toC();

    boolean isEasyToRead();

    default boolean isComparison() {
        return false;
    }

    Bounds getBounds();

    /**
     * while i<n     -> applyBounds(i<n)
     *   i+=1        -> i.setBounds(+1)
     *
     * @param step the condition applies within the same step
     * @param condition
     */
    // apply bounds to all the variables
    default void applyBoundCondition(Expression scope, boolean reversed) {

    }

    /**
     * Whether the expression is a constant or a variable. Operations
     * (specially if they involve multiple variables) are not simple.
     */
    boolean isSimple();

    /**
     * Write (assignment) functions for sub-expressions and return the variable.
     *
     * @param target the list of statements
     * @return the variable
     */
    Expression writeStatements(Parser parser, ArrayList<Statement> target);

}
