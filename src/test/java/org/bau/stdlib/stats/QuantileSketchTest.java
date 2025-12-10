package org.bau.stdlib.stats;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

public class QuantileSketchTest {

    @Test
    public void test() {
        Random random = new Random(1);
        QuantileSketch sketch = new QuantileSketch(random, 1000);
        int count = 10_000;
        double[] list = new double[count];
        for (int i = 0; i < count; i++) {
            double x = random.nextDouble();
            sketch.add(x);
            list[i] = x;
        }
        Arrays.sort(list);
        double diff;
        for(double d = 0.01; d < 1.0; d += 0.1) {
            diff = Math.abs(sketch.quantile(d) - list[(int) (count * d)]);
            assertTrue(diff < 0.002);
        }
    }

}
