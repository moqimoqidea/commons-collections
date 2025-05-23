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
package org.apache.commons.collections4.keyvalue;

import java.util.Map;
import java.util.Objects;

import org.apache.commons.collections4.KeyValue;

/**
 * Provides a base decorator that allows additional functionality to be
 * added to a {@link java.util.Map.Entry Map.Entry}.
 *
 * @param <K> the type of keys
 * @param <V> the type of mapped values
 * @since 3.0
 */
public abstract class AbstractMapEntryDecorator<K, V> implements Map.Entry<K, V>, KeyValue<K, V> {

    /** The {@code Map.Entry} to decorate */
    private final Map.Entry<K, V> entry;

    /**
     * Constructor that wraps (not copies).
     *
     * @param entry  the {@code Map.Entry} to decorate, must not be null
     * @throws NullPointerException if the collection is null
     */
    public AbstractMapEntryDecorator(final Map.Entry<K, V> entry) {
        this.entry = Objects.requireNonNull(entry, "entry");
    }

    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        return entry.equals(object);
    }

    @Override
    public K getKey() {
        return entry.getKey();
    }

    /**
     * Gets the map being decorated.
     *
     * @return the decorated map
     */
    protected Map.Entry<K, V> getMapEntry() {
        return entry;
    }

    @Override
    public V getValue() {
        return entry.getValue();
    }

    @Override
    public int hashCode() {
        return entry.hashCode();
    }

    @Override
    public V setValue(final V value) {
        return entry.setValue(value);
    }

    @Override
    public String toString() {
        return entry.toString();
    }

}
