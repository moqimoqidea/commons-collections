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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Comparator;

import org.apache.commons.collections4.Predicate;
import org.apache.commons.collections4.SortedBag;
import org.apache.commons.collections4.functors.TruePredicate;
import org.junit.jupiter.api.Test;

/**
 * Extension of {@link AbstractSortedBagTest} for exercising the {@link PredicatedSortedBag}
 * implementation.
 */
public class PredicatedSortedBagTest<T> extends AbstractSortedBagTest<T> {

    private final SortedBag<T> nullBag = null;

    protected Predicate<T> truePredicate = TruePredicate.<T>truePredicate();

    protected SortedBag<T> decorateBag(final SortedBag<T> bag, final Predicate<T> predicate) {
        return PredicatedSortedBag.predicatedSortedBag(bag, predicate);
    }

    @Override
    public String getCompatibilityVersion() {
        return "4";
    }

    @Override
    public SortedBag<T> makeObject() {
        return decorateBag(new TreeBag<>(), truePredicate);
    }

    protected SortedBag<T> makeTestBag() {
        return decorateBag(new TreeBag<>(), stringPredicate());
    }

    protected Predicate<T> stringPredicate() {
        return String.class::isInstance;
    }

    @Test
    void testDecorate() {
        final SortedBag<T> bag = decorateBag(new TreeBag<>(), stringPredicate());
        ((PredicatedSortedBag<T>) bag).decorated();

        assertThrows(NullPointerException.class, () -> decorateBag(new TreeBag<>(), null));

        assertThrows(NullPointerException.class, () -> decorateBag(nullBag, stringPredicate()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testSortOrder() {
        final SortedBag<T> bag = decorateBag(new TreeBag<>(), stringPredicate());
        final String one = "one";
        final String two = "two";
        final String three = "three";
        bag.add((T) one);
        bag.add((T) two);
        bag.add((T) three);
        assertEquals(bag.first(), one, "first element");
        assertEquals(bag.last(), two, "last element");
        final Comparator<? super T> c = bag.comparator();
        assertNull(c, "natural order, so comparator should be null");
    }

//    void testCreate() throws Exception {
//        Bag<T> bag = makeObject();
//        writeExternalFormToDisk((java.io.Serializable) bag, "src/test/resources/data/test/PredicatedSortedBag.emptyCollection.version4.obj");
//        bag = makeFullCollection();
//        writeExternalFormToDisk((java.io.Serializable) bag, "src/test/resources/data/test/PredicatedSortedBag.fullCollection.version4.obj");
//    }

}
