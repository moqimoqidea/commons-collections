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
package org.apache.commons.collections4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.apache.commons.collections4.multiset.HashMultiSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for MultiSetUtils.
 */
class MultiSetUtilsTest {

    private String[] fullArray;
    private MultiSet<String> multiSet;

    @BeforeEach
    public void setUp() {
        fullArray = new String[]{
            "a", "a", "b", "c", "d", "d", "d"
        };
        multiSet = new HashMultiSet<>(Arrays.asList(fullArray));
    }

    /**
     * Tests {@link MultiSetUtils#emptyMultiSet()}.
     */
    @Test
    void testEmptyMultiSet() {
        final MultiSet<Integer> empty = MultiSetUtils.emptyMultiSet();
        assertEquals(0, empty.size());

        assertThrows(UnsupportedOperationException.class, () -> empty.add(55),
                "Empty multi set must be read-only");
    }

    /**
     * Tests {@link MultiSetUtils#predicatedMultiSet(org.apache.commons.collections4.MultiSet, org.apache.commons.collections4.Predicate)}.
     */
    @Test
    void testPredicatedMultiSet() {
        final Predicate<String> predicate = object -> object.length() == 1;
        final MultiSet<String> predicated = MultiSetUtils.predicatedMultiSet(multiSet, predicate);
        assertEquals(multiSet.size(), predicated.size());
        assertEquals(multiSet.getCount("a"), predicated.getCount("a"));

        assertThrows(NullPointerException.class, () -> MultiSetUtils.predicatedMultiSet(null, predicate),
                "Expecting NPE");

        assertThrows(NullPointerException.class, () -> MultiSetUtils.predicatedMultiSet(multiSet, null),
                "Expecting NPE");

        assertThrows(IllegalArgumentException.class, () -> MultiSetUtils.predicatedMultiSet(multiSet, object -> object.equals("a")),
                "Predicate is violated for all elements not being 'a'");
    }

    /**
     * Tests {@link MultiSetUtils#unmodifiableMultiSet(org.apache.commons.collections4.MultiSet) ()}.
     */
    @Test
    void testSynchronizedMultiSet() {
        final MultiSet<String> synced = MultiSetUtils.synchronizedMultiSet(multiSet);
        assertEquals(multiSet, synced);
        synced.add("a"); // ensure adding works
    }

    /**
     * Tests {@link MultiSetUtils#unmodifiableMultiSet(org.apache.commons.collections4.MultiSet) ()}.
     */
    @Test
    void testUnmodifiableMultiSet() {
        final MultiSet<String> unmodifiable = MultiSetUtils.unmodifiableMultiSet(multiSet);
        assertEquals(multiSet, unmodifiable);

        assertThrows(UnsupportedOperationException.class, () -> unmodifiable.add("a"),
                "Empty multi set must be read-only");

        assertThrows(NullPointerException.class, () -> MultiSetUtils.unmodifiableMultiSet(null),
                "Expecting NPE");
    }

}
