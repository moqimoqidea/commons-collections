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

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

import org.apache.commons.collections4.OrderedIterator;
import org.apache.commons.collections4.OrderedMap;
import org.apache.commons.collections4.OrderedMapIterator;
import org.apache.commons.collections4.ResettableIterator;
import org.apache.commons.collections4.iterators.EmptyOrderedIterator;
import org.apache.commons.collections4.iterators.EmptyOrderedMapIterator;

/**
 * An abstract implementation of a hash-based map that links entries to create an
 * ordered map and which provides numerous points for subclasses to override.
 * <p>
 * This class implements all the features necessary for a subclass linked
 * hash-based map. Key-value entries are stored in instances of the
 * {@code LinkEntry} class which can be overridden and replaced.
 * The iterators can similarly be replaced, without the need to replace the KeySet,
 * EntrySet and Values view classes.
 * </p>
 * <p>
 * Overridable methods are provided to change the default hashing behavior, and
 * to change how entries are added to and removed from the map. Hopefully, all you
 * need for unusual subclasses is here.
 * </p>
 * <p>
 * This implementation maintains order by original insertion, but subclasses
 * may work differently. The {@code OrderedMap} interface is implemented
 * to provide access to bidirectional iteration and extra convenience methods.
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
 *
 * @param <K> the type of the keys in this map
 * @param <V> the type of the values in this map
 * @since 3.0
 */
public abstract class AbstractLinkedMap<K, V> extends AbstractHashedMap<K, V> implements OrderedMap<K, V> {

    /**
     * EntrySet iterator.
     *
     * @param <K> the key type.
     * @param <V> the value type.
     */
    protected static class EntrySetIterator<K, V> extends LinkIterator<K, V> implements
            OrderedIterator<Map.Entry<K, V>>, ResettableIterator<Map.Entry<K, V>> {

        /**
         * Constructs a new instance.
         *
         * @param parent The parent AbstractLinkedMap.
         */
        protected EntrySetIterator(final AbstractLinkedMap<K, V> parent) {
            super(parent);
        }

        @Override
        public Map.Entry<K, V> next() {
            return super.nextEntry();
        }

        @Override
        public Map.Entry<K, V> previous() {
            return super.previousEntry();
        }
    }

    /**
     * KeySet iterator.
     *
     * @param <K> the key type.
     */
    protected static class KeySetIterator<K> extends LinkIterator<K, Object> implements
            OrderedIterator<K>, ResettableIterator<K> {

        /**
         * Constructs a new instance.
         *
         * @param parent The parent AbstractLinkedMap.
         */
        @SuppressWarnings("unchecked")
        protected KeySetIterator(final AbstractLinkedMap<K, ?> parent) {
            super((AbstractLinkedMap<K, Object>) parent);
        }

        @Override
        public K next() {
            return super.nextEntry().getKey();
        }

        @Override
        public K previous() {
            return super.previousEntry().getKey();
        }
    }

    /**
     * LinkEntry that stores the data.
     * <p>
     * If you subclass {@code AbstractLinkedMap} but not {@code LinkEntry}
     * then you will not be able to access the protected fields.
     * The {@code entryXxx()} methods on {@code AbstractLinkedMap} exist
     * to provide the necessary access.
     * </p>
     *
     * @param <K> the key type.
     * @param <V> the value type.
     */
    protected static class LinkEntry<K, V> extends HashEntry<K, V> {
        /** The entry before this one in the order */
        protected LinkEntry<K, V> before;
        /** The entry after this one in the order */
        protected LinkEntry<K, V> after;

        /**
         * Constructs a new entry.
         *
         * @param next  the next entry in the hash bucket sequence
         * @param hashCode  the hash code
         * @param key  the key
         * @param value  the value
         */
        protected LinkEntry(final HashEntry<K, V> next, final int hashCode, final Object key, final V value) {
            super(next, hashCode, key, value);
        }
    }

    /**
     * Base Iterator that iterates in link order.
     *
     * @param <K> the key type.
     * @param <V> the value type.
     */
    protected abstract static class LinkIterator<K, V> {

        /** The parent map */
        protected final AbstractLinkedMap<K, V> parent;

