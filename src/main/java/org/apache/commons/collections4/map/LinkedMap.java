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
package org.apache.commons.collections4.map;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.function.Predicate;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.iterators.UnmodifiableIterator;
import org.apache.commons.collections4.iterators.UnmodifiableListIterator;
import org.apache.commons.collections4.list.UnmodifiableList;

/**
 * A {@code Map} implementation that maintains the order of the entries.
 * In this implementation order is maintained by original insertion.
 * <p>
 * This implementation improves on the JDK1.4 LinkedHashMap by adding the
 * {@link org.apache.commons.collections4.MapIterator MapIterator}
 * functionality, additional convenience methods and allowing
 * bidirectional iteration. It also implements {@code OrderedMap}.
 * In addition, non-interface methods are provided to access the map by index.
 * </p>
 * <p>
 * The {@code orderedMapIterator()} method provides direct access to a
 * bidirectional iterator. The iterators from the other views can also be cast
 * to {@code OrderedIterator} if required.
 * </p>
 * <p>
 * All the available iterators can be reset back to the start by casting to
 * {@code ResettableIterator} and calling {@code reset()}.
 * </p>
 * <p>
 * The implementation is also designed to be subclassed, with lots of useful
 * methods exposed.
 * </p>
 * <p>
 * <strong>Note that LinkedMap is not synchronized and is not thread-safe.</strong>
 * If you wish to use this map from multiple threads concurrently, you must use
 * appropriate synchronization. The simplest approach is to wrap this map
 * using {@link java.util.Collections#synchronizedMap(Map)}. This class may throw
 * exceptions when accessed by concurrent threads without synchronization.
 * </p>
 *
 * @param <K> the type of the keys in this map
 * @param <V> the type of the values in this map
 * @since 3.0
 */
public class LinkedMap<K, V> extends AbstractLinkedMap<K, V> implements Serializable, Cloneable {

    /**
     * List view of map.
     */
    static class LinkedMapList<K> extends AbstractList<K> {

        private final LinkedMap<K, ?> parent;

        LinkedMapList(final LinkedMap<K, ?> parent) {
            this.parent = parent;
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean contains(final Object obj) {
            return parent.containsKey(obj);
        }

        @Override
        public boolean containsAll(final Collection<?> coll) {
            return parent.keySet().containsAll(coll);
        }

        @Override
        public K get(final int index) {
            return parent.get(index);
        }

        @Override
        public int indexOf(final Object obj) {
            return parent.indexOf(obj);
        }

        @Override
        public Iterator<K> iterator() {
            return UnmodifiableIterator.unmodifiableIterator(parent.keySet().iterator());
        }

        @Override
        public int lastIndexOf(final Object obj) {
            return parent.indexOf(obj);
        }

        @Override
        public ListIterator<K> listIterator() {
            return UnmodifiableListIterator.unmodifiableListIterator(super.listIterator());
        }

        @Override
        public ListIterator<K> listIterator(final int fromIndex) {
            return UnmodifiableListIterator.unmodifiableListIterator(super.listIterator(fromIndex));
        }

        @Override
        public K remove(final int index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(final Object obj) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(final Collection<?> coll) {
            throw new UnsupportedOperationException();
        }

        /**
         * @since 4.4
         */
        @Override
        public boolean removeIf(final Predicate<? super K> filter) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(final Collection<?> coll) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int size() {
            return parent.size();
        }

        @Override
        public List<K> subList(final int fromIndexInclusive, final int toIndexExclusive) {
            return UnmodifiableList.unmodifiableList(super.subList(fromIndexInclusive, toIndexExclusive));
        }

        @Override
        public Object[] toArray() {
            return parent.keySet().toArray();
        }

        @Override
        public <T> T[] toArray(final T[] array) {
            return parent.keySet().toArray(array);
        }
    }

    /** Serialization version */
    private static final long serialVersionUID = 9077234323521161066L;

    /**
     * Constructs a new empty map with default size and load factor.
     */
    public LinkedMap() {
        super(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR, DEFAULT_THRESHOLD);
    }

    /**
     * Constructs a new, empty map with the specified initial capacity.
     *
     * @param initialCapacity  the initial capacity
     * @throws IllegalArgumentException if the initial capacity is negative
     */
    public LinkedMap(final int initialCapacity) {
        super(initialCapacity);
    }

    /**
     * Constructs a new, empty map with the specified initial capacity and
     * load factor.
     *
     * @param initialCapacity  the initial capacity
     * @param loadFactor  the load factor
     * @throws IllegalArgumentException if the initial capacity is negative
     * @throws IllegalArgumentException if the load factor is less than zero
     */
    public LinkedMap(final int initialCapacity, final float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    /**
     * Constructor copying elements from another map.
     *
     * @param map  the map to copy
     * @throws NullPointerException if the map is null
     */
    public LinkedMap(final Map<? extends K, ? extends V> map) {
        super(map);
    }

    /**
     * Gets an unmodifiable List view of the keys.
     * <p>
     * The returned list is unmodifiable because changes to the values of
     * the list (using {@link java.util.ListIterator#set(Object)}) will
     * effectively remove the value from the list and reinsert that value at
     * the end of the list, which is an unexpected side effect of changing the
     * value of a list.  This occurs because changing the key, changes when the
     * mapping is added to the map and thus where it appears in the list.
     * </p>
     * <p>
     * An alternative to this method is to use {@link #keySet()}.
     * </p>
     *
     * @see #keySet()
     * @return The ordered list of keys.
     */
    public List<K> asList() {
        return new LinkedMapList<>(this);
    }

    /**
     * Clones the map without cloning the keys or values.
     *
     * @return a shallow clone
     */
    @Override
    public LinkedMap<K, V> clone() {
        return (LinkedMap<K, V>) super.clone();
    }

    /**
     * Gets the key at the specified index.
     *
     * @param index  the index to retrieve
     * @return the key at the specified index
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    public K get(final int index) {
        return getEntry(index).getKey();
    }

    /**
     * Gets the value at the specified index.
     *
     * @param index  the index to retrieve
     * @return the value at the specified index
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    public V getValue(final int index) {
        return getEntry(index).getValue();
    }

    /**
     * Gets the index of the specified key.
     *
     * @param key  the key to find the index of
     * @return the index, or -1 if not found
     */
    public int indexOf(Object key) {
        key = convertKey(key);
        int i = 0;
        for (LinkEntry<K, V> entry = header.after; entry != header; entry = entry.after, i++) {
            if (isEqualKey(key, entry.key)) {
                return i;
            }
        }
        return CollectionUtils.INDEX_NOT_FOUND;
    }

    /**
     * Deserializes the map in using a custom routine.
     *
     * @param in the input stream
     * @throws IOException if an error occurs while reading from the stream
     * @throws ClassNotFoundException if an object read from the stream cannot be loaded
     */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        doReadObject(in);
    }

    /**
     * Removes the element at the specified index.
     *
     * @param index  the index of the object to remove
     * @return the previous value corresponding the {@code key},
     *  or {@code null} if none existed
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    public V remove(final int index) {
        return remove(get(index));
    }

    /**
     * Serializes this object to an ObjectOutputStream.
     *
     * @param out the target ObjectOutputStream.
     * @throws IOException thrown when an I/O errors occur writing to the target stream.
     */
    private void writeObject(final ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        doWriteObject(out);
    }

}
