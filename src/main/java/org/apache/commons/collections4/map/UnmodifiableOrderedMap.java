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
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.OrderedMap;
import org.apache.commons.collections4.OrderedMapIterator;
import org.apache.commons.collections4.Unmodifiable;
import org.apache.commons.collections4.collection.UnmodifiableCollection;
import org.apache.commons.collections4.iterators.UnmodifiableOrderedMapIterator;
import org.apache.commons.collections4.set.UnmodifiableSet;

/**
 * Decorates another {@code OrderedMap} to ensure it can't be altered.
 * <p>
 * This class is Serializable from Commons Collections 3.1.
 * </p>
 * <p>
 * Attempts to modify it will result in an UnsupportedOperationException.
 * </p>
 *
 * @param <K> the type of the keys in this map
 * @param <V> the type of the values in this map
 * @since 3.0
 */
public final class UnmodifiableOrderedMap<K, V> extends AbstractOrderedMapDecorator<K, V> implements
        Unmodifiable, Serializable {

    /** Serialization version */
    private static final long serialVersionUID = 8136428161720526266L;

    /**
     * Factory method to create an unmodifiable sorted map.
     *
     * @param <K>  the key type
     * @param <V>  the value type
     * @param map  the map to decorate, must not be null
     * @return a new ordered map
     * @throws NullPointerException if map is null
     * @since 4.0
     */
    public static <K, V> OrderedMap<K, V> unmodifiableOrderedMap(final OrderedMap<? extends K, ? extends V> map) {
        if (map instanceof Unmodifiable) {
            @SuppressWarnings("unchecked") // safe to upcast
            final OrderedMap<K, V> tmpMap = (OrderedMap<K, V>) map;
            return tmpMap;
        }
        return new UnmodifiableOrderedMap<>(map);
    }

    /**
     * Constructor that wraps (not copies).
     *
     * @param map  the map to decorate, must not be null
     * @throws NullPointerException if map is null
     */
    @SuppressWarnings("unchecked") // safe to upcast
    private UnmodifiableOrderedMap(final OrderedMap<? extends K, ? extends V> map) {
        super((OrderedMap<K, V>) map);
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        final Set<Map.Entry<K, V>> set = super.entrySet();
        return UnmodifiableEntrySet.unmodifiableEntrySet(set);
    }

    @Override
    public Set<K> keySet() {
        final Set<K> set = super.keySet();
        return UnmodifiableSet.unmodifiableSet(set);
    }

    @Override
    public OrderedMapIterator<K, V> mapIterator() {
        final OrderedMapIterator<K, V> it = decorated().mapIterator();
        return UnmodifiableOrderedMapIterator.unmodifiableOrderedMapIterator(it);
    }

    @Override
    public V put(final K key, final V value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(final Map<? extends K, ? extends V> mapToCopy) {
        throw new UnsupportedOperationException();
    }

    /**
     * Deseializes the map in using a custom routine.
     *
     * @param in  the input stream
     * @throws IOException if an error occurs while reading from the stream
     * @throws ClassNotFoundException if an object read from the stream cannot be loaded
     * @since 3.1
     */
    @SuppressWarnings("unchecked") // (1) should only fail if input stream is incorrect
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        map = (Map<K, V>) in.readObject(); // (1)
    }

    @Override
    public V remove(final Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<V> values() {
        final Collection<V> coll = super.values();
        return UnmodifiableCollection.unmodifiableCollection(coll);
    }

    /**
     * Serializes this object to an ObjectOutputStream.
     *
     * @param out the target ObjectOutputStream.
     * @throws IOException thrown when an I/O errors occur writing to the target stream.
     * @since 3.1
     */
    private void writeObject(final ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeObject(map);
    }

}
