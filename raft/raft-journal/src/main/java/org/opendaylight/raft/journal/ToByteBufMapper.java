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
package org.opendaylight.raft.journal;

import io.netty.buffer.ByteBuf;
import java.io.IOException;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Interface for transforming internal represetation to bytes.
 *
 * @param <T> Internal representation type
 */
@NonNullByDefault
@FunctionalInterface
public interface ToByteBufMapper<T> {
    /**
     * Converts an object into a series of bytes in the specified {@link ByteBuf}.
     *
     * @param obj the object
     * @param buf target buffer
     * @return {@code true} if the writeout completed, {@code false} if the buffer has insufficient capacity
     * @throws IOException if an I/O error occurs
     */
    boolean objectToBytes(T obj, ByteBuf buf) throws IOException;
}
