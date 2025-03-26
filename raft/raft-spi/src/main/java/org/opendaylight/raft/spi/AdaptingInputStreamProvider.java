/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.spi;

import java.io.IOException;
import java.io.InputStream;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * An {@link InputStreamProvider} which performs on-the-fly transformation of the stream provided by a delegate.
 *
 * @param delegate the delegate
 * @param adapter the transform
 */
@NonNullByDefault
public record AdaptingInputStreamProvider(InputStreamProvider delegate, OpenStreamAdapter adapter)
        implements InputStreamProvider {
    /**
     * The stream adaptation method, called upon just after the delegate is open.
     */
    @FunctionalInterface
    public interface OpenStreamAdapter {
        /**
         * Returns an adapted {@link InputStream}.
         *
         * @param source the source to adapt
         * @return an adapted {@link InputStream}
         * @throws IOException if an I/O error occurs
         */
        InputStream adaptStream(InputStream source) throws IOException;
    }

    @Override
    public @NonNull InputStream openStream() throws IOException {
        final var source = delegate.openStream();
        try {
            return adapter.adaptStream(source);
        } catch (IOException e) {
            source.close();
            throw e;
        }
    }
}
