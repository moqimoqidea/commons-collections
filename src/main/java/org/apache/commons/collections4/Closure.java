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

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Defines a functor interface implemented by classes that do something.
 * <p>
 * A {@code Closure} represents a block of code which is executed from
 * inside some block, function or iteration. It operates an input object.
 * </p>
 * <p>
 * Standard implementations of common closures are provided by
 * {@link ClosureUtils}. These include method invocation and for/while loops.
 * </p>
 *
 * @param <T> the type of the input to the operation.
 * @since 1.0
 * This will be deprecated in 5.0 in favor of {@link Supplier}.
 */
//@Deprecated
public interface Closure<T> extends Consumer<T> {

    @Override
    default void accept(final T input) {
        execute(input);
    }

    /**
     * Performs an action on the specified input object.
     *
     * @param input  the input to execute on
     * @throws ClassCastException (runtime) if the input is the wrong class
     * @throws IllegalArgumentException (runtime) if the input is invalid
     * @throws FunctorException (runtime) if any other error occurs
     */
    void execute(T input);

}
