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
package io.atomix.storage.journal;

import io.netty.buffer.ByteBuf;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Support for serialization of {@link ByteBufJournal} entries.
 */
@NonNullByDefault
public interface ByteBufMapper<T> {
    /**
     * Converts an object into a series of bytes in a {@link ByteBuf}.
     *
     * @param obj the object
     * @return resulting buffer
     */
    ByteBuf objectToBytes(T obj) ;

    /**
     * Converts the contents of a {@link ByteBuf} to an object.
     *
     * @param buf buffer to convert
     * @return resulting object
     */
    T bytesToObject(ByteBuf buf);
}
