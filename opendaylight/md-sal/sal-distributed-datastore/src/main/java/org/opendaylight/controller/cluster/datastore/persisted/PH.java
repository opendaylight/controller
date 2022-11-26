/*
 * Copyright (c) 2022 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import java.io.DataInput;
import java.io.IOException;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.datastore.persisted.AbstractIdentifiablePayload.AbstractProxy;

/**
 * Serialization proxy for {@link PurgeLocalHistoryPayload}.
 */
final class PH extends AbstractProxy<LocalHistoryIdentifier> {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("checkstyle:RedundantModifier")
    public PH() {
        // For Externalizable
    }

    PH(final byte[] serialized) {
        super(serialized);
    }

    @Override
    protected LocalHistoryIdentifier readIdentifier(final DataInput in) throws IOException {
        return LocalHistoryIdentifier.readFrom(in);
    }

    @Override
    protected PurgeLocalHistoryPayload createObject(final LocalHistoryIdentifier identifier, final byte[] serialized) {
        return new PurgeLocalHistoryPayload(identifier, serialized);
    }
}
