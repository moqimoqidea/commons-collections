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
package org.apache.commons.collections4.iterators;

import java.util.Iterator;
import java.util.Map;

import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.ResettableIterator;

/**
 * Implements a {@code MapIterator} using a Map entrySet.
 * Reverse iteration is not supported.
 * <pre>
 * MapIterator it = map.mapIterator();
 * while (it.hasNext()) {
 *   Object key = it.next();
 *   Object value = it.getValue();
 *   it.setValue(newValue);
 * }
 * </pre>
 *
 * @param <K> the type of keys
 * @param <V> the type of mapped values
 * @since 3.0
 */
public class EntrySetMapIterator<K, V> implements MapIterator<K, V>, ResettableIterator<K> {

    private final Map<K, V> map;
    private Iterator<Map.Entry<K, V>> iterator;
    private Map.Entry<K, V> last;
    private boolean canRemove;

    /**
     * Constructs a new instance.
     *
     * @param map  the map to iterate over
     */
    public EntrySetMapIterator(final Map<K, V> map) {
        this.map = map;
        this.iterator = map.entrySet().iterator();
    }

    /**
     * Gets the current key, which is the key returned by the last call
     * to {@code next()}.
     *
     * @return the current key
     * @throws IllegalStateException if {@code next()} has not yet been called
     */
    @Override
    public K getKey() {
        if (last == null) {
            throw new IllegalStateException("Iterator getKey() can only be called after next() and before remove()");
        }
        return last.getKey();
    }

    /**
     * Gets the current value, which is the value associated with the last key
     * returned by {@code next()}.
     *
     * @return the current value
     * @throws IllegalStateException if {@code next()} has not yet been called
     */
    @Override
    public V getValue() {
        if (last == null) {
            throw new IllegalStateException("Iterator getValue() can only be called after next() and before remove()");
        }
        return last.getValue();
    }

    /**
     * Checks to see if there are more entries still to be iterated.
     *
     * @return {@code true} if the iterator has more elements
     */
    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    /**
     * Gets the next <em>key</em> from the {@code Map}.
     *
     * @return the next key in the iteration
     * @throws java.util.NoSuchElementException if the iteration is finished
     */
    @Override
    public K next() {
        last = iterator.next();
        canRemove = true;
        return last.getKey();
    }

    /**
     * Removes the last returned key from the underlying {@code Map}.
     * <p>
     * This method can be called once per call to {@code next()}.
     *
     * @throws UnsupportedOperationException if remove is not supported by the map
     * @throws IllegalStateException if {@code next()} has not yet been called
     * @throws IllegalStateException if {@code remove()} has already been called
     *  since the last call to {@code next()}
     */
    @Override
    public void remove() {
        if (!canRemove) {
            throw new IllegalStateException("Iterator remove() can only be called once after next()");
        }
        iterator.remove();
        last = null;
        canRemove = false;
    }

    /**
     * Resets the state of the iterator.
     */
    @Override
    public void reset() {
        iterator = map.entrySet().iterator();
        last = null;
        canRemove = false;
    }

    /**
     * Sets the value associated with the current key.
     *
     * @param value  the new value
     * @return the previous value
     * @throws UnsupportedOperationException if setValue is not supported by the map
     * @throws IllegalStateException if {@code next()} has not yet been called
     * @throws IllegalStateException if {@code remove()} has been called since the
     *  last call to {@code next()}
     */
    @Override
    public V setValue(final V value) {
        if (last == null) {
            throw new IllegalStateException("Iterator setValue() can only be called after next() and before remove()");
        }
        return last.setValue(value);
    }

    /**
     * Gets the iterator as a String.
     *
     * @return a string version of the iterator
     */
    @Override
    public String toString() {
        if (last != null) {
            return "MapIterator[" + getKey() + "=" + getValue() + "]";
        }
        return "MapIterator[]";
    }

}
