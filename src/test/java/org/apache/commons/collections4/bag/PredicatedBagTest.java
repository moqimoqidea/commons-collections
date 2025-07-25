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
package org.apache.commons.collections4.bag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.collections4.functors.TruePredicate;
import org.junit.jupiter.api.Test;

/**
 * Extension of {@link AbstractBagTest} for exercising the {@link PredicatedBag}
 * implementation.
 */
public class PredicatedBagTest<T> extends AbstractBagTest<T> {

    protected Predicate<T> truePredicate = TruePredicate.<T>truePredicate();

    protected Bag<T> decorateBag(final HashBag<T> bag, final Predicate<T> predicate) {
        return PredicatedBag.predicatedBag(bag, predicate);
    }

    @Override
    public String getCompatibilityVersion() {
        return "4";
    }

    @Override
    protected int getIterationBehaviour() {
        return UNORDERED;
    }

    @Override
    public Bag<T> makeObject() {
        return decorateBag(new HashBag<>(), truePredicate);
    }

    protected Bag<T> makeTestBag() {
        return decorateBag(new HashBag<>(), stringPredicate());
    }

    protected Predicate<T> stringPredicate() {
        return String.class::isInstance;
    }

    @Test
    @SuppressWarnings("unchecked")
    void testIllegalAdd() {
        final Bag<T> bag = makeTestBag();
        final Integer i = 3;

        assertThrows(IllegalArgumentException.class, () -> bag.add((T) i));

        assertFalse(bag.contains(i), "Collection shouldn't contain illegal element");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testIllegalDecorate() {
        final HashBag<Object> elements = new HashBag<>();
        elements.add("one");
        elements.add("two");
        elements.add(3);
        elements.add("four");

        assertThrows(IllegalArgumentException.class, () -> decorateBag((HashBag<T>) elements, stringPredicate()));

        assertThrows(NullPointerException.class, () -> decorateBag(new HashBag<>(), null));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testLegalAddRemove() {
        final Bag<T> bag = makeTestBag();
        assertEquals(0, bag.size());
        final T[] els = (T[]) new Object[] { "1", "3", "5", "7", "2", "4", "1" };
        for (int i = 0; i < els.length; i++) {
            bag.add(els[i]);
            assertEquals(i + 1, bag.size());
            assertTrue(bag.contains(els[i]));
        }
        Set<T> set = bag.uniqueSet();
        assertTrue(set.contains(els[0]), "Unique set contains the first element");
        assertTrue(bag.remove(els[0]));
        set = bag.uniqueSet();
        assertFalse(set.contains(els[0]), "Unique set now does not contain the first element");
    }

//    void testCreate() throws Exception {
//        Bag<T> bag = makeObject();
//        writeExternalFormToDisk((java.io.Serializable) bag, "src/test/resources/data/test/PredicatedBag.emptyCollection.version4.obj");
//        bag = makeFullCollection();
//        writeExternalFormToDisk((java.io.Serializable) bag, "src/test/resources/data/test/PredicatedBag.fullCollection.version4.obj");
//    }

}
