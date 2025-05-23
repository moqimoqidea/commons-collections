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

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.collections4.BoundedMap;
import org.apache.commons.collections4.KeyValue;
import org.apache.commons.collections4.OrderedMap;
import org.apache.commons.collections4.OrderedMapIterator;
import org.apache.commons.collections4.ResettableIterator;
import org.apache.commons.collections4.iterators.SingletonIterator;
import org.apache.commons.collections4.keyvalue.TiedMapEntry;

/**
 * A {@code Map} implementation that holds a single item and is fixed size.
 * <p>
 * The single key/value pair is specified at creation.
 * The map is fixed size so any action that would change the size is disallowed.
 * However, the {@code put} or {@code setValue} methods can <em>change</em>
 * the value associated with the key.
 * </p>
 * <p>
 * If trying to remove or clear the map, an UnsupportedOperationException is thrown.
 * If trying to put a new mapping into the map, an  IllegalArgumentException is thrown.
 * The put method will only succeed if the key specified is the same as the
 * singleton key.
 * </p>
 * <p>
 * The key and value can be obtained by:
 * </p>
 * <ul>
 * <li>normal Map methods and views
 * <li>the {@code MapIterator}, see {@link #mapIterator()}
 * <li>the {@code KeyValue} interface (just cast - no object creation)
 * </ul>
 *
 * @param <K> the type of the keys in this map
 * @param <V> the type of the values in this map
 * @since 3.1
 */
