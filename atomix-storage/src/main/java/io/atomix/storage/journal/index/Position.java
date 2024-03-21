/*
 * Copyright 2018-2021 Open Networking Foundation
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
package io.atomix.storage.journal.index;

import java.util.Map.Entry;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Journal index position.
 */
public record Position(long index, int position) {
    public Position(final Entry<Long, Integer> entry) {
        this(entry.getKey(), entry.getValue());
    }

    public static @Nullable Position ofNullable(final Entry<Long, Integer> entry) {
        return entry == null ? null : new Position(entry);
    }
}
