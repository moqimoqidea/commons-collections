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
package org.apache.commons.collections4;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.apache.commons.collections4.bag.HashBag;
import org.apache.commons.collections4.functors.DefaultEquator;
import org.apache.commons.collections4.list.FixedSizeList;
import org.apache.commons.collections4.list.LazyList;
import org.apache.commons.collections4.list.PredicatedList;
import org.apache.commons.collections4.list.TransformedList;
import org.apache.commons.collections4.list.UnmodifiableList;
import org.apache.commons.collections4.sequence.CommandVisitor;
import org.apache.commons.collections4.sequence.EditScript;
import org.apache.commons.collections4.sequence.SequencesComparator;

/**
 * Provides utility methods and decorators for {@link List} instances.
 *
 * @since 1.0
 */
public class ListUtils {

    /**
     * A simple wrapper to use a CharSequence as List.
     */
    private static final class CharSequenceAsList extends AbstractList<Character> {
        private final CharSequence sequence;

        CharSequenceAsList(final CharSequence sequence) {
            this.sequence = sequence;
        }

        @Override
        public Character get(final int index) {
            return Character.valueOf(sequence.charAt(index));
        }

        @Override
        public int size() {
            return sequence.length();
        }
    }

    /**
     * A helper class used to construct the longest common subsequence.
     */
    private static final class LcsVisitor<E> implements CommandVisitor<E> {
        private final ArrayList<E> sequence;

        LcsVisitor() {
            sequence = new ArrayList<>();
        }

        public List<E> getSubSequence() {
            return sequence;
        }

        @Override
        public void visitDeleteCommand(final E object) {
            // noop
        }

        @Override
        public void visitInsertCommand(final E object) {
            // noop
        }

        @Override
        public void visitKeepCommand(final E object) {
            sequence.add(object);
        }
    }

    /**
     * Provides a partition view on a {@link List}.
     * @since 4.0
     */
    private static final class Partition<T> extends AbstractList<List<T>> {
        private final List<T> list;
        private final int size;

        private Partition(final List<T> list, final int size) {
            this.list = list;
            this.size = size;
        }

        @Override
        public List<T> get(final int index) {
            final int listSize = size();
            if (index < 0) {
                throw new IndexOutOfBoundsException("Index " + index + " must not be negative");
            }
            if (index >= listSize) {
                throw new IndexOutOfBoundsException("Index " + index + " must be less than size " +
                                                    listSize);
            }
            final int start = index * size;
            final int end = Math.min(start + size, list.size());
            return list.subList(start, end);
        }

        @Override
        public boolean isEmpty() {
            return list.isEmpty();
        }

        @Override
        public int size() {
            return (int) Math.ceil((double) list.size() / (double) size);
        }
    }

    /**
     * Returns either the passed in list, or if the list is {@code null},
     * the value of {@code defaultList}.
     *
     * @param <T> the element type
     * @param list  the list, possibly {@code null}
     * @param defaultList  the returned values if list is {@code null}
     * @return an empty list if the argument is {@code null}
     * @since 4.0
     */
    public static <T> List<T> defaultIfNull(final List<T> list, final List<T> defaultList) {
        return list == null ? defaultList : list;
    }

    /**
     * Returns an immutable empty list if the argument is {@code null},
     * or the argument itself otherwise.
     *
     * @param <T> the element type
     * @param list the list, possibly {@code null}
     * @return an empty list if the argument is {@code null}
     */
    public static <T> List<T> emptyIfNull(final List<T> list) {
        return list == null ? Collections.<T>emptyList() : list;
    }

    /**
     * Returns a fixed-sized list backed by the given list.
     * Elements may not be added or removed from the returned list, but
     * existing elements can be changed (for instance, via the
     * {@link List#set(int, Object)} method).
     *
     * @param <E>  the element type
     * @param list  the list whose size to fix, must not be null
     * @return a fixed-size list backed by that list
     * @throws NullPointerException  if the List is null
     */
    public static <E> List<E> fixedSizeList(final List<E> list) {
        return FixedSizeList.fixedSizeList(list);
    }

