package org.bau.stdlib.graphics;

import java.util.ArrayList;
import java.util.Arrays;

public class Image {

    private static final int[] FONT3x5 = new int[] { 8338, 45, 24445, 12191, 21157, 31643, 18, 8778, 10530, 2728, 1488,
            5120, 448, 8192, 5268, 31599, 9370, 29671, 31143, 18925, 31183, 31689, 18727, 31727, 31215, 1040, 5136,
            17492, 3640, 5393, 8615, 25450, 23534, 31467, 25166, 15211, 29391, 4815, 31566, 23533, 29847, 31524, 23277,
            29257, 23549, 23403, 15214, 5103, 26474, 23279, 14798, 9367, 27501, 9581, 32749, 21653, 31213, 29351, 25750,
            18569, 13459, 42, 28672, 34, 24432, 15064, 25200, 15192, 25336, 4856, 31344, 24424, 29880, 13456, 23272,
            25160, 23544, 23384, 15216, 8048, 26448, 22360, 14448, 9400, 27496, 9576, 32744, 21672, 10728, 29496, 25686,
            9362, 13587, 408 };

    private static final int[] FONT5x6 = new int[] { 134353028, 330, 368389098, 525277124, 866263651, 748325026, 132,
            409012428, 214180230, 156718208, 139432064, 71303168, 1015808, 2097152, 35791360, 488162862, 474091716,
            1041283599, 520632847, 277849385, 520633407, 488160318, 138551839, 488159790, 487094830, 4198400, 71307264,
            409049472, 32537600, 214708416, 134357550, 471782958, 588840494, 521715247, 487622190, 521717295,
            1041284159, 34651199, 1025045566, 311737641, 474091662, 488129052, 580029609, 1041269793, 588830577,
            589092465, 488162862, 34651695, 748340782, 580042287, 520632382, 138547359, 488162865, 138750513, 346736177,
            588583249, 138547537, 1041305887, 406982796, 545394753, 205656198, 17732, 1040187392, 260, 1025458624,
            521719201, 1007716288, 1025034192, 1008715200, 69699660, 487548608, 588828065, 474093572, 239210500,
            582133281, 406982790, 593155424, 588826080, 488162752, 47826400, 560777152, 34639776, 520553920, 421595618,
            1025033760, 145049120, 364566048, 581052960, 487540256, 1042424800, 809603224, 138547332, 105648259, 316992,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            138547204, 165844932, 1042745420, 585447968, 139432273, 138543236, 205858972, 18, 15388334, 1041188414,
            681749120, 556032, 0, 15322862, 31, 206118, 32510948, 397580, 467342, 68, 35104032, 692805596, 4096, 0,
            4230, 15735078, 178923680, 563941681, 482773161, 563944819, 487657476, 603502599, 603502620, 603502916,
            603502619, 603502602, 603502926, 849324508, 971015230, 1041497095, 1041497116, 1041497412, 1041497098,
            474101766, 474101772, 474102084, 474101770, 489250382, 588823566, 488159239, 976318520, 488159556,
            488159246, 488159242, 10627072, 523949886, 488162311, 488162332, 488162318, 488162314, 138757148, 41196577,
            177509670, 1025046535, 1025046556, 1025046852, 1025046558, 1025046538, 1025046924, 1012355552, 434145216,
            1039792135, 1039792156, 1039792452, 1039792148, 474101767, 474101788, 474102084, 474101770, 488159263,
            588823583, 488159239, 488159260, 488159556, 488159263, 488159242, 135233664, 48937472, 488162311, 488162332,
            488162318, 488162314, 487539726, 41196577, 487539722 };

    public final byte[] imageData;
    public final int width;
    public final int height;
    public int color;
    public int backColor;

    Image(int width, int height) {
        this.width = width;
        this.height = height;
        imageData = new byte[width * height];
        Arrays.fill(imageData, (byte) 0xff);
    }

    public int getTextLength(String text, int size) {
        return text.length() * 4 * size;
    }

    public int getTextHeight(String text, int size) {
        return 6 * size;
    }

