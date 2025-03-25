/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.spi;

import com.google.common.io.ByteSource;
import java.io.IOException;
import java.io.InputStream;
import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public final class ByteSourceLz4SnapshotSource extends ByteSourceSnapshotSource implements PlainSnapshotSource {
    ByteSourceLz4SnapshotSource(final ByteSource source) {
        super(source);
    }

    @Override
    public InputStream openStream() throws IOException {
        return byteSource().openStream();
    }
}
