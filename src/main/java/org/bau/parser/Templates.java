package org.bau.parser;

import java.util.Collections;
import java.util.List;

public class Templates {

    public static void checkMacroFunction(FunctionDefinition def) {
        // currently there are no checks
    }

    static String convertTemplate(String template, List<String> find, List<String> replace) {
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
                    buff.append(raw.replace(f, r));
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

    static String convertTemplate(String template, String find, String replace) {
        return convertTemplate(template, Collections.singletonList(find), Collections.singletonList(replace));
    }

}
