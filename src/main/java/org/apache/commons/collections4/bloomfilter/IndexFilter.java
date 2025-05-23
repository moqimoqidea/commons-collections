/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.collections4.bloomfilter;

import java.util.function.IntPredicate;

/**
 * A convenience class for Hasher implementations to filter out duplicate indices.
 *
 * <p><em>If the index is negative the behavior is not defined.</em></p>
 *
 * <p>This is conceptually a unique filter implemented as an {@link IntPredicate}.</p>
 *
 * @since 4.5.0-M1
 */
public final class IndexFilter {

    /**
     * An IndexTracker implementation that uses an array of integers to track whether or not a
     * number has been seen. Suitable for Shapes that have few hash functions.
     * @since 4.5.0
     */
    static class ArrayTracker implements IntPredicate {
        private final int[] seen;
        private int populated;

        /**
         * Constructs the tracker based on the shape.
         * @param shape the shape to build the tracker for.
         */
        ArrayTracker(final Shape shape) {
            seen = new int[shape.getNumberOfHashFunctions()];
        }

        @Override
        public boolean test(final int number) {
            if (number < 0) {
                throw new IndexOutOfBoundsException("number may not be less than zero. " + number);
            }
            for (int i = 0; i < populated; i++) {
                if (seen[i] == number) {
                    return false;
                }
            }
            seen[populated++] = number;
            return true;
        }
    }

    /**
     * An IndexTracker implementation that uses an array of bit maps to track whether or not a
     * number has been seen.
     */
    static class BitMapTracker implements IntPredicate {
        private final long[] bits;

        /**
         * Constructs a bit map based tracker for the specified shape.
         * @param shape The shape that is being generated.
         */
        BitMapTracker(final Shape shape) {
            bits = BitMaps.newBitMap(shape);
        }

        @Override
        public boolean test(final int number) {
            final boolean retval = !BitMaps.contains(bits, number);
            BitMaps.set(bits, number);
            return retval;
        }
    }

    /**
     * Creates an instance optimized for the specified shape.
     *
     * @param shape The shape that is being generated.
     * @param consumer The consumer to accept the values.
     * @return an IndexFilter optimized for the specified shape.
     */
    public static IntPredicate create(final Shape shape, final IntPredicate consumer) {
        return new IndexFilter(shape, consumer)::test;
    }

    private final IntPredicate tracker;

    private final int size;

    private final IntPredicate consumer;

    /**
     * Creates an instance optimized for the specified shape.
     *
     * @param shape The shape that is being generated.
     * @param consumer The consumer to accept the values.
     */
    private IndexFilter(final Shape shape, final IntPredicate consumer) {
        this.size = shape.getNumberOfBits();
        this.consumer = consumer;
        if (BitMaps.numberOfBitMaps(shape) * Long.BYTES < (long) shape.getNumberOfHashFunctions() * Integer.BYTES) {
            this.tracker = new BitMapTracker(shape);
        } else {
            this.tracker = new ArrayTracker(shape);
        }
    }

    /**
     * Test if the number should be processed by the {@code consumer}.
     *
     * <p>If the number has <em>not</em> been seen before it is passed to the {@code consumer} and the result returned.
     * If the number has been seen before the {@code consumer} is not called and {@code true} returned.</p>
     *
     * <p><em>If the input is not in the range [0,size) an IndexOutOfBoundsException exception is thrown.</em></p>
     *
     * @param number the number to check.
     * @return {@code true} if processing should continue, {@code false} otherwise.
     */
    public boolean test(final int number) {
        if (number >= size) {
            throw new IndexOutOfBoundsException(String.format("number too large %d >= %d", number, size));
        }
        if (tracker.test(number)) {
            return consumer.test(number);
        }
        return true;
    }
}
