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

import org.apache.commons.collections4.MapIterator;

/**
 * Provides an implementation of an empty map iterator.
 *
 * @param <K> the type of keys
 * @param <V> the type of mapped values
 * @since 3.1
 */
public class EmptyMapIterator<K, V> extends AbstractEmptyMapIterator<K, V> implements
        MapIterator<K, V> {

    /**
     * Singleton instance of the iterator.
     * @since 3.1
     */
    @SuppressWarnings("rawtypes")
    public static final MapIterator INSTANCE = new EmptyMapIterator<>();

    /**
     * Gets a typed instance of the iterator.
     * @param <K> the key type
     * @param <V> the value type
     * @return {@link MapIterator}&lt;K, V&gt;
     */
    public static <K, V> MapIterator<K, V> emptyMapIterator() {
        return INSTANCE;
    }

    /**
     * Constructs a new instance.
     */
    protected EmptyMapIterator() {
    }

}
