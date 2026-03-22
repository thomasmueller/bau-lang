package org.bau.parser;

import java.util.HashSet;

import org.bau.runtime.Memory;

/**

# if: original
a := 1; if condition { a += 1 } a += 2
# SSA form
a0: 1; if condition { a1: a0 + 1 } a2: phi(a0, a1); a3: a2 + 1
# C, almost SSO (without phi nodes)
a0: 1; a2 := a0; if condition { a1: a0 + 1; a2 = a1 } a3: a2 + 1

# loop: original
a := 1; loop condition { a += 1 } a += 2
# SSA form
a0: 1; loop condition { a1: phi(a0, a2); a2: a1 + 1 } a3: phi(a0, a3); a4: a3 + 2
# C, almost SSO (without phi nodes)
a0: 1; a3 := a0; a1 := a0; while condition { a2: a1 + 1; a1 = a3 = a2 } a4: a3 + 2

 */
public class PhiBlock implements Statement {

    private BasicBlock basicBlock;

    @Override
    public Statement replace(Variable old, Expression with) {
        return this;
    }

    @Override
    public StatementResult run(Memory m) {
        return StatementResult.OK;
    }

    @Override
    public void optimize(ProgramContext context) {
    }

    @Override
    public String toC() {
        if (basicBlock != null) {
            return basicBlock.toC();
        }
        return "";
    }

    @Override
    public String toString() {
        return "";
    }

    @Override
    public void collectTypes(HashSet<DataType> set, MemoryType memoryType) {
    }

    @Override
    public void used(Program program) {
    }

    public void setBasicBlock(BasicBlock basicBlock) {
        this.basicBlock = basicBlock;
    }

    @Override
    public DataType canThrowException() {
        return null;
    }

    @Override
    public void setVariableVersions(String name, int oldVersion, int newVersion) {
    }

}
