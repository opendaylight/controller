/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
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

import static java.util.Objects.requireNonNull;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Workaround for https://github.com/EsotericSoftware/kryo/issues/505. We cannot use ByteBufferInput because its
 * readAscii() method performs a ByteBuffer.put().
 */
final class ByteBufferInputStream extends InputStream {
    private final ByteBuffer buffer;

    ByteBufferInputStream(final ByteBuffer buffer) {
        this.buffer = requireNonNull(buffer);
    }

    @Override
    public int read() {
        return buffer.hasRemaining() ? buffer.get() & 0xFF : -1;
    }

    @Override
    public int read(final byte b[], final int off, final int len) {
        Objects.checkFromIndexSize(off, len, b.length);
        if (len == 0) {
            return 0;
        }
        final int readLen = Math.min(len, buffer.remaining());
        if (readLen == 0) {
            return -1;
        }
        buffer.get(b, off, readLen);
        return readLen;
    }
}
