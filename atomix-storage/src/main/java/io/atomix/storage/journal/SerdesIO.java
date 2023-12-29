/*
 * Copyright (c) 2023 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.atomix.storage.journal;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import org.eclipse.jdt.annotation.NonNull;

/**
 * Run-time binding ot a single {@link SerdesSupport} to a type ID.
 *
 * @param <T> Object type
 */
record SerdesIO<T>(int typeId, @NonNull SerdesTypeWidth width, @NonNull SerdesSupport<T> support) {
    SerdesIO {
        requireNonNull(support);
        width.checkRange(typeId);
    }

    @NonNull T readObject(final @NonNull SerdesDataInput input) throws IOException {
        final var type = width.readType(input);
        if (type != typeId) {
            throw new IOException("Expecting type " + typeId + ", got " + type);
        }
        return readRawObject(input);
    }

    @NonNull T readRawObject(final @NonNull SerdesDataInput input) throws IOException {
        final var read = support.readObject(input);
        if (read == null) {
            throw new IOException(support + " produced null object");
        }
        return read;
    }

    void writeObject(final @NonNull SerdesDataOutput output, final Object obj) throws IOException {
        final T cast;
        try {
            cast = support.typeClass().cast(requireNonNull(obj));
        } catch (ClassCastException e) {
            throw new IOException("Incompatible object for " + support, e);
        }
        width.writeType(output, typeId);
        support.writeObject(cast, output);
    }
}
