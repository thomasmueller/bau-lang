package org.bau.parser;

import java.util.LinkedHashSet;

public class ProgramContext {
    int indent;
    int nextSkipLabel;
    int nextCatchLabel;
    FunctionDefinition function;
    DataType needToCatch;
    public LinkedHashSet<String> delareList = new LinkedHashSet<>();
    
    void nextFunction() {
        nextCatchLabel = 0;
        nextSkipLabel = 0;
        needToCatch = null;
    }
    
}
