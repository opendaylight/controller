/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.atomix.storage.journal;

import java.nio.ByteBuffer;

/**
 * An {@link Indexed} entry read from {@link JournalSegment}.
 */
record SegmentEntry(int checksum, ByteBuffer bytes) {
    /**
     * The size of the header, comprising of:
     * <ul>
     *   <li>32-bit signed entry length</li>
     *   <li>32-bit unsigned CRC32 checksum</li>
     * </li>
     */
    static final int HEADER_BYTES = Integer.BYTES + Integer.BYTES;

    SegmentEntry {
        if (bytes.remaining() < 1) {
            throw new IllegalArgumentException("Invalid entry bytes " + bytes);
        }
    }
}
