package org.bau.stdlib.stats;

public class Statistics {

    private long count;
    private double m2;
    private double mean;
    private double sum;
    private double sumCompensation;
    private double min = Double.POSITIVE_INFINITY;
    private double max = Double.NEGATIVE_INFINITY;

    /**
     * Adds a value. NaN entries are ignored.
     *
     * @param x
     */
    void add(double x) {
        if (Double.isNaN(x)) {
            return;
        }
        // Calculate the mean using Welford's method
        // https://en.wikipedia.org/wiki/Algorithms_for_calculating_variance
        // https://www.johndcook.com/standard_deviation.html
        // and the sum using Kahan summation
        // https://en.wikipedia.org/wiki/Kahan_summation_algorithm
        count++;
        double d = x - sumCompensation;
        double temp = sum + d;
        sumCompensation = (temp - sum) - d;
        sum = temp;
        if (count == 1) {
            mean = x;
            m2 = 0;
        } else {
            double delta = x - mean;
            mean += delta / count;
            m2 += delta * (x - mean);
        }
        min = Math.min(min, x);
        max = Math.max(max, x);
    }

    double standardDeviationSample() {
        if (count < 2) {
            return Double.NaN;
        }
        return Math.sqrt(m2 / (count - 1));
    }

    double varianceSample() {
        if (count < 2) {
            return Double.NaN;
        }
        return m2 / (count - 1);
    }

    double variancePopulation() {
        if (count < 1) {
            return Double.NaN;
        }
        return m2 / count;
    }

    double standardDeviationPopulation() {
        if (count < 1) {
            return Double.NaN;
        }
        return Math.sqrt(m2 / count);
    }

    double sumSquaredDiffs() {
        return m2;
    }

    double sum() {
        return sum;
    }

    long count() {
        return count;
    }

    double mean() {
        if (count == 0) {
            return Double.NaN;
        }
        return mean;
    }

    double min() {
        return min;
    }

    double max() {
        return max;
    }

}
