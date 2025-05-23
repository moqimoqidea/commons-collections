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

/**
 * Abstract Pair class to assist with creating correct
 * {@link java.util.Map.Entry Map.Entry} implementations.
 *
 * @param <K> the type of keys
 * @param <V> the type of mapped values
 * @since 3.0
 */
public abstract class AbstractMapEntry<K, V> extends AbstractKeyValue<K, V> implements Map.Entry<K, V> {

    /**
     * Constructs a new entry with the given key and given value.
     *
     * @param key  the key for the entry, may be null
     * @param value  the value for the entry, may be null
     */
    protected AbstractMapEntry(final K key, final V value) {
        super(key, value);
    }

    /**
     * Compares this {@code Map.Entry} with another {@code Map.Entry}.
     * <p>
     * Implemented per API documentation of {@link java.util.Map.Entry#equals(Object)}
     *
     * @param obj  the object to compare to
     * @return true if equal key and value
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Map.Entry)) {
            return false;
        }
        final Map.Entry<?, ?> other = (Map.Entry<?, ?>) obj;
        return Objects.equals(getKey(), other.getKey()) &&
               Objects.equals(getValue(), other.getValue());
    }

    /**
     * Gets a hashCode compatible with the equals method.
     * <p>
     * Implemented per API documentation of {@link java.util.Map.Entry#hashCode()}
     *
     * @return a suitable hash code
     */
    @Override
    public int hashCode() {
        return (getKey() == null ? 0 : getKey().hashCode()) ^
               (getValue() == null ? 0 : getValue().hashCode());
    }

    /**
     * Sets the value stored in this {@code Map.Entry}.
     * <p>
     * This {@code Map.Entry} is not connected to a Map, so only the
     * local data is changed.
     *
     * @param value  the new value
     * @return the previous value
     */
    @Override
    public V setValue(final V value) { // NOPMD
        return super.setValue(value);
    }

}
