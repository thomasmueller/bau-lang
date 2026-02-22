package org.bau.stdlib.graphics;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

public class ImageTest {

    @Test
    public void test() throws IOException {
        int w = 360 * 2;
        int h = 288 * 2;
        Image img = new Image(w, h);
        String fox = "the quick brown fox jumps over the lazy dog äöüéàè";
//        img.drawText5x6(0, 10, 12, "Hello world 123!", 2);
//        img.drawText5x6(0, 10, 26, fox, 2);
//        img.drawText5x6(0, 10, 46, fox.toUpperCase(Locale.ROOT), 2);
//        img.drawText5x6(0, 10, 80, "0123456789 +\"'*ç%&/()=?_:;-.,", 2);
//
//        img.drawText3x5(150, 210, 5, "Hello world 123!", 2);
//        img.drawText3x5(150, 210, 20, fox, 2);
//        img.drawText3x5(150, 210, 40, fox.toUpperCase(Locale.ROOT), 2);
//        img.drawText3x5(150, 210, 70, "0123456789 +\"'*ç%&/()=?_:;-.,", 2);
//
//        StringBuilder buff = new StringBuilder();
        int rh = 16 * img.getTextHeight(fox, 2);
        img.setColor(255);
//        img.fillRectangle(
//                10, 100,
//                10 + img.getTextLength(fox, 2), 100 + rh);
//        img.drawLine(
//                10 + img.getTextLength(fox, 2), 100,
//                10, 100 + rh);
//        img.drawLineAntialiased(
//                10, 100,
//                10 + img.getTextLength(fox, 2), 100 + rh);
//        img.drawQuadBezierSeg(
//                10 + img.getTextLength(fox, 2), 100,
//                10 + img.getTextLength(fox, 2), 100 + rh,
//                10, 100 + rh);
//        for(int x0 = 20; x0 < 100; x0 += 20) {
//            for(int y0 = 20; y0 < 100; y0 += 20) {
//                img.drawQuadBezierAA(
//                        x0, y0,
//                        200 - x0, 200 - y0,
//                        20, 100 + rh);
//                img.drawQuadBezierAA(
//                        300 + 20, 100 + rh,
//                        300 + 200 - x0, 200 - y0,
//                        300 + x0, y0);
//
//            }
//
//        }

//        img.drawQuadBezierAA(
//                20 + img.getTextLength(fox, 2), 100,
//                20 + img.getTextLength(fox, 2), 100 + rh,
//                20, 100 + rh);
        img.drawLineAntialiasedWidth(
                20 + img.getTextLength(fox, 2), 100,
                20, 100 + rh,
                3.0);


        img.setColor(128);
        img.drawPoly(new int[][] {
            {10, 10, 30, 10, 30, 30, 10, 30, 10, 10},
            {12, 12, 12, 22, 22, 22, 22, 12, 12, 12},
            {15, 15, 25, 15, 25, 25, 15, 25, 15, 15} });
//        img.drawCircle(190, 50, 140, 30, true);
//        img.drawCircle(0, 50, 140, 30, false);
//        img.drawCircle(190, 350, 340, 31, true);
//        img.drawCircle(0, 350, 340, 30, false);
//        for (int i = 0; i < 256; i++) {
//            buff.append((char) i);
//        }
//        String s = buff.toString();
//        int y = 320;
//        for (int i = 32; i < 256; i += 32) {
//            img.drawText5x6(0, 10, y, s.substring(i, i + 32), 2);
//            y += 20;
//        }
        File out = new File("target/test.tiff");
//        Tiff.writeGrayscaleTiff(img.imageData, w, h, out);
    }