public class SingletonMap<K, V>
        implements OrderedMap<K, V>, BoundedMap<K, V>, KeyValue<K, V>, Serializable, Cloneable {

    /**
     * SingletonMapIterator.
     */
    static class SingletonMapIterator<K, V> implements OrderedMapIterator<K, V>, ResettableIterator<K> {
        private final SingletonMap<K, V> parent;
        private boolean hasNext = true;
        private boolean canGetSet;

        SingletonMapIterator(final SingletonMap<K, V> parent) {
            this.parent = parent;
        }

        @Override
        public K getKey() {
            if (!canGetSet) {
                throw new IllegalStateException(AbstractHashedMap.GETKEY_INVALID);
            }
            return parent.getKey();
        }

        @Override
        public V getValue() {
            if (!canGetSet) {
                throw new IllegalStateException(AbstractHashedMap.GETVALUE_INVALID);
            }
            return parent.getValue();
        }

        @Override
        public boolean hasNext() {
            return hasNext;
        }

        @Override
        public boolean hasPrevious() {
            return !hasNext;
        }

        @Override
        public K next() {
            if (!hasNext) {
                throw new NoSuchElementException(AbstractHashedMap.NO_NEXT_ENTRY);
            }
            hasNext = false;
            canGetSet = true;
            return parent.getKey();
        }

        @Override
        public K previous() {
            if (hasNext) {
                throw new NoSuchElementException(AbstractHashedMap.NO_PREVIOUS_ENTRY);
            }
            hasNext = true;
            return parent.getKey();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void reset() {
            hasNext = true;
        }

        @Override
        public V setValue(final V value) {
            if (!canGetSet) {
                throw new IllegalStateException(AbstractHashedMap.SETVALUE_INVALID);
            }
            return parent.setValue(value);
        }

        @Override
        public String toString() {
            if (hasNext) {
                return "Iterator[]";
            }
            return "Iterator[" + getKey() + "=" + getValue() + "]";
        }
    }

    /**
     * Values implementation for the SingletonMap.
     * This class is needed as values is a view that must update as the map updates.
     *
     * @param <V> the type of the values in this set.
     */
    static class SingletonValues<V> extends AbstractSet<V> implements Serializable {
        private static final long serialVersionUID = -3689524741863047872L;
        private final SingletonMap<?, V> parent;

        SingletonValues(final SingletonMap<?, V> parent) {
            this.parent = parent;
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }
        @Override
        public boolean contains(final Object object) {
            return parent.containsValue(object);
        }
        @Override
        public boolean isEmpty() {
            return false;
        }
        @Override
        public Iterator<V> iterator() {
            return new SingletonIterator<>(parent.getValue(), false);
        }
        @Override
        public int size() {
            return 1;
        }
    }
    /** Serialization version */
    private static final long serialVersionUID = -8931271118676803261L;

    /** Singleton key */
    private final K key;

    /** Singleton value */
    private V value;

    /**
     * Constructor that creates a map of {@code null} to {@code null}.
     */
    public SingletonMap() {
        this.key = null;
    }

    /**
     * Constructor specifying the key and value.
     *
     * @param key  the key to use
     * @param value  the value to use
     */
    public SingletonMap(final K key, final V value) {
        this.key = key;
        this.value = value;
    }

    /**
     * Constructor specifying the key and value as a {@code KeyValue}.
     *
     * @param keyValue  the key value pair to use
     */
    public SingletonMap(final KeyValue<K, V> keyValue) {
        this.key = keyValue.getKey();
        this.value = keyValue.getValue();
    }

    /**
     * Constructor specifying the key and value as a {@code MapEntry}.
     *
     * @param mapEntry  the mapEntry to use
     */
    public SingletonMap(final Map.Entry<? extends K, ? extends V> mapEntry) {
        this.key = mapEntry.getKey();
        this.value = mapEntry.getValue();
    }

    /**
     * Constructor copying elements from another map.
     *
     * @param map  the map to copy, must be size 1
     * @throws NullPointerException if the map is null
     * @throws IllegalArgumentException if the size is not 1
     */
    public SingletonMap(final Map<? extends K, ? extends V> map) {
        if (map.size() != 1) {
            throw new IllegalArgumentException("The map size must be 1");
        }
        final Map.Entry<? extends K, ? extends V> entry = map.entrySet().iterator().next();
        this.key = entry.getKey();
        this.value = entry.getValue();
    }

    /**
     * Unsupported operation.
     */
    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    /**
     * Clones the map without cloning the key or value.
     *
     * @return a shallow clone
     */
    @Override
    @SuppressWarnings("unchecked")
    public SingletonMap<K, V> clone() {
        try {
            return (SingletonMap<K, V>) super.clone();
        } catch (final CloneNotSupportedException ex) {
            throw new UnsupportedOperationException(ex);
        }
    }

    /**
     * Checks whether the map contains the specified key.
     *
     * @param key  the key to search for
     * @return true if the map contains the key
     */
    @Override
    public boolean containsKey(final Object key) {
        return isEqualKey(key);
    }

    /**
     * Checks whether the map contains the specified value.
     *
     * @param value  the value to search for
     * @return true if the map contains the key
     */
    @Override
    public boolean containsValue(final Object value) {
        return isEqualValue(value);
    }

    /**
     * Gets the entrySet view of the map.
     * Changes made via {@code setValue} affect this map.
     * To simply iterate through the entries, use {@link #mapIterator()}.
     *
     * @return the entrySet view
     */
    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        final Map.Entry<K, V> entry = new TiedMapEntry<>(this, getKey());
        return Collections.singleton(entry);
    }

    /**
     * Compares this map with another.
     *
     * @param obj  the object to compare to
     * @return true if equal
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Map)) {
            return false;
        }
        final Map<?, ?> other = (Map<?, ?>) obj;
        if (other.size() != 1) {
            return false;
        }
        final Map.Entry<?, ?> entry = other.entrySet().iterator().next();
        return isEqualKey(entry.getKey()) && isEqualValue(entry.getValue());
    }

    /**
     * Gets the first (and only) key in the map.
     *
     * @return the key
     */
    @Override
    public K firstKey() {
        return getKey();
    }

    // Map
    /**
     * Gets the value mapped to the key specified.
     *
     * @param key  the key
     * @return the mapped value, null if no match
     */
    @Override
    public V get(final Object key) {
        if (isEqualKey(key)) {
            return value;
        }
        return null;
    }

    // KeyValue
    /**
     * Gets the key.
     *
     * @return the key
     */
    @Override
    public K getKey() {
        return key;
    }

    /**
     * Gets the value.
     *
     * @return the value
     */
    @Override
    public V getValue() {
        return value;
    }

    /**
     * Gets the standard Map hashCode.
     *
     * @return the hash code defined in the Map interface
     */
    @Override
    public int hashCode() {
        return (getKey() == null ? 0 : getKey().hashCode()) ^
               (getValue() == null ? 0 : getValue().hashCode());
    }

    /**
     * Checks whether the map is currently empty, which it never is.
     *
     * @return false always
     */
    @Override
    public boolean isEmpty() {
        return false;
    }

    /**
     * Compares the specified key to the stored key.
     *
     * @param key  the key to compare
     * @return true if equal
     */
    protected boolean isEqualKey(final Object key) {
        return Objects.equals(key, getKey());
    }

    /**
     * Compares the specified value to the stored value.
     *
     * @param value  the value to compare
     * @return true if equal
     */
    protected boolean isEqualValue(final Object value) {
        return Objects.equals(value, getValue());
    }

    // BoundedMap
    /**
     * Is the map currently full, always true.
     *
     * @return true always
     */
    @Override
    public boolean isFull() {
        return true;
    }

    /**
     * Gets the unmodifiable keySet view of the map.
     * Changes made to the view affect this map.
     * To simply iterate through the keys, use {@link #mapIterator()}.
     *
     * @return the keySet view
     */
    @Override
    public Set<K> keySet() {
        return Collections.singleton(key);
    }

    /**
     * Gets the last (and only) key in the map.
     *
     * @return the key
     */
    @Override
    public K lastKey() {
        return getKey();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OrderedMapIterator<K, V> mapIterator() {
        return new SingletonMapIterator<>(this);
    }

    /**
     * Gets the maximum size of the map, always 1.
     *
     * @return 1 always
     */
    @Override
    public int maxSize() {
        return 1;
    }

    /**
     * Gets the next key after the key specified, always null.
     *
     * @param key  the next key
     * @return null always
     */
    @Override
    public K nextKey(final K key) {
        return null;
    }

    /**
     * Gets the previous key before the key specified, always null.
     *
     * @param key  the next key
     * @return null always
     */
    @Override
    public K previousKey(final K key) {
        return null;
    }

    /**
     * Puts a key-value mapping into this map where the key must match the existing key.
     * <p>
     * An IllegalArgumentException is thrown if the key does not match as the map
     * is fixed size.
     * </p>
     *
     * @param key  the key to set, must be the key of the map
     * @param value  the value to set
     * @return the value previously mapped to this key, null if none
     * @throws IllegalArgumentException if the key does not match
     */
    @Override
    public V put(final K key, final V value) {
        if (isEqualKey(key)) {
            return setValue(value);
        }
        throw new IllegalArgumentException("Cannot put new key/value pair - Map is fixed size singleton");
    }

    /**
     * Puts the values from the specified map into this map.
     * <p>
     * The map must be of size 0 or size 1.
     * If it is size 1, the key must match the key of this map otherwise an
     * IllegalArgumentException is thrown.
     * </p>
     *
     * @param map  the map to add, must be size 0 or 1, and the key must match
     * @throws NullPointerException if the map is null
     * @throws IllegalArgumentException if the key does not match
     */
    @Override
    public void putAll(final Map<? extends K, ? extends V> map) {
        switch (map.size()) {
        case 0:
            return;

        case 1:
            final Map.Entry<? extends K, ? extends V> entry = map.entrySet().iterator().next();
            put(entry.getKey(), entry.getValue());
            return;

        default:
            throw new IllegalArgumentException("The map size must be 0 or 1");
        }
    }

    /**
     * Unsupported operation.
     *
     * @param key  the mapping to remove
     * @return the value mapped to the removed key, null if key not in map
     * @throws UnsupportedOperationException always
     */
    @Override
    public V remove(final Object key) {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the value.
     *
     * @param value  the new value to set
     * @return the old value
     */
    public V setValue(final V value) {
        final V old = this.value;
        this.value = value;
        return old;
    }

    /**
     * Gets the size of the map, always 1.
     *
     * @return the size of 1
     */
    @Override
    public int size() {
        return 1;
    }

    /**
     * Gets the map as a String.
     *
     * @return a string version of the map
     */
    @Override
    public String toString() {
        return new StringBuilder(128)
            .append('{')
            .append(getKey() == this ? "(this Map)" : getKey())
            .append('=')
            .append(getValue() == this ? "(this Map)" : getValue())
            .append('}')
            .toString();
    }

    /**
     * Gets the unmodifiable values view of the map.
     * Changes made to the view affect this map.
     * To simply iterate through the values, use {@link #mapIterator()}.
     *
     * @return the values view
     */
    @Override
    public Collection<V> values() {
        return new SingletonValues<>(this);
    }

}
