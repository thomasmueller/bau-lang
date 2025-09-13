package org.bau.ena;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Preprocessor {

    private final String text;

    Preprocessor(String text) {
        this.text = text;
    }

    public static String convert(String text) {
        Preprocessor p = new Preprocessor(text);
        return p.convert();
    }

    private String convert() {
        String src = text.replace("\r\n", "\n").replace('\r', '\n');
        boolean endsWithNewline = src.endsWith("\n");
        ExtractResult er = extractMacrosAndLines(src);
        List<String> lines = er.lines;
        Map<String, Def> macros = er.macros;
        StringBuilder out = new StringBuilder();
        String[] linesArr = lines.toArray(new String[0]);
        for (int i = 0; i < linesArr.length; i++) {
            ExpansionResult res = expandLine(linesArr, i, macros);
            if (res == null) {
                out.append(linesArr[i]);
                if (i < linesArr.length - 1)
                    out.append('\n');
            } else {
                // Recursively expand generated lines until no further macros remain on those lines
                java.util.LinkedList<String> work = new java.util.LinkedList<>(res.generated);
                List<String> finalGen = new ArrayList<>();
                while (!work.isEmpty()) {
                    String gl = work.removeFirst();
                    ExpansionResult r2 = expandLine(new String[] { gl }, 0, macros);
                    if (r2 == null) {
                        finalGen.add(gl);
                    } else {
                        // Inline expansion in-place: push generated lines to the front in reverse order
                        for (int gi = r2.generated.size() - 1; gi >= 0; gi--) {
                            work.addFirst(r2.generated.get(gi));
                        }
                    }
                }
                for (int k = 0; k < finalGen.size(); k++) {
                    out.append(finalGen.get(k));
                    if (i < linesArr.length - 1 || k < finalGen.size() - 1)
                        out.append('\n');
                }
                i = res.skipToIndex; // consumed by expansion (no-op here)
            }
        }
        if (endsWithNewline && (out.length() == 0 || out.charAt(out.length() - 1) != '\n'))
            out.append('\n');
        return out.toString();
    }

    private static final class ExtractResult {
        final Map<String, Def> macros;
        final List<String> lines;

        ExtractResult(Map<String, Def> macros, List<String> lines) {
            this.macros = macros;
            this.lines = lines;
        }
    }

    private ExtractResult extractMacrosAndLines(String src) {
        List<PPTok> toks = tokenizeLossless(src);
        Map<String, Def> macros = new LinkedHashMap<>();
        List<String> outLines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean atLineStart = true;
        int i = 0;
        while (i < toks.size()) {
            PPTok t = toks.get(i);
            if (atLineStart) {
                // measure indent
                int j = i;
                int indentSpaces = 0;
                while (j < toks.size() && toks.get(j).type == PPType.SPACE) {
                    indentSpaces += toks.get(j).text.length();
                    j++;
                }
                boolean isDef = (j < toks.size() && toks.get(j).type == PPType.IDENT && "def".equals(toks.get(j).text));
                if (isDef) {
                    // build header text to parse
                    StringBuilder header = new StringBuilder();
                    j++; // skip 'def'
                    while (j < toks.size() && toks.get(j).type != PPType.NEWLINE) {
                        header.append(toks.get(j).text);
                        j++;
                    }
                    Def def = parseDef(header.toString().trim());
                    // capture body lines more indented than indentSpaces
                    int k = (j < toks.size() && toks.get(j).type == PPType.NEWLINE) ? j + 1 : j;
                    int bodyBase = -1;
                    while (k < toks.size()) {
                        // read one full line
                        int lineStart = k;
                        int lineIndent = 0;
                        while (k < toks.size() && toks.get(k).type == PPType.SPACE) {
                            lineIndent += toks.get(k).text.length();
                            k++;
                        }
                        int tokStart = k;
                        while (k < toks.size() && toks.get(k).type != PPType.NEWLINE) {
                            k++;
                        }
                        boolean isEmptyAndEOF = (tokStart == k && k >= toks.size());
                        if (isEmptyAndEOF)
                            break; // EOF
                        if (lineIndent <= indentSpaces) {
                            // not part of body
                            // emit this non-body line later by continuing normal flow
                            // but first, advance i to this line start so outer loop will process it
                            i = lineStart;
                            atLineStart = true;
                            current.setLength(0);
                            break;
                        }
                        if (bodyBase < 0)
                            bodyBase = lineIndent;
                        List<PPTok> lineTokens = new ArrayList<>();
                        for (int tpos = tokStart; tpos < k; tpos++)
                            lineTokens.add(toks.get(tpos));
                        def.body.add(new BodyLine(lineIndent - bodyBase, lineTokens));
                        // skip newline
                        if (k < toks.size() && toks.get(k).type == PPType.NEWLINE)
                            k++;
                    }
                    macros.put(def.name, def);
                    // continue after body processing; k already points to next line start
                    i = k;
                    atLineStart = true;
                    continue;
                }
            }
            if (t.type == PPType.NEWLINE) {
                outLines.add(current.toString());
                current.setLength(0);
                atLineStart = true;
                i++;
                continue;
            }
            current.append(t.text);
            if (t.type != PPType.SPACE)
                atLineStart = false;
            i++;
        }
        // last line (even if file ends without newline)
        if (current.length() > 0 || outLines.isEmpty())
            outLines.add(current.toString());
        return new ExtractResult(macros, outLines);
    }

    private ExpansionResult expandLine(String[] lines, int index, Map<String, Def> macros) {
        String line = lines[index];
        int indent = leadingSpaces(line);
        if (line.substring(indent).isEmpty())
            return null;
        List<PPTok> toks = tokenizeLossless(line);
        for (Def def : macros.values()) {
            int[] headSpan = findHeadSpan(toks, def.name);
            if (headSpan == null)
                continue;
            int headIdx = headSpan[0];
            int headEnd = headSpan[1];
            // LHS tokens are [0, headIdx)
            List<PPTok> lhsToks = new ArrayList<>(toks.subList(0, headIdx));
            String lhs = stripTrailingColon(trimEdgeSpaces(lhsToks));

            // Arg tokens are (headIdx, end]
            List<PPTok> argToks = new ArrayList<>();
            for (int i = headEnd; i < toks.size(); i++)
                argToks.add(toks.get(i));

            // parse args
            String[] args;
            if (def.params.length == 0) {
                args = new String[0];
            } else {
                List<List<PPTok>> partsTok = splitArgsByTopLevelCommas(argToks);
                if (def.params.length == 1 && partsTok.isEmpty()) {
                    args = new String[] { tokensToString(argToks) };
                } else if (partsTok.size() == def.params.length) {
                    String[] arr = new String[partsTok.size()];
                    for (int i = 0; i < partsTok.size(); i++)
                        arr[i] = tokensToString(trimEdgeSpaces(partsTok.get(i)));
                    args = arr;
                } else {
                    continue; // not a match
                }
            }

            // capture block if needed (detect @body lazily by scanning body)
            List<BlockLine> blk = Collections.emptyList();
            boolean needsBlk = false;
            for (BodyLine bl : def.body) {
                if ((bl.tokens.size() == 1 && "@{body}".equals(bl.tokens.get(0).text))
                        || (bl.tokens.size() == 4 && "@".equals(bl.tokens.get(0).text)
                                && "{".equals(bl.tokens.get(1).text) && bl.tokens.get(2).type == PPType.IDENT
                                && "body".equals(bl.tokens.get(2).text) && "}".equals(bl.tokens.get(3).text))) {
                    needsBlk = true;
                    break;
                }
            }
            if (needsBlk) {
                int j = index + 1;
                List<BlockLine> captured = new ArrayList<>();
                if (j < lines.length) {
                    int base = -1;
                    while (j < lines.length) {
                        String bl = lines[j];
                        if (bl.isEmpty()) {
                            captured.add(new BlockLine(0, new ArrayList<>()));
                            j++;
                            continue;
                        }
                        int bi = leadingSpaces(bl);
                        if (bi <= indent)
                            break;
                        if (base < 0)
                            base = bi;
                        int off = Math.max(0, bi - base);
                        String content = bl.substring(Math.min(bi, bl.length()));
                        captured.add(new BlockLine(off, tokenizeLossless(content)));
                        j++;
                    }
                }
                blk = captured;
                return new ExpansionResult(expand(def, indent, lhs, args, blk, tokensToString(argToks)), j - 1);
            }
            boolean usesRest = false;
            for (BodyLine bl : def.body) {
                for (PPTok t : bl.tokens) {
                    if (t.type == PPType.PLACEHOLDER_REST) {
                        usesRest = true;
                        break;
                    }
                }
                if (usesRest)
                    break;
            }
            List<String> gen = expand(def, indent, lhs, args, blk, tokensToString(argToks));
            if (def.params.length == 0 && !usesRest) {
                String suffix = tokensToString(argToks);
                if (!suffix.isEmpty() && !gen.isEmpty()) {
                    int last = gen.size() - 1;
                    gen.set(last, gen.get(last) + suffix);
                }
            }
            return new ExpansionResult(gen, index);
        }
        return null;
    }

    private List<String> expand(Def def, int lineIndent, String lhs, String[] args, List<BlockLine> blk,
            String restStr) {
        long id = def.nextId++;
        String restTrim = restStr == null ? "" : restStr.replaceAll("^\\s+", "");
        Map<String, String> argMap = new HashMap<>();
        for (int i = 0; i < def.params.length; i++) {
            argMap.put(def.params[i], args.length > i ? args[i].trim() : "");
        }
        List<String> out = new ArrayList<>();
        for (BodyLine bl : def.body) {
            // Handle block injection early and in order
            // Check block injection marker: exactly one token which is OTHER("@{body}")
            if ((bl.tokens.size() == 1 && "@{body}".equals(bl.tokens.get(0).text))
                    || (bl.tokens.size() == 4 && "@".equals(bl.tokens.get(0).text) && "{".equals(bl.tokens.get(1).text)
                            && bl.tokens.get(2).type == PPType.IDENT && "body".equals(bl.tokens.get(2).text)
                            && "}".equals(bl.tokens.get(3).text))) {
                for (BlockLine b : blk) {
                    StringBuilder sb = new StringBuilder();
                    for (PPTok t : b.tokens)
                        sb.append(t.text);
                    out.add(spaces(lineIndent + bl.indentOffset + b.indentOffset) + sb.toString());
                }
                continue;
            }

            List<PPTok> toks = new ArrayList<>(bl.tokens);
            List<PPTok> outToks = new ArrayList<>();
            for (int ti = 0; ti < toks.size(); ti++) {
                PPTok t = toks.get(ti);
                switch (t.type) {
                case SPACE:
                case OTHER:
                case SYMBOL:
                    outToks.add(t);
                    break;
                case IDENT: {
                    String repl = argMap.get(t.text);
                    if (repl != null)
                        outToks.add(new PPTok(PPType.OTHER, repl));
                    else
                        outToks.add(t);
                    break;
                }
                case PLACEHOLDER_ID:
                    outToks.add(new PPTok(PPType.OTHER, String.valueOf(id)));
                    break;
                case TEXT_PARAM: {
                    String pn = t.meta;
                    String val = "rest".equals(pn) ? restTrim : argMap.getOrDefault(pn, "");
                    outToks.add(new PPTok(PPType.OTHER, quoteText(val)));
                    break;
                }
                case PLACEHOLDER_REST: {
                    outToks.add(new PPTok(PPType.OTHER, restTrim));
                    break;
                }
                case PLACEHOLDER_LEFT: {
                    int j = ti + 1;
                    while (j < toks.size() && toks.get(j).type == PPType.SPACE)
                        j++;
                    boolean hasColon = j < toks.size() && toks.get(j).type == PPType.SYMBOL
                            && ":".equals(toks.get(j).text);
                    boolean isLineStart = true;
                    for (int k = ti - 1; k >= 0; k--) {
                        PPType kt = toks.get(k).type;
                        if (kt == PPType.SPACE)
                            continue;
                        isLineStart = false;
                        break;
                    }
                    if (hasColon) {
                        outToks.add(new PPTok(PPType.OTHER, lhs + ":"));
                        ti = j;
                    } else if (isLineStart) {
                        outToks.add(new PPTok(PPType.OTHER, lhs + ": "));
                        ti = j - 1;
                    } else {
                        // Elsewhere in an expression: expand to lhs if present, else nothing
                        if (!lhs.isEmpty()) {
                            outToks.add(new PPTok(PPType.OTHER, lhs));
                        }
                    }
                    break;
                }
                case NEWLINE:
                }
            }
            StringBuilder sbLine = new StringBuilder();
            for (PPTok t : outToks)
                sbLine.append(t.text);
            String content = sbLine.toString();
            if (content.contains("@{left}")) {
                String name = lhs.isEmpty() ? def.name : lhs;
                content = content.replace("@{left}", "@{" + name + "}");
            }
            out.add(spaces(lineIndent + bl.indentOffset) + content);
        }
        return out;
    }

    private static String quoteText(String raw) {
        StringBuilder sb = new StringBuilder();
        sb.append('\'');
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '\'')
                sb.append("''");
            else
                sb.append(c);
        }
        sb.append('\'');
        return sb.toString();
    }

    private static int[] findHeadSpan(List<PPTok> toks, String name) {
        for (int i = 0; i < toks.size(); i++) {
            if (toks.get(i).type == PPType.SPACE || toks.get(i).type == PPType.NEWLINE)
                continue;
            int j = i;
            int consumed = 0;
            while (j < toks.size() && consumed < name.length()) {
                PPTok t = toks.get(j);
                if (t.type == PPType.SPACE || t.type == PPType.NEWLINE)
                    break;
                String tt = t.text;
                int need = name.length() - consumed;
                if (tt.length() <= need && name.regionMatches(consumed, tt, 0, tt.length())) {
                    consumed += tt.length();
                    j++;
                    if (consumed == name.length()) {
                        return new int[] { i, j };
                    }
                } else {
                    break;
                }
            }
        }
        return null;
    }

    private static List<PPTok> trimEdgeSpaces(List<PPTok> in) {
        int a = 0, b = in.size();
        while (a < b && in.get(a).type == PPType.SPACE)
            a++;
        while (b > a && in.get(b - 1).type == PPType.SPACE)
            b--;
        return new ArrayList<>(in.subList(a, b));
    }

    private static String stripTrailingColon(List<PPTok> lhsTrim) {
        int colonAt = -1;
        for (int i = lhsTrim.size() - 1; i >= 0; i--) {
            PPTok t = lhsTrim.get(i);
            if (t.type == PPType.SYMBOL && ":".equals(t.text)) {
                colonAt = i;
                break;
            } else if (t.type != PPType.SPACE) {
                // first non-space from right is not a colon: no strip
                break;
            }
        }
        List<PPTok> slice = colonAt >= 0 ? lhsTrim.subList(0, colonAt) : lhsTrim;
        return tokensToString(trimEdgeSpaces(slice));
    }

    private static List<List<PPTok>> splitArgsByTopLevelCommas(List<PPTok> argToks) {
        List<List<PPTok>> parts = new ArrayList<>();
        List<PPTok> cur = new ArrayList<>();
        for (int i = 0; i < argToks.size(); i++) {
            PPTok t = argToks.get(i);
            if (t.type == PPType.SYMBOL) {
                if (",".equals(t.text)) {
                    parts.add(trimEdgeSpaces(cur));
                    cur = new ArrayList<>();
                    continue;
                }
            }
            cur.add(t);
        }
        if (!cur.isEmpty())
            parts.add(trimEdgeSpaces(cur));
        // drop empty segments if there were no args
        if (parts.size() == 1 && tokensToString(parts.get(0)).trim().isEmpty())
            return new ArrayList<>();
        return parts;
    }

    private static String tokensToString(List<PPTok> toks) {
        StringBuilder sb = new StringBuilder();
        for (PPTok t : toks)
            sb.append(t.text);
        return sb.toString();
    }

    private int leadingSpaces(String s) {
        int n = 0;
        while (n < s.length() && s.charAt(n) == ' ')
            n++;
        return n;
    }

    private static String spaces(int n) {
        char[] arr = new char[n];
        Arrays.fill(arr, ' ');
        return new String(arr);
    }

    private static final class BodyLine {
        final int indentOffset;
        final List<PPTok> tokens;

        BodyLine(int indentOffset, List<PPTok> tokens) {
            this.indentOffset = indentOffset;
            this.tokens = tokens;
        }
    }

    private static final class BlockLine {
        final int indentOffset;
        final List<PPTok> tokens;

        BlockLine(int indentOffset, List<PPTok> tokens) {
            this.indentOffset = indentOffset;
            this.tokens = tokens;
        }
    }

    private static final class ExpansionResult {
        final List<String> generated;
        final int skipToIndex; // index of last consumed source line (for @body)

        ExpansionResult(List<String> generated, int skipToIndex) {
            this.generated = generated;
            this.skipToIndex = skipToIndex;
        }
    }

    private static final class Def {
        final String name;
        final String[] params;
        final List<BodyLine> body = new ArrayList<>();
        long nextId = 0;

        Def(String name, String[] params) {
            this.name = name;
            this.params = params;
        }
    }

    private Def parseDef(String header) {
        String h = header.trim();
        List<PPTok> toks = tokenizeLossless(h);
        // name: tokens up to first SPACE
        StringBuilder nameSb = new StringBuilder();
        int i = 0;
        while (i < toks.size() && toks.get(i).type != PPType.SPACE) {
            nameSb.append(toks.get(i).text);
            i++;
        }
        String name = nameSb.toString();
        // skip spaces
        while (i < toks.size() && toks.get(i).type == PPType.SPACE)
            i++;
        List<String> params = new ArrayList<>();
        if (i < toks.size()) {
            // rest tokens
            boolean hasComma = false;
            for (int k = i; k < toks.size(); k++) {
                if (toks.get(k).type == PPType.SYMBOL && ",".equals(toks.get(k).text)) {
                    hasComma = true;
                    break;
                }
            }
            if (hasComma) {
                List<PPTok> cur = new ArrayList<>();
                for (int k = i; k < toks.size(); k++) {
                    PPTok t = toks.get(k);
                    if (t.type == PPType.SYMBOL && ",".equals(t.text)) {
                        String pn = tokensToString(trimEdgeSpaces(cur)).trim();
                        if (!pn.isEmpty())
                            params.add(pn);
                        cur.clear();
                        continue;
                    }
                    cur.add(t);
                }
                String pn = tokensToString(trimEdgeSpaces(cur)).trim();
                if (!pn.isEmpty())
                    params.add(pn);
            } else {
                StringBuilder sb = new StringBuilder();
                for (int k = i; k < toks.size(); k++) {
                    PPTok t = toks.get(k);
                    if (t.type == PPType.SPACE) {
                        String pn = sb.toString().trim();
                        if (!pn.isEmpty())
                            params.add(pn);
                        sb.setLength(0);
                        continue;
                    }
                    sb.append(t.text);
                }
                String pn = sb.toString().trim();
                if (!pn.isEmpty())
                    params.add(pn);
            }
        }
        Def d = new Def(name, params.toArray(new String[0]));
        return d;
    }

    private enum PPType {
        SPACE, IDENT, SYMBOL, OTHER, PLACEHOLDER_ID, PLACEHOLDER_LEFT, PLACEHOLDER_REST, TEXT_PARAM, NEWLINE
    }

    private static final class PPTok {
        final PPType type;
        final String text;
        final String meta; // e.g., param name for TEXT_PARAM

        PPTok(PPType type, String text) {
            this(type, text, null);
        }

        PPTok(PPType type, String text, String meta) {
            this.type = type;
            this.text = text;
            this.meta = meta;
        }
    }

    private List<PPTok> tokenizeLossless(String s) {
        List<PPTok> out = new ArrayList<>();
        int i = 0, n = s.length();
        while (i < n) {
            char c = s.charAt(i);
            if (c == '\'') { // text literal
                int start = i++;
                while (i < n) {
                    char d = s.charAt(i++);
                    if (d == '\'' && (i >= n || s.charAt(i) != '\''))
                        break;
                    if (d == '\'' && i < n && s.charAt(i) == '\'')
                        i++; // consume doubled quote
                }
                out.add(new PPTok(PPType.OTHER, s.substring(start, i)));
                continue;
            }
            if (c == '#') { // comment to end of line
                int start = i;
                while (i < n && s.charAt(i) != '\n')
                    i++;
                out.add(new PPTok(PPType.OTHER, s.substring(start, i)));
                continue;
            }
            if (c == '\n') {
                out.add(new PPTok(PPType.NEWLINE, "\n"));
                i++;
                continue;
            }
            if (c == ' ' || c == '\t' || c == '\f') {
                int start = i++;
                while (i < n) {
                    char d = s.charAt(i);
                    if (d == ' ' || d == '\t' || d == '\f')
                        i++;
                    else
                        break;
                }
                out.add(new PPTok(PPType.SPACE, s.substring(start, i)));
                continue;
            }
            if (c == '@') {
                // Escape @@ => literal '@'
                if (i + 1 < n && s.charAt(i + 1) == '@') {
                    out.add(new PPTok(PPType.OTHER, "@"));
                    i += 2;
                    continue;
                }
                // @{...}
                if (i + 1 < n && s.charAt(i + 1) == '{') {
                    int j = s.indexOf('}', i + 2);
                    if (j > 0) {
                        String name = s.substring(i + 2, j);
                        if ("id".equals(name)) {
                            out.add(new PPTok(PPType.PLACEHOLDER_ID, s.substring(i, j + 1)));
                            i = j + 1;
                            continue;
                        }
                        if ("left".equals(name)) {
                            out.add(new PPTok(PPType.PLACEHOLDER_LEFT, s.substring(i, j + 1)));
                            i = j + 1;
                            continue;
                        }
                        if ("rest".equals(name)) {
                            out.add(new PPTok(PPType.PLACEHOLDER_REST, s.substring(i, j + 1)));
                            i = j + 1;
                            continue;
                        }
                        if (name.endsWith(".text")) {
                            String param = name.substring(0, name.length() - 5);
                            out.add(new PPTok(PPType.TEXT_PARAM, s.substring(i, j + 1), param));
                            i = j + 1;
                            continue;
                        }
                    }
                }
                out.add(new PPTok(PPType.SYMBOL, String.valueOf(c)));
                i++;
                continue;
            }
            if (isLetter(c) || c == '_') {
                int start = i++;
                while (i < n) {
                    char d = s.charAt(i);
                    if (isLetterOrDigit(d) || d == '_')
                        i++;
                    else
                        break;
                }
                out.add(new PPTok(PPType.IDENT, s.substring(start, i)));
                continue;
            }
            // single char symbol or other
            if ("()[],:.+-*/%<>=|&^".indexOf(c) >= 0) {
                out.add(new PPTok(PPType.SYMBOL, String.valueOf(c)));
                i++;
                continue;
            }
            out.add(new PPTok(PPType.OTHER, String.valueOf(c)));
            i++;
        }
        return out;
    }

    private boolean isLetter(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }

    private boolean isLetterOrDigit(char c) {
        return isLetter(c) || (c >= '0' && c <= '9');
    }
}
