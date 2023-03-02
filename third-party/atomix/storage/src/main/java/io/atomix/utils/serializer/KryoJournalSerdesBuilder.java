/*
 * Copyright 2014-2021 Open Networking Foundation
 * Copyright 2023 PANTHEON.tech, s.r.o.
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

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import io.atomix.storage.journal.JournalSerdes;
import io.atomix.storage.journal.JournalSerdes.Builder;
import io.atomix.storage.journal.JournalSerdes.EntrySerdes;
import java.util.ArrayList;
import java.util.List;

public final class KryoJournalSerdesBuilder implements Builder {
    private final List<RegisteredType> types = new ArrayList<>();
    private ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    @Override
    public KryoJournalSerdesBuilder register(final EntrySerdes<?> serdes, final Class<?>... classes) {
        types.add(new RegisteredType(new EntrySerializer<>(serdes), classes));
        return this;
    }

    @Override
    public KryoJournalSerdesBuilder setClassLoader(final ClassLoader classLoader) {
        this.classLoader = requireNonNull(classLoader);
        return this;
    }

    @Override
    public JournalSerdes build() {
        return build(KryoJournalSerdes.NO_NAME);
    }

    @Override
    public JournalSerdes build(final String friendlyName) {
        checkState(!types.isEmpty(), "No serializers registered");
        return new KryoJournalSerdes(types, classLoader, friendlyName);
    }
}