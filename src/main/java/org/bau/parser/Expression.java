package org.bau.parser;

import java.util.ArrayList;
import java.util.List;

import org.bau.parser.Solver.Rule;
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

    /**
     * After "using" owned variables and fields, they are null
     * (calling a method, assigning to another variable)
     */
    void setOwnedBoundsToNull(Solver solver, int depth, boolean loop);

    /**
     * Whether the expression is a constant or a variable. Operations
     * (specially if they involve multiple variables) are not simple.
     */
    boolean isSimple();

    /**
     * Write (assignment) functions for sub-expressions and return the variable.
     *
     * @param target the list of statements
     * @param assignment if the result is assigned to a variable
     * @return the variable
     */
    Expression writeStatements(Parser parser, boolean assignment, ArrayList<Statement> target);

    /**
     * Get the list of owned fields or variables that are "used",
     * for example when calling a function.
     * Those need to be set to NULL afterwards.
     */
    default List<Expression> getUsedOwned() {
        return List.of();
    }

    void used(Program program);

    default List<Rule> getRules() {
        return List.of();
    }

    boolean containsModifiableVariables();

}
