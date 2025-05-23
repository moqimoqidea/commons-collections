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

/**
 * Defines a functor interface implemented by classes that perform a predicate
 * test on an object.
 * <p>
 * A {@code Predicate} is the object equivalent of an {@code if} statement.
 * It uses the input object to return a true or false value, and is often used in
 * validation or filtering.
 * </p>
 * <p>
 * Standard implementations of common predicates are provided by
 * {@link PredicateUtils}. These include true, false, instanceof, equals, and,
 * or, not, method invocation and null testing.
 * </p>
 *
 * @param <T> the type of the input to the predicate.
 * @since 1.0
 * This will be deprecated in 5.0 in favor of {@link java.util.function.Predicate}.
 */
//@Deprecated
public interface Predicate<T> extends java.util.function.Predicate<T> {

    /**
     * Use the specified parameter to perform a test that returns true or false.
     *
     * @param object  the object to evaluate, should not be changed
     * @return true or false
     * @throws ClassCastException (runtime) if the input is the wrong class
     * @throws IllegalArgumentException (runtime) if the input is invalid
     * @throws FunctorException (runtime) if the predicate encounters a problem
     */
    boolean evaluate(T object);

    @Override
    default boolean test(final T t) {
        return evaluate(t);
    }

}
