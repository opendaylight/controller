/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.journal;

import java.nio.ByteBuffer;
import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
sealed interface MappedFile permits NettyMappedFile, ArenaMappedFile {
    /**
     * {@return the mapped {@link ByteBuffer}}
     */
    ByteBuffer buffer();

    /**
     * Synchronize the mapping.
     *
     * @throws UncheckedIOExcpetion if an I/O error occurs
     */
    void sync();

    /**
     * Unmap the this object.
     */
    void unmap();
}
