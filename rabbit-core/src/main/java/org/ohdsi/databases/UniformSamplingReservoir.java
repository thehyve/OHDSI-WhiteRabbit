/*******************************************************************************
 * Copyright 2023 Observational Health Data Sciences and Informatics & The Hyve
 *
 * This file is part of WhiteRabbit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.ohdsi.databases;

import org.apache.commons.math3.random.RandomDataGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Uniform sampling reservoir for streaming. This should capture the input distribution in order
 * to compute quartiles, using so-called Algorithm R.
 *
 * <p>The maximum size of the reservoir can be increased to get more accurate quartile estimations.
 * As long as the number of samples is lower than the maximum size of the reservoir, the quartiles
 * are computed exactly. Otherwise they are an estimate. The minimum, maximum and average are always
 * calculated exactly.
 */
public class UniformSamplingReservoir {
    static Logger logger = LoggerFactory.getLogger(UniformSamplingReservoir.class);

    private double[] samples;
    private int maxSize;
    private long populationCount;
    private BigDecimal populationSum;
    private double populationMinimum = Double.POSITIVE_INFINITY;
    private double populationMaximum = Double.NEGATIVE_INFINITY;
    private int currentSampleLength;

    private static RandomUtil.ThreadLocalRandomWrapper randomWrapper = null;


    private boolean trimmed = false;

    /**
     * Create an empty reservoir.
     * @param maxSize maximum reservoir size.
     * @throws NullPointerException if given allValues are {@code null}
     */
    public UniformSamplingReservoir(int maxSize) {
        if (randomWrapper == null) {
            randomWrapper = new RandomUtil.ThreadLocalRandomWrapper();
        }
        if (maxSize <= 0) {
            throw new IllegalArgumentException("Reservoir maximum size must be strictly positive");
        }
        this.maxSize = maxSize;
        this.samples = new double[maxSize];
        this.populationSum = new BigDecimal(0);
        this.populationCount = 0;
        this.currentSampleLength = 0;
    }

    /** Add a sample to the reservoir. */
    public void add(double value) {
        if (trimmed) {
            return;
        }

        if (currentSampleLength == maxSize) {
            long removeIndex = randomWrapper.nextLong(populationCount);
            if (removeIndex < maxSize) {
                removeAndAdd((int)removeIndex, value);
            }
        } else {
            removeAndAdd(currentSampleLength, value);
            currentSampleLength++;
        }

        populationSum = populationSum.add(new BigDecimal(value));
        populationMinimum = Math.min(value, populationMinimum);
        populationMaximum = Math.max(value, populationMaximum);
        populationCount++;
    }

    private void removeAndAdd(int removeIndex, double value) {
        if (trimmed) {
            return;
        }
        int addIndex = Arrays.binarySearch(samples, 0, currentSampleLength, value);
        if (addIndex < 0) {
            addIndex = -addIndex - 1;
        }

        // start: [a, b, c, d, e]
        if (removeIndex < addIndex) {
            // removeIndex = 2, value = d2 -> addIndex = 4
            addIndex--;
            // new addIndex -> 3
            // copy(3 -> 2, len 1)
            // end: [a, b, d, d2, e]
            if (removeIndex < addIndex) {
                System.arraycopy(samples, removeIndex + 1, samples, removeIndex,
                        addIndex - removeIndex);
            }
        } else if (removeIndex > addIndex) {
            // removeIndex = 2, value = a2 -> addIndex = 1
            // copy(1 -> 2, len 1)
            // end: [a, a2, b, d, e]
            System.arraycopy(samples, addIndex, samples, addIndex + 1,
                    removeIndex - addIndex);
        }

        samples[addIndex] = value;
    }

    public void setTrimmed(boolean b) {
        this.trimmed = true;
    }

    /**
     * Get the quartiles of the underlying distribution. If the number of samples is larger than
     * the maximum size of the reservoir, this will be an estimate.
     * @return list with size three, of the 25, 50 and 75 percentiles.
     */
    public List<Double> getSampleQuartiles() {
        List<Double> quartiles = new ArrayList<>(3);

        switch (currentSampleLength) {
            case 0:
                quartiles.add(Double.NaN);
                quartiles.add(Double.NaN);
                quartiles.add(Double.NaN);
                break;
            case 1:
                quartiles.add(samples[0]);
                quartiles.add(samples[0]);
                quartiles.add(samples[0]);
                break;
            default:
                for (int i = 1; i <= 3; i++) {
                    double pos = i * (currentSampleLength + 1) * 0.25d; // 25 percentile steps
                    int intPos = (int) pos;
                    if (intPos == 0) {
                        quartiles.add(samples[0]);
                    } else if (intPos == currentSampleLength) {
                        quartiles.add(samples[currentSampleLength - 1]);
                    } else {
                        double diff = pos - intPos;
                        double base = samples[intPos - 1];
                        quartiles.add(base + diff * (samples[intPos] - base));
                    }
                }
                break;
        }

        return quartiles;
    }

