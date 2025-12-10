package org.bau.stdlib.graphics;

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



    public void fillRectangle(int color, int x0, int y0, int x1, int y1) {
        for(int x = x0; x <= x1; x++) {
            for(int y = y0; y <= y1; y++) {
                imageData[x + y * width] = (byte) color;
            }
        }
    }

    // https://en.wikipedia.org/wiki/Bresenham%27s_line_algorithm
    public void drawLine(int color, int x0, int y0, int x1, int y1) {
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

    public void drawLineAntialiased(int c, int cBack, int x0, int y0, int x1, int y1) {
        if (x0 == x1 || y0 == y1) {
            drawLine(c, x0, y0, x1, y1);
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
        imageData[x0 + y0 * width] = (byte) c;
        imageData[x1 + y1 * width] = (byte) c;
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
            int c0 = cBack + (((c - cBack) * (I - v)) >> 8);
            int c1 = cBack + (((c - cBack) * v) >> 8);
            imageData[x0 + y0 * width] = (byte) c0;
            imageData[x1 + y1 * width] = (byte) c0;
            imageData[x0 + (y0 + 1) * width] = (byte) c1;
            imageData[x1 + (y1 - 1) * width] = (byte) c1;
        }
    }

    public void drawCircle(int color, int x, int y, int r, boolean fill) {
        int t1 = r / 16;
        int x0 = r;
        int y0 = 0;
        while (x0 >= y0) {
            if (fill) {
                fillRectangle(color, x - x0, y + y0, x + x0, y + y0);
                fillRectangle(color, x - x0, y - y0, x + x0, y - y0);
                fillRectangle(color, x - y0, y + x0, x + y0, y + x0);
                fillRectangle(color, x - y0, y - x0, x + y0, y - x0);
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

}
