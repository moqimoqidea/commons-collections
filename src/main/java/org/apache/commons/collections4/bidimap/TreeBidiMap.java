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
package org.apache.commons.collections4.bidimap;

import static org.apache.commons.collections4.bidimap.TreeBidiMap.DataElement.KEY;
import static org.apache.commons.collections4.bidimap.TreeBidiMap.DataElement.VALUE;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractSet;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.collections4.KeyValue;
import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.OrderedBidiMap;
import org.apache.commons.collections4.OrderedIterator;
import org.apache.commons.collections4.OrderedMapIterator;
import org.apache.commons.collections4.iterators.EmptyOrderedMapIterator;
import org.apache.commons.collections4.keyvalue.UnmodifiableMapEntry;

/**
 * Red-Black tree-based implementation of BidiMap where all objects added
 * implement the {@code Comparable} interface.
 * <p>
 * This class guarantees that the map will be in both ascending key order
 * and ascending value order, sorted according to the natural order for
 * the key's and value's classes.
 * </p>
 * <p>
 * This Map is intended for applications that need to be able to look
 * up a key-value pairing by either key or value, and need to do so
 * with equal efficiency.
 * </p>
 * <p>
 * While that goal could be accomplished by taking a pair of TreeMaps
 * and redirecting requests to the appropriate TreeMap (for example,
 * containsKey would be directed to the TreeMap that maps values to
 * keys, containsValue would be directed to the TreeMap that maps keys
 * to values), there are problems with that implementation.
 * If the data contained in the TreeMaps is large, the cost of redundant
 * storage becomes significant. The {@link DualTreeBidiMap} and
 * {@link DualHashBidiMap} implementations use this approach.
 * </p>
 * <p>
 * This solution keeps minimizes the data storage by holding data only once.
 * The red-black algorithm is based on {@link java.util.TreeMap}, but has been modified
 * to simultaneously map a tree node by key and by value. This doubles the
 * cost of put operations (but so does using two TreeMaps), and nearly doubles
 * the cost of remove operations (there is a savings in that the lookup of the
 * node to be removed only has to be performed once). And since only one node
 * contains the key and value, storage is significantly less than that
 * required by two TreeMaps.
 * </p>
 * <p>
 * The Map.Entry instances returned by the appropriate methods will
 * not allow setValue() and will throw an
 * UnsupportedOperationException on attempts to call that method.
 * </p>
 *
 * @param <K> the type of the keys in this map
 * @param <V> the type of the values in this map
 * @since 3.0 (previously DoubleOrderedMap v2.0)
 */