    public void drawText3x5(int color, int x, int y, String text, int size) {
        // size = 1;
        for (int i = 0; i < text.length(); i++, x += 4 * size) {
            char c = text.charAt(i);
            if (c > 126 || c < 33) {
                continue;
            }
            int slot = c - 33;
            int data = FONT3x5[slot];
            for (int yp = 0; yp < 5; yp++) {
                for (int xp = 0; xp < 3; xp++) {
                    int b = (data >>> (xp + 3 * yp)) & 1;
                    if (b == 1) {
                        for(int xs = 0; xs < size; xs++) {
                            for(int ys = 0; ys < size; ys++) {
                                imageData[x + xp * size + xs + (y + yp * size + ys) * width] = (byte) color;
                            }
                        }
                    }
                }
            }

        }
    }

    public void drawText5x6(int color, int x, int y, String text, int size) {
        // size = 1;
        for (int i = 0; i < text.length(); i++, x += 6 * size) {
            char c = text.charAt(i);
            if (c >= 256 || c < 33) {
                continue;
            }
            int slot = c - 33;
            int data = FONT5x6[slot];
            for (int yp = 0; yp < 6; yp++) {
                for (int xp = 0; xp < 5; xp++) {
                    int b = (data >>> (xp + 5 * yp)) & 1;
                    if (b == 1) {
                        for(int xs = 0; xs < size; xs++) {
                            for(int ys = 0; ys < size; ys++) {
                                imageData[x + xp * size + xs + (y + yp * size + ys) * width] = (byte) color;
                            }
                        }
                    }
                }
            }

        }
    }

    public void setColor(int color) {
        this.color = color;
    }

    public void setColorBackground(int backColor) {
        this.backColor = backColor;
    }

    public void fillRectangle(int x0, int y0, int x1, int y1) {
        for(int x = x0; x <= x1; x++) {
            for(int y = y0; y <= y1; y++) {
                imageData[x + y * width] = (byte) color;
            }
        }
    }

    // https://en.wikipedia.org/wiki/Bresenham%27s_line_algorithm
    public void drawLine(int x0, int y0, int x1, int y1) {
        int sx = x0 < x1 ? 1 : -1;
        int dx = sx * (x1 - x0);
        int sy = y0 < y1 ? 1 : -1;
        int dy = -sy * (y1 - y0);
        int error = dx + dy;
        while (true) {
            imageData[x0 + y0 * width] = (byte) color;
            int e2 = 2 * error;
            if (e2 >= dy) {
                if (x0 == x1) {
                    break;
                }
                error += dy;
                x0 += sx;
            }
            if (e2 <= dx) {
                if (y0 == y1) {
                    break;
                }
                error += dx;
                y0 += sy;
            }
        }
    }

    public void drawQuadBezierSeg(int x0, int y0, int x1, int y1, int x2, int y2)
    {
      int sx = x2-x1, sy = y2-y1;
      long xx = x0-x1, yy = y0-y1, xy;         /* relative values for checks */
      double dx, dy, err, cur = xx*sy-yy*sx;                    /* curvature */

      assert(xx*sx <= 0 && yy*sy <= 0);  /* sign of gradient must not change */

      if (sx*(long)sx+sy*(long)sy > xx*xx+yy*yy) { /* begin with longer part */
        x2 = x0; x0 = sx+x1; y2 = y0; y0 = sy+y1; cur = -cur;  /* swap P0 P2 */
      }
      if (cur != 0) {                                    /* no straight line */
        xx += sx; xx *= sx = x0 < x2 ? 1 : -1;           /* x step direction */
        yy += sy; yy *= sy = y0 < y2 ? 1 : -1;           /* y step direction */
        xy = 2*xx*yy; xx *= xx; yy *= yy;          /* differences 2nd degree */
        if (cur*sx*sy < 0) {                           /* negated curvature? */
          xx = -xx; yy = -yy; xy = -xy; cur = -cur;
        }
        dx = 4.0*sy*cur*(x1-x0)+xx-xy;             /* differences 1st degree */
        dy = 4.0*sx*cur*(y0-y1)+yy-xy;
        xx += xx; yy += yy; err = dx+dy+xy;                /* error 1st step */
        do {
            imageData[x0 + y0 * width] = (byte) color;
          // setPixel(x0,y0);                                     /* plot curve */
          if (x0 == x2 && y0 == y2) return;  /* last pixel -> curve finished */
          boolean s = (boolean) (2*err < dx);                  /* save value for test of y step */
          if (2*err > dy) { x0 += sx; dx -= xy; err += dy += yy; } /* x step */
          if (    s    ) { y0 += sy; dy -= xy; err += dx += xx; } /* y step */
        } while (dy < dx );           /* gradient negates -> algorithm fails */
      }
      drawLine(x0,y0, x2,y2);                  /* plot remaining part to end */
    }

