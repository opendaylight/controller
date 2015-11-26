/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.InputStream;
import javax.annotation.Nonnull;

public final class NormalizedNodeStreams {
    private NormalizedNodeStreams() {
        throw new UnsupportedOperationException();
    }

    public static NormalizedNodeStreamReader newStreamReader(@Nonnull final InputStream stream) {
        final DataInput dis;

        if (stream instanceof DataInputStream) {
            dis = (DataInputStream) stream;
        } else {
            dis = new DataInputStream(stream);
        }

        return new NormalizedNodeInputStreamReader(dis);
    }

    public static NormalizedNodeStreamReader newReaderForDictionary(final AbstractStreamDictionary dictionary) {
        // FIXME: implement this
        return null;
    }

}
