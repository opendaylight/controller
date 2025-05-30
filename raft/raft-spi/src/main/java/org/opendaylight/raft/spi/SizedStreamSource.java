/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.spi;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * An {@link StreamSource} which knows its size. Essentially the moral equivalent of Guava's {@code ByteSource}.
 */
@NonNullByDefault
public non-sealed interface SizedStreamSource extends StreamSource {
    /**
     * Returns the size of this data source, which is to say the number of bytes available for reading from the stream
     * returned by {@link #openStream()}.
     *
     * @return the size of this data source
     */
    long size();

    @Override
    default SizedStreamSource toSizedStreamSource() {
        return this;
    }
}