    void drawQuadBezierAA(int x0, int y0, int x1, int y1, int x2, int y2)
    { /* plot any quadratic Bezier curve */
    int x = x0-x1, y = y0-y1;
    double t = x0-2*x1+x2, r;
    if ((long)x*(x2-x1) > 0) { /* horizontal cut at P4? */
    if ((long)y*(y2-y1) > 0) /* vertical cut at P6 too? */
    if (Math.abs((y0-2*y1+y2)/t*x) > Math.abs(y)) { /* which first? */
    x0 = x2; x2 = x+x1; y0 = y2; y2 = y+y1; /* swap points */
    } /* now horizontal cut at P4 comes first */
    t = (x0-x1)/t;
    r = (1-t)*((1-t)*y0+2.0*t*y1)+t*t*y2; t = (x0*x2-x1*x1)*t/(x0-x1); /* By(t=P4) */
    /* gradient dP4/dx=0 */
    x = floor(t+0.5); y = floor(r+0.5);
    r = (y1-y0)*(t-x0)/(x1-x0)+y0; drawQuadBezierSegAA(x0,y0, x,floor(r+0.5), x,y);
    r = (y1-y2)*(t-x2)/(x1-x2)+y2; x0 = x1 = x; y0 = y; y1 = floor(r+0.5); /* intersect P3 | P0 P1 */
    /* intersect P4 | P1 P2 */
    /* P0 = P4, P1 = P8 */
    }
    if ((long)(y0-y1)*(y2-y1) > 0) { t = y0-2*y1+y2; t = (y0-y1)/t;
    r = (1-t)*((1-t)*x0+2.0*t*x1)+t*t*x2; t = (y0*y2-y1*y1)*t/(y0-y1); x = floor(r+0.5); y = floor(t+0.5);
    r = (x1-x0)*(t-y0)/(y1-y0)+x0; drawQuadBezierSegAA(x0,y0, floor(r+0.5),y, x,y);
    r = (x1-x2)*(t-y2)/(y1-y2)+x2; x0 = x; x1 = floor(r+0.5); y0 = y1 = y; /* vertical cut at P6? */
    /* Bx(t=P6) */
    /* gradient dP6/dy=0 */
    /* intersect P6 | P0 P1 */
    /* intersect P7 | P1 P2 */
    /* P0 = P6, P1 = P7 */
    }
    drawQuadBezierSegAA(x0,y0, x1,y1, x2,y2); /* remaining part */
    }

    static int floor(double x) {
        return (int) Math.floor(x);
    }



