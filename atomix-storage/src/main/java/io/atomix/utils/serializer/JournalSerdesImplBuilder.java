package io.atomix.utils.serializer;

import io.atomix.storage.journal.JournalSerdes;

import java.util.ArrayList;
import java.util.List;

public class JournalSerdesImplBuilder implements JournalSerdes.Builder {
    private final List<RegisteredType> types = new ArrayList<>();
    private byte id = JournalSerdesImpl.INITIAL_ID;

    @Override
    public JournalSerdes build() {
        return new JournalSerdesImpl(types, JournalSerdesImpl.NO_NAME);
    }

    @Override
    public JournalSerdes build(String friendlyName) {
        return new JournalSerdesImpl(types, friendlyName);
    }

    @Override
    public JournalSerdes.Builder register(JournalSerdes.EntrySerdes<?> serdes, Class<?>... classes) {
        types.add(new RegisteredType(serdes, classes, id));
        id++;
        return this;
    }

    @Override
    public JournalSerdes.Builder setClassLoader(ClassLoader classLoader) {
        //TODO: couldn't find use for this
        return null;
    }
}
