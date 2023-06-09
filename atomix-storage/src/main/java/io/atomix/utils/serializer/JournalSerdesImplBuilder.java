/*
 * Copyright (c) 2023 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.atomix.utils.serializer;

import io.atomix.storage.journal.JournalSerdes;

import java.util.HashMap;
import java.util.Map;

public class JournalSerdesImplBuilder implements JournalSerdes.Builder {
    private final Map<Byte, RegisteredType> types = new HashMap<>();
    private byte id = JournalSerdesImpl.INITIAL_ID;

    @Override
    public JournalSerdes build() {
        return new JournalSerdesImpl(types);
    }

    @Override
    public JournalSerdes build(final String friendlyName) {
        return new JournalSerdesImpl(types);
    }

    @Override
    public JournalSerdes.Builder register(final JournalSerdes.EntrySerdes<?> serdes, final Class<?>... classes) {
        types.put(id, new RegisteredType(serdes, classes));
        id++;
        return this;
    }

    @Override
    public JournalSerdes.Builder setClassLoader(final ClassLoader classLoader) {
        //TODO: implement if actually needed
        return this;
    }
}
