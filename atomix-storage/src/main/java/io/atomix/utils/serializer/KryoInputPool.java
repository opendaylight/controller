/*
 * Copyright 2014-2022 Open Networking Foundation and others.  All rights reserved.
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
package io.atomix.utils.serializer;

import com.esotericsoftware.kryo.io.Input;

@Deprecated(since = "11.0.0", forRemoval = true)
final class KryoInputPool extends KryoIOPool<Input> {
    static final int MAX_POOLED_BUFFER_SIZE = 512 * 1024;

    @Override
    Input create(final int bufferSize) {
        return new Input(bufferSize);
    }

    @Override
    boolean recycle(final Input input) {
        if (input.getBuffer().length >= MAX_POOLED_BUFFER_SIZE) {
            // discard
            return false;
        }
        input.setInputStream(null);
        return true;
    }
}
