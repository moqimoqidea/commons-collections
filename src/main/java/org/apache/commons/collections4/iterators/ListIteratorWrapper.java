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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;

import org.apache.commons.collections4.ResettableListIterator;

/**
 * Converts an {@link Iterator} into a {@link ResettableListIterator}.
 * For plain {@code Iterator}s this is accomplished by caching the returned
 * elements.  This class can also be used to simply add
 * {@link org.apache.commons.collections4.ResettableIterator ResettableIterator}
 * functionality to a given {@link ListIterator}.
 * <p>
 * The {@code ListIterator} interface has additional useful methods
 * for navigation - {@code previous()} and the index methods.
 * This class allows a regular {@code Iterator} to behave as a
 * {@code ListIterator}. It achieves this by building a list internally
 * of as the underlying iterator is traversed.
 * </p>
 * <p>
 * The optional operations of {@code ListIterator} are not supported for plain {@code Iterator}s.
 * </p>
 * <p>
 * This class implements ResettableListIterator from Commons Collections 3.2.
 * </p>
 *
 * @param <E> the type of elements in this iterator.
 * @since 2.1
 */
public class ListIteratorWrapper<E> implements ResettableListIterator<E> {

    /** Message used when set or add are called. */
    private static final String UNSUPPORTED_OPERATION_MESSAGE =
        "ListIteratorWrapper does not support optional operations of ListIterator.";

    /** Message used when set or add are called. */
    private static final String CANNOT_REMOVE_MESSAGE = "Cannot remove element at index {0}.";

    /** The underlying iterator being decorated. */
    private final Iterator<? extends E> iterator;
    /** The list being used to cache the iterator. */
    private final List<E> list = new ArrayList<>();

    /** The current index of this iterator. */
    private int currentIndex;
    /** The current index of the wrapped iterator. */
    private int wrappedIteratorIndex;
    /** Recall whether the wrapped iterator's "cursor" is in such a state as to allow remove() to be called */
    private boolean removeState;

    /**
     * Constructs a new {@code ListIteratorWrapper} that will wrap
     * the given iterator.
     *
     * @param iterator  the iterator to wrap
     * @throws NullPointerException if the iterator is null
     */
    public ListIteratorWrapper(final Iterator<? extends E> iterator) {
        this.iterator = Objects.requireNonNull(iterator, "iterator");
    }

    /**
     * Throws {@link UnsupportedOperationException}
     * unless the underlying {@code Iterator} is a {@code ListIterator}.
     *
     * @param obj  the object to add
     * @throws UnsupportedOperationException if the underlying iterator is not of
     * type {@link ListIterator}
     */
    @Override
    public void add(final E obj) throws UnsupportedOperationException {
        if (iterator instanceof ListIterator) {
            @SuppressWarnings("unchecked")
            final ListIterator<E> li = (ListIterator<E>) iterator;
            li.add(obj);
            return;
        }
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    /**
     * Returns true if there are more elements in the iterator.
     *
     * @return true if there are more elements
     */
    @Override
    public boolean hasNext() {
        if (currentIndex == wrappedIteratorIndex || iterator instanceof ListIterator) {
            return iterator.hasNext();
        }
        return true;
    }

    /**
     * Returns true if there are previous elements in the iterator.
     *
     * @return true if there are previous elements
     */
    @Override
    public boolean hasPrevious() {
        if (iterator instanceof ListIterator) {
            final ListIterator<?> li = (ListIterator<?>) iterator;
            return li.hasPrevious();
        }
        return currentIndex > 0;
    }

    /**
     * Returns the next element from the iterator.
     *
     * @return the next element from the iterator
     * @throws NoSuchElementException if there are no more elements
     */
    @Override
    public E next() throws NoSuchElementException {
        if (iterator instanceof ListIterator) {
            return iterator.next();
        }

        if (currentIndex < wrappedIteratorIndex) {
            ++currentIndex;
            return list.get(currentIndex - 1);
        }

        final E retval = iterator.next();
        list.add(retval);
        ++currentIndex;
        ++wrappedIteratorIndex;
        removeState = true;
        return retval;
    }

    /**
     * Returns the index of the next element.
     *
     * @return the index of the next element
     */
    @Override
    public int nextIndex() {
        if (iterator instanceof ListIterator) {
            final ListIterator<?> li = (ListIterator<?>) iterator;
            return li.nextIndex();
        }
        return currentIndex;
    }

    /**
     * Returns the previous element.
     *
     * @return the previous element
     * @throws NoSuchElementException  if there are no previous elements
     */
    @Override
    public E previous() throws NoSuchElementException {
        if (iterator instanceof ListIterator) {
            @SuppressWarnings("unchecked")
            final ListIterator<E> li = (ListIterator<E>) iterator;
            return li.previous();
        }

        if (currentIndex == 0) {
            throw new NoSuchElementException();
        }
        removeState = wrappedIteratorIndex == currentIndex;
        return list.get(--currentIndex);
    }

    /**
     * Returns the index of the previous element.
     *
     * @return  the index of the previous element
     */
    @Override
    public int previousIndex() {
        if (iterator instanceof ListIterator) {
            final ListIterator<?> li = (ListIterator<?>) iterator;
            return li.previousIndex();
        }
        return currentIndex - 1;
    }

    /**
     * Removes the last element that was returned by {@link #next()} or {@link #previous()} from the underlying collection.
     * This call can only be made once per call to {@code next} or {@code previous} and only if {@link #add(Object)} was not called in between.
     *
     * @throws IllegalStateException if {@code next} or {@code previous} have not been called before, or if {@code remove} or {@code add} have been called after the last call to {@code next} or {@code previous}
     */
    @Override
    public void remove() throws IllegalStateException {
        if (iterator instanceof ListIterator) {
            iterator.remove();
            return;
        }
        int removeIndex = currentIndex;
        if (currentIndex == wrappedIteratorIndex) {
            --removeIndex;
        }
        if (!removeState || wrappedIteratorIndex - currentIndex > 1) {
            throw new IllegalStateException(MessageFormat.format(CANNOT_REMOVE_MESSAGE, Integer.valueOf(removeIndex)));
        }
        iterator.remove();
        list.remove(removeIndex);
        currentIndex = removeIndex;
        wrappedIteratorIndex--;
        removeState = false;
    }

    /**
     * Resets this iterator back to the position at which the iterator
     * was created.
     *
     * @since 3.2
     */
    @Override
    public void reset()  {
        if (iterator instanceof ListIterator) {
            final ListIterator<?> li = (ListIterator<?>) iterator;
            while (li.previousIndex() >= 0) {
                li.previous();
            }
            return;
        }
        currentIndex = 0;
    }

    /**
     * Throws {@link UnsupportedOperationException}
     * unless the underlying {@code Iterator} is a {@code ListIterator}.
     *
     * @param obj  the object to set
     * @throws UnsupportedOperationException if the underlying iterator is not of
     * type {@link ListIterator}
     */
    @Override
    public void set(final E obj) throws UnsupportedOperationException {
        if (iterator instanceof ListIterator) {
            @SuppressWarnings("unchecked")
            final ListIterator<E> li = (ListIterator<E>) iterator;
            li.set(obj);
            return;
        }
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

}
