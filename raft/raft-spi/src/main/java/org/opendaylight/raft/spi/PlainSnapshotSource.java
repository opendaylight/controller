/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.spi;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects.ToStringHelper;
import java.io.IOException;
import java.io.InputStream;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * A {@link SnapshotSource} corresponding directly to the serialization format of a snapshot.
 */
@NonNullByDefault
public final class PlainSnapshotSource extends SnapshotSource {
    private final DataSource provider;

    /**
     * Default constructor.
     *
     * @param provider the {@link DataSource}
     */

    public PlainSnapshotSource(final DataSource provider) {
        this.provider = requireNonNull(provider);
    }

    @Override
    public PlainSnapshotSource toPlainSource() {
        return this;
    }

    @Override
    public InputStream openStream() throws IOException {
        return provider.openStream();
    }

    @Override
    ToStringHelper addToStringAttributes(final ToStringHelper helper) {
        return helper.add("provider", provider);
    }
}