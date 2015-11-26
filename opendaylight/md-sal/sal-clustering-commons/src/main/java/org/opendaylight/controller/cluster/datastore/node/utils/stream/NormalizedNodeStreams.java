/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import java.io.DataInput;
import java.io.DataOutput;
import javax.annotation.Nonnull;

public final class NormalizedNodeStreams {
    private NormalizedNodeStreams() {
        throw new UnsupportedOperationException();
    }

    public static NormalizedNodeInputStreamReader newStreamReader(@Nonnull final DataInput stream) {
        return new NormalizedNodeInputStreamReader(stream);
    }

    public static NormalizedNodeInputStreamReader newReaderForDictionary(@Nonnull final DataInput stream,
            @Nonnull final StreamReaderDictionary dictionary) {
        return new NormalizedNodeInputStreamReader(stream, dictionary);
    }

    public static NormalizedNodeOutputStreamWriter newStreamWriter(@Nonnull final DataOutput stream) {
        return new NormalizedNodeOutputStreamWriter(stream);
    }

    public static NormalizedNodeOutputStreamWriter newWriterForDictionary(@Nonnull final DataOutput stream,
            @Nonnull final StreamWriterDictionary dictionary) {
        return new NormalizedNodeOutputStreamWriter(stream, dictionary);
    }
}
