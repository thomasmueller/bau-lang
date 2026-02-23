package org.bau.stdlib.graphics;

import java.util.ArrayList;

public class SvgUtils {

    private static final double MAX_DIST = 0.01;
    private final String path;
    private int pos;
    private char token;
    private double value;

    public SvgUtils(String path) {
        this.path = path;
    }

    int[][] convertPathToLines(int factor) {
        read();
        ArrayList<ArrayList<Double>> polys = new ArrayList<>();
        ArrayList<Double> cp = new ArrayList<>();
        double xp = 0, yp = 0;
        double xs = 0, ys = 0;
        while(true) {
            if (token == 0) {
                break;
            }
            switch (token) {
            case 'm':
                read();
                double x = readValue(), y = readValue();
                xs += x;
                ys += y;
                xp += x;
                yp += y;
                while (token == '0') {
                    x = readValue();
                    y = readValue();
                    cp.add(xp);
                    cp.add(yp);
                    xp += x;
                    yp += y;

                };
                break;
            case 'M':
                read();
                xs = xp = readValue();
                ys = yp = readValue();
                while (token == '0') {
                    cp.add(xp);
                    cp.add(yp);
                    xp = readValue();
                    yp = readValue();
                };
                break;
            case 'h':
                read();
                do {
                    cp.add(xp);
                    cp.add(yp);
                    xp += readValue();
                } while (token == '0');
                break;
            case 'H':
                read();
                do {
                    cp.add(xp);
                    cp.add(yp);
                    xp = readValue();
                } while (token == '0');
                break;
            case 'L':
                read();
                do {
                    cp.add(xp);
                    cp.add(yp);
                    xp = readValue();
                    yp = readValue();
                } while (token == '0');
                break;
            case 'l':
                read();
                do {
                    cp.add(xp);
                    cp.add(yp);
                    xp += readValue();
                    yp += readValue();
                } while (token == '0');
                break;
            case 'v':
                read();
                do {
                    cp.add(xp);
                    cp.add(yp);
                    yp += readValue();
                } while (token == '0');
                break;
            case 'V':
                read();
                do {
                    cp.add(xp);
                    cp.add(yp);
                    yp = readValue();
                } while (token == '0');
                break;
            case 'z':
            case 'Z':
                read();
                cp.add(xp);
                cp.add(yp);
                cp.add(xs);
                cp.add(ys);
                xp = xs;
                yp = ys;
                polys.add(cp);
                cp = new ArrayList<>();
                break;
            case 'q': {
                read();
                do {
                    cp.add(xp);
                    cp.add(yp);
                    double x1 = xp + readValue();
                    double y1 = yp + readValue();
                    double x2 = xp + readValue();
                    double y2 = yp + readValue();
                    if (dist(xp, yp, x1, y1, x2, y2) > MAX_DIST) {
                        splitBezier(cp, xp, yp, x1, y1, x2, y2);
                    }
                    xp = x2;
                    yp = y2;
                } while (token == '0');
                break;
            }
            case 'Q': {
                read();
                do {
                    cp.add(xp);
                    cp.add(yp);
                    double x1 = readValue();
                    double y1 = readValue();
                    double x2 = readValue();
                    double y2 = readValue();
                    if (dist(xp, yp, x1, y1, x2, y2) > MAX_DIST) {
                        splitBezier(cp, xp, yp, x1, y1, x2, y2);
                    }
                    xp = x2;
                    yp = y2;
                } while (token == '0');
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported command: " + token + " at \n" + path.substring(0, pos) + " [*] " + path.substring(pos));
            }
        }
        int[][] poly = new int[polys.size()][];
        int polyCount = 0;
        for (int i = 0; i < polys.size(); i++) {
            ArrayList<Double> p = polys.get(i);
            int[] co = new int[p.size()];
            for (int j = 0; j < p.size(); j++) {
                co[j] = (int) (p.get(j).doubleValue() * factor);
                polyCount++;
            }
            poly[i] = co;
        }
        System.out.println("polys: " + polyCount);
        return poly;
    }

    private static double dist(double x0, double y0, double x1, double y1, double x2, double y2) {
        double xm = (x2 + x0) / 2, ym = (y2 + y0) / 2;
        double xd = xm - x1, yd = ym - y1;
        return xd * xd + yd * yd;
    }

    private void splitBezier(ArrayList<Double> cp, double x0, double y0, double x1, double y1, double x2, double y2) {
        double xr1 = (x0 + x1) / 2, yr1 = (y0 + y1) / 2;
        double xr2 = (x1 + x2) / 2, yr2 = (y1 + y2) / 2;
        double xs = (xr1 + xr2) / 2, ys = (yr1 + yr2) / 2;
        if (dist(x0, y0, xr1, yr1, xs, ys) > MAX_DIST) {
            splitBezier(cp, x0, y0, xr1, yr1, xs, ys);
        } else {
            cp.add(xs);
            cp.add(ys);
        }
        if (dist(xs, ys, xr2, yr2, x2, y2) > MAX_DIST) {
            splitBezier(cp, xs, ys, xr2, yr2, x2, y2);
        }
    }

    private double readValue() {
        if (token != '0') {
            throw new IllegalStateException();
        }
        double r = value;
        read();
        return r;
    }

    private void read() {
        token = 0;
        if (pos >= path.length()) {
            return;
        }
        while (true) {
            token = path.charAt(pos++);
            if (token > ' ' && token != ',') {
                break;
            }
        }
        if ((token >= '0' && token <= '9') || token == '+' || token == '-') {
            int sign = 1;
            value = 0;
            if (token == '-') {
                sign = -1;
            } else if (token != '+') {
                value = token - '0';
            }
            long div = 0;
            while(pos < path.length()) {
                token = path.charAt(pos);
                if (token >= '0' && token <= '9') {
                    value = value * 10 + token - '0';
                    if (div > 0) {
                        div *= 10;
                    }
                } else if (token == '.') {
                    div = 1;
                } else {
                    break;
                }
                pos++;
            }
            token = '0';
            value = sign * value;
            if (div != 0) {
                value /= div;
            }
            return;
        }
        if (token >= 'a' && token <= 'z' || token >= 'A' && token <= 'Z') {
            // ok
        } else {
            throw new IllegalStateException();
        }
    }

}
