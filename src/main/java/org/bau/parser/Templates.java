package org.bau.parser;

import java.util.Collections;
import java.util.List;

public class Templates {

    public static void checkMacroFunction(FunctionDefinition def) {
        // currently there are no checks
    }

    static String convertTemplate(String template, List<String> find, List<String> replace, Program program) {
        StringBuilder buff = new StringBuilder();
        Parser p = new Parser(template);
        int pos = 0;
        p.read();
        while(true) {
            if (p.type == Parser.TokenType.END) {
                break;
            }
            int p2 = p.pos;
            String raw = p.text.substring(pos, p2);
            pos = p2;
            boolean replaced = false;
            for (int i = 0; i < find.size(); i++) {
                String f = find.get(i);
                String r = replace.get(i);
                if (p.token.equals(f)) {
                    if (p.text.charAt(p2) == '.') {
                        String next = p.text.substring(pos);
                        if (next.startsWith(".name") && !Character.isJavaIdentifierPart(next.charAt(5))) {
                            p.read();
                            p.read();
                            pos = p.pos;
                            buff.append(" '" + StringLiteral.escape(r) + "' ");
                        } else if (next.startsWith(".fieldCount") && !Character.isJavaIdentifierPart(next.charAt(11))) {
                            p.read();
                            p.read();
                            pos = p.pos;
                            buff.append(" " + getFields(r, false, program).length + " ");
                        } else if (next.startsWith(".fieldNames") && !Character.isJavaIdentifierPart(next.charAt(11))) {
                            p.read();
                            p.read();
                            pos = p.pos;
                            buff.append(" '" + StringLiteral.escape(String.join(",", getFields(r, false, program))) + "' ");
                        } else if (next.startsWith(".fieldTypes") && !Character.isJavaIdentifierPart(next.charAt(11))) {
                            p.read();
                            p.read();
                            pos = p.pos;
                            buff.append(" '" + StringLiteral.escape(String.join(",", getFields(r, true, program))) + "' ");
                        } else {
                            // do not replace anything else
                            buff.append(raw.replace(f, r));
                        }
                    } else {
                        buff.append(raw.replace(f, r));
                    }
                    replaced = true;
                    break;
                } else if (p.token.endsWith(f + "_")) {
                    buff.append(raw.replace(f + "_", r.replace('.', '_').replace("[]", "_array")));
                    replaced = true;
                    break;
                }
            }
            if (!replaced) {
                if (p.token.equals("@@")) {
                    // ignore
                } else {
                    buff.append(raw);
                }
            }
            p.read();
        }
        return buff.toString();
    }

    private static String[] getFields(String typeName, boolean types, Program program) {
        String module = null;
        int lastDot = typeName.lastIndexOf('.');
        if (lastDot >= 0) {
            module = typeName.substring(0, lastDot);
            typeName = typeName.substring(lastDot + 1);
        }
        DataType type = program.getType(module, typeName);
        if (type == null || type.isArray() || type.isFunctionPointer || type.isTrait() || type.isNumber()) {
            return new String[0];
        }
        if (types) {
            return type.getFieldTypes();
        } else {
            return type.getFieldNames();
        }
    }

    static String convertTemplate(String template, String find, String replace, Program program) {
        return convertTemplate(template, Collections.singletonList(find), Collections.singletonList(replace), program);
    }

}
