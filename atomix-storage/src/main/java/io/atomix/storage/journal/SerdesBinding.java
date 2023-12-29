/*
 * Copyright (c) 2023 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.atomix.storage.journal;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * A run-time binding of a {@link SerdesManifest} to a set of {@link SerdesSupport}s.
 */
// FIXME: better name
final class SerdesBinding {
    private final ImmutableMap<Class<?>, SerdesIO<?>> classToIO;
    private final SerdesIO<?>[] idToIO;
    private final @NonNull SerdesTypeWidth typeWidth;

    // FIXME: this deals with manifest + runtime -> mapping path
    // FIXME: we also need the initial state, i.e. where manifest is derived from available serdeses
    SerdesBinding(final SerdesManifest manifest, final Collection<SerdesSupport<?>> serdeses) {
        // FIXME: optimize width
        final var maxId = Integer.MAX_VALUE;
        typeWidth = SerdesTypeWidth.forMaximum(maxId);
        idToIO = new SerdesIO<?>[maxId + 1];

        final var symbolicNameToTypeId = manifest.objIdToSymbolicName().inverse();
        final var serdesBySymbolicName = new HashMap<String, SerdesSupport<?>>();
        final var builder = ImmutableMap.<Class<?>, SerdesIO<?>>builder();
        for (var serdes : serdeses) {
            final var symbolicName = serdes.symbolicName();
            final var typeId = symbolicNameToTypeId.get(symbolicName);
            if (typeId == null) {
                // Not present in manifest, ignore
                continue;
            }

            final var existing = serdesBySymbolicName.putIfAbsent(symbolicName, serdes);
            if (existing != null) {
                if (existing != serdes) {
                    throw new IllegalArgumentException(
                        "Symbolic name clash on " + symbolicName + " between " + existing + " and " + serdes);
                }
                // Same instance, silently skip over
                continue;
            }

            final int id = typeId;
            final var io = new SerdesIO<>(id, typeWidth, serdes);
            idToIO[id] = io;
            builder.put(serdes.typeClass(), io);
        }

        for (int i = 0; i < idToIO.length; ++i) {
            if (idToIO[i] == null) {
                // FIXME: lookup name?
                throw new IllegalArgumentException("No binding for type " + i + " name "
                    + manifest.objIdToSymbolicName().get(i));
            }
        }

        classToIO = builder.buildOrThrow();
    }

    @Nullable SerdesIO<?> ioFor(final Class<?> typeClass) {
        return classToIO.get(requireNonNull(typeClass));
    }

    @NonNull Object readObject(final @NonNull SerdesDataInput input) throws IOException {
        final int typeId = typeWidth.readType(input);
        final SerdesIO<?> io;
        try {
            io = idToIO[typeId];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IOException("Unknown type" + typeId);
        }
        return io.readRawObject(input);
    }
}