    void drawQuadBezierSegAA(int x0, int y0, int x1, int y1, int x2, int y2)
    {
        System.out.println(x0 +"/" + y0 + " " + x1 + "/" + y1 + " " + x2 + "/" + y2);
       int sx = x2-x1, sy = y2-y1;
       long xx = x0-x1, yy = y0-y1, xy;         /* relative values for checks */
       double dx, dy, err, ed, cur = xx*sy-yy*sx;                /* curvature */

//       assert(xx*sx >= 0 && yy*sy >= 0);  /* sign of gradient must not change */

       if (sx*(long)sx+sy*(long)sy > xx*xx+yy*yy) { /* begin with longer part */
          x2 = x0; x0 = sx+x1; y2 = y0; y0 = sy+y1; cur = -cur; /* swap P0 P2 */
       }
       if (cur != 0)
       {                                                  /* no straight line */
          xx += sx; xx *= sx = x0 < x2 ? 1 : -1;          /* x step direction */
          yy += sy; yy *= sy = y0 < y2 ? 1 : -1;          /* y step direction */
          xy = 2*xx*yy; xx *= xx; yy *= yy;         /* differences 2nd degree */
          if (cur*sx*sy < 0) {                          /* negated curvature? */
             xx = -xx; yy = -yy; xy = -xy; cur = -cur;
          }
          dx = 4.0*sy*(x1-x0)*cur+xx-xy;            /* differences 1st degree */
          dy = 4.0*sx*(y0-y1)*cur+yy-xy;
          xx += xx; yy += yy; err = dx+dy+xy;               /* error 1st step */
          do {
             cur = Math.min(dx+xy,-xy-dy);
             ed = Math.max(dx+xy,-xy-dy);           /* approximate error distance */
             ed = 255/(ed+2*ed*cur*cur/(4.*ed*ed+cur*cur));
             setPixel(x0,y0, ed*Math.abs(err-dx-dy-xy));          /* plot curve */
             if (x0 == x2 && y0 == y2) return;/* last pixel -> curve finished */
             x1 = x0; cur = dx-err;
             boolean yy1 = 2*err+dy < 0;
             if (2*err+dx > 0) {                                    /* x step */
                if (err-dy < ed) setPixel(x0,y0+sy, ed*Math.abs(err-dy));
                x0 += sx; dx -= xy; err += dy += yy;
             }
             if (yy1) {                                              /* y step */
                if (cur < ed) setPixel(x1+sx,y0, ed*Math.abs(cur));
                y0 += sy; dy -= xy; err += dx += xx;
             }
          } while (dy < dx);              /* gradient negates -> close curves */
       }
       drawLineAA(x0,y0, x2,y2);              /* plot remaining needle to end */
    }


//    void drawQuadBezierSegAntialiased(int x0, int y0, int x1, int y1, int x2, int y2)
//    {
//       int sx = x2-x1, sy = y2-y1;
//       long xx = x0-x1, yy = y0-y1, xy;         /* relative values for checks */
//       double dx, dy, err, ed, cur = xx*sy-yy*sx;                /* curvature */
//
//       assert(xx*sx >= 0 && yy*sy >= 0);  /* sign of gradient must not change */
//
//       if (sx*(long)sx+sy*(long)sy > xx*xx+yy*yy) { /* begin with longer part */
//          x2 = x0; x0 = sx+x1; y2 = y0; y0 = sy+y1; cur = -cur; /* swap P0 P2 */
//       }
//       if (cur != 0)
//       {                                                  /* no straight line */
//          xx += sx; xx *= sx = x0 < x2 ? 1 : -1;          /* x step direction */
//          yy += sy; yy *= sy = y0 < y2 ? 1 : -1;          /* y step direction */
//          xy = 2*xx*yy; xx *= xx; yy *= yy;         /* differences 2nd degree */
//          if (cur*sx*sy < 0) {                          /* negated curvature? */
//             xx = -xx; yy = -yy; xy = -xy; cur = -cur;
//          }
//          dx = 4.0*sy*(x1-x0)*cur+xx-xy;            /* differences 1st degree */
//          dy = 4.0*sx*(y0-y1)*cur+yy-xy;
//          xx += xx; yy += yy; err = dx+dy+xy;               /* error 1st step */
//          do {
//             cur = Math.min(dx+xy,-xy-dy);
//             ed = Math.max(dx+xy,-xy-dy);           /* approximate error distance */
//             ed = 255/(ed+2*ed*cur*cur/(4.*ed*ed+cur*cur));
//
//
//             // int c1 = (int) (cBack + (((c - cBack) * (ed*Math.abs(err-dx-dy-xy))) / 256));
//
//             setPixel(x0,y0, (int) (ed*Math.abs(err-dx-dy-xy)));
//             // setPixelAA(x0,y0, ed*Math.abs(err-dx-dy-xy));          /* plot curve */
//
//
//             if (x0 == x2 && y0 == y2) return;/* last pixel -> curve finished */
//             x1 = x0; cur = dx-err;
//             boolean b = 2*err+dy < 0;
//             if (2*err+dx > 0) {                                    /* x step */
//                if (err-dy < ed) {
//                    setPixel(x0,y0+sy, (int) (ed*Math.abs(err-dy)));
//                   // setPixelAA(x0,y0+sy, ed*Math.abs(err-dy));
//                }
//                x0 += sx; dx -= xy; err += dy += yy;
//             }
//             if (b) {                                              /* y step */
//                if (cur < ed) {
//                    setPixel(x1, y0, (int) (ed*Math.abs(cur)));
//                    // setPixelAA(x1+sx,y0, ed*Math.abs(cur));
//                }
//                y0 += sy; dy -= xy; err += dx += xx;
//             }
//          } while (dy < dx);              /* gradient negates -> close curves */
//       }
//       drawLineAntialiased(x0,y0, x2,y2);              /* plot remaining needle to end */
//    }

