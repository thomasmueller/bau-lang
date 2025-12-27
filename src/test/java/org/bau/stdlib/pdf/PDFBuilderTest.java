package org.bau.stdlib.pdf;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

public class PDFBuilderTest {

    @Test
    public void builder() throws IOException {
        PDFBuilder builder = new PDFBuilder();
        builder.page(612, 792);
        builder.color(0, 0, 255);
        builder.text("Hello World", 72, 720, 12, -1, -1);
        builder.endPage();
        builder.end();
//        FileWriter w = new FileWriter("hello2.pdf");
//        w.append(builder.toString());
//        w.close();
        assertEquals("%PDF-1.4\n"
                + "%ÿÿÿÿ\n"
                + "2 0 obj\n"
                + "<<\n"
                + "/Length 3 0 R\n"
                + ">>\n"
                + "stream\n"
                + "0 0 255 rg\n"
                + "BT\n"
                + "/F1 12 Tf\n"
                + "72 720 Td\n"
                + "(Hello World) Tj\n"
                + "ET\n"
                + "endstream\n"
                + "endobj\n"
                + "3 0 obj\n"
                + "54\n"
                + "endobj\n"
                + "4 0 obj\n"
                + "<<\n"
                + "/Type /Font\n"
                + "/Subtype /Type1\n"
                + "/BaseFont /Helvetica\n"
                + "/Encoding /WinAnsiEncoding\n"
                + ">>\n"
                + "endobj\n"
                + "5 0 obj\n"
                + "<<\n"
                + "/MediaBox [0 0 612.0 792.0 ]\n"
                + "/Resources <<\n"
                + "/Font <<\n"
                + "/F1 4 0 R\n"
                + ">>\n"
                + ">>\n"
                + "/Contents 2 0 R\n"
                + "/Type /Page\n"
                + "/Parent 1 0 R\n"
                + ">>\n"
                + "endobj\n"
                + "1 0 obj\n"
                + "<<\n"
                + "/Type /Pages\n"
                + "/Kids [5 0 R ]\n"
                + "/Count 1\n"
                + ">>\n"
                + "endobj\n"
                + "6 0 obj\n"
                + "<<\n"
                + "/Type /Catalog\n"
                + "/Pages 1 0 R\n"
                + ">>\n"
                + "endobj\n"
                + "xref\n"
                + "0 7\n"
                + "0000000000 65535 f \n"
                + "0000000371 00000 n \n"
                + "0000000019 00000 n \n"
                + "0000000125 00000 n \n"
                + "0000000143 00000 n \n"
                + "0000000240 00000 n \n"
                + "0000000429 00000 n \n"
                + "trailer\n"
                + "<<\n"
                + "/Root 6 0 R\n"
                + "/Size 7\n"
                + ">>\n"
                + "startxref\n"
                + "478\n"
                + "%%EOF\n"
                + "", builder.toString());
    }

}
