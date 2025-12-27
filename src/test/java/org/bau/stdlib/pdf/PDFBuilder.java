package org.bau.stdlib.pdf;

import java.util.ArrayList;

public class PDFBuilder {

    // See also PDF Reference, pdfreference1.4.pdf

    private static final int[] WIDTHS = { 278, 278, 355, 556, 556, 889, 667, 191, 333, 333, 389, 584, 278, 333, 278,
            278, 556, 556, 556, 556, 556, 556, 556, 556, 556, 556, 278, 278, 584, 584, 584, 556, 1015, 667, 667, 722,
            722, 667, 611, 778, 722, 278, 500, 667, 556, 833, 722, 778, 667, 778, 722, 667, 611, 722, 667, 944, 667,
            667, 611, 278, 278, 278, 469, 556, 333, 556, 556, 500, 556, 556, 278, 556, 556, 222, 222, 500, 222, 833,
            556, 556, 556, 556, 333, 500, 278, 556, 500, 722, 500, 500, 500, 334, 260, 334, 584, 0, 556, 402, 440, 1000,
            950, 0, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000,
            1000, 584, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 278, 333, 556, 556, 556, 556, 260, 556, 333, 737,
            370, 556, 584, 0, 737, 333, 400, 549, 333, 333, 333, 576, 537, 278, 333, 333, 365, 556, 834, 834, 834, 611,
            667, 667, 667, 667, 667, 667, 1000, 722, 667, 667, 667, 667, 278, 278, 278, 278, 722, 722, 778, 778, 778,
            778, 778, 584, 778, 722, 722, 722, 722, 667, 667, 611, 556, 556, 556, 556, 556, 556, 889, 500, 556, 556,
            556, 556, 278, 278, 278, 278, 556, 556, 556, 556, 556, 556, 556, 549, 611, 556, 556, 556, 556, 500, 556,
            500 };

    private final StringBuilder buff = new StringBuilder();
    private ArrayList<Integer> pageIds = new ArrayList<>();

    private int pagesId = 0;
    private ArrayList<Integer> objectOffsets = new ArrayList<>();
    private double width, height;

    private int streamStart;

    PDFBuilder() {
        buff.append("%PDF-1.4\n%\u00ff\u00ff\u00ff\u00ff\n");
        nextId();
        pagesId = nextId();
    }

    PDFBuilder page(double width, double height) {
        this.width = width;
        this.height = height;
        stream();
        return this;
    }

    public static double measureText(String str, double size) {
        int w = 0;
        for (char c : str.toCharArray()) {
            if (c >= 32 && c <= 256)
                w += WIDTHS[c - 32];
            else
                w += 556;
        }
        return w * size / 1000.0;
    }

    PDFBuilder color(int red, int green, int blue) {
        buff.append(red + " " + green + " " + blue + " rg\n");
        return this;
    }

    PDFBuilder text(String str, double x, double y, int fontSize, int align, double width) {
        double tx = x;
        if (align != -1 && width != -1) {
            double tw = measureText(str, fontSize);
            if (align == 0)
                tx += (width - tw) / 2;
            if (align == 1)
                tx += (width - tw);
        }

        buff.append("BT\n" +
                "/F1 " + fontSize + " Tf\n");
        buff.append((int) tx + " " + (int) y + " Td\n");
        appendString(str);
        buff.append(" Tj\n");
        buff.append("ET\n");
        return this;
    }

    void appendString(String s) {
        buff.append('(');
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '\\') {
                buff.append("\\\\");
            } else if (ch == '(') {
                buff.append("\\(");
            } else if (ch == ')') {
                buff.append("\\)");
            } else if (ch == '\n') {
                buff.append("\\n");
            } else if (ch == '\r') {
                buff.append("\\r");
            } else if (ch >= ' ' && ch < 127) {
                buff.append(ch);
            } else if (ch > 127 && ch <= 255) {
                buff.append('\\');
                buff.append(Integer.toOctalString(0x200 + ch).substring(1));
            } else {
                throw new IllegalArgumentException("unsupported char " + ch + " in " + s);
            }
        }
        buff.append(')');
    }

    PDFBuilder endPage() {
        endStream();
        int fontId = indirectObject(nextId());
        buff.append("<<\n" +
                "/Type /Font\n" +
                "/Subtype /Type1\n" +
                "/BaseFont /Helvetica\n" +
                "/Encoding /WinAnsiEncoding\n" +
                ">>\n");
        endIndirectObject();

        int pageId = nextId();
        pageIds.add(indirectObject(pageId));
        buff.append("<<\n" +
                "/MediaBox [0 0 " + width + " " + height + " ]\n" +
                "/Resources <<\n" +
                "/Font <<\n" +
                "/F1 " + fontId + " 0 R\n" +
                ">>\n" +
                ">>\n" +
                "/Contents " + (pageId - 3) + " 0 R\n" +
                "/Type /Page\n" +
                "/Parent " + pagesId + " 0 R\n" +
                ">>\n");
        endIndirectObject();
        return this;
    }

    private int stream() {
        int id = indirectObject(nextId());
        buff.append("<<\n" +
                "/Length " + (id + 1) + " 0 R\n" +
                ">>\n");
        buff.append("stream\n");
        streamStart = pos();
        return id;
    }

    private void endStream() {
        int streamLength = pos() - streamStart;
        buff.append("endstream\n");
        endIndirectObject();
        indirectObject(nextId());
        buff.append("" + streamLength + "\n");
        endIndirectObject();
    }

    private int nextId() {
        int id = objectOffsets.size();
        objectOffsets.add(0);
        return id;
    }

    private int indirectObject(int id) {
        int offset = pos();
        objectOffsets.set(id, offset);
        buff.append(id + " 0 obj\n");
        return id;
    }

    private void endIndirectObject() {
        buff.append("endobj\n");
    }

    private int pos() {
        return buff.length() + 4;
    }

    PDFBuilder end() {
        indirectObject(pagesId);
        buff.append("<<\n" +
                "/Type /Pages\n" +
                "/Kids [");
        for(int i=0; i < pageIds.size(); i++) {
            buff.append(pageIds.get(i) + " 0 R ");
        }
        buff.append("]\n" +
        "/Count " + pageIds.size() + "\n"
        + ">>\n");
        endIndirectObject();

        int rootId = indirectObject(nextId());
        buff.append("<<\n" +
                "/Type /Catalog\n" +
                "/Pages " + pagesId + " 0 R\n"
        + ">>\n");
        endIndirectObject();

        int startXref = pos();
        buff.append("xref\n" +
                "0 " + objectOffsets.size() + "\n" +
                "0000000000 65535 f \n");
        for (int i = 1; i < objectOffsets.size(); i++) {
            buff.append(String.format("%010d 00000 n \n", objectOffsets.get(i)));
        }
        buff.append("trailer\n" +
                "<<\n" +
                "/Root " + rootId + " 0 R\n" +
                "/Size " + objectOffsets.size() + "\n" +
                ">>\n" +
                "startxref\n" +
                startXref + "\n" +
                "%%EOF\n");
        return this;
    }

    public String toString() {
        return buff.toString();
    }

}
