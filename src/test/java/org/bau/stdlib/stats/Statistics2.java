package org.bau.stdlib.stats;

public class Statistics2 {

    private double sumY, sumX, sumYX;
    private double m2y, meanY;
    private double m2x, meanX;
    private double minX = Double.POSITIVE_INFINITY;
    private double maxX = Double.NEGATIVE_INFINITY;
    private double minY = Double.POSITIVE_INFINITY;
    private double maxY = Double.NEGATIVE_INFINITY;
    private long count;

    void add(double y, double x) {
        if (Double.isNaN(x) || Double.isNaN(y)) {
            return;
        }
        sumY += y;
        sumX += x;
        sumYX += y * x;
        count++;
        if (count == 1) {
            meanY = y;
            meanX = x;
            m2x = m2y = 0;
        } else {
            double delta = y - meanY;
            meanY += delta / count;
            m2y += delta * (y - meanY);
            delta = x - meanX;
            meanX += delta / count;
            m2x += delta * (x - meanX);
        }
        minX = Math.min(minX, x);
        minY = Math.min(minY, y);
        maxX = Math.max(maxX, x);
        maxY = Math.max(maxY, y);
    }

    double standardDeviationX() {
        return Math.sqrt(varianceX());
    }

    double standardDeviationY() {
        return Math.sqrt(varianceY());
    }

    double covariancePopulation() {
        return sumOfProducts() / count;
    }

    double covarianceSample() {
        if (count < 2) {
            return Double.NaN;
        }
        return sumOfProducts() / (count - 1);
    }

    double meanX() {
        if (count < 1) {
            return Double.NaN;
        }
        return meanX;
    }

    double meanY() {
        if (count < 1) {
            return Double.NaN;
        }
        return meanY;
    }

    double intercept() {
        if (m2x == 0) {
            return Double.NaN;
        }
        return meanY - slope() * meanX;
    }

    double sumOfProducts() {
        if (count < 1) {
            return Double.NaN;
        }
        return sumYX - sumX * sumY / count;
    }

    double correlation() {
        double div = Math.sqrt(m2y * m2x);
        if (count < 2 || div == 0) {
            return Double.NaN;
        }
        return sumOfProducts() / div;
    }

    double slope() {
        if (m2x == 0) {
            return Double.NaN;
        }
        return sumOfProducts() / m2x;
    }

    double regressionR2() {
        if (m2x == 0) {
            return Double.NaN;
        }
        if (m2y == 0) {
            return 1.0;
        }
        double v = sumOfProducts();
        return v * v / (m2y * m2x);
    }

    long count() {
        return count;
    }

    double varianceX() {
        if (count < 2) {
            return Double.NaN;
        }
        return m2x / (count - 1);
    }

    double varianceY() {
        if (count < 2) {
            return Double.NaN;
        }
        return m2y / (count - 1);
    }

    double minX() {
        return minX;
    }

    double maxX() {
        return maxX;
    }

    double minY() {
        return minY;
    }

    double maxY() {
        return maxY;
    }

}
