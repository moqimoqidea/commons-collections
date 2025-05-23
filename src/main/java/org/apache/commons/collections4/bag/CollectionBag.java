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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.collections4.Bag;

/**
 * Decorates another {@link Bag} to comply with the Collection contract.
 * <p>
 * By decorating an existing {@link Bag} instance with a {@link CollectionBag},
 * it can be safely passed on to methods that require Collection types that
 * are fully compliant with the Collection contract.
 * </p>
 * <p>
 * The method Javadoc highlights the differences compared to the original Bag interface.
 * </p>
 *
 * @see Bag
 * @param <E> the type of elements in this bag
 * @since 4.0
 */
public final class CollectionBag<E> extends AbstractBagDecorator<E> {

    /** Serialization version */
    private static final long serialVersionUID = -2560033712679053143L;

    /**
     * Factory method to create a bag that complies to the Collection contract.
     *
     * @param <E> the type of the elements in the bag
     * @param bag  the bag to decorate, must not be null
     * @return a Bag that complies to the Collection contract
     * @throws NullPointerException if bag is null
     */
    public static <E> Bag<E> collectionBag(final Bag<E> bag) {
        return new CollectionBag<>(bag);
    }

    /**
     * Constructor that wraps (not copies).
     *
     * @param bag  the bag to decorate, must not be null
     * @throws NullPointerException if bag is null
     */
    public CollectionBag(final Bag<E> bag) {
        super(bag);
    }

    /**
     * <em>(Change)</em> Adds one copy of the specified object to the Bag.
     * <p>
     * Since this method always increases the size of the bag, it will always return {@code true}.
     * </p>
     *
     * @param object the object to add
     * @return {@code true}, always
     * @throws ClassCastException if the class of the specified element prevents it from being added to this collection
     */
    @Override
    public boolean add(final E object) {
        return add(object, 1);
    }

    /**
     * <em>(Change)</em>
     * Adds {@code count} copies of the specified object to the Bag.
     * <p>
     * Since this method always increases the size of the bag, it
     * will always return {@code true}.
     *
     * @param object  the object to add
     * @param count  the number of copies to add
     * @return {@code true}, always
     * @throws ClassCastException if the class of the specified element prevents it from being added to this collection
     */
    @Override
    public boolean add(final E object, final int count) {
        decorated().add(object, count);
        return true;
    }

    @Override
    public boolean addAll(final Collection<? extends E> coll) {
        boolean changed = false;
        for (final E current : coll) {
            final boolean added = add(current, 1);
            changed = changed || added;
        }
        return changed;
    }

    /**
     * <em>(Change)</em>
     * Returns {@code true} if the bag contains all elements in
     * the given collection, <strong>not</strong> respecting cardinality. That is,
     * if the given collection {@code coll} contains at least one of
     * every object contained in this object.
     *
     * @param coll  the collection to check against
     * @return {@code true} if the Bag contains at least one of every object in the collection
     */
    @Override
    public boolean containsAll(final Collection<?> coll) {
        return coll.stream().allMatch(this::contains);
    }

    /**
     * Reads the collection in using a custom routine.
     *
     * @param in  the input stream
     * @throws IOException if an error occurs while reading from the stream
     * @throws ClassNotFoundException if an object read from the stream cannot be loaded
     * @throws ClassCastException if deserialized object has wrong type
     */
    @SuppressWarnings("unchecked") // will throw CCE, see Javadoc
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        setCollection((Collection<E>) in.readObject());
    }

    /**
     * <em>(Change)</em>
     * Removes the first occurrence of the given object from the bag.
     * <p>
     * This will also remove the object from the {@link #uniqueSet()} if the
     * bag contains no occurrence anymore of the object after this operation.
     * </p>
     *
     * @param object  the object to remove
     * @return {@code true} if this call changed the collection
     */
    @Override
    public boolean remove(final Object object) {
        return remove(object, 1);
    }

    /**
     * <em>(Change)</em>
     * Remove all elements represented in the given collection,
     * <strong>not</strong> respecting cardinality. That is, remove <em>all</em>
     * occurrences of every object contained in the given collection.
     *
     * @param coll  the collection to remove
     * @return {@code true} if this call changed the collection
     */
    @Override
    public boolean removeAll(final Collection<?> coll) {
        if (coll != null) {
            boolean result = false;
            for (final Object obj : coll) {
                final boolean changed = remove(obj, getCount(obj));
                result = result || changed;
            }
            return result;
        }
        // let the decorated bag handle the case of null argument
        return decorated().removeAll(null);
    }

    /**
     * <em>(Change)</em>
     * Remove any members of the bag that are not in the given collection,
     * <em>not</em> respecting cardinality. That is, any object in the given
     * collection {@code coll} will be retained in the bag with the same
     * number of copies prior to this operation. All other objects will be
     * completely removed from this bag.
     * <p>
     * This implementation iterates over the elements of this bag, checking
     * each element in turn to see if it's contained in {@code coll}.
     * If it's not contained, it's removed from this bag. As a consequence,
     * it is advised to use a collection type for {@code coll} that provides
     * a fast (for example O(1)) implementation of {@link Collection#contains(Object)}.
     * </p>
     *
     * @param coll  the collection to retain
     * @return {@code true} if this call changed the collection
     */
    @Override
    public boolean retainAll(final Collection<?> coll) {
        if (coll != null) {
            boolean modified = false;
            final Iterator<E> e = iterator();
            while (e.hasNext()) {
                if (!coll.contains(e.next())) {
                    e.remove();
                    modified = true;
                }
            }
            return modified;
        }
        // let the decorated bag handle the case of null argument
        return decorated().retainAll(null);
    }

    /**
     * Writes the collection out using a custom routine.
     *
     * @param out  the output stream
     * @throws IOException if an error occurs while writing to the stream
     */
    private void writeObject(final ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeObject(decorated());
    }

}
