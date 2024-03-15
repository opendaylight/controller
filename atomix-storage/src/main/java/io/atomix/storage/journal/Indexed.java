/*
 * Copyright 2017-2022 Open Networking Foundation and others.  All rights reserved.
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

import com.google.common.base.MoreObjects;

/**
 * Indexed journal entry.
 *
 * @param <E> entry type
 * @param index the entry index
 * @param entry the indexed entry
 * @param size the serialized entry size
 */
// FIXME: add @NonNullByDefault and enforce non-null entry once we can say that entries cannot be null
// FIXME: it seems 'index' has to be non-zero, we should enforce that if that really is the case
// FIXME: it seems 'size' has not be non-zero, we should enforce that if that really is the case
public record Indexed<E>(long index, E entry, int size) {
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("index", index).add("entry", entry).toString();
    }
}
