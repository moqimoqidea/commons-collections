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

import java.util.Arrays;
import java.util.Objects;
import java.util.function.LongPredicate;

/**
 * Produces bit map longs for a Bloom filter.
 * <p>
 * Each bit map is a little-endian long value representing a block of bits of in a filter.
 * </p>
 * <p>
 * The returned array will have length {@code ceil(m / 64)} where {@code m} is the number of bits in the filter and {@code ceil} is the ceiling function. Bits
 * 0-63 are in the first long. A value of 1 at a bit position indicates the bit index is enabled.
 * </p>
 * <p>
 * <em>The default implementations of the {@code makePredicate()} and {@code asBitMapArray} methods are slow and should be reimplemented in the implementing
 * classes where possible.</em>
 * </p>
 *
 * @since 4.5.0-M2
 */
@FunctionalInterface
public interface BitMapExtractor {

    /**
     * Creates a BitMapExtractor from an array of Long.
     *
     * @param bitMaps the bit maps to return.
     * @return a BitMapExtractor.
     */
    static BitMapExtractor fromBitMapArray(final long... bitMaps) {
        return new BitMapExtractor() {
            @Override
            public long[] asBitMapArray() {
                return Arrays.copyOf(bitMaps, bitMaps.length);
            }

            @Override
            public boolean processBitMapPairs(final BitMapExtractor other, final LongBiPredicate func) {
                final CountingLongPredicate p = new CountingLongPredicate(bitMaps, func);
                return other.processBitMaps(p) && p.processRemaining();
            }

            @Override
            public boolean processBitMaps(final LongPredicate predicate) {
                for (final long word : bitMaps) {
                    if (!predicate.test(word)) {
                        return false;
                    }
                }
                return true;
            }
        };
    }

    /**
     * Creates a BitMapExtractor from an IndexExtractor.
     *
     * @param extractor the IndexExtractor that specifies the indexes of the bits to enable.
     * @param numberOfBits the number of bits in the Bloom filter.
     * @return A BitMapExtractor that produces the bit maps equivalent of the Indices from the extractor.
     */
    static BitMapExtractor fromIndexExtractor(final IndexExtractor extractor, final int numberOfBits) {
        Objects.requireNonNull(extractor, "extractor");

        final long[] result = BitMaps.newBitMap(numberOfBits);
        extractor.processIndices(i -> {
            BitMaps.set(result, i);
            return true;
        });
        return fromBitMapArray(result);
    }

    /**
     * Return a copy of the BitMapExtractor data as a bit map array.
     * <p>
     * The default implementation of this method is slow. It is recommended
     * that implementing classes reimplement this method.
     * </p>
     * @return An array of bit map data.
     */
    default long[] asBitMapArray() {
        final class Bits {
            private long[] data = new long[16];
            private int size;

            boolean add(final long bits) {
                if (size == data.length) {
                    // This will throw an out-of-memory error if there are too many bits.
                    // Since bits are addressed using 32-bit signed integer indices
                    // the maximum length should be ~2^31 / 2^6 = ~2^25.
                    // Any more is a broken implementation.
                    data = Arrays.copyOf(data, size * 2);
                }
                data[size++] = bits;
                return true;
            }

            long[] toArray() {
                // Edge case to avoid a large array copy
                return size == data.length ? data : Arrays.copyOf(data, size);
            }
        }
        final Bits bits = new Bits();
        processBitMaps(bits::add);
        return bits.toArray();
    }

    /**
     * Applies the {@code func} to each bit map pair in order. Will apply all of the bit maps from the other BitMapExtractor to this extractor. If this
     * extractor does not have as many bit maps it will provide 0 (zero) for all excess calls to the LongBiPredicate.
     * <p>
     * <em>The default implementation of this method uses {@code asBitMapArray()}. It is recommended that implementations of BitMapExtractor that have local
     * arrays reimplement this method.</em>
     * </p>
     *
     * @param other The other BitMapExtractor that provides the y values in the (x,y) pair.
     * @param func  The function to apply.
     * @return A LongPredicate that tests this BitMapExtractor's bitmap values in order.
     */
    default boolean processBitMapPairs(final BitMapExtractor other, final LongBiPredicate func) {
        final CountingLongPredicate p = new CountingLongPredicate(asBitMapArray(), func);
        return other.processBitMaps(p) && p.processRemaining();
    }

    /**
     * Each bit map is passed to the predicate in order. The predicate is applied to each
     * bit map value, if the predicate returns {@code false} the execution is stopped, {@code false}
     * is returned, and no further bit maps are processed.
     *
     * <p>If the extractor is empty this method will return true.</p>
     *
     * <p>Any exceptions thrown by the action are relayed to the caller.</p>
     *
     * @param predicate the function to execute
     * @return {@code true} if all bit maps returned {@code true}, {@code false} otherwise.
     * @throws NullPointerException if the specified consumer is null
     */
    boolean processBitMaps(LongPredicate predicate);
}
