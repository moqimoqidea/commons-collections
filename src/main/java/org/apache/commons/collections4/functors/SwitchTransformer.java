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
package org.apache.commons.collections4.functors;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.collections4.Predicate;
import org.apache.commons.collections4.Transformer;

/**
 * Transformer implementation calls the transformer whose predicate returns true,
 * like a switch statement.
 *
 * @param <T> the type of the input to the function.
 * @param <R> the type of the result of the function.
 * @since 3.0
 */
public class SwitchTransformer<T, R> implements Transformer<T, R>, Serializable {

    /** Serial version UID */
    private static final long serialVersionUID = -6404460890903469332L;

    /**
     * Create a new Transformer that calls one of the transformers depending
     * on the predicates.
     * <p>
     * The Map consists of Predicate keys and Transformer values. A transformer
     * is called if its matching predicate returns true. Each predicate is evaluated
     * until one returns true. If no predicates evaluate to true, the default
     * transformer is called. The default transformer is set in the map with a
     * null key. The ordering is that of the iterator() method on the entryset
     * collection of the map.
     *
     * @param <I>  the input type
     * @param <O>  the output type
     * @param map  a map of predicates to transformers
     * @return the {@code switch} transformer
     * @throws NullPointerException if the map is null
     * @throws NullPointerException if any transformer in the map is null
     * @throws ClassCastException  if the map elements are of the wrong type
     */
    @SuppressWarnings("unchecked")
    public static <I, O> Transformer<I, O> switchTransformer(
            final Map<? extends Predicate<? super I>, ? extends Transformer<? super I, ? extends O>> map) {

        Objects.requireNonNull(map, "map");
        if (map.isEmpty()) {
            return ConstantTransformer.<I, O>nullTransformer();
        }
        // convert to array like this to guarantee iterator() ordering
        final Transformer<? super I, ? extends O> defaultTransformer = map.remove(null);
        final int size = map.size();
        if (size == 0) {
            return (Transformer<I, O>) (defaultTransformer == null ? ConstantTransformer.<I, O>nullTransformer() :
                                                                     defaultTransformer);
        }
        final Transformer<? super I, ? extends O>[] transformers = new Transformer[size];
        final Predicate<? super I>[] preds = new Predicate[size];
        int i = 0;
        for (final Map.Entry<? extends Predicate<? super I>,
                             ? extends Transformer<? super I, ? extends O>> entry : map.entrySet()) {
            preds[i] = entry.getKey();
            transformers[i] = entry.getValue();
            i++;
        }
        return new SwitchTransformer<>(false, preds, transformers, defaultTransformer);
    }
    /**
     * Factory method that performs validation and copies the parameter arrays.
     *
     * @param <I>  the input type
     * @param <O>  the output type
     * @param predicates  array of predicates, cloned, no nulls
     * @param transformers  matching array of transformers, cloned, no nulls
     * @param defaultTransformer  the transformer to use if no match, null means return null
     * @return the {@code chained} transformer
     * @throws NullPointerException if either array is null
     * @throws NullPointerException if any element in the arrays is null
     * @throws IllegalArgumentException if the arrays have different sizes
     */
    @SuppressWarnings("unchecked")
    public static <I, O> Transformer<I, O> switchTransformer(final Predicate<? super I>[] predicates,
            final Transformer<? super I, ? extends O>[] transformers,
            final Transformer<? super I, ? extends O> defaultTransformer) {
        FunctorUtils.validate(predicates);
        FunctorUtils.validate(transformers);
        if (predicates.length != transformers.length) {
            throw new IllegalArgumentException("The predicate and transformer arrays must be the same size");
        }
        if (predicates.length == 0) {
            return (Transformer<I, O>) (defaultTransformer == null ? ConstantTransformer.<I, O>nullTransformer() :
                                                                     defaultTransformer);
        }
        return new SwitchTransformer<>(predicates, transformers, defaultTransformer);
    }
    /** The tests to consider */
    private final Predicate<? super T>[] iPredicates;

    /** The matching transformers to call */
    private final Transformer<? super T, ? extends R>[] iTransformers;

    /** The default transformer to call if no tests match */
    private final Transformer<? super T, ? extends R> iDefault;

    /**
     * Hidden constructor for the use by the static factory methods.
     *
     * @param clone  if {@code true} the input arguments will be cloned
     * @param predicates  array of predicates, no nulls
     * @param transformers  matching array of transformers, no nulls
     * @param defaultTransformer  the transformer to use if no match, null means return null
     */
    private SwitchTransformer(final boolean clone, final Predicate<? super T>[] predicates,
                             final Transformer<? super T, ? extends R>[] transformers,
                             final Transformer<? super T, ? extends R> defaultTransformer) {
        iPredicates = clone ? FunctorUtils.copy(predicates) : predicates;
        iTransformers = clone ? FunctorUtils.copy(transformers) : transformers;
        iDefault = defaultTransformer == null ?
                ConstantTransformer.<T, R>nullTransformer() : defaultTransformer;
    }

    /**
     * Constructor that performs no validation.
     * Use {@code switchTransformer} if you want that.
     *
     * @param predicates  array of predicates, cloned, no nulls
     * @param transformers  matching array of transformers, cloned, no nulls
     * @param defaultTransformer  the transformer to use if no match, null means return null
     */
    public SwitchTransformer(final Predicate<? super T>[] predicates,
            final Transformer<? super T, ? extends R>[] transformers,
            final Transformer<? super T, ? extends R> defaultTransformer) {
        this(true, predicates, transformers, defaultTransformer);
    }

    /**
     * Gets the default transformer.
     *
     * @return the default transformer
     * @since 3.1
     */
    public Transformer<? super T, ? extends R> getDefaultTransformer() {
        return iDefault;
    }

    /**
     * Gets the predicates.
     *
     * @return a copy of the predicates
     * @since 3.1
     */
    public Predicate<? super T>[] getPredicates() {
        return FunctorUtils.copy(iPredicates);
    }

    /**
     * Gets the transformers.
     *
     * @return a copy of the transformers
     * @since 3.1
     */
    public Transformer<? super T, ? extends R>[] getTransformers() {
        return FunctorUtils.copy(iTransformers);
    }

    /**
     * Transforms the input to result by calling the transformer whose matching
     * predicate returns true.
     *
     * @param input  the input object to transform
     * @return the transformed result
     */
    @Override
    public R transform(final T input) {
        for (int i = 0; i < iPredicates.length; i++) {
            if (iPredicates[i].test(input)) {
                return iTransformers[i].apply(input);
            }
        }
        return iDefault.apply(input);
    }

}