        /** The current (last returned) entry */
        protected LinkEntry<K, V> last;

        /** The next entry */
        protected LinkEntry<K, V> next;

        /** The modification count expected */
        protected int expectedModCount;

        /**
         * Constructs a new instance.
         *
         * @param parent The parent AbstractLinkedMap.
         */
        protected LinkIterator(final AbstractLinkedMap<K, V> parent) {
            this.parent = Objects.requireNonNull(parent);
            this.next = parent.header.after;
            this.expectedModCount = parent.modCount;
        }

        /**
         * Gets the current entry.
         *
         * @return the current entry.
         */
        protected LinkEntry<K, V> currentEntry() {
            return last;
        }

        /**
         * Tests whether there is another entry.
         *
         * @return whether there is another entry.
         */
        public boolean hasNext() {
            return next != parent.header;
        }

        /**
         * Tests whether there is a previous entry.
         *
         * @return whether there is a previous entry.
         */
        public boolean hasPrevious() {
            return next.before != parent.header;
        }

        /**
         * Gets the next entry.
         *
         * @return the next entry.
         */
        protected LinkEntry<K, V> nextEntry() {
            if (parent.modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            if (next == parent.header)  {
                throw new NoSuchElementException(NO_NEXT_ENTRY);
            }
            last = next;
            next = next.after;
            return last;
        }

        /**
         * Gets the previous entry.
         *
         * @return the previous entry.
         */
        protected LinkEntry<K, V> previousEntry() {
            if (parent.modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            final LinkEntry<K, V> previous = next.before;
            if (previous == parent.header)  {
                throw new NoSuchElementException(NO_PREVIOUS_ENTRY);
            }
            next = previous;
            last = previous;
            return last;
        }

        /**
         * Removes the current entry.
         */
        public void remove() {
            if (last == null) {
                throw new IllegalStateException(REMOVE_INVALID);
            }
            if (parent.modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            parent.remove(last.getKey());
            last = null;
            expectedModCount = parent.modCount;
        }

        /**
         * Resets the state to the end.
         */
        public void reset() {
            last = null;
            next = parent.header.after;
        }

        @Override
        public String toString() {
            if (last != null) {
                return "Iterator[" + last.getKey() + "=" + last.getValue() + "]";
            }
            return "Iterator[]";
        }
    }

    /**
     * MapIterator implementation.
     *
     * @param <K> the key type.
     * @param <V> the value type.
     */
    protected static class LinkMapIterator<K, V> extends LinkIterator<K, V> implements
            OrderedMapIterator<K, V>, ResettableIterator<K> {

        /**
         * Constructs a new instance.
         *
         * @param parent The parent AbstractLinkedMap.
         */
        protected LinkMapIterator(final AbstractLinkedMap<K, V> parent) {
            super(parent);
        }

        @Override
        public K getKey() {
            final LinkEntry<K, V> current = currentEntry();
            if (current == null) {
                throw new IllegalStateException(GETKEY_INVALID);
            }
            return current.getKey();
        }

        @Override
        public V getValue() {
            final LinkEntry<K, V> current = currentEntry();
            if (current == null) {
                throw new IllegalStateException(GETVALUE_INVALID);
            }
            return current.getValue();
        }

        @Override
        public K next() {
            return super.nextEntry().getKey();
        }

        @Override
        public K previous() {
            return super.previousEntry().getKey();
        }

        @Override
        public V setValue(final V value) {
            final LinkEntry<K, V> current = currentEntry();
            if (current == null) {
                throw new IllegalStateException(SETVALUE_INVALID);
            }
            return current.setValue(value);
        }
    }

    /**
     * Values iterator.
     *
     * @param <V> the value type.
     */
    protected static class ValuesIterator<V> extends LinkIterator<Object, V> implements
            OrderedIterator<V>, ResettableIterator<V> {

        /**
         * Constructs a new instance.
         *
         * @param parent The parent AbstractLinkedMap.
         */
        @SuppressWarnings("unchecked")
        protected ValuesIterator(final AbstractLinkedMap<?, V> parent) {
            super((AbstractLinkedMap<Object, V>) parent);
        }

        @Override
        public V next() {
            return super.nextEntry().getValue();
        }

        @Override
        public V previous() {
            return super.previousEntry().getValue();
        }
    }

    /** Header in the linked list */
    transient LinkEntry<K, V> header;

    /**
     * Constructor only used in deserialization, do not use otherwise.
     */
    protected AbstractLinkedMap() {
    }

    /**
     * Constructs a new, empty map with the specified initial capacity.
     *
     * @param initialCapacity  the initial capacity
     * @throws IllegalArgumentException if the initial capacity is negative
     */
    protected AbstractLinkedMap(final int initialCapacity) {
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
    protected AbstractLinkedMap(final int initialCapacity, final float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    /**
     * Constructor which performs no validation on the passed in parameters.
     *
     * @param initialCapacity  the initial capacity, must be a power of two
     * @param loadFactor  the load factor, must be &gt; 0.0f and generally &lt; 1.0f
     * @param threshold  the threshold, must be sensible
     */
    protected AbstractLinkedMap(final int initialCapacity, final float loadFactor, final int threshold) {
        super(initialCapacity, loadFactor, threshold);
    }

    /**
     * Constructor copying elements from another map.
     *
     * @param map  the map to copy
     * @throws NullPointerException if the map is null
     */
    protected AbstractLinkedMap(final Map<? extends K, ? extends V> map) {
        super(map);
    }

    /**
     * Adds an entry into this map, maintaining insertion order.
     * <p>
     * This implementation adds the entry to the data storage table and
     * to the end of the linked list.
     * </p>
     *
     * @param entry  the entry to add
     * @param hashIndex  the index into the data array to store at
     */
    @Override
    protected void addEntry(final HashEntry<K, V> entry, final int hashIndex) {
        final LinkEntry<K, V> link = (LinkEntry<K, V>) entry;
        link.after  = header;
        link.before = header.before;
        header.before.after = link;
        header.before = link;
        data[hashIndex] = link;
    }

    /**
     * Clears the map, resetting the size to zero and nullifying references
     * to avoid garbage collection issues.
     */
    @Override
    public void clear() {
        // override to reset the linked list
        super.clear();
        header.before = header.after = header;
    }

    /**
     * Checks whether the map contains the specified value.
     *
     * @param value  the value to search for
     * @return true if the map contains the value
     */
    @Override
    public boolean containsValue(final Object value) {
        // override uses faster iterator
        if (value == null) {
            for (LinkEntry<K, V> entry = header.after; entry != header; entry = entry.after) {
                if (entry.getValue() == null) {
                    return true;
                }
            }
        } else {
            for (LinkEntry<K, V> entry = header.after; entry != header; entry = entry.after) {
                if (isEqualValue(value, entry.getValue())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Creates an entry to store the data.
     * <p>
     * This implementation creates a new LinkEntry instance.
     * </p>
     *
     * @param next  the next entry in sequence
     * @param hashCode  the hash code to use
     * @param key  the key to store
     * @param value  the value to store
     * @return the newly created entry
     */
    @Override
    protected LinkEntry<K, V> createEntry(final HashEntry<K, V> next, final int hashCode, final K key, final V value) {
        return new LinkEntry<>(next, hashCode, convertKey(key), value);
    }

    /**
     * Creates an entry set iterator.
     * Subclasses can override this to return iterators with different properties.
     *
     * @return the entrySet iterator
     */
    @Override
    protected Iterator<Map.Entry<K, V>> createEntrySetIterator() {
        if (isEmpty()) {
            return EmptyOrderedIterator.<Map.Entry<K, V>>emptyOrderedIterator();
        }
        return new EntrySetIterator<>(this);
    }

    /**
     * Creates a key set iterator.
     * Subclasses can override this to return iterators with different properties.
     *
     * @return the keySet iterator
     */
    @Override
    protected Iterator<K> createKeySetIterator() {
        if (isEmpty()) {
            return EmptyOrderedIterator.<K>emptyOrderedIterator();
        }
        return new KeySetIterator<>(this);
    }

    /**
     * Creates a values iterator.
     * Subclasses can override this to return iterators with different properties.
     *
     * @return the values iterator
     */
    @Override
    protected Iterator<V> createValuesIterator() {
        if (isEmpty()) {
            return EmptyOrderedIterator.<V>emptyOrderedIterator();
        }
        return new ValuesIterator<>(this);
    }

    /**
     * Gets the {@code after} field from a {@code LinkEntry}.
     * Used in subclasses that have no visibility of the field.
     *
     * @param entry  the entry to query, must not be null
     * @return the {@code after} field of the entry
     * @throws NullPointerException if the entry is null
     * @since 3.1
     */
    protected LinkEntry<K, V> entryAfter(final LinkEntry<K, V> entry) {
        return entry.after;
    }

    /**
     * Gets the {@code before} field from a {@code LinkEntry}.
     * Used in subclasses that have no visibility of the field.
     *
     * @param entry  the entry to query, must not be null
     * @return the {@code before} field of the entry
     * @throws NullPointerException if the entry is null
     * @since 3.1
     */
    protected LinkEntry<K, V> entryBefore(final LinkEntry<K, V> entry) {
        return entry.before;
    }

    /**
     * Gets the first key in the map, which is the first inserted.
     *
     * @return the eldest key
     */
    @Override
    public K firstKey() {
        if (size == 0) {
            throw new NoSuchElementException("Map is empty");
        }
        return header.after.getKey();
    }

    /**
     * Gets the key at the specified index.
     *
     * @param index  the index to retrieve
     * @return the key at the specified index
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    protected LinkEntry<K, V> getEntry(final int index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index " + index + " is less than zero");
        }
        if (index >= size) {
            throw new IndexOutOfBoundsException("Index " + index + " is invalid for size " + size);
        }
        LinkEntry<K, V> entry;
        if (index < size / 2) {
            // Search forwards
            entry = header.after;
            for (int currentIndex = 0; currentIndex < index; currentIndex++) {
                entry = entry.after;
            }
        } else {
            // Search backwards
            entry = header;
            for (int currentIndex = size; currentIndex > index; currentIndex--) {
                entry = entry.before;
            }
        }
        return entry;
    }

    @Override
    protected LinkEntry<K, V> getEntry(final Object key) {
        return (LinkEntry<K, V>) super.getEntry(key);
    }

    /**
     * Initialize this subclass during construction.
     * <p>
     * Note: As from v3.2 this method calls
     * {@link #createEntry(HashEntry, int, Object, Object)} to create
     * the map entry object.
     * </p>
     */
    @Override
    protected void init() {
        header = createEntry(null, -1, null, null);
        header.before = header.after = header;
    }

    /**
     * Gets the last key in the map, which is the most recently inserted.
     *
     * @return the most recently inserted key
     */
    @Override
    public K lastKey() {
        if (size == 0) {
            throw new NoSuchElementException("Map is empty");
        }
        return header.before.getKey();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OrderedMapIterator<K, V> mapIterator() {
        if (size == 0) {
            return EmptyOrderedMapIterator.<K, V>emptyOrderedMapIterator();
        }
        return new LinkMapIterator<>(this);
    }

    /**
     * Gets the next key in sequence.
     *
     * @param key  the key to get after
     * @return the next key
     */
    @Override
    public K nextKey(final Object key) {
        final LinkEntry<K, V> entry = getEntry(key);
        return entry == null || entry.after == header ? null : entry.after.getKey();
    }

    /**
     * Gets the previous key in sequence.
     *
     * @param key  the key to get before
     * @return the previous key
     */
    @Override
    public K previousKey(final Object key) {
        final LinkEntry<K, V> entry = getEntry(key);
        return entry == null || entry.before == header ? null : entry.before.getKey();
    }

    /**
     * Removes an entry from the map and the linked list.
     * <p>
     * This implementation removes the entry from the linked list chain, then
     * calls the superclass implementation.
     * </p>
     *
     * @param entry  the entry to remove
     * @param hashIndex  the index into the data structure
     * @param previous  the previous entry in the chain
     */
    @Override
    protected void removeEntry(final HashEntry<K, V> entry, final int hashIndex, final HashEntry<K, V> previous) {
        final LinkEntry<K, V> link = (LinkEntry<K, V>) entry;
        link.before.after = link.after;
        link.after.before = link.before;
        link.after = null;
        link.before = null;
        super.removeEntry(entry, hashIndex, previous);
    }

}
