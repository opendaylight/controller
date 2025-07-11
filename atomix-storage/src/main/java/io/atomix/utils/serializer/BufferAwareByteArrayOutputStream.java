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

import java.io.ByteArrayOutputStream;

/**
 * Exposes protected byte array length in {@link ByteArrayOutputStream}.
 */
@Deprecated(since = "11.0.0", forRemoval = true)
final class BufferAwareByteArrayOutputStream extends ByteArrayOutputStream {
    BufferAwareByteArrayOutputStream(final int size) {
        super(size);
    }

    int getBufferSize() {
        return buf.length;
    }
}
