/*
 * Copyright (c) 2023 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.atomix.utils.serializer;

import io.atomix.storage.journal.JournalSerdes;

import java.util.ArrayList;
import java.util.List;

public class JournalSerdesImplBuilder implements JournalSerdes.Builder {
    private final List<RegisteredType> types = new ArrayList<>();
    private byte id = JournalSerdesImpl.INITIAL_ID;

    @Override
    public JournalSerdes build() {
        return new JournalSerdesImpl(types);
    }

    @Override
    public JournalSerdes build(String friendlyName) {
        return new JournalSerdesImpl(types);
    }

    @Override
    public JournalSerdes.Builder register(JournalSerdes.EntrySerdes<?> serdes, Class<?>... classes) {
        types.add(new RegisteredType(serdes, classes, id));
        id++;
        return this;
    }

    @Override
    public JournalSerdes.Builder setClassLoader(ClassLoader classLoader) {
        //TODO: implement if actually needed
        return null;
    }
}