    /**
     * Gets the first element of a list.
     * <p>
     * Shorthand for {@code list.get(0)}
     * </p>
     *
     * @param <T> The list type.
     * @param list The list.
     * @return the first element of a list.
     * @see List#get(int)
     * @since 4.5.0-M1
     */
    public static <T> T getFirst(final List<T> list) {
        return Objects.requireNonNull(list, "list").get(0);
    }

    /**
     * Gets the last element of a list.
     * <p>
     * Shorthand for {@code list.get(list.size() - 1)}
     * </p>
     *
     * @param <T> The list type.
     * @param list The list.
     * @return the last element of a list.
     * @see List#get(int)
     * @since 4.5.0-M1
     */
    public static <T> T getLast(final List<T> list) {
        return Objects.requireNonNull(list, "list").get(list.size() - 1);
    }

    /**
     * Generates a hash code using the algorithm specified in
     * {@link java.util.List#hashCode()}.
     * <p>
     * This method is useful for implementing {@code List} when you cannot
     * extend AbstractList. The method takes Collection instances to enable other
     * collection types to use the List implementation algorithm.
     * </p>
     *
     * @see java.util.List#hashCode()
     * @param list  the list to generate the hashCode for, may be null
     * @return the hash code
     */
    public static int hashCodeForList(final Collection<?> list) {
        if (list == null) {
            return 0;
        }
        int hashCode = 1;

        for (final Object obj : list) {
            hashCode = 31 * hashCode + (obj == null ? 0 : obj.hashCode());
        }
        return hashCode;
    }

    /**
     * Finds the first index in the given List which matches the given predicate.
     * <p>
     * If the input List or predicate is null, or no element of the List
     * matches the predicate, -1 is returned.
     * </p>
     *
     * @param <E>  the element type
     * @param list the List to search, may be null
     * @param predicate  the predicate to use, may be null
     * @return the first index of an Object in the List which matches the predicate or -1 if none could be found
     */
    public static <E> int indexOf(final List<E> list, final Predicate<E> predicate) {
        if (list != null && predicate != null) {
            for (int i = 0; i < list.size(); i++) {
                final E item = list.get(i);
                if (predicate.test(item)) {
                    return i;
                }
            }
        }
        return CollectionUtils.INDEX_NOT_FOUND;
    }

    /**
     * Returns a new list containing all elements that are contained in
     * both given lists.
     *
     * @param <E> the element type
     * @param list1  the first list
     * @param list2  the second list
     * @return  the intersection of those two lists
     * @throws NullPointerException if either list is null
     */
    public static <E> List<E> intersection(final List<? extends E> list1, final List<? extends E> list2) {
        final List<E> result = new ArrayList<>();

        List<? extends E> smaller = list1;
        List<? extends E> larger = list2;
        if (list1.size() > list2.size()) {
            smaller = list2;
            larger = list1;
        }

        final HashSet<E> hashSet = new HashSet<>(smaller);

        for (final E e : larger) {
            if (hashSet.contains(e)) {
                result.add(e);
                hashSet.remove(e);
            }
        }
        return result;
    }

    /**
     * Tests two lists for value-equality as per the equality contract in
     * {@link java.util.List#equals(Object)}.
     * <p>
     * This method is useful for implementing {@code List} when you cannot
     * extend AbstractList. The method takes Collection instances to enable other
     * collection types to use the List implementation algorithm.
     * </p>
     * <p>
     * The relevant text (slightly paraphrased as this is a static method) is:
     * </p>
     * <blockquote>
     * Compares the two list objects for equality.  Returns
     * {@code true} if and only if both
     * lists have the same size, and all corresponding pairs of elements in
     * the two lists are <em>equal</em>.  (Two elements {@code e1} and
     * {@code e2} are <em>equal</em> if {@code (e1==null ? e2==null :
     * e1.equals(e2))}.)  In other words, two lists are defined to be
     * equal if they contain the same elements in the same order.  This
     * definition ensures that the equals method works properly across
     * different implementations of the {@code List} interface.
     * </blockquote>
     * <p>
     * <strong>Note:</strong> The behavior of this method is undefined if the lists are
     * modified during the equals comparison.
     * </p>
     *
     * @see java.util.List
     * @param list1  the first list, may be null
     * @param list2  the second list, may be null
     * @return whether the lists are equal by value comparison
     */
    public static boolean isEqualList(final Collection<?> list1, final Collection<?> list2) {
        if (list1 == list2) {
            return true;
        }
        if (list1 == null || list2 == null || list1.size() != list2.size()) {
            return false;
        }

        final Iterator<?> it1 = list1.iterator();
        final Iterator<?> it2 = list2.iterator();

        while (it1.hasNext() && it2.hasNext()) {
            final Object obj1 = it1.next();
            final Object obj2 = it2.next();

            if (!Objects.equals(obj1, obj2)) {
                return false;
            }
        }

        return !(it1.hasNext() || it2.hasNext());
    }

