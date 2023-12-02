/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.wear.watchface.dfx.memory;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A lazy iterator, allowing to generate all combinations of a set without getting into stack
 * overflows.
 */
interface SizedIterator<T> extends Iterator<T> {
    /** The size of the collection of elements that is generated */
    long getSize();

    /** Constructs a sized iterator from a plain java iterator and a size. */
    static <T> SizedIterator<T> fromIterator(Iterator<T> iterator, long size) {
        return new SizedIterator<T>() {
            @Override
            public long getSize() {
                return size;
            }

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public T next() {
                return iterator.next();
            }
        };
    }

    /**
     * Lazily combines an existing iterator with a new collection of elements by taking every
     * element of the new collection and combining it with every element of the existing iterator.
     * The resulting iterator will have a size of iterator.size() * elements.size().
     *
     * @param iterator the existing iterator
     * @param elements the collection providing the new elements that are combined with the already
     *     generated ones.
     * @param combineFn the function used to combine each element of the elements argument with each
     *     element of the existing iterator.
     * @param mapFn the function used to map a single element of the elements collection to elements
     *     of the iterator, used when the existing iterator is empty and combineFn cannot be called.
     * @return a new iterator with the combined elements.
     * @param <T> the elements of the iterator
     * @param <U> the elements of the collection
     */
    static <T, U> SizedIterator<T> combine(
            SizedIterator<T> iterator,
            Collection<U> elements,
            BiFunction<T, U, T> combineFn,
            Function<U, T> mapFn) {
        // if the current key does not have any configuration values, then ignore it
        // and return the next iterator.
        if (elements.size() == 0) {
            return iterator;
        }

        // if the rest iterator has no configuration values, then start a new iterator
        // from the current keys.
        if (!iterator.hasNext()) {
            return fromIterator(elements.stream().map(mapFn).iterator(), elements.size());
        }

        // lazily consume each partial config from the rest iterator and append each value for the
        // current key, producing new partial configs.
        return new SizedIterator<T>() {
            private final long size = elements.size() * iterator.getSize();
            private Iterator<U> crtValuesIter = elements.iterator();
            private T currentFromTail = iterator.next();

            @Override
            public long getSize() {
                return size;
            }

            @Override
            public boolean hasNext() {
                return crtValuesIter.hasNext() || iterator.hasNext();
            }

            @Override
            public T next() {
                if (!crtValuesIter.hasNext()) {
                    crtValuesIter = elements.iterator();
                    currentFromTail = iterator.next();
                }
                return combineFn.apply(currentFromTail, crtValuesIter.next());
            }
        };
    }
}