    public double getSampleMean() {
        return Arrays.stream(samples).limit(currentSampleLength).sum() / currentSampleLength;
    }

    /**
     * Get the standard deviation of the underlying distribution. If the number of samples is larger than
     * the maximum size of the reservoir, this will be an estimate.
     * @return double standard deviation
     */
    public double getSampleStandardDeviation() {
        double sampleMean = getSampleMean();
        double varianceSum = Arrays.stream(samples).limit(currentSampleLength).map(x -> Math.pow(x - sampleMean, 2d)).sum();
        return Math.sqrt(varianceSum / (currentSampleLength-1));
    }

    public double getPopulationMean() {
        if (populationCount == 0) {
            return Double.NaN;
        }
        BigDecimal popCount = BigDecimal.valueOf(populationCount);
        return populationSum.divide(popCount, populationSum.scale() + popCount.precision(), RoundingMode.HALF_UP).doubleValue();
    }

    public double getPopulationMinimum() {
        return populationMinimum;
    }

    public double getPopulationMaximum() {
        return populationMaximum;
    }

    /** Get the currently stored samples. */
    public List<Double> getSamples() {
        List<Double> doubleList = new ArrayList<>(currentSampleLength);
        for (int i = 0; i < currentSampleLength; i++) {
            doubleList.add(samples[i]);
        }
        return doubleList;
    }

    /** Get the maximum size of this reservoir. */
    public int getMaxSize() {
        return maxSize;
    }

    /** Get the number of samples that are being represented by the reservoir. */
    public long getCount() {
        return populationCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UniformSamplingReservoir that = (UniformSamplingReservoir) o;
        return populationCount == that.populationCount
                && maxSize == that.maxSize
                && Arrays.equals(samples, that.samples);
    }

    @Override
    public int hashCode() {
        return Objects.hash(samples, maxSize, populationCount);
    }

    @Override
    public String toString() {
        return "UniformSamplingReservoir{"
                + "samples=" + Arrays.toString(samples)
                + ", maxSize=" + maxSize
                + ", count=" + populationCount
                + '}';
    }

    public static void main(String[] args) {
        UniformSamplingReservoir us = new UniformSamplingReservoir(50);
        for (int i = 0; i < 21; i++) {
            us.add(i);
        }

        if (logger.isInfoEnabled()) {
            logger.info(us.getSamples().toString());
            logger.info(String.valueOf(us.getCount()));
            logger.info(us.getSampleQuartiles().toString());
            logger.info(String.valueOf(us.populationSum.doubleValue()));
            logger.info(String.valueOf(us.getPopulationMean()));
            logger.info(String.valueOf(us.getPopulationMinimum()));
            logger.info(String.valueOf(us.getPopulationMaximum()));
            logger.info(String.valueOf(us.getSampleMean()));
            logger.info(String.valueOf(us.getSampleStandardDeviation()));
        }
    }

    /**
     * Production code uses type ThreadLocalRandom, which cannot be seeded. For test purposed, where repeatability is
     * desirable, this can be set to a seeded behaviour, provided that RandomUtil.setSeed() is called before
     * ThreadLocalRandomWrapper.nextLong() is called.
     *
     * Note: nextLong() will throw an ArithmeticException if it is called with an upperbound exceeding Integer.MAX_VALUE,
     * and a seed was set.
     */
    public static class RandomUtil {
        private static long seed = 0;   // 0 will give normal ThreadRandomLocal behaviour, to maintain the original implementation
        private static final ThreadLocal<Random> RANDOM_THREAD_LOCAL = ThreadLocal.withInitial(Random::new);

        public static class ThreadLocalRandomWrapper {
            private ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
            private RandomDataGenerator randomDataGenerator = new RandomDataGenerator();

            public ThreadLocalRandomWrapper() {
                if (seed != 0) {
                    randomDataGenerator.reSeed(seed);
                    logger.warn("Initializing {} with seed {}, next value {}", ThreadLocalRandomWrapper.class.getCanonicalName(), RandomUtil.seed, this.nextLong(seed));
                }
            }

            public long nextLong(long upperBound) {
                if (seed == 0) {
                    return threadLocalRandom.nextLong(upperBound);
                } else {
                    int checkedUpperBound = Math.toIntExact(upperBound); // causes an exception if upperBound > Integer.MAX_VALUE
                    return randomDataGenerator.nextInt(0, checkedUpperBound);
                }
            }
        }
        public static void setSeed(long seed) {
            RandomUtil.seed = seed;
        }
    }
}