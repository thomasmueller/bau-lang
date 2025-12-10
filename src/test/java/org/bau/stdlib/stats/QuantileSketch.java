package org.bau.stdlib.stats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class QuantileSketch {

    private final Random random;
    private final List<List<Double>> levels = new ArrayList<>();
    private final int maxEntriesPerLevel;

    public QuantileSketch(Random random, int maxEntriesPerLevel) {
        this.random = random;
        this.maxEntriesPerLevel = maxEntriesPerLevel;
        levels.add(new ArrayList<>());
    }

    /**
     * Add an entry.
     *
     * @param x the value (NaN entries are ignored)
     */
    public void add(double x) {
        if (Double.isNaN(x)) {
            return;
        }
        List<Double> level0 = levels.get(0);
        level0.add(x);
        if (level0.size() > maxEntriesPerLevel) {
            compact(0);
        }
    }

    private void compact(int levelIndex) {
        List<Double> level = levels.get(levelIndex);
        Collections.sort(level);
        List<Double> nextLevelData = new ArrayList<>();
        int start = random.nextInt(2);
        for (int i = start; i < level.size(); i += 2) {
            nextLevelData.add(level.get(i));
        }
        level.clear();
        if (levels.size() == levelIndex + 1) {
            levels.add(new ArrayList<>());
        }
        List<Double> nextLevel = levels.get(levelIndex + 1);
        nextLevel.addAll(nextLevelData);
        if (nextLevel.size() > maxEntriesPerLevel) {
            compact(levelIndex + 1);
        }
    }

    /**
     * Get the approximate median. This is like calling quantile(0.5).
     *
     * @return the median
     */
    public double median() {
        return quantile(0.5);
    }

    /**
     * Get the approximate value at the specified quantile.
     *
     * @param q the quantile (0.5 for median)
     * @return the approximation
     */
    public double quantile(double q) {
        List<Pair> all = new ArrayList<>();
        for (int i = 0; i < levels.size(); i++) {
            int weight = 1 << i;
            for (double v : levels.get(i)) {
                all.add(new Pair(v, weight));
            }
        }
        all.sort(Comparator.comparingDouble(a -> a.value));
        long totalWeight = all.stream().mapToLong(p -> p.weight).sum();
        long rank = (long) Math.ceil(q * totalWeight);
        long sum = 0;
        for (Pair p : all) {
            sum += p.weight;
            if (sum >= rank) {
                return p.value;
            }
        }
        return all.get(all.size() - 1).value;
    }

    private static class Pair {
        final double value;
        final int weight;

        Pair(double v, int w) {
            value = v;
            weight = w;
        }
    }

}