    /**
     * Returns a "lazy" list whose elements will be created on demand.
     * <p>
     * When the index passed to the returned list's {@link List#get(int) get}
     * method is greater than the list's size, then the factory will be used
     * to create a new object and that object will be inserted at that index.
     * </p>
     * <p>
     * For instance:
     * </p>
     * <pre>
     * Factory&lt;Date&gt; factory = new Factory&lt;Date&gt;() {
     *     public Date create() {
     *         return new Date();
     *     }
     * }
     * List&lt;Date&gt; lazy = ListUtils.lazyList(new ArrayList&lt;Date&gt;(), factory);
     * Date date = lazy.get(3);
     * </pre>
     * <p>
     * After the above code is executed, {@code date} will refer to
     * a new {@code Date} instance. Furthermore, that {@code Date}
     * instance is the fourth element in the list.  The first, second,
     * and third element are all set to {@code null}.
     * </p>
     *
     * @param <E> the element type
     * @param list  the list to make lazy, must not be null
     * @param factory  the factory for creating new objects, must not be null
     * @return a lazy list backed by the given list
     * @throws NullPointerException if the List or Factory is null
     */
    public static <E> List<E> lazyList(final List<E> list, final Factory<? extends E> factory) {
        return LazyList.lazyList(list, factory);
    }

    /**
     * Returns a "lazy" list whose elements will be created on demand.
     * <p>
     * When the index passed to the returned list's {@link List#get(int) get}
     * method is greater than the list's size, then the transformer will be used
     * to create a new object and that object will be inserted at that index.
     * </p>
     * <p>
     * For instance:
     * </p>
     * <pre>
     * List&lt;Integer&gt; hours = Arrays.asList(7, 5, 8, 2);
     * Transformer&lt;Integer,Date&gt; transformer = input -&gt; LocalDateTime.now().withHour(hours.get(input));
     * List&lt;LocalDateTime&gt; lazy = ListUtils.lazyList(new ArrayList&lt;LocalDateTime&gt;(), transformer);
     * Date date = lazy.get(3);
     * </pre>
     * <p>
     * After the above code is executed, {@code date} will refer to
     * a new {@code Date} instance. Furthermore, that {@code Date}
     * instance is the fourth element in the list.  The first, second,
     * and third element are all set to {@code null}.
     * </p>
     *
     * @param <E> the element type
     * @param list  the list to make lazy, must not be null
     * @param transformer  the transformer for creating new objects, must not be null
     * @return a lazy list backed by the given list
     * @throws NullPointerException if the List or Transformer is null
     */
    public static <E> List<E> lazyList(final List<E> list, final Transformer<Integer, ? extends E> transformer) {
        return LazyList.lazyList(list, transformer);
    }

    /**
     * Returns the longest common subsequence (LCS) of two {@link CharSequence} objects.
     * <p>
     * This is a convenience method for using {@link #longestCommonSubsequence(List, List)}
     * with {@link CharSequence} instances.
     * </p>
     *
     * @param charSequenceA  the first sequence
     * @param charSequenceB  the second sequence
     * @return the longest common subsequence as {@link String}
     * @throws NullPointerException if either sequence is {@code null}
     * @since 4.0
     */
    public static String longestCommonSubsequence(final CharSequence charSequenceA, final CharSequence charSequenceB) {
        Objects.requireNonNull(charSequenceA, "charSequenceA");
        Objects.requireNonNull(charSequenceB, "charSequenceB");
        final List<Character> lcs = longestCommonSubsequence(new CharSequenceAsList(charSequenceA),
                new CharSequenceAsList(charSequenceB));
        final StringBuilder sb = new StringBuilder();
        for (final Character ch : lcs) {
            sb.append(ch);
        }
        return sb.toString();
    }

