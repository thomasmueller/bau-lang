package org.bau.parser;

import java.util.ArrayList;
import java.util.List;

import org.bau.parser.Solver.Rule;
import org.bau.runtime.Memory;
import org.bau.runtime.Value;

public class VarVersion implements Expression, LeftValue {

    private final Variable variable;
    private final int version;

    VarVersion(Variable variable, int version) {
        this.variable = variable;
        this.version = version;
    }

    public LeftValue nextVersion() {
        return new VarVersion(variable, version + 1);
    }

    @Override
    public String assignmentC() {
        return variable.assignmentC();
    }

    @Override
    public Expression replace(Variable old, Expression with) {
        Expression result = variable.replace(old, with);
        if (result == variable) {
            // unchanged
            return this;
        }
        if (result instanceof Variable) {
            return new VarVersion((Variable) result, 0);
        }
        return result;
    }

    @Override
    public String decrementRefCountC() {
        return variable.decrementRefCountC();
    }

    @Override
    public String incrementRefCountC() {
        return variable.incrementRefCountC();
    }

    @Override
    public Value setValue(Memory memory, Value val, boolean incRefCount, boolean initial) {
        return variable.setValue(memory, val, incRefCount, initial);
    }

    @Override
    public boolean isConstant() {
        return variable.isConstant();
    }

    @Override
    public void incrementReassignCount() {
        variable.incrementReassignCount();

    }

    @Override
    public Value eval(Memory memory) {
        return variable.eval(memory);
    }

    @Override
    public DataType type() {
        return variable.type();
    }

    @Override
    public DataType canThrowException() {
        return variable.canThrowException();
    }

    @Override
    public Expression simplify() {
        Expression result = variable.simplify();
        if (result == variable) {
            // unchanged
            return this;
        }
        return result;
    }

    @Override
    public String toC() {
        return variable.toC();
    }

    @Override
    public boolean isEasyToRead() {
        return variable.isEasyToRead();
    }

    @Override
    public void setOwnedBoundsToNull(Solver solver, int depth, boolean loop) {
        variable.setOwnedBoundsToNull(solver, depth, loop);
    }

    @Override
    public boolean isSimple() {
        return variable.isSimple();
    }

    @Override
    public Expression writeStatements(Parser parser, boolean assignment, ArrayList<Statement> target) {
        Expression result = variable.writeStatements(parser, assignment, target);
        if (result == variable) {
            // unchanged
            return this;
        }
        return result;
    }

    @Override
    public void used(Program program) {
        variable.used(program);
    }

    @Override
    public boolean containsModifiableVariables() {
        return variable.containsModifiableVariables();
    }

    @Override
    public List<Rule> getRules() {
        return variable.getRules();
    }

}
