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
package org.apache.commons.collections4.collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections4.Transformer;
import org.apache.commons.collections4.TransformerUtils;
import org.junit.jupiter.api.Test;

/**
 * Extension of {@link AbstractCollectionTest} for exercising the {@link TransformedCollection}
 * implementation.
 */
public class TransformedCollectionTest extends AbstractCollectionTest<Object> {

    private static final class StringToInteger implements Transformer<Object, Object> {
        @Override
        public Object transform(final Object input) {
            return Integer.valueOf((String) input);
        }
    }

    private static final class ToLowerCase implements Transformer<Object, Object> {
        @Override
        public Object transform(final Object input) {
            return ((String) input).toLowerCase();
        }
    }

    public static final Transformer<Object, Object> NOOP_TRANSFORMER = TransformerUtils.nopTransformer();
    public static final Transformer<Object, Object> STRING_TO_INTEGER_TRANSFORMER = new StringToInteger();
    public static final Transformer<Object, Object> TO_LOWER_CASE_TRANSFORMER = new ToLowerCase();

    @Override
    public String getCompatibilityVersion() {
        return "4";
    }

    @Override
    public Object[] getFullElements() {
        return new Object[] {"1", "3", "5", "7", "2", "4", "6"};
    }

    @Override
    public Object[] getOtherElements() {
        return new Object[] {"9", "88", "678", "87", "98", "78", "99"};
    }

    @Override
    public Collection<Object> makeConfirmedCollection() {
        return new ArrayList<>();
    }

    @Override
    public Collection<Object> makeConfirmedFullCollection() {
        return new ArrayList<>(Arrays.asList(getFullElements()));
    }

    @Override
    public Collection<Object> makeFullCollection() {
        final List<Object> list = new ArrayList<>(Arrays.asList(getFullElements()));
        return TransformedCollection.transformingCollection(list, NOOP_TRANSFORMER);
    }

    @Override
    public Collection<Object> makeObject() {
        return TransformedCollection.transformingCollection(new ArrayList<>(), NOOP_TRANSFORMER);
    }

    @Test
    void testTransformedCollection() {
        final Collection<Object> coll = TransformedCollection.transformingCollection(new ArrayList<>(), STRING_TO_INTEGER_TRANSFORMER);
        assertEquals(0, coll.size());
        final Object[] elements = getFullElements();
        for (int i = 0; i < elements.length; i++) {
            coll.add(elements[i]);
            assertEquals(i + 1, coll.size());
            assertTrue(coll.contains(Integer.valueOf((String) elements[i])));
            assertFalse(coll.contains(elements[i]));
        }

        assertTrue(coll.remove(Integer.valueOf((String) elements[0])));
    }

    @Test
    void testTransformedCollection_decorateTransform() {
        final Collection<Object> originalCollection = new ArrayList<>();
        final Object[] elements = getFullElements();
        Collections.addAll(originalCollection, elements);
        final Collection<Object> collection = TransformedCollection.transformedCollection(originalCollection, TransformedCollectionTest.STRING_TO_INTEGER_TRANSFORMER);
        assertEquals(elements.length, collection.size());
        for (final Object element : elements) {
            assertTrue(collection.contains(Integer.valueOf((String) element)));
            assertFalse(collection.contains(element));
        }

        assertFalse(collection.remove(elements[0]));
        assertTrue(collection.remove(Integer.valueOf((String) elements[0])));
    }

//    void testCreate() throws Exception {
//        resetEmpty();
//        writeExternalFormToDisk((java.io.Serializable) getCollection(), "src/test/resources/data/test/TransformedCollection.emptyCollection.version4.obj");
//        resetFull();
//        writeExternalFormToDisk((java.io.Serializable) getCollection(), "src/test/resources/data/test/TransformedCollection.fullCollection.version4.obj");
//    }

}
