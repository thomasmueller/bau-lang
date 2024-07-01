package org.bau.parser;

import org.bau.runtime.Memory;

public interface Statement {
    
    public static String indent(String s) {
        if (s.isEmpty()) {
            return s;
        }
        boolean nl = s.endsWith("\n");
        s = s.trim().replaceAll("\n", "\n    ");
        if (nl) {
            s += "\n";
        }
        return "    " + s;
    }

    Statement replace(Variable old, Expression with);
    
    /**
     * @return whether to break
     */
    boolean run(Memory m);

    String toC(ProgramContext context);

    default void setBounds(Expression scope) {
        
    }

}
