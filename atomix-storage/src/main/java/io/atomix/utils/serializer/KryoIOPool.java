/*
 * Copyright 2014-2022 Open Networking Foundation and others.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.atomix.utils.serializer;

import java.lang.ref.SoftReference;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;

@Deprecated(since = "11.0.0", forRemoval = true)
abstract class KryoIOPool<T> {
    private final ConcurrentLinkedQueue<SoftReference<T>> queue = new ConcurrentLinkedQueue<>();

    private T borrow(final int bufferSize) {
        T element;
        SoftReference<T> reference;
        while ((reference = queue.poll()) != null) {
            if ((element = reference.get()) != null) {
                return element;
            }
        }
        return create(bufferSize);
    }

    abstract T create(int bufferSize);

    abstract boolean recycle(T element);

    <R> R run(final Function<T, R> function, final int bufferSize) {
        final T element = borrow(bufferSize);
        try {
            return function.apply(element);
        } finally {
            if (recycle(element)) {
                queue.offer(new SoftReference<>(element));
            }
        }
    }
}
