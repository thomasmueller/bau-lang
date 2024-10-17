package org.bau.parser;

import java.util.LinkedHashSet;

public class ProgramContext {
    FunctionDefinition function;
    int nextExceptionVariableId;
    int nextSkipLabel;
    int nextCatchLabel;
    DataType needToCatch;
    public LinkedHashSet<String> delareList = new LinkedHashSet<>();

    void nextFunction() {
        nextExceptionVariableId = 0;
        nextSkipLabel = 0;
        nextCatchLabel = 0;
        needToCatch = null;
        delareList.clear();
    }

}