    private void setPixel(int x, int y, double vf) {
        int v = (int) vf;
        int s = backColor + (((color - backColor) * v) >> 8);
        imageData[x + y * width] = (byte) s;

    }

    // http://members.chello.at/easyfilter/bresenham.html
    public void drawLineAntialiasedWidth(int x0, int y0, int x1, int y1, double wdt) {
        int dx = Math.abs(x1 - x0), sx = x0 < x1 ? 1 : -1;
        int dy = Math.abs(y1 - y0), sy = y0 < y1 ? 1 : -1;
        int err = dx - dy, e2, x2, y2; /* error value e_xy */
        float ed = (dx + dy == 0) ? 1.0f : (float) Math.sqrt((float) dx * dx + (float) dy * dy);
        for (wdt = (wdt + 1) / 2;;) { /* pixel loop */
            int v = Math.max(0, (int) (255 * (Math.abs(err - dx + dy) / ed - wdt + 1)));
            setPixel(x0, y0, v);
//            int s = cBack + (((c - cBack) * v) >> 8);
//            imageData[x0 + y0 * width] = (byte) s;
            e2 = err;
            x2 = x0;
            if (2 * e2 >= -dx) { /* x step */
                for (e2 += dy, y2 = y0; e2 < ed * wdt && (y1 != y2 || dx > dy); e2 += dx)
                    v = Math.max(0, (int) (255 * (Math.abs(e2) / ed - wdt + 1)));
                y2 += sy;
                setPixel(x0, y2, v);
//                s = cBack + (((c - cBack) * v) >> 8);
//                imageData[x0 + y2 * width] = (byte) s;
                if (x0 == x1)
                    break;
                e2 = err;
                err -= dy;
                x0 += sx;
            }
            if (2 * e2 <= dy) { /* y step */
                for (e2 = dx - e2; e2 < ed * wdt && (x1 != x2 || dx < dy); e2 += dy)
                    v = Math.max(0, (int) (255 * (Math.abs(e2) / ed - wdt + 1)));
                x2 += sx;
                setPixel(x2, y0, v);
//                s = cBack + (((c - cBack) * v) >> 8);
//               imageData[x2 + y0 * width] = (byte) s;
                if (y0 == y1)
                    break;
                err += dx;
                y0 += sy;
            }
        }
    }

    public void drawLineAA(int x0, int y0, int x1, int y1) {
        if (x0 == x1 || y0 == y1) {
            drawLine(x0, y0, x1, y1);
            return;
        }
        if (x0 > x1) {
            int t = x0;
            x0 = x1;
            x1 = t;
        }
        if (y0 > y1) {
            int t = y0;
            y0 = y1;
            y1 = t;
        }
        int N = 8;
        int M = 15;
        int I = 255;
        double k = (double) (y1 - y0) / (x1 - x0);
        int d = (int) ((k * (1 << M)) + 0.5);
        imageData[x0 + y0 * width] = (byte) color;
        imageData[x1 + y1 * width] = (byte) color;
        int D = 0;
        while(true) {
            x0++;
            x1--;
            if (x0 > x1) {
                break;
            }
            D += d;
            if( D > (1 << M)) {
                y0++;
                y1--;
                D -= 1 << M;
            }
            int v = D >> (M - N);
            int c0 = backColor + (((color - backColor) * (I - v)) >> 8);
            int c1 = backColor + (((color - backColor) * v) >> 8);
            imageData[x0 + y0 * width] = (byte) c0;
            imageData[x1 + y1 * width] = (byte) c0;
            imageData[x0 + (y0 + 1) * width] = (byte) c1;
            imageData[x1 + (y1 - 1) * width] = (byte) c1;
        }
    }

