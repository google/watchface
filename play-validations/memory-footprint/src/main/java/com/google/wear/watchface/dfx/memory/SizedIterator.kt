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
package com.google.wear.watchface.dfx.memory

/**
 * A lazy iterator, allowing to generate all combinations of a set without getting into stack
 * overflows.
 */
internal interface SizedIterator<T> : Iterator<T> {
    /** The size of the collection of elements that is generated */
    fun getSize(): Long

    companion object {
        /** Constructs a sized iterator from a plain java iterator and a size. */
        fun <T> fromIterator(iterator: Iterator<T>, size: Long): SizedIterator<T> {
            return object : SizedIterator<T> {
                override fun getSize(): Long {
                    return size
                }

                override fun hasNext(): Boolean {
                    return iterator.hasNext()
                }

                override fun next(): T {
                    return iterator.next()
                }
            }
        }

        /**
         * Lazily combines an existing iterator with a new collection of elements by taking every
         * element of the new collection and combining it with every element of the existing
         * iterator. The resulting iterator will have a size of iterator.size() * elements.size().
         *
         * @param iterator the existing iterator
         * @param elements the collection providing the new elements that are combined with the
         *   already generated ones.
         * @param combineFn the function used to combine each element of the elements argument with
         *   each element of the existing iterator.
         * @param mapFn the function used to map a single element of the elements collection to
         *   elements of the iterator, used when the existing iterator is empty and combineFn cannot
         *   be called.
         * @return a new iterator with the combined elements.
         * @param <T> the elements of the iterator
         * @param <U> the elements of the collection
         */
        fun <T, U> combine(
            iterator: SizedIterator<T>,
            elements: Collection<U>,
            combineFn: (T, U) -> T,
            mapFn: (U) -> T
        ): SizedIterator<T> {
            // if the current key does not have any configuration values, then ignore it
            // and return the next iterator.
            if (elements.isEmpty()) {
                return iterator
            }

            // if the rest iterator has no configuration values, then start a new iterator
            // from the current keys.
            if (!iterator.hasNext()) {
                return fromIterator(elements.stream().map(mapFn).iterator(), elements.size.toLong())
            }

            // lazily consume each partial config from the rest iterator and append each value for
            // the
            // current key, producing new partial configs.
            return object : SizedIterator<T> {
                private val _size = elements.size * iterator.getSize()
                private var crtValuesIter = elements.iterator()
                private var currentFromTail = iterator.next()
                override fun getSize(): Long {
                    return _size
                }

                override fun hasNext(): Boolean {
                    return crtValuesIter.hasNext() || iterator.hasNext()
                }

                override fun next(): T {
                    if (!crtValuesIter.hasNext()) {
                        crtValuesIter = elements.iterator()
                        currentFromTail = iterator.next()
                    }
                    return combineFn(currentFromTail, crtValuesIter.next())
                }
            }
        }
    }
}