    /**
     * Returns the longest common subsequence (LCS) of two sequences (lists).
     *
     * @param <E>  the element type
     * @param a  the first list
     * @param b  the second list
     * @return the longest common subsequence
     * @throws NullPointerException if either list is {@code null}
     * @since 4.0
     */
    public static <E> List<E> longestCommonSubsequence(final List<E> a, final List<E> b) {
        return longestCommonSubsequence(a, b, DefaultEquator.defaultEquator());
    }

    /**
     * Returns the longest common subsequence (LCS) of two sequences (lists).
     *
     * @param <E>  the element type
     * @param listA  the first list
     * @param listB  the second list
     * @param equator  the equator used to test object equality
     * @return the longest common subsequence
     * @throws NullPointerException if either list or the equator is {@code null}
     * @since 4.0
     */
    public static <E> List<E> longestCommonSubsequence(final List<E> listA, final List<E> listB,
                                                       final Equator<? super E> equator) {
        Objects.requireNonNull(listA, "listA");
        Objects.requireNonNull(listB, "listB");
        Objects.requireNonNull(equator, "equator");

        final SequencesComparator<E> comparator = new SequencesComparator<>(listA, listB, equator);
        final EditScript<E> script = comparator.getScript();
        final LcsVisitor<E> visitor = new LcsVisitor<>();
        script.visit(visitor);
        return visitor.getSubSequence();
    }