    public void drawCircle( int x, int y, int r, boolean fill) {
        int t1 = r / 16;
        int x0 = r;
        int y0 = 0;
        while (x0 >= y0) {
            if (fill) {
                fillRectangle(x - x0, y + y0, x + x0, y + y0);
                fillRectangle(x - x0, y - y0, x + x0, y - y0);
                fillRectangle(x - y0, y + x0, x + y0, y + x0);
                fillRectangle(x - y0, y - x0, x + y0, y - x0);
            } else {
                imageData[(x + x0) + (y + y0) * width] = (byte) color;
                imageData[(x - x0) + (y + y0) * width] = (byte) color;
                imageData[(x - x0) + (y - y0) * width] = (byte) color;
                imageData[(x + x0) + (y - y0) * width] = (byte) color;
                imageData[(x + y0) + (y + x0) * width] = (byte) color;
                imageData[(x - y0) + (y + x0) * width] = (byte) color;
                imageData[(x - y0) + (y - x0) * width] = (byte) color;
                imageData[(x + y0) + (y - x0) * width] = (byte) color;
            }
            y0++;
            t1 += y0;
            int t2 = t1 - x0;
            if (t2 >= 0) {
                t1 = t2;
                x0--;
            }
        }
    }

    public void drawPoly(int[][] coordinates) {
        int x0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE;
        int y0 = Integer.MAX_VALUE, y1 = Integer.MIN_VALUE;
        for(int i = 0; i<coordinates.length; i++) {
            int[] poly = coordinates[i];
            for(int j = 0; j<poly.length; j+=2) {
                int x = poly[j], y = poly[j+1];
                x0 = Math.min(x0, x);
                y0 = Math.min(y0, y);
                x1 = Math.max(x1, x);
                y1 = Math.max(y1, y);
            }
        }
        for(int x = Math.max(0, x0); x < Math.min(x1, width); x++) {
            for(int y = Math.max(0, y0); y < Math.min(y1, height); y++) {
                if (windingNumber(x, y, coordinates) != 0) {
                    imageData[x + y * width] = (byte) color;
                }
            }
        }
    }

    private int windingNumber(int x, int y, int[][] coordinates) {
        int w = 0;
        for (int i = 0; i < coordinates.length; i++) {
            int[] poly = coordinates[i];
            for (int j = 0; j < poly.length - 2; j += 2) {
                int x0 = poly[j], y0 = poly[j + 1];
                int x1 = poly[j + 2], y1 = poly[j + 3];
                if (y0 <= y) {
                    if (y1 > y) {
                        if (isLeft(x0, y0, x1, y1, x, y) > 0) {
                            w++;
                        }
                    }
                } else {
                    if (y1 <= y) {
                        if (isLeft(x0, y0, x1, y1, x, y) < 0) {
                            w--;
                        }
                    }
                }
            }
        }
        return w;
    }

    // >0 for P2 left of the line through P0 and P1
    // =0 for P2  on the line
    // <0 for P2  right of the line
    private int isLeft(int x0, int y0, int x1, int y1, int x2, int y2) {
        return ( (x1 - x0) * (y2 - y0)
                - (x2 -  x0) * (y1 - y0) );
    }

    public void downscale(int factor) {
        for (int x = 0; x < width; x += factor) {
            for (int y = 0; y < height; y += factor) {
                int sum = 0;
                for (int x0 = 0; x0 < factor; x0++) {
                    for (int y0 = 0; y0 < factor; y0++) {
                        sum += imageData[(x + x0) + (y + y0) * width] & 0xff;
                    }
                }
                imageData[x / factor + y / factor * width] = (byte) (sum / factor / factor);
            }
        }
    }

}
