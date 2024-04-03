/*
 * Copyright (c) 2024 PANTHEON.tech s.r.o. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.atomix.storage.journal;

/**
 * Support for serialization of {@link Journal} entries.
 */
public interface JournalSerializer<T> {

    /**
     * Serializes given object to byte array.
     *
     * @param obj Object to serialize
     *
     */
    byte[] serialize(T obj) ;

    /**
     * Deserializes given byte array to Object.
     *
     * @param bytes serialized bytes
     * @return deserialized Object
     */
    T deserialize(final byte[] bytes);

    static <E> JournalSerializer<E> wrap(final JournalSerdes serdes) {
        return new JournalSerializer<>() {
            @Override
            public byte[] serialize(final E obj) {
                return serdes.serialize(obj);
            }

            @Override
            public E deserialize(final byte[] bytes) {
                return serdes.deserialize(bytes) ;
            }
        };
    }
}