public class TreeBidiMap<K extends Comparable<K>, V extends Comparable<V>>
    implements OrderedBidiMap<K, V>, Serializable {

    /**
     * A view of this map.
     */
    abstract class AbstractView<E> extends AbstractSet<E> {

        /** Whether to return KEY or VALUE order. */
        final DataElement orderType;

        /**
         * Constructs a new instance.
         * @param orderType  the KEY or VALUE int for the order
         */
        AbstractView(final DataElement orderType) {
            this.orderType = orderType;
        }

        @Override
        public void clear() {
            TreeBidiMap.this.clear();
        }

        @Override
        public int size() {
            return TreeBidiMap.this.size();
        }
    }

    /**
     * An iterator over the map.
     */
    abstract class AbstractViewIterator {

        /** Whether to return KEY or VALUE order. */
        private final DataElement orderType;
        /** The last node returned by the iterator. */
        Node<K, V> lastReturnedNode;
        /** The next node to be returned by the iterator. */
        private Node<K, V> nextNode;
        /** The previous node in the sequence returned by the iterator. */
        private Node<K, V> previousNode;
        /** The modification count. */
        private int expectedModifications;

        /**
         * Constructs a new instance.
         * @param orderType  the KEY or VALUE int for the order
         */
        AbstractViewIterator(final DataElement orderType) {
            this.orderType = orderType;
            expectedModifications = modifications;
            nextNode = leastNode(rootNode[orderType.ordinal()], orderType);
            lastReturnedNode = null;
            previousNode = null;
        }

        public final boolean hasNext() {
            return nextNode != null;
        }

        public boolean hasPrevious() {
            return previousNode != null;
        }

        protected Node<K, V> navigateNext() {
            if (nextNode == null) {
                throw new NoSuchElementException();
            }
            if (modifications != expectedModifications) {
                throw new ConcurrentModificationException();
            }
            lastReturnedNode = nextNode;
            previousNode = nextNode;
            nextNode = nextGreater(nextNode, orderType);
            return lastReturnedNode;
        }

        protected Node<K, V> navigatePrevious() {
            if (previousNode == null) {
                throw new NoSuchElementException();
            }
            if (modifications != expectedModifications) {
                throw new ConcurrentModificationException();
            }
            nextNode = lastReturnedNode;
            if (nextNode == null) {
                nextNode = nextGreater(previousNode, orderType);
            }
            lastReturnedNode = previousNode;
            previousNode = nextSmaller(previousNode, orderType);
            return lastReturnedNode;
        }

        public final void remove() {
            if (lastReturnedNode == null) {
                throw new IllegalStateException();
            }
            if (modifications != expectedModifications) {
                throw new ConcurrentModificationException();
            }
            doRedBlackDelete(lastReturnedNode);
            expectedModifications++;
            lastReturnedNode = null;
            if (nextNode == null) {
                previousNode = greatestNode(rootNode[orderType.ordinal()], orderType);
            } else {
                previousNode = nextSmaller(nextNode, orderType);
            }
        }
    }

    enum DataElement {
        KEY("key"), VALUE("value");

        private final String description;

        /**
         * Creates a new TreeBidiMap.DataElement.
         *
         * @param description  the description for the element
         */
        DataElement(final String description) {
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }
    /**
     * A view of this map.
     */
    final class EntryView extends AbstractView<Map.Entry<K, V>> {

        EntryView() {
            super(KEY);
        }

        @Override
        public boolean contains(final Object obj) {
            if (!(obj instanceof Map.Entry)) {
                return false;
            }
            final Map.Entry<?, ?> entry = (Map.Entry<?, ?>) obj;
            final Object value = entry.getValue();
            final Node<K, V> node = lookupKey(entry.getKey());
            return node != null && Objects.equals(node.getValue(), value);
        }

        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            return new ViewMapEntryIterator();
        }

        @Override
        public boolean remove(final Object obj) {
            if (!(obj instanceof Map.Entry)) {
                return false;
            }
            final Map.Entry<?, ?> entry = (Map.Entry<?, ?>) obj;
            final Object value = entry.getValue();
            final Node<K, V> node = lookupKey(entry.getKey());
            if (node != null && Objects.equals(node.getValue(), value)) {
                doRedBlackDelete(node);
                return true;
            }
            return false;
        }
    }
    /**
     * The inverse map implementation.
     */
    final class Inverse implements OrderedBidiMap<V, K> {

        /** Store the keySet once created. */
        private Set<V> inverseKeySet;
        /** Store the valuesSet once created. */
        private Set<K> inverseValuesSet;
        /** Store the entrySet once created. */
        private Set<Map.Entry<V, K>> inverseEntrySet;

        @Override
        public void clear() {
            TreeBidiMap.this.clear();
        }

        @Override
        public boolean containsKey(final Object key) {
            return TreeBidiMap.this.containsValue(key);
        }

        @Override
        public boolean containsValue(final Object value) {
            return TreeBidiMap.this.containsKey(value);
        }

        @Override
        public Set<Map.Entry<V, K>> entrySet() {
            if (inverseEntrySet == null) {
                inverseEntrySet = new InverseEntryView();
            }
            return inverseEntrySet;
        }

        @Override
        public boolean equals(final Object obj) {
            return TreeBidiMap.this.doEquals(obj, VALUE);
        }

        @Override
        public V firstKey() {
            if (TreeBidiMap.this.nodeCount == 0) {
                throw new NoSuchElementException("Map is empty");
            }
            return leastNode(TreeBidiMap.this.rootNode[VALUE.ordinal()], VALUE).getValue();
        }

        @Override
        public K get(final Object key) {
            return TreeBidiMap.this.getKey(key);
        }

        @Override
        public V getKey(final Object value) {
            return TreeBidiMap.this.get(value);
        }

        @Override
        public int hashCode() {
            return TreeBidiMap.this.doHashCode(VALUE);
        }

        @Override
        public OrderedBidiMap<K, V> inverseBidiMap() {
            return TreeBidiMap.this;
        }

        @Override
        public boolean isEmpty() {
            return TreeBidiMap.this.isEmpty();
        }

        @Override
        public Set<V> keySet() {
            if (inverseKeySet == null) {
                inverseKeySet = new ValueView(VALUE);
            }
            return inverseKeySet;
        }

        @Override
        public V lastKey() {
            if (TreeBidiMap.this.nodeCount == 0) {
                throw new NoSuchElementException("Map is empty");
            }
            return greatestNode(TreeBidiMap.this.rootNode[VALUE.ordinal()], VALUE).getValue();
        }

        @Override
        public OrderedMapIterator<V, K> mapIterator() {
            if (isEmpty()) {
                return EmptyOrderedMapIterator.<V, K>emptyOrderedMapIterator();
            }
            return new InverseViewMapIterator(VALUE);
        }

        @Override
        public V nextKey(final V key) {
            checkKey(key);
            final Node<K, V> node = nextGreater(TreeBidiMap.this.<V>lookup(key, VALUE), VALUE);
            return node == null ? null : node.getValue();
        }

        @Override
        public V previousKey(final V key) {
            checkKey(key);
            final Node<K, V> node = TreeBidiMap.this.nextSmaller(TreeBidiMap.this.<V>lookup(key, VALUE), VALUE);
            return node == null ? null : node.getValue();
        }

        @Override
        public K put(final V key, final K value) {
            final K result = get(key);
            TreeBidiMap.this.doPut(value, key);
            return result;
        }

        @Override
        public void putAll(final Map<? extends V, ? extends K> map) {
            for (final Map.Entry<? extends V, ? extends K> e : map.entrySet()) {
                put(e.getKey(), e.getValue());
            }
        }

        @Override
        public K remove(final Object key) {
            return TreeBidiMap.this.removeValue(key);
        }

        @Override
        public V removeValue(final Object value) {
            return TreeBidiMap.this.remove(value);
        }

        @Override
        public int size() {
            return TreeBidiMap.this.size();
        }

        @Override
        public String toString() {
            return TreeBidiMap.this.doToString(VALUE);
        }

        @Override
        public Set<K> values() {
            if (inverseValuesSet == null) {
                inverseValuesSet = new KeyView(VALUE);
            }
            return inverseValuesSet;
        }
    }
    /**
     * A view of this map.
     */
    final class InverseEntryView extends AbstractView<Map.Entry<V, K>> {

        InverseEntryView() {
            super(VALUE);
        }

        @Override
        public boolean contains(final Object obj) {
            if (!(obj instanceof Map.Entry)) {
                return false;
            }
            final Map.Entry<?, ?> entry = (Map.Entry<?, ?>) obj;
            final Object value = entry.getValue();
            final Node<K, V> node = lookupValue(entry.getKey());
            return node != null && Objects.equals(node.getKey(), value);
        }

        @Override
        public Iterator<Map.Entry<V, K>> iterator() {
            return new InverseViewMapEntryIterator();
        }

        @Override
        public boolean remove(final Object obj) {
            if (!(obj instanceof Map.Entry)) {
                return false;
            }
            final Map.Entry<?, ?> entry = (Map.Entry<?, ?>) obj;
            final Object value = entry.getValue();
            final Node<K, V> node = lookupValue(entry.getKey());
            if (node != null && Objects.equals(node.getKey(), value)) {
                doRedBlackDelete(node);
                return true;
            }
            return false;
        }
    }
    /**
     * An iterator over the inverse map entries.
     */
    final class InverseViewMapEntryIterator extends AbstractViewIterator implements OrderedIterator<Map.Entry<V, K>> {

        /**
         * Constructs a new instance.
         */
        InverseViewMapEntryIterator() {
            super(VALUE);
        }

        private Map.Entry<V, K> createEntry(final Node<K, V> node) {
            return new UnmodifiableMapEntry<>(node.getValue(), node.getKey());
        }

        @Override
        public Map.Entry<V, K> next() {
            return createEntry(navigateNext());
        }

        @Override
        public Map.Entry<V, K> previous() {
            return createEntry(navigatePrevious());
        }
    }
    /**
     * An iterator over the map.
     */
    final class InverseViewMapIterator extends AbstractViewIterator implements OrderedMapIterator<V, K> {

        /**
         * Creates a new TreeBidiMap.InverseViewMapIterator.
         */
        InverseViewMapIterator(final DataElement orderType) {
            super(orderType);
        }

        @Override
        public V getKey() {
            if (lastReturnedNode == null) {
                throw new IllegalStateException(
                        "Iterator getKey() can only be called after next() and before remove()");
            }
            return lastReturnedNode.getValue();
        }

        @Override
        public K getValue() {
            if (lastReturnedNode == null) {
                throw new IllegalStateException(
                        "Iterator getValue() can only be called after next() and before remove()");
            }
            return lastReturnedNode.getKey();
        }

        @Override
        public V next() {
            return navigateNext().getValue();
        }

        @Override
        public V previous() {
            return navigatePrevious().getValue();
        }

        @Override
        public K setValue(final K value) {
            throw new UnsupportedOperationException();
        }
    }
    final class KeyView extends AbstractView<K> {

        /**
         * Creates a new TreeBidiMap.KeyView.
         */
        KeyView(final DataElement orderType) {
            super(orderType);
        }

        @Override
        public boolean contains(final Object obj) {
            checkNonNullComparable(obj, KEY);
            return lookupKey(obj) != null;
        }

        @Override
        public Iterator<K> iterator() {
            return new ViewMapIterator(orderType);
        }

        @Override
        public boolean remove(final Object o) {
            return doRemoveKey(o) != null;
        }

    }

    /**
     * A node used to store the data.
     */
    static class Node<K extends Comparable<K>, V extends Comparable<V>> implements Map.Entry<K, V>, KeyValue<K, V> {

        private final K key;
        private final V value;
        private final Node<K, V>[] leftNode;
        private final Node<K, V>[] rightNode;
        private final Node<K, V>[] parentNode;
        private final boolean[] blackColor;
        private int hashCodeValue;
        private boolean calculatedHashCode;

        /**
         * Makes a new cell with given key and value, and with null
         * links, and black (true) colors.
         *
         * @param key the key of this node
         * @param value the value of this node
         */
        @SuppressWarnings("unchecked")
        Node(final K key, final V value) {
            this.key = key;
            this.value = value;
            leftNode = new Node[2];
            rightNode = new Node[2];
            parentNode = new Node[2];
            blackColor = new boolean[] { true, true };
            calculatedHashCode = false;
        }

        /**
         * Makes this node the same color as another.
         *
         * @param node  the node whose color we're adopting
         * @param dataElement  either the {@link DataElement#KEY key}
         *                     or the {@link DataElement#VALUE value}.
         */
        private void copyColor(final Node<K, V> node, final DataElement dataElement) {
            blackColor[dataElement.ordinal()] = node.blackColor[dataElement.ordinal()];
        }

        /**
         * Compares the specified object with this entry for equality.
         * Returns true if the given object is also a map entry and
         * the two entries represent the same mapping.
         *
         * @param obj  the object to be compared for equality with this entry.
         * @return true if the specified object is equal to this entry.
         */
        @Override
        public boolean equals(final Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof Map.Entry)) {
                return false;
            }
            final Map.Entry<?, ?> e = (Map.Entry<?, ?>) obj;
            return Objects.equals(getKey(), e.getKey()) && Objects.equals(getValue(), e.getValue());
        }

        private Object getData(final DataElement dataElement) {
            switch (dataElement) {
            case KEY:
                return getKey();
            case VALUE:
                return getValue();
            default:
                throw new IllegalArgumentException();
            }
        }

        /**
         * Gets the key.
         *
         * @return the key corresponding to this entry.
         */
        @Override
        public K getKey() {
            return key;
        }

        private Node<K, V> getLeft(final DataElement dataElement) {
            return leftNode[dataElement.ordinal()];
        }

        /**
         * Gets the parent node.
         *
         * @param dataElement  either the {@link DataElement#KEY key}
         *                     or the {@link DataElement#VALUE value}.
         * @return the parent node, may be null
         */
        private Node<K, V> getParent(final DataElement dataElement) {
            return parentNode[dataElement.ordinal()];
        }

        private Node<K, V> getRight(final DataElement dataElement) {
            return rightNode[dataElement.ordinal()];
        }

        /**
         * Gets the value.
         *
         * @return the value corresponding to this entry.
         */
        @Override
        public V getValue() {
            return value;
        }

        /**
         * @return the hash code value for this map entry.
         */
        @Override
        public int hashCode() {
            if (!calculatedHashCode) {
                hashCodeValue = getKey().hashCode() ^ getValue().hashCode();
                calculatedHashCode = true;
            }
            return hashCodeValue;
        }

        /**
         * Is this node black?
         *
         * @param dataElement  either the {@link DataElement#KEY key}
         *                     or the {@link DataElement#VALUE value}.
         * @return true if black (which is represented as a true boolean)
         */
        private boolean isBlack(final DataElement dataElement) {
            return blackColor[dataElement.ordinal()];
        }

        private boolean isLeftChild(final DataElement dataElement) {
            return parentNode[dataElement.ordinal()] != null
                    && parentNode[dataElement.ordinal()].leftNode[dataElement.ordinal()] == this;
        }

        /**
         * Is this node red?
         *
         * @param dataElement  either the {@link DataElement#KEY key}
         *                     or the {@link DataElement#VALUE value}.
         * @return true if non-black
         */
        private boolean isRed(final DataElement dataElement) {
            return !blackColor[dataElement.ordinal()];
        }

        private boolean isRightChild(final DataElement dataElement) {
            return parentNode[dataElement.ordinal()] != null
                    && parentNode[dataElement.ordinal()].rightNode[dataElement.ordinal()] == this;
        }

        /**
         * Makes this node black.
         *
         * @param dataElement  either the {@link DataElement#KEY key}
         *                     or the {@link DataElement#VALUE value}.
         */
        private void setBlack(final DataElement dataElement) {
            blackColor[dataElement.ordinal()] = true;
        }

        private void setLeft(final Node<K, V> node, final DataElement dataElement) {
            leftNode[dataElement.ordinal()] = node;
        }

        /**
         * Sets this node's parent node.
         *
         * @param node  the new parent node
         * @param dataElement  either the {@link DataElement#KEY key}
         *                     or the {@link DataElement#VALUE value}.
         */
        private void setParent(final Node<K, V> node, final DataElement dataElement) {
            parentNode[dataElement.ordinal()] = node;
        }

        /**
         * Makes this node red.
         *
         * @param dataElement  either the {@link DataElement#KEY key}
         *                     or the {@link DataElement#VALUE value}.
         */
        private void setRed(final DataElement dataElement) {
            blackColor[dataElement.ordinal()] = false;
        }

        private void setRight(final Node<K, V> node, final DataElement dataElement) {
            rightNode[dataElement.ordinal()] = node;
        }

        /**
         * Optional operation that is not permitted in this implementation.
         *
         * @param ignored this parameter is ignored.
         * @return does not return
         * @throws UnsupportedOperationException always
         */
        @Override
        public V setValue(final V ignored) throws UnsupportedOperationException {
            throw new UnsupportedOperationException("Map.Entry.setValue is not supported");
        }

        /**
         * Exchanges colors with another node.
         *
         * @param node  the node to swap with
         * @param dataElement  either the {@link DataElement#KEY key}
         *                     or the {@link DataElement#VALUE value}.
         */
        private void swapColors(final Node<K, V> node, final DataElement dataElement) {
            // Swap colors -- old hacker's trick
            blackColor[dataElement.ordinal()]      ^= node.blackColor[dataElement.ordinal()];
            node.blackColor[dataElement.ordinal()] ^= blackColor[dataElement.ordinal()];
            blackColor[dataElement.ordinal()]      ^= node.blackColor[dataElement.ordinal()];
        }
    }

    final class ValueView extends AbstractView<V> {

        /**
         * Creates a new TreeBidiMap.ValueView.
         */
        ValueView(final DataElement orderType) {
            super(orderType);
        }

        @Override
        public boolean contains(final Object obj) {
            checkNonNullComparable(obj, VALUE);
            return lookupValue(obj) != null;
        }

        @Override
        public Iterator<V> iterator() {
            return new InverseViewMapIterator(orderType);
        }

        @Override
        public boolean remove(final Object o) {
            return doRemoveValue(o) != null;
        }

    }

    /**
     * An iterator over the map entries.
     */
    final class ViewMapEntryIterator extends AbstractViewIterator implements OrderedIterator<Map.Entry<K, V>> {

        /**
         * Constructs a new instance.
         */
        ViewMapEntryIterator() {
            super(KEY);
        }

        @Override
        public Map.Entry<K, V> next() {
            return navigateNext();
        }

        @Override
        public Map.Entry<K, V> previous() {
            return navigatePrevious();
        }
    }

    /**
     * An iterator over the map.
     */
    final class ViewMapIterator extends AbstractViewIterator implements OrderedMapIterator<K, V> {

        /**
         * Constructs a new instance.
         */
        ViewMapIterator(final DataElement orderType) {
            super(orderType);
        }

        @Override
        public K getKey() {
            if (lastReturnedNode == null) {
                throw new IllegalStateException(
                        "Iterator getKey() can only be called after next() and before remove()");
            }
            return lastReturnedNode.getKey();
        }

        @Override
        public V getValue() {
            if (lastReturnedNode == null) {
                throw new IllegalStateException(
                        "Iterator getValue() can only be called after next() and before remove()");
            }
            return lastReturnedNode.getValue();
        }

        @Override
        public K next() {
            return navigateNext().getKey();
        }

        @Override
        public K previous() {
            return navigatePrevious().getKey();
        }

        @Override
        public V setValue(final V value) {
            throw new UnsupportedOperationException();
        }
    }

    private static final long serialVersionUID = 721969328361807L;

    /**
     * Checks a key for validity (non-null and implements Comparable)
     *
     * @param key the key to be checked
     * @throws NullPointerException if key is null
     * @throws ClassCastException if key is not Comparable
     */
    private static void checkKey(final Object key) {
        checkNonNullComparable(key, KEY);
    }

    /**
     * Checks a key and a value for validity (non-null and implements
     * Comparable)
     *
     * @param key the key to be checked
     * @param value the value to be checked
     * @throws NullPointerException if key or value is null
     * @throws ClassCastException if key or value is not Comparable
     */
    private static void checkKeyAndValue(final Object key, final Object value) {
        checkKey(key);
        checkValue(value);
    }

    /**
     * Checks if an object is fit to be proper input ... has to be
     * Comparable and non-null.
     *
     * @param obj the object being checked
     * @param dataElement  either the {@link DataElement#KEY key}
     *                     or the {@link DataElement#VALUE value}.
     *
     * @throws NullPointerException if o is null
     * @throws ClassCastException if o is not Comparable
     */
    private static void checkNonNullComparable(final Object obj, final DataElement dataElement) {
        Objects.requireNonNull(obj, Objects.toString(dataElement));
        if (!(obj instanceof Comparable)) {
            throw new ClassCastException(dataElement + " must be Comparable");
        }
    }

    /**
     * Checks a value for validity (non-null and implements Comparable)
     *
     * @param value the value to be checked
     * @throws NullPointerException if value is null
     * @throws ClassCastException if value is not Comparable
     */
    private static void checkValue(final Object value) {
        checkNonNullComparable(value, VALUE);
    }

    /**
     * Compares two objects.
     *
     * @param o1  the first object
     * @param o2  the second object
     * @return negative value if o1 &lt; o2; 0 if o1 == o2; positive
     *         value if o1 &gt; o2
     */
    private static <T extends Comparable<T>> int compare(final T o1, final T o2) {
        return o1.compareTo(o2);
    }

    /**
     * Is the specified black red? If the node does not exist, sure,
     * it's black, thank you.
     *
     * @param node the node (may be null) in question
     * @param dataElement  either the {@link DataElement#KEY key}
     *                     or the {@link DataElement#VALUE value}.
     */
    private static boolean isBlack(final Node<?, ?> node, final DataElement dataElement) {
        return node == null || node.isBlack(dataElement);
    }

    /**
     * Is the specified node red? If the node does not exist, no, it's
     * black, thank you.
     *
     * @param node the node (may be null) in question
     * @param dataElement  either the {@link DataElement#KEY key}
     *                     or the {@link DataElement#VALUE value}.
     */
    private static boolean isRed(final Node<?, ?> node, final DataElement dataElement) {
        return node != null && node.isRed(dataElement);
    }

    /**
     * Forces a node (if it exists) black.
     *
     * @param node the node (may be null) in question
     * @param dataElement  either the {@link DataElement#KEY key}
     *                     or the {@link DataElement#VALUE value}.
     */
    private static void makeBlack(final Node<?, ?> node, final DataElement dataElement) {
        if (node != null) {
            node.setBlack(dataElement);
        }
    }

    /**
     * Forces a node (if it exists) red.
     *
     * @param node the node (may be null) in question
     * @param dataElement  either the {@link DataElement#KEY key}
     *                     or the {@link DataElement#VALUE value}.
     */
    private static void makeRed(final Node<?, ?> node, final DataElement dataElement) {
        if (node != null) {
            node.setRed(dataElement);
        }
    }

    private transient Node<K, V>[] rootNode;

    private transient int nodeCount;

    private transient int modifications;

    private transient Set<K> keySet;

    private transient Set<V> valuesSet;

    private transient Set<Map.Entry<K, V>> entrySet;

    private transient Inverse inverse;

    /**
     * Constructs a new empty TreeBidiMap.
     */
    @SuppressWarnings("unchecked")
    public TreeBidiMap() {
        rootNode = new Node[2];
    }

    /**
     * Constructs a new TreeBidiMap by copying an existing Map.
     *
     * @param map  the map to copy
     * @throws ClassCastException if the keys/values in the map are
     *  not Comparable or are not mutually comparable
     * @throws NullPointerException if any key or value in the map is null
     */
    public TreeBidiMap(final Map<? extends K, ? extends V> map) {
        this();
        putAll(map);
    }

    /**
     * Removes all mappings from this map.
     */
    @Override
    public void clear() {
        modify();

        nodeCount = 0;
        rootNode[KEY.ordinal()] = null;
        rootNode[VALUE.ordinal()] = null;
    }

    /**
     * Checks whether this map contains a mapping for the specified key.
     * <p>
     * The key must implement {@code Comparable}.
     *
     * @param key  key whose presence in this map is to be tested
     * @return true if this map contains a mapping for the specified key
     * @throws ClassCastException if the key is of an inappropriate type
     * @throws NullPointerException if the key is null
     */
    @Override
    public boolean containsKey(final Object key) {
        checkKey(key);
        return lookupKey(key) != null;
    }

    /**
     * Checks whether this map contains a mapping for the specified value.
     * <p>
     * The value must implement {@code Comparable}.
     *
     * @param value  value whose presence in this map is to be tested
     * @return true if this map contains a mapping for the specified value
     * @throws ClassCastException if the value is of an inappropriate type
     * @throws NullPointerException if the value is null
     */
    @Override
    public boolean containsValue(final Object value) {
        checkValue(value);
        return lookupValue(value) != null;
    }

    /**
     * Copies the color from one node to another, dealing with the fact
     * that one or both nodes may, in fact, be null.
     *
     * @param from the node whose color we're copying; may be null
     * @param to the node whose color we're changing; may be null
     * @param dataElement  either the {@link DataElement#KEY key}
     *                     or the {@link DataElement#VALUE value}.
     */
    private void copyColor(final Node<K, V> from, final Node<K, V> to, final DataElement dataElement) {
        if (to != null) {
            if (from == null) {
                // by default, make it black
                to.setBlack(dataElement);
            } else {
                to.copyColor(from, dataElement);
            }
        }
    }

    /**
     * Compares for equals as per the API.
     *
     * @param obj  the object to compare to
     * @param dataElement  either the {@link DataElement#KEY key}
     *                     or the {@link DataElement#VALUE value}.
     * @return true if equal
     */
    private boolean doEquals(final Object obj, final DataElement dataElement) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Map)) {
            return false;
        }
        final Map<?, ?> other = (Map<?, ?>) obj;
        if (other.size() != size()) {
            return false;
        }

        if (nodeCount > 0) {
            try {
                for (final MapIterator<?, ?> it = getMapIterator(dataElement); it.hasNext(); ) {
                    final Object key = it.next();
                    final Object value = it.getValue();
                    if (!value.equals(other.get(key))) {
                        return false;
                    }
                }
            } catch (final ClassCastException | NullPointerException ex) {
                return false;
            }
        }
        return true;
    }

    /**
     * Gets the hash code value for this map as per the API.
     *
     * @param dataElement  either the {@link DataElement#KEY key}
     *                     or the {@link DataElement#VALUE value}.
     * @return the hash code value for this map
     */
    private int doHashCode(final DataElement dataElement) {
        int total = 0;
        if (nodeCount > 0) {
            for (final MapIterator<?, ?> it = getMapIterator(dataElement); it.hasNext(); ) {
                final Object key = it.next();
                final Object value = it.getValue();
                total += key.hashCode() ^ value.hashCode();
            }
        }
        return total;
    }

    /**
     * Puts logic.
     *
     * @param key  the key, always the main map key
     * @param value  the value, always the main map value
     */
    private void doPut(final K key, final V value) {
        checkKeyAndValue(key, value);

        // store previous and remove previous mappings
        doRemoveKey(key);
        doRemoveValue(value);

        Node<K, V> node = rootNode[KEY.ordinal()];
        if (node == null) {
            // map is empty
            final Node<K, V> root = new Node<>(key, value);
            rootNode[KEY.ordinal()] = root;
            rootNode[VALUE.ordinal()] = root;
            grow();

        } else {
            // add new mapping
            while (true) {
                final int cmp = compare(key, node.getKey());

                if (cmp == 0) {
                    // shouldn't happen
                    throw new IllegalArgumentException("Cannot store a duplicate key (\"" + key + "\") in this Map");
                }
                if (cmp < 0) {
                    if (node.getLeft(KEY) == null) {
                        final Node<K, V> newNode = new Node<>(key, value);

                        insertValue(newNode);
                        node.setLeft(newNode, KEY);
                        newNode.setParent(node, KEY);
                        doRedBlackInsert(newNode, KEY);
                        grow();

                        break;
                    }
                    node = node.getLeft(KEY);
                } else { // cmp > 0
                    if (node.getRight(KEY) == null) {
                        final Node<K, V> newNode = new Node<>(key, value);

                        insertValue(newNode);
                        node.setRight(newNode, KEY);
                        newNode.setParent(node, KEY);
                        doRedBlackInsert(newNode, KEY);
                        grow();

                        break;
                    }
                    node = node.getRight(KEY);
                }
            }
        }
    }

    /**
     * Complicated red-black delete stuff. Based on Sun's TreeMap
     * implementation, though it's barely recognizable anymore.
     *
     * @param deletedNode the node to be deleted
     */
    private void doRedBlackDelete(final Node<K, V> deletedNode) {
        for (final DataElement dataElement : DataElement.values()) {
            // if deleted node has both left and children, swap with
            // the next greater node
            if (deletedNode.getLeft(dataElement) != null && deletedNode.getRight(dataElement) != null) {
                swapPosition(nextGreater(deletedNode, dataElement), deletedNode, dataElement);
            }
            final Node<K, V> replacement = deletedNode.getLeft(dataElement) != null ? deletedNode.getLeft(dataElement) : deletedNode.getRight(dataElement);
            if (replacement != null) {
                replacement.setParent(deletedNode.getParent(dataElement), dataElement);
                if (deletedNode.getParent(dataElement) == null) {
                    rootNode[dataElement.ordinal()] = replacement;
                } else if (deletedNode == deletedNode.getParent(dataElement).getLeft(dataElement)) {
                    deletedNode.getParent(dataElement).setLeft(replacement, dataElement);
                } else {
                    deletedNode.getParent(dataElement).setRight(replacement, dataElement);
                }
                deletedNode.setLeft(null, dataElement);
                deletedNode.setRight(null, dataElement);
                deletedNode.setParent(null, dataElement);
                if (isBlack(deletedNode, dataElement)) {
                    doRedBlackDeleteFixup(replacement, dataElement);
                }
            } else if (deletedNode.getParent(dataElement) == null) {
                // replacement is null
                // empty tree
                rootNode[dataElement.ordinal()] = null;
            } else {
                // deleted node had no children
                if (isBlack(deletedNode, dataElement)) {
                    doRedBlackDeleteFixup(deletedNode, dataElement);
                }
                if (deletedNode.getParent(dataElement) != null) {
                    if (deletedNode == deletedNode.getParent(dataElement).getLeft(dataElement)) {
                        deletedNode.getParent(dataElement).setLeft(null, dataElement);
                    } else {
                        deletedNode.getParent(dataElement).setRight(null, dataElement);
                    }
                    deletedNode.setParent(null, dataElement);
                }
            }
        }
        shrink();
    }

    /**
     * Complicated red-black delete stuff. Based on Sun's TreeMap
     * implementation, though it's barely recognizable anymore. This
     * rebalances the tree (somewhat, as red-black trees are not
     * perfectly balanced -- perfect balancing takes longer)
     *
     * @param replacementNode the node being replaced
     * @param dataElement  the KEY or VALUE int
     */
    private void doRedBlackDeleteFixup(final Node<K, V> replacementNode, final DataElement dataElement) {
        Node<K, V> currentNode = replacementNode;

        while (currentNode != rootNode[dataElement.ordinal()] && isBlack(currentNode, dataElement)) {
            if (currentNode.isLeftChild(dataElement)) {
                Node<K, V> siblingNode = getRightChild(getParent(currentNode, dataElement), dataElement);

                if (isRed(siblingNode, dataElement)) {
                    makeBlack(siblingNode, dataElement);
                    makeRed(getParent(currentNode, dataElement), dataElement);
                    rotateLeft(getParent(currentNode, dataElement), dataElement);

                    siblingNode = getRightChild(getParent(currentNode, dataElement), dataElement);
                }

                if (isBlack(getLeftChild(siblingNode, dataElement), dataElement)
                    && isBlack(getRightChild(siblingNode, dataElement), dataElement)) {
                    makeRed(siblingNode, dataElement);

                    currentNode = getParent(currentNode, dataElement);
                } else {
                    if (isBlack(getRightChild(siblingNode, dataElement), dataElement)) {
                        makeBlack(getLeftChild(siblingNode, dataElement), dataElement);
                        makeRed(siblingNode, dataElement);
                        rotateRight(siblingNode, dataElement);

                        siblingNode = getRightChild(getParent(currentNode, dataElement), dataElement);
                    }

                    copyColor(getParent(currentNode, dataElement), siblingNode, dataElement);
                    makeBlack(getParent(currentNode, dataElement), dataElement);
                    makeBlack(getRightChild(siblingNode, dataElement), dataElement);
                    rotateLeft(getParent(currentNode, dataElement), dataElement);

                    currentNode = rootNode[dataElement.ordinal()];
                }
            } else {
                Node<K, V> siblingNode = getLeftChild(getParent(currentNode, dataElement), dataElement);

                if (isRed(siblingNode, dataElement)) {
                    makeBlack(siblingNode, dataElement);
                    makeRed(getParent(currentNode, dataElement), dataElement);
                    rotateRight(getParent(currentNode, dataElement), dataElement);

                    siblingNode = getLeftChild(getParent(currentNode, dataElement), dataElement);
                }

                if (isBlack(getRightChild(siblingNode, dataElement), dataElement)
                    && isBlack(getLeftChild(siblingNode, dataElement), dataElement)) {
                    makeRed(siblingNode, dataElement);

                    currentNode = getParent(currentNode, dataElement);
                } else {
                    if (isBlack(getLeftChild(siblingNode, dataElement), dataElement)) {
                        makeBlack(getRightChild(siblingNode, dataElement), dataElement);
                        makeRed(siblingNode, dataElement);
                        rotateLeft(siblingNode, dataElement);

                        siblingNode = getLeftChild(getParent(currentNode, dataElement), dataElement);
                    }

                    copyColor(getParent(currentNode, dataElement), siblingNode, dataElement);
                    makeBlack(getParent(currentNode, dataElement), dataElement);
                    makeBlack(getLeftChild(siblingNode, dataElement), dataElement);
                    rotateRight(getParent(currentNode, dataElement), dataElement);

                    currentNode = rootNode[dataElement.ordinal()];
                }
            }
        }

        makeBlack(currentNode, dataElement);
    }

    /**
     * Complicated red-black insert stuff. Based on Sun's TreeMap
     * implementation, though it's barely recognizable anymore.
     *
     * @param insertedNode the node to be inserted
     * @param dataElement  the KEY or VALUE int
     */
    private void doRedBlackInsert(final Node<K, V> insertedNode, final DataElement dataElement) {
        Node<K, V> currentNode = insertedNode;
        makeRed(currentNode, dataElement);

        while (currentNode != null
            && currentNode != rootNode[dataElement.ordinal()]
            && isRed(currentNode.getParent(dataElement), dataElement)) {
            if (currentNode.isLeftChild(dataElement)) {
                final Node<K, V> y = getRightChild(getGrandParent(currentNode, dataElement), dataElement);

                if (isRed(y, dataElement)) {
                    makeBlack(getParent(currentNode, dataElement), dataElement);
                    makeBlack(y, dataElement);
                    makeRed(getGrandParent(currentNode, dataElement), dataElement);

                    currentNode = getGrandParent(currentNode, dataElement);
                } else {
                    //dead code?
                    if (currentNode.isRightChild(dataElement)) {
                        currentNode = getParent(currentNode, dataElement);

                        rotateLeft(currentNode, dataElement);
                    }

                    makeBlack(getParent(currentNode, dataElement), dataElement);
                    makeRed(getGrandParent(currentNode, dataElement), dataElement);

                    if (getGrandParent(currentNode, dataElement) != null) {
                        rotateRight(getGrandParent(currentNode, dataElement), dataElement);
                    }
                }
            } else {

                // just like clause above, except swap left for right
                final Node<K, V> y = getLeftChild(getGrandParent(currentNode, dataElement), dataElement);

                if (isRed(y, dataElement)) {
                    makeBlack(getParent(currentNode, dataElement), dataElement);
                    makeBlack(y, dataElement);
                    makeRed(getGrandParent(currentNode, dataElement), dataElement);

                    currentNode = getGrandParent(currentNode, dataElement);
                } else {
                    //dead code?
                    if (currentNode.isLeftChild(dataElement)) {
                        currentNode = getParent(currentNode, dataElement);

                        rotateRight(currentNode, dataElement);
                    }

                    makeBlack(getParent(currentNode, dataElement), dataElement);
                    makeRed(getGrandParent(currentNode, dataElement), dataElement);

                    if (getGrandParent(currentNode, dataElement) != null) {
                        rotateLeft(getGrandParent(currentNode, dataElement), dataElement);
                    }
                }
            }
        }

        makeBlack(rootNode[dataElement.ordinal()], dataElement);
    }

    private V doRemoveKey(final Object key) {
        final Node<K, V> node = lookupKey(key);
        if (node == null) {
            return null;
        }
        doRedBlackDelete(node);
        return node.getValue();
    }

    private K doRemoveValue(final Object value) {
        final Node<K, V> node = lookupValue(value);
        if (node == null) {
            return null;
        }
        doRedBlackDelete(node);
        return node.getKey();
    }

    /**
     * Gets the string form of this map as per AbstractMap.
     *
     * @param dataElement  either the {@link DataElement#KEY key}
     *                     or the {@link DataElement#VALUE value}.
     * @return the string form of this map
     */
    private String doToString(final DataElement dataElement) {
        if (nodeCount == 0) {
            return "{}";
        }
        final StringBuilder buf = new StringBuilder(nodeCount * 32);
        buf.append('{');
        final MapIterator<?, ?> it = getMapIterator(dataElement);
        boolean hasNext = it.hasNext();
        while (hasNext) {
            final Object key = it.next();
            final Object value = it.getValue();
            buf.append(key == this ? "(this Map)" : key)
                .append('=')
                .append(value == this ? "(this Map)" : value);

            hasNext = it.hasNext();
            if (hasNext) {
                buf.append(", ");
            }
        }

        buf.append('}');
        return buf.toString();
    }

    /**
     * Returns a set view of the entries contained in this map in key order.
     * For simple iteration through the map, the MapIterator is quicker.
     * <p>
     * The set is backed by the map, so changes to the map are reflected in
     * the set, and vice-versa. If the map is modified while an iteration over
     * the set is in progress, the results of the iteration are undefined.
     * <p>
     * The set supports element removal, which removes the corresponding mapping
     * from the map. It does not support the add or addAll operations.
     * The returned MapEntry objects do not support setValue.
     *
     * @return a set view of the values contained in this map.
     */
    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        if (entrySet == null) {
            entrySet = new EntryView();
        }
        return entrySet;
    }

    /**
     * Compares for equals as per the API.
     *
     * @param obj  the object to compare to
     * @return true if equal
     */
    @Override
    public boolean equals(final Object obj) {
        return this.doEquals(obj, KEY);
    }

    /**
     * Gets the first (lowest) key currently in this map.
     *
     * @return the first (lowest) key currently in this sorted map
     * @throws NoSuchElementException if this map is empty
     */
    @Override
    public K firstKey() {
        if (nodeCount == 0) {
            throw new NoSuchElementException("Map is empty");
        }
        return leastNode(rootNode[KEY.ordinal()], KEY).getKey();
    }

    /**
     * Gets the value to which this map maps the specified key.
     * Returns null if the map contains no mapping for this key.
     * <p>
     * The key must implement {@code Comparable}.
     *
     * @param key  key whose associated value is to be returned
     * @return the value to which this map maps the specified key,
     *  or null if the map contains no mapping for this key
     * @throws ClassCastException if the key is of an inappropriate type
     * @throws NullPointerException if the key is null
     */
    @Override
    public V get(final Object key) {
        checkKey(key);
        final Node<K, V> node = lookupKey(key);
        return node == null ? null : node.getValue();
    }

    /**
     * Gets a node's grandparent. mind you, the node, its parent, or
     * its grandparent may not exist. No problem.
     *
     * @param node the node (may be null) in question
     * @param dataElement  either the {@link DataElement#KEY key}
     *                     or the {@link DataElement#VALUE value}.
     */
    private Node<K, V> getGrandParent(final Node<K, V> node, final DataElement dataElement) {
        return getParent(getParent(node, dataElement), dataElement);
    }

    /**
     * Gets the key to which this map maps the specified value.
     * Returns null if the map contains no mapping for this value.
     * <p>
     * The value must implement {@code Comparable}.
     *
     * @param value  value whose associated key is to be returned.
     * @return the key to which this map maps the specified value,
     *  or null if the map contains no mapping for this value.
     * @throws ClassCastException if the value is of an inappropriate type
     * @throws NullPointerException if the value is null
     */
    @Override
    public K getKey(final Object value) {
        checkValue(value);
        final Node<K, V> node = lookupValue(value);
        return node == null ? null : node.getKey();
    }

    /**
     * Gets a node's left child. mind you, the node may not exist. no
     * problem.
     *
     * @param node the node (may be null) in question
     * @param dataElement  either the {@link DataElement#KEY key}
     *                     or the {@link DataElement#VALUE value}.
     */
    private Node<K, V> getLeftChild(final Node<K, V> node, final DataElement dataElement) {
        return node == null ? null : node.getLeft(dataElement);
    }

    private MapIterator<?, ?> getMapIterator(final DataElement dataElement) {
        switch (dataElement) {
        case KEY:
            return new ViewMapIterator(KEY);
        case VALUE:
            return new InverseViewMapIterator(VALUE);
        default:
            throw new IllegalArgumentException();
        }
    }

    /**
     * Gets a node's parent. mind you, the node, or its parent, may not
     * exist. no problem.
     *
     * @param node the node (may be null) in question
     * @param dataElement  either the {@link DataElement#KEY key}
     *                     or the {@link DataElement#VALUE value}.
     */
    private Node<K, V> getParent(final Node<K, V> node, final DataElement dataElement) {
        return node == null ? null : node.getParent(dataElement);
    }

    /**
     * Gets a node's right child. mind you, the node may not exist. no
     * problem.
     *
     * @param node the node (may be null) in question
     * @param dataElement  either the {@link DataElement#KEY key}
     *                     or the {@link DataElement#VALUE value}.
     */
    private Node<K, V> getRightChild(final Node<K, V> node, final DataElement dataElement) {
        return node == null ? null : node.getRight(dataElement);
    }

    /**
     * Finds the greatest node from a given node.
     *
     * @param node  the node from which we will start searching
     * @param dataElement  either the {@link DataElement#KEY key}
     *                     or the {@link DataElement#VALUE value}.
     * @return the greatest node, from the specified node
     */
    private Node<K, V> greatestNode(final Node<K, V> node, final DataElement dataElement) {
        Node<K, V> rval = node;
        if (rval != null) {
            while (rval.getRight(dataElement) != null) {
                rval = rval.getRight(dataElement);
            }
        }
        return rval;
    }

    /**
     * Bumps up the size and note that the map has changed.
     */
    private void grow() {
        modify();
        nodeCount++;
    }

    /**
     * Gets the hash code value for this map as per the API.
     *
     * @return the hash code value for this map
     */
    @Override
    public int hashCode() {
        return this.doHashCode(KEY);
    }

    /**
     * Inserts a node by its value.
     *
     * @param newNode the node to be inserted
     * @throws IllegalArgumentException if the node already exists
     *                                     in the value mapping
     */
    private void insertValue(final Node<K, V> newNode) throws IllegalArgumentException {
        Node<K, V> node = rootNode[VALUE.ordinal()];

        while (true) {
            final int cmp = compare(newNode.getValue(), node.getValue());

            if (cmp == 0) {
                throw new IllegalArgumentException(
                    "Cannot store a duplicate value (\"" + newNode.getData(VALUE) + "\") in this Map");
            }
            if (cmp < 0) {
                if (node.getLeft(VALUE) == null) {
                    node.setLeft(newNode, VALUE);
                    newNode.setParent(node, VALUE);
                    doRedBlackInsert(newNode, VALUE);

                    break;
                }
                node = node.getLeft(VALUE);
            } else { // cmp > 0
                if (node.getRight(VALUE) == null) {
                    node.setRight(newNode, VALUE);
                    newNode.setParent(node, VALUE);
                    doRedBlackInsert(newNode, VALUE);

                    break;
                }
                node = node.getRight(VALUE);
            }
        }
    }

    /**
     * Gets the inverse map for comparison.
     *
     * @return the inverse map
     */
    @Override
    public OrderedBidiMap<V, K> inverseBidiMap() {
        if (inverse == null) {
            inverse = new Inverse();
        }
        return inverse;
    }

    /**
     * Checks whether the map is empty or not.
     *
     * @return true if the map is empty
     */
    @Override
    public boolean isEmpty() {
        return nodeCount == 0;
    }

    /**
     * Returns a set view of the keys contained in this map in key order.
     * <p>
     * The set is backed by the map, so changes to the map are reflected in
     * the set, and vice-versa. If the map is modified while an iteration over
     * the set is in progress, the results of the iteration are undefined.
     * <p>
     * The set supports element removal, which removes the corresponding mapping
     * from the map. It does not support the add or addAll operations.
     *
     * @return a set view of the keys contained in this map.
     */
    @Override
    public Set<K> keySet() {
        if (keySet == null) {
            keySet = new KeyView(KEY);
        }
        return keySet;
    }

    /**
     * Gets the last (highest) key currently in this map.
     *
     * @return the last (highest) key currently in this sorted map
     * @throws NoSuchElementException if this map is empty
     */
    @Override
    public K lastKey() {
        if (nodeCount == 0) {
            throw new NoSuchElementException("Map is empty");
        }
        return greatestNode(rootNode[KEY.ordinal()], KEY).getKey();
    }

    /**
     * Finds the least node from a given node.
     *
     * @param node  the node from which we will start searching
     * @param dataElement  either the {@link DataElement#KEY key}
     *                     or the {@link DataElement#VALUE value}.
     * @return the smallest node, from the specified node, in the
     *         specified mapping
     */
    private Node<K, V> leastNode(final Node<K, V> node, final DataElement dataElement) {
        Node<K, V> rval = node;
        if (rval != null) {
            while (rval.getLeft(dataElement) != null) {
                rval = rval.getLeft(dataElement);
            }
        }
        return rval;
    }

    /**
     * Does the actual lookup of a piece of data.
     *
     * @param data the key or value to be looked up
     * @param dataElement  either the {@link DataElement#KEY key}
     *                     or the {@link DataElement#VALUE value}.
     * @return the desired Node, or null if there is no mapping of the
     *         specified data
     */
    @SuppressWarnings("unchecked")
    private <T extends Comparable<T>> Node<K, V> lookup(final Object data, final DataElement dataElement) {
        Node<K, V> rval = null;
        Node<K, V> node = rootNode[dataElement.ordinal()];

        while (node != null) {
            final int cmp = compare((T) data, (T) node.getData(dataElement));
            if (cmp == 0) {
                rval = node;
                break;
            }
            node = cmp < 0 ? node.getLeft(dataElement) : node.getRight(dataElement);
        }

        return rval;
    }

    private Node<K, V> lookupKey(final Object key) {
        return this.<K>lookup(key, KEY);
    }

    private Node<K, V> lookupValue(final Object value) {
        return this.<V>lookup(value, VALUE);
    }

    @Override
    public OrderedMapIterator<K, V> mapIterator() {
        if (isEmpty()) {
            return EmptyOrderedMapIterator.<K, V>emptyOrderedMapIterator();
        }
        return new ViewMapIterator(KEY);
    }

    /**
     * Increments the modification count -- used to check for
     * concurrent modification of the map through the map and through
     * an Iterator from one of its Set or Collection views.
     */
    private void modify() {
        modifications++;
    }

    /**
     * Gets the next larger node from the specified node.
     *
     * @param node the node to be searched from
     * @param dataElement  either the {@link DataElement#KEY key}
     *                     or the {@link DataElement#VALUE value}.
     * @return the specified node
     */
    private Node<K, V> nextGreater(final Node<K, V> node, final DataElement dataElement) {
        final Node<K, V> rval;
        if (node == null) {
            rval = null;
        } else if (node.getRight(dataElement) != null) {
            // everything to the node's right is larger. The least of
            // the right node's descendants is the next larger node
            rval = leastNode(node.getRight(dataElement), dataElement);
        } else {
            // traverse up our ancestry until we find an ancestor that
            // is null or one whose left child is our ancestor. If we
            // find a null, then this node IS the largest node in the
            // tree, and there is no greater node. Otherwise, we are
            // the largest node in the subtree on that ancestor's left
            // ... and that ancestor is the next greatest node
            Node<K, V> parent = node.getParent(dataElement);
            Node<K, V> child = node;

            while (parent != null && child == parent.getRight(dataElement)) {
                child = parent;
                parent = parent.getParent(dataElement);
            }
            rval = parent;
        }
        return rval;
    }

    /**
     * Gets the next key after the one specified.
     * <p>
     * The key must implement {@code Comparable}.
     *
     * @param key the key to search for next from
     * @return the next key, null if no match or at end
     */
    @Override
    public K nextKey(final K key) {
        checkKey(key);
        final Node<K, V> node = nextGreater(lookupKey(key), KEY);
        return node == null ? null : node.getKey();
    }

    /**
     * Gets the next smaller node from the specified node.
     *
     * @param node the node to be searched from
     * @param dataElement  either the {@link DataElement#KEY key}
     *                     or the {@link DataElement#VALUE value}.
     * @return the specified node
     */
    private Node<K, V> nextSmaller(final Node<K, V> node, final DataElement dataElement) {
        final Node<K, V> rval;
        if (node == null) {
            rval = null;
        } else if (node.getLeft(dataElement) != null) {
            // everything to the node's left is smaller. The greatest of
            // the left node's descendants is the next smaller node
            rval = greatestNode(node.getLeft(dataElement), dataElement);
        } else {
            // traverse up our ancestry until we find an ancestor that
            // is null or one whose right child is our ancestor. If we
            // find a null, then this node IS the largest node in the
            // tree, and there is no greater node. Otherwise, we are
            // the largest node in the subtree on that ancestor's right
            // ... and that ancestor is the next greatest node
            Node<K, V> parent = node.getParent(dataElement);
            Node<K, V> child = node;

            while (parent != null && child == parent.getLeft(dataElement)) {
                child = parent;
                parent = parent.getParent(dataElement);
            }
            rval = parent;
        }
        return rval;
    }

    /**
     * Gets the previous key before the one specified.
     * <p>
     * The key must implement {@code Comparable}.
     *
     * @param key the key to search for previous from
     * @return the previous key, null if no match or at start
     */
    @Override
    public K previousKey(final K key) {
        checkKey(key);
        final Node<K, V> node = nextSmaller(lookupKey(key), KEY);
        return node == null ? null : node.getKey();
    }

    /**
     * Puts the key-value pair into the map, replacing any previous pair.
     * <p>
     * When adding a key-value pair, the value may already exist in the map
     * against a different key. That mapping is removed, to ensure that the
     * value only occurs once in the inverse map.
     * <pre>
     *  BidiMap map1 = new TreeBidiMap();
     *  map.put("A","B");  // contains A mapped to B, as per Map
     *  map.put("A","C");  // contains A mapped to C, as per Map
     *
     *  BidiMap map2 = new TreeBidiMap();
     *  map.put("A","B");  // contains A mapped to B, as per Map
     *  map.put("C","B");  // contains C mapped to B, key A is removed
     * </pre>
     * <p>
     * Both key and value must implement {@code Comparable}.
     *
     * @param key  key with which the specified value is to be  associated
     * @param value  value to be associated with the specified key
     * @return the previous value for the key
     * @throws ClassCastException if the key is of an inappropriate type
     * @throws NullPointerException if the key is null
     */
    @Override
    public V put(final K key, final V value) {
        final V result = get(key);
        doPut(key, value);
        return result;
    }

    /**
     * Puts all the mappings from the specified map into this map.
     * <p>
     * All keys and values must implement {@code Comparable}.
     *
     * @param map  the map to copy from
     */
    @Override
    public void putAll(final Map<? extends K, ? extends V> map) {
        for (final Map.Entry<? extends K, ? extends V> e : map.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    /**
     * Deserializes the content of the stream.
     *
     * @param stream the input stream
     * @throws IOException if an error occurs while reading from the stream
     * @throws ClassNotFoundException if an object read from the stream cannot be loaded
     */
    @SuppressWarnings("unchecked") // This will fail at runtime if the stream is incorrect
    private void readObject(final ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        rootNode = new Node[2];
        final int size = stream.readInt();
        for (int i = 0; i < size; i++) {
            final K k = (K) stream.readObject();
            final V v = (V) stream.readObject();
            put(k, v);
        }
    }

    /**
     * Removes the mapping for this key from this map if present.
     * <p>
     * The key must implement {@code Comparable}.
     *
     * @param key  key whose mapping is to be removed from the map.
     * @return previous value associated with specified key,
     *  or null if there was no mapping for key.
     * @throws ClassCastException if the key is of an inappropriate type
     * @throws NullPointerException if the key is null
     */
    @Override
    public V remove(final Object key) {
        return doRemoveKey(key);
    }

    /**
     * Removes the mapping for this value from this map if present.
     * <p>
     * The value must implement {@code Comparable}.
     *
     * @param value  value whose mapping is to be removed from the map
     * @return previous key associated with specified value,
     *  or null if there was no mapping for value.
     * @throws ClassCastException if the value is of an inappropriate type
     * @throws NullPointerException if the value is null
     */
    @Override
    public K removeValue(final Object value) {
        return doRemoveValue(value);
    }

    /**
     * Does a rotate left. standard fare in the world of balanced trees.
     *
     * @param node the node to be rotated
     * @param dataElement  either the {@link DataElement#KEY key}
     *                     or the {@link DataElement#VALUE value}.
     */
    private void rotateLeft(final Node<K, V> node, final DataElement dataElement) {
        final Node<K, V> rightChild = node.getRight(dataElement);
        node.setRight(rightChild.getLeft(dataElement), dataElement);

        if (rightChild.getLeft(dataElement) != null) {
            rightChild.getLeft(dataElement).setParent(node, dataElement);
        }
        rightChild.setParent(node.getParent(dataElement), dataElement);

        if (node.getParent(dataElement) == null) {
            // node was the root ... now its right child is the root
            rootNode[dataElement.ordinal()] = rightChild;
        } else if (node.getParent(dataElement).getLeft(dataElement) == node) {
            node.getParent(dataElement).setLeft(rightChild, dataElement);
        } else {
            node.getParent(dataElement).setRight(rightChild, dataElement);
        }

        rightChild.setLeft(node, dataElement);
        node.setParent(rightChild, dataElement);
    }

    /**
     * Does a rotate right. standard fare in the world of balanced trees.
     *
     * @param node the node to be rotated
     * @param dataElement  either the {@link DataElement#KEY key}
     *                     or the {@link DataElement#VALUE value}.
     */
    private void rotateRight(final Node<K, V> node, final DataElement dataElement) {
        final Node<K, V> leftChild = node.getLeft(dataElement);
        node.setLeft(leftChild.getRight(dataElement), dataElement);
        if (leftChild.getRight(dataElement) != null) {
            leftChild.getRight(dataElement).setParent(node, dataElement);
        }
        leftChild.setParent(node.getParent(dataElement), dataElement);

        if (node.getParent(dataElement) == null) {
            // node was the root ... now its left child is the root
            rootNode[dataElement.ordinal()] = leftChild;
        } else if (node.getParent(dataElement).getRight(dataElement) == node) {
            node.getParent(dataElement).setRight(leftChild, dataElement);
        } else {
            node.getParent(dataElement).setLeft(leftChild, dataElement);
        }

        leftChild.setRight(node, dataElement);
        node.setParent(leftChild, dataElement);
    }

    /**
     * Decrements the size and note that the map has changed.
     */
    private void shrink() {
        modify();
        nodeCount--;
    }

    /**
     * Returns the number of key-value mappings in this map.
     *
     * @return the number of key-value mappings in this map
     */
    @Override
    public int size() {
        return nodeCount;
    }

    /**
     * Swaps two nodes (except for their content), taking care of
     * special cases where one is the other's parent ... hey, it
     * happens.
     *
     * @param x one node
     * @param y another node
     * @param dataElement  the KEY or VALUE int
     */
    private void swapPosition(final Node<K, V> x, final Node<K, V> y, final DataElement dataElement) {
        // Save initial values.
        final Node<K, V> xFormerParent = x.getParent(dataElement);
        final Node<K, V> xFormerLeftChild = x.getLeft(dataElement);
        final Node<K, V> xFormerRightChild = x.getRight(dataElement);
        final Node<K, V> yFormerParent = y.getParent(dataElement);
        final Node<K, V> yFormerLeftChild = y.getLeft(dataElement);
        final Node<K, V> yFormerRightChild = y.getRight(dataElement);
        final boolean xWasLeftChild =
                x.getParent(dataElement) != null && x == x.getParent(dataElement).getLeft(dataElement);
        final boolean yWasLeftChild =
                y.getParent(dataElement) != null && y == y.getParent(dataElement).getLeft(dataElement);

        // Swap, handling special cases of one being the other's parent.
        if (x == yFormerParent) { // x was y's parent
            x.setParent(y, dataElement);

            if (yWasLeftChild) {
                y.setLeft(x, dataElement);
                y.setRight(xFormerRightChild, dataElement);
            } else {
                y.setRight(x, dataElement);
                y.setLeft(xFormerLeftChild, dataElement);
            }
        } else {
            x.setParent(yFormerParent, dataElement);

            if (yFormerParent != null) {
                if (yWasLeftChild) {
                    yFormerParent.setLeft(x, dataElement);
                } else {
                    yFormerParent.setRight(x, dataElement);
                }
            }

            y.setLeft(xFormerLeftChild, dataElement);
            y.setRight(xFormerRightChild, dataElement);
        }

        if (y == xFormerParent) { // y was x's parent
            y.setParent(x, dataElement);

            if (xWasLeftChild) {
                x.setLeft(y, dataElement);
                x.setRight(yFormerRightChild, dataElement);
            } else {
                x.setRight(y, dataElement);
                x.setLeft(yFormerLeftChild, dataElement);
            }
        } else {
            y.setParent(xFormerParent, dataElement);

            if (xFormerParent != null) {
                if (xWasLeftChild) {
                    xFormerParent.setLeft(y, dataElement);
                } else {
                    xFormerParent.setRight(y, dataElement);
                }
            }

            x.setLeft(yFormerLeftChild, dataElement);
            x.setRight(yFormerRightChild, dataElement);
        }

        // Fix children's parent pointers
        if (x.getLeft(dataElement) != null) {
            x.getLeft(dataElement).setParent(x, dataElement);
        }

        if (x.getRight(dataElement) != null) {
            x.getRight(dataElement).setParent(x, dataElement);
        }

        if (y.getLeft(dataElement) != null) {
            y.getLeft(dataElement).setParent(y, dataElement);
        }

        if (y.getRight(dataElement) != null) {
            y.getRight(dataElement).setParent(y, dataElement);
        }

        x.swapColors(y, dataElement);

        // Check if root changed
        if (rootNode[dataElement.ordinal()] == x) {
            rootNode[dataElement.ordinal()] = y;
        } else if (rootNode[dataElement.ordinal()] == y) {
            rootNode[dataElement.ordinal()] = x;
        }
    }

    /**
     * Returns a string version of this Map in standard format.
     *
     * @return a standard format string version of the map
     */
    @Override
    public String toString() {
        return this.doToString(KEY);
    }

    /**
     * Returns a set view of the values contained in this map in key order.
     * The returned object can be cast to a Set.
     * <p>
     * The set is backed by the map, so changes to the map are reflected in
     * the set, and vice-versa. If the map is modified while an iteration over
     * the set is in progress, the results of the iteration are undefined.
     * <p>
     * The set supports element removal, which removes the corresponding mapping
     * from the map. It does not support the add or addAll operations.
     *
     * @return a set view of the values contained in this map.
     */
    @Override
    public Set<V> values() {
        if (valuesSet == null) {
            valuesSet = new ValueView(KEY);
        }
        return valuesSet;
    }

    /**
     * Serializes this object to an ObjectOutputStream.
     *
     * @param out the target ObjectOutputStream.
     * @throws IOException thrown when an I/O errors occur writing to the target stream.
     */
    private void writeObject(final ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeInt(this.size());
        for (final Entry<K, V> entry : entrySet()) {
            out.writeObject(entry.getKey());
            out.writeObject(entry.getValue());
        }
    }

}
