/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.atomix.storage.journal;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Encapsulation of a {@link JournalSegment}'s state;
 */
@NonNullByDefault
sealed interface SegmentState {

    record Active(FileAccess access, JournalSegmentWriter writer) implements SegmentState {
        public Active {
            requireNonNull(access);
            requireNonNull(writer);
        }

        Inactive close() {
            final var ret = writer.toInactive();
            access.close();
            return ret;
        }
    }

    record Inactive(int position) implements SegmentState {
        // Nothing else
    }
}
