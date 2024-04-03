/*
 * Copyright (c) 2024 PANTHEON.tech s.r.o. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.atomix.storage.journal;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

/**
 * Support for serialization of {@link Journal} entries.
 */
public interface JournalSerializer<T> {

    /**
     * Serializes given object to byte array.
     *
     * @param obj Object to serialize
     * @return serialized bytes as {@link ByteBuf}
     */
    ByteBuf serialize(T obj) ;

    /**
     * Deserializes given byte array to Object.
     *
     * @param buf serialized bytes as {@link ByteBuf}
     * @return deserialized Object
     */
    T deserialize(final ByteBuf buf);

    static <E> JournalSerializer<E> wrap(final JournalSerdes serdes) {
        return new JournalSerializer<>() {
            @Override
            public ByteBuf serialize(final E obj) {
                return Unpooled.wrappedBuffer(serdes.serialize(obj));
            }

            @Override
            public E deserialize(final ByteBuf buf) {
                return serdes.deserialize(ByteBufUtil.getBytes(buf));
            }
        };
    }
}
