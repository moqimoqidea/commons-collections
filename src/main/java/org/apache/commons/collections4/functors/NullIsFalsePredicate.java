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
import java.util.Objects;

import org.apache.commons.collections4.Predicate;

/**
 * Predicate implementation that returns false if the input is null.
 *
 * @param <T> the type of the input to the predicate.
 * @since 3.0
 */
public final class NullIsFalsePredicate<T> extends AbstractPredicate<T> implements PredicateDecorator<T>, Serializable {

    /** Serial version UID */
    private static final long serialVersionUID = -2997501534564735525L;

    /**
     * Creates the null false predicate.
     *
     * @param <T> the type that the predicate queries
     * @param predicate  the predicate to decorate, not null
     * @return the predicate
     * @throws NullPointerException if the predicate is null
     */
    public static <T> Predicate<T> nullIsFalsePredicate(final Predicate<? super T> predicate) {
        return new NullIsFalsePredicate<>(Objects.requireNonNull(predicate, "predicate"));
    }

    /** The predicate to decorate */
    private final Predicate<? super T> iPredicate;

    /**
     * Constructor that performs no validation.
     * Use {@code nullIsFalsePredicate} if you want that.
     *
     * @param predicate  the predicate to call after the null check
     */
    public NullIsFalsePredicate(final Predicate<? super T> predicate) {
        iPredicate = predicate;
    }

    /**
     * Gets the predicate being decorated.
     *
     * @return the predicate as the only element in an array
     * @since 3.1
     */
    @Override
    @SuppressWarnings("unchecked")
    public Predicate<? super T>[] getPredicates() {
        return new Predicate[] { iPredicate };
    }

    /**
     * Evaluates the predicate returning the result of the decorated predicate
     * once a null check is performed.
     *
     * @param object  the input object
     * @return true if decorated predicate returns true, false if input is null
     */
    @Override
    public boolean test(final T object) {
        if (object == null) {
            return false;
        }
        return iPredicate.test(object);
    }

}
