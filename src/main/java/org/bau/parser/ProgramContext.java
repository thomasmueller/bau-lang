package org.bau.parser;

import java.util.LinkedHashSet;

public class ProgramContext {
    public FunctionDefinition function;
    public int nextExceptionVariableId;
    public int nextSkipLabel;
    public int nextCatchLabel;
    public DataType needToCatch;
    public LinkedHashSet<String> delareList = new LinkedHashSet<>();

    void nextFunction() {
        nextExceptionVariableId = 0;
        nextSkipLabel = 0;
        nextCatchLabel = 0;
        needToCatch = null;
        delareList.clear();
    }

}
