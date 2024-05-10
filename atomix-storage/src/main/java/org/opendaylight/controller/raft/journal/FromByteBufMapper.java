/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.raft.journal;

import io.netty.buffer.ByteBuf;
import java.io.IOException;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Interface for transforming bytes into their internal representation.
 *
 * @param <T> Internal representation type
 */
@FunctionalInterface
@NonNullByDefault
public interface FromByteBufMapper<T> {
    /**
     * Converts the contents of a {@link ByteBuf} to an object.
     *
     * @param index entry index
     * @param bytes entry bytes
     * @return resulting internal representation object
     * @throws IOException if an I/O error occurs
     */
    T bytesToObject(long index, ByteBuf bytes) throws IOException;
}