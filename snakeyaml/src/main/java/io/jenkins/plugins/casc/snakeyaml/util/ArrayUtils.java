/**
 * Copyright (c) 2008, http://www.snakeyaml.org
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
package io.jenkins.plugins.casc.snakeyaml.util;

import java.util.AbstractList;
import java.util.Collections;
import java.util.List;

public class ArrayUtils {

    private ArrayUtils() {
    }

    /**
     * Returns an unmodifiable {@code List} backed by the given array. The method doesn't copy the array, so the changes
     * to the array will affect the {@code List} as well.
     * @param <E> class of the elements in the array
     * @param elements - array to convert
     * @return {@code List} backed by the given array
     */
    public static <E> List<E> toUnmodifiableList(E[] elements) {
        return elements.length == 0 ? Collections.<E> emptyList()
                : new UnmodifiableArrayList<E>(elements);
    }

    /**
     * Returns an unmodifiable {@code List} containing the second array appended to the first one. The method doesn't copy
     * the arrays, so the changes to the arrays will affect the {@code List} as well.
     * @param <E> class of the elements in the array
     * @param array1 - the array to extend
     * @param array2 - the array to add to the first
     * @return {@code List} backed by the given arrays
     */
    public static <E> List<E> toUnmodifiableCompositeList(E[] array1, E[] array2) {
        List<E> result;
        if (array1.length == 0) {
            result = toUnmodifiableList(array2);
        } else if (array2.length == 0) {
            result = toUnmodifiableList(array1);
        } else {
            result = new CompositeUnmodifiableArrayList<E>(array1, array2);
        }
        return result;
    }

    private static class UnmodifiableArrayList<E> extends AbstractList<E> {

        private final E[] array;

        UnmodifiableArrayList(E[] array) {
            this.array = array;
        }

        @Override
        public E get(int index) {
            if (index >= array.length) {
                throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size());
            }
            return array[index];
        }

        @Override
        public int size() {
            return array.length;
        }
    }

    private static class CompositeUnmodifiableArrayList<E> extends AbstractList<E> {

        private final E[] array1;
        private final E[] array2;

        CompositeUnmodifiableArrayList(E[] array1, E[] array2) {
            this.array1 = array1;
            this.array2 = array2;
        }

        @Override
        public E get(int index) {
            E element;
            if (index < array1.length) {
                element = array1[index];
            } else if (index - array1.length < array2.length) {
                element = array2[index - array1.length];
            } else {
                throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size());
            }
            return element;
        }

        @Override
        public int size() {
            return array1.length + array2.length;
        }
    }
}
