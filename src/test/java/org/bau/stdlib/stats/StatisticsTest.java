package org.bau.stdlib.stats;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class StatisticsTest {

    @Test
    public void oneVariable() {
        Statistics stats = new Statistics();
        stats.add(Double.NaN);
        assertTrue(Double.isNaN(stats.mean()));
        assertTrue(Double.isNaN(stats.variancePopulation()));
        assertTrue(Double.isNaN(stats.varianceSample()));
        assertTrue(Double.NEGATIVE_INFINITY == stats.max());
        assertTrue(Double.POSITIVE_INFINITY == stats.min());
        assertTrue(Double.isNaN(stats.standardDeviationPopulation()));
        assertTrue(Double.isNaN(stats.standardDeviationSample()));
        assertTrue(0 == stats.sum());
        assertEquals(0, stats.count());

        stats.add(0);
        assertTrue(0 == stats.mean());
        assertTrue(0 == stats.variancePopulation());
        assertTrue(0 == stats.max());
        assertTrue(0 == stats.min());
        assertTrue(0 == stats.standardDeviationPopulation());
        assertTrue(0 == stats.sum());
        assertEquals(1, stats.count());

        stats = new Statistics();
        stats.add(10);
        assertEquals(1, stats.count());
        assertTrue(10 == stats.sum());
        stats.add(-10);
        assertEquals(2, stats.count());
        assertTrue(0 == stats.mean());
        assertTrue("" + stats.variancePopulation(), 100.0 == stats.variancePopulation());
        assertTrue("" + stats.varianceSample(), 200.0 == stats.varianceSample());
        assertTrue(10 == stats.max());
        assertTrue(-10 == stats.min());
        assertTrue("" + stats.standardDeviationPopulation(), 10.0 == stats.standardDeviationPopulation());
        assertTrue("" + stats.standardDeviationSample(), Math.sqrt(200) == stats.standardDeviationSample());
        assertTrue(0 == stats.sum());

        stats = new Statistics();
        for(int i=0; i<1_000_000; i++) {
            stats.add(1e-6);
        }
        assertTrue("" + stats.sum(), 1 == stats.sum());
        assertTrue("" + stats.mean(), 1e-6 == stats.mean());

    }


    @Test
    public void twoVariables() {
        Statistics2 s = new Statistics2();
        assertEquals(0, s.count());
        s.add(Double.NaN, 0);
        assertEquals(0, s.count());
        s.add(0, Double.NaN);
        assertEquals(0, s.count());
        assertTrue(Double.NEGATIVE_INFINITY == s.maxX());
        assertTrue(Double.POSITIVE_INFINITY == s.minX());
        assertTrue(Double.NEGATIVE_INFINITY == s.maxY());
        assertTrue(Double.POSITIVE_INFINITY == s.minY());
        assertTrue(Double.isNaN(s.covarianceSample()));
        assertTrue(Double.isNaN(s.varianceX()));
        assertTrue(Double.isNaN(s.varianceY()));
        assertTrue(Double.isNaN(s.meanX()));
        assertTrue(Double.isNaN(s.meanY()));
        assertTrue(Double.isNaN(s.sumOfProducts()));
        assertTrue(Double.isNaN(s.correlation()));
        assertTrue(Double.isNaN(s.intercept()));
        assertTrue(Double.isNaN(s.regressionR2()));
        s.add(2, 1);
        assertEquals(1, s.count());
        assertTrue(Double.isNaN(s.covarianceSample()));
        assertTrue(Double.isNaN(s.correlation()));
        assertTrue(Double.isNaN(s.slope()));
        assertTrue(Double.isNaN(s.varianceX()));
        assertTrue(Double.isNaN(s.varianceY()));
        assertTrue(Double.isNaN(s.regressionR2()));
        s.add(4, 2);
        assertEquals(2, s.count());
        s.add(5, 3);
        assertEquals(3, s.count());
        assertEquals(2.0, s.meanX(), 1e-12);
        assertEquals(11.0 / 3.0, s.meanY(), 1e-12);
        assertEquals(1.0, s.minX(), 1e-12);
        assertEquals(3.0, s.maxX(), 1e-12);
        assertEquals(2.0, s.minY(), 1e-12);
        assertEquals(5.0, s.maxY(), 1e-12);
        assertEquals(1.0, s.varianceX(), 1e-12);
        assertEquals(7.0 / 3.0, s.varianceY(), 1e-12);
        assertEquals(1.5, s.covarianceSample(), 1e-12);
        assertEquals(1.0, s.covariancePopulation(), 1e-12);
        assertEquals(1.5, s.slope(), 1e-12);
        assertEquals(2.0/3.0, s.intercept(), 1e-12);
        assertEquals(1.0, s.standardDeviationX(), 1e-12);
        assertEquals(Math.sqrt(7.0 / 3.0), s.standardDeviationY(), 1e-12);
        double expectedCorrelation = 1.5 / Math.sqrt((1.0) * (7.0/3.0));
        assertEquals(expectedCorrelation, s.correlation(), 1e-12);
        assertEquals(expectedCorrelation * expectedCorrelation, s.regressionR2(), 1e-12);

        s = new Statistics2();
        s.add(0, 1);
        s.add(0, 2);
        assertEquals(1.0, s.regressionR2(), 1e-12);
        assertEquals(0.0, s.slope(), 1e-12);
        assertTrue(Double.isNaN(s.correlation()));
    }

}