    @Test
    public void font() throws IOException {
        // M = moveto (move from one point to another point)
        // L = lineto (create a line)
        // H = horizontal lineto (create a horizontal line)
        // V = vertical lineto (create a vertical line)
        // C = curveto (create a curve)
        // S = smooth curveto (create a smooth curve)
        // Q = quadratic Bézier curve (create a quadratic Bézier curve)
        // T = smooth quadratic Bézier curveto (create a smooth quadratic Bézier curve)
        // A = elliptical Arc (create a elliptical arc)
        // Z = closepath (close the path)

        // Uppercase (M, H, L, Q): Absolute coordinates.
        // Lowercase (m, h, l, q): Relative to the current position.

        // https://svgwg.org/specs/paths/

        String a = "M 91.041076,88.444855 H 82.152227 L 76.001649,70.962047 H 48.871701 L 42.721123,88.444855 H 34.253546 L 57.086514,25.717383 H 68.208108 Z M 73.431886,63.800415 62.436675,33.005397 51.399336,63.800415 Z";
        String b = "m 84.308363,65.283704 q 0,3.985507 -1.508029,7.037472 -1.50803,3.051965 -4.057318,5.026766 -3.01606,2.369761 -6.642512,3.375114 -3.590547,1.005353 -9.155895,1.005353 H 43.986521 V 28.265165 h 15.834312 q 5.852592,0 8.760935,0.430866 2.908343,0.430865 5.565348,1.795273 2.944248,1.543935 4.27275,3.985507 1.328503,2.405667 1.328503,5.780781 0,3.80598 -1.938896,6.49889 -1.938895,2.657005 -5.170387,4.272751 v 0.287243 q 5.421726,1.11307 8.545501,4.775428 3.123776,3.626452 3.123776,9.1918 z M 72.351842,41.191134 q 0,-1.938895 -0.646298,-3.267398 -0.646299,-1.328502 -2.082518,-2.154328 -1.687557,-0.969447 -4.093223,-1.18488 -2.405667,-0.251339 -5.960308,-0.251339 h -8.473691 v 15.439352 h 9.1918 q 3.339209,0 5.31401,-0.323149 1.974801,-0.359055 3.662358,-1.436219 1.687557,-1.077164 2.369761,-2.764721 0.718109,-1.723462 0.718109,-4.057318 z m 4.559995,24.379814 q 0,-3.231493 -0.969448,-5.134482 -0.969448,-1.90299 -3.518736,-3.231493 -1.723463,-0.897636 -4.20094,-1.148975 -2.441572,-0.287243 -5.960308,-0.287243 H 51.095804 v 19.89163 h 9.407233 q 4.667711,0 7.647865,-0.466772 2.980154,-0.502676 4.883144,-1.795273 2.010706,-1.400313 2.944248,-3.195587 0.933543,-1.795273 0.933543,-4.631805 z";

        File out = new File("target/test.tiff");
        int w = 360 * 2;
        int h = 288 * 2;
        Image img = new Image(w, h);
        img.drawPoly(new SvgUtils(b).convertPathToLines(4));
        img.downscale(4);
        Tiff.writeGrayscaleTiff(img.imageData, w, h, out);
    }


//    function flattenQuadraticBezier(P0, P1, P2, tolerance, lines_output_list):
//        # Calculate midpoint of the chord
//        mid_chord_x = (P0.x + P2.x) / 2
//        mid_chord_y = (P0.y + P2.y) / 2
//
//        # Calculate distance from P1 to the chord (simple approximation of error)
//        # A more robust calculation uses the actual distance formula
//        error = distance_to_chord(P0, P1, P2)
//
//        if error <= tolerance:
//            # Add a single line segment to the output list
//            add_line(P0, P2, lines_output_list)
//        else:
//            # Subdivide the curve at t=0.5
//            P01 = interpolate(P0, P1, 0.5)
//            P12 = interpolate(P1, P2, 0.5)
//            Q1 = interpolate(P01, P12, 0.5) # Point on the curve
//
//            # Recursively call the function for the two new curves
//            flattenQuadraticBezier(P0, P01, Q1, tolerance, lines_output_list)
//            flattenQuadraticBezier(Q1, P12, P2, tolerance, lines_output_list)



}
