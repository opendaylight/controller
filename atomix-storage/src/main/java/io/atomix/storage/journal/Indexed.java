/*
 * Copyright 2017-2022 Open Networking Foundation and others.  All rights reserved.
 * Copyright (c) 2024 PANTHEON.tech, s.r.o.
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

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import io.atomix.storage.journal.index.Position;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Indexed journal entry.
 *
 * @param <E> entry type
 * @param index the entry index
 * @param entry the indexed entry
 * @param size the serialized entry size
 */
// FIXME: it seems 'index' has to be non-zero, we should enforce that if that really is the case
// FIXME: it seems 'size' has not be non-zero, we should enforce that if that really is the case
@NonNullByDefault
public record Indexed<E>(long index, E entry, int size) {
    public Indexed {
        requireNonNull(entry);
    }

    Indexed(final Position position, final E entry, final int size) {
        this(position.index(), entry, size);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("index", index).add("entry", entry).toString();
    }
}