    /**
     * Returns consecutive {@link List#subList(int, int) sublists} of a
     * list, each of the same size (the final list may be smaller). For example,
     * partitioning a list containing {@code [a, b, c, d, e]} with a partition
     * size of 3 yields {@code [[a, b, c], [d, e]]} -- an outer list containing
     * two inner lists of three and two elements, all in the original order.
     * <p>
     * The outer list is unmodifiable, but reflects the latest state of the
     * source list. The inner lists are sublist views of the original list,
     * produced on demand using {@link List#subList(int, int)}, and are subject
     * to all the usual caveats about modification as explained in that API.
     * </p>
     * <p>
     * Adapted from https://github.com/google/guava
     * </p>
     *
     * @param <T> the element type
     * @param list  the list to return consecutive sublists of
     * @param size  the desired size of each sublist (the last may be smaller)
     * @return a list of consecutive sublists
     * @throws NullPointerException if list is null
     * @throws IllegalArgumentException if size is not strictly positive
     * @since 4.0
     */
    public static <T> List<List<T>> partition(final List<T> list, final int size) {
        Objects.requireNonNull(list, "list");
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be greater than 0");
        }
        return new Partition<>(list, size);
    }

    /**
     * Returns a predicated (validating) list backed by the given list.
     * <p>
     * Only objects that pass the test in the given predicate can be added to the list.
     * Trying to add an invalid object results in an IllegalArgumentException.
     * It is important not to use the original list after invoking this method,
     * as it is a backdoor for adding invalid objects.
     * </p>
     *
     * @param <E> the element type
     * @param list  the list to predicate, must not be null
     * @param predicate  the predicate for the list, must not be null
     * @return a predicated list backed by the given list
     * @throws NullPointerException if the List or Predicate is null
     */
    public static <E> List<E> predicatedList(final List<E> list, final Predicate<E> predicate) {
        return PredicatedList.predicatedList(list, predicate);
    }

    /**
     * Removes the elements in {@code remove} from {@code collection}. That is, this
     * method returns a list containing all the elements in {@code collection}
     * that are not in {@code remove}. The cardinality of an element {@code e}
     * in the returned collection is the same as the cardinality of {@code e}
     * in {@code collection} unless {@code remove} contains {@code e}, in which
     * case the cardinality is zero. This method is useful if you do not wish to modify
     * {@code collection} and thus cannot call {@code collection.removeAll(remove);}.
     * <p>
     * This implementation iterates over {@code collection}, checking each element in
     * turn to see if it's contained in {@code remove}. If it's not contained, it's added
     * to the returned list. As a consequence, it is advised to use a collection type for
     * {@code remove} that provides a fast (for example O(1)) implementation of
     * {@link Collection#contains(Object)}.
     * </p>
     *
     * @param <E>  the element type
     * @param collection  the collection from which items are removed (in the returned collection)
     * @param remove  the items to be removed from the returned {@code collection}
     * @return a {@code List} containing all the elements of {@code c} except
     * any elements that also occur in {@code remove}.
     * @throws NullPointerException if either parameter is null
     * @since 3.2
     */
    public static <E> List<E> removeAll(final Collection<E> collection, final Collection<?> remove) {
        Objects.requireNonNull(collection, "collection");
        Objects.requireNonNull(remove, "remove");
        final List<E> list = new ArrayList<>();
        for (final E obj : collection) {
            if (!remove.contains(obj)) {
                list.add(obj);
            }
        }
        return list;
    }

    /**
     * Returns a List containing all the elements in {@code collection}
     * that are also in {@code retain}. The cardinality of an element {@code e}
     * in the returned list is the same as the cardinality of {@code e}
     * in {@code collection} unless {@code retain} does not contain {@code e}, in which
     * case the cardinality is zero. This method is useful if you do not wish to modify
     * the collection {@code c} and thus cannot call {@code collection.retainAll(retain);}.
     * <p>
     * This implementation iterates over {@code collection}, checking each element in
     * turn to see if it's contained in {@code retain}. If it's contained, it's added
     * to the returned list. As a consequence, it is advised to use a collection type for
     * {@code retain} that provides a fast (for example O(1)) implementation of
     * {@link Collection#contains(Object)}.
     * </p>
     *
     * @param <E>  the element type
     * @param collection  the collection whose contents are the target of the #retailAll operation
     * @param retain  the collection containing the elements to be retained in the returned collection
     * @return a {@code List} containing all the elements of {@code c}
     * that occur at least once in {@code retain}.
     * @throws NullPointerException if either parameter is null
     * @since 3.2
     */
    public static <E> List<E> retainAll(final Collection<E> collection, final Collection<?> retain) {
        final List<E> list = new ArrayList<>(Math.min(collection.size(), retain.size()));

        for (final E obj : collection) {
            if (retain.contains(obj)) {
                list.add(obj);
            }
        }
        return list;
    }

    /**
     * Selects all elements from input collection which match the given
     * predicate into an output list.
     * <p>
     * A {@code null} predicate matches no elements.
     * </p>
     *
     * @param <E> the element type
     * @param inputCollection  the collection to get the input from, may not be null
     * @param predicate  the predicate to use, may be null
     * @return the elements matching the predicate (new list)
     * @throws NullPointerException if the input list is null
     * @since 4.0
     * @see CollectionUtils#select(Iterable, Predicate)
     */
    public static <E> List<E> select(final Collection<? extends E> inputCollection,
            final Predicate<? super E> predicate) {
        return CollectionUtils.select(inputCollection, predicate, new ArrayList<>(inputCollection.size()));
    }

    /**
     * Selects all elements from inputCollection which don't match the given
     * predicate into an output collection.
     * <p>
     * If the input predicate is {@code null}, the result is an empty list.
     * </p>
     *
     * @param <E> the element type
     * @param inputCollection the collection to get the input from, may not be null
     * @param predicate the predicate to use, may be null
     * @return the elements <strong>not</strong> matching the predicate (new list)
     * @throws NullPointerException if the input collection is null
     * @since 4.0
     * @see CollectionUtils#selectRejected(Iterable, Predicate)
     */
    public static <E> List<E> selectRejected(final Collection<? extends E> inputCollection,
            final Predicate<? super E> predicate) {
        return CollectionUtils.selectRejected(inputCollection, predicate, new ArrayList<>(inputCollection.size()));
    }

    /**
     * Subtracts all elements in the second list from the first list,
     * placing the results in a new list.
     * <p>
     * This differs from {@link List#removeAll(Collection)} in that
     * cardinality is respected; if <Code>list1</Code> contains two
     * occurrences of <Code>null</Code> and <Code>list2</Code> only
     * contains one occurrence, then the returned list will still contain
     * one occurrence.
     * </p>
     *
     * @param <E> the element type
     * @param list1  the list to subtract from
     * @param list2  the list to subtract
     * @return a new list containing the results
     * @throws NullPointerException if either list is null
     */
    public static <E> List<E> subtract(final List<E> list1, final List<? extends E> list2) {
        final ArrayList<E> result = new ArrayList<>();
        final HashBag<E> bag = new HashBag<>(list2);
        for (final E e : list1) {
            if (!bag.remove(e, 1)) {
                result.add(e);
            }
        }
        return result;
    }

    /**
     * Returns the sum of the given lists.  This is their intersection
     * subtracted from their union.
     *
     * @param <E> the element type
     * @param list1  the first list
     * @param list2  the second list
     * @return  a new list containing the sum of those lists
     * @throws NullPointerException if either list is null
     */
    public static <E> List<E> sum(final List<? extends E> list1, final List<? extends E> list2) {
        return subtract(union(list1, list2), intersection(list1, list2));
    }

    /**
     * Returns a synchronized list backed by the given list.
     * <p>
     * You must manually synchronize on the returned list's iterator to
     * avoid non-deterministic behavior:
     * </p>
     * <pre>
     * List list = ListUtils.synchronizedList(myList);
     * synchronized (list) {
     *     Iterator i = list.iterator();
     *     while (i.hasNext()) {
     *         process (i.next());
     *     }
     * }
     * </pre>
     * <p>
     * This method is just a wrapper for {@link Collections#synchronizedList(List)}.
     * </p>
     *
     * @param <E> the element type
     * @param list  the list to synchronize, must not be null
     * @return a synchronized list backed by the given list
     * @throws NullPointerException if the list is null
     */
    public static <E> List<E> synchronizedList(final List<E> list) {
        return Collections.synchronizedList(list);
    }

    /**
     * Returns a transformed list backed by the given list.
     * <p>
     * This method returns a new list (decorating the specified list) that
     * will transform any new entries added to it.
     * Existing entries in the specified list will not be transformed.
     * </p>
     * <p>
     * Each object is passed through the transformer as it is added to the
     * List. It is important not to use the original list after invoking this
     * method, as it is a backdoor for adding untransformed objects.
     * </p>
     * <p>
     * Existing entries in the specified list will not be transformed.
     * If you want that behavior, see {@link TransformedList#transformedList}.
     * </p>
     *
     * @param <E> the element type
     * @param list  the list to predicate, must not be null
     * @param transformer  the transformer for the list, must not be null
     * @return a transformed list backed by the given list
     * @throws NullPointerException if the List or Transformer is null
     */
    public static <E> List<E> transformedList(final List<E> list,
                                              final Transformer<? super E, ? extends E> transformer) {
        return TransformedList.transformingList(list, transformer);
    }

    /**
     * Returns a new list containing the second list appended to the
     * first list.  The {@link List#addAll(Collection)} operation is
     * used to append the two given lists into a new list.
     *
     * @param <E> the element type
     * @param list1  the first list
     * @param list2  the second list
     * @return a new list containing the union of those lists
     * @throws NullPointerException if either list is null
     */
    public static <E> List<E> union(final List<? extends E> list1, final List<? extends E> list2) {
        final ArrayList<E> result = new ArrayList<>(list1.size() + list2.size());
        result.addAll(list1);
        result.addAll(list2);
        return result;
    }

    /**
     * Returns an unmodifiable list backed by the given list.
     * <p>
     * This method uses the implementation in the decorators subpackage.
     * </p>
     *
     * @param <E>  the element type
     * @param list  the list to make unmodifiable, must not be null
     * @return an unmodifiable list backed by the given list
     * @throws NullPointerException if the list is null
     */
    public static <E> List<E> unmodifiableList(final List<? extends E> list) {
        return UnmodifiableList.unmodifiableList(list);
    }

    /**
     * Don't allow instances.
     */
    private ListUtils() {
        // empty
    }

}
