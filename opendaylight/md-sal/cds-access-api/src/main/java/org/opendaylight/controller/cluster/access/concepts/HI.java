/*
 * Copyright (c) 2022 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.yangtools.concepts.WritableObjects;

/**
 * Serialization proxy for {@link LocalHistoryIdentifier}.
 *
 * @implNote
 *     cookie is currently required only for module-based sharding, which is implemented as part of normal
 *     DataBroker interfaces. For DOMDataTreeProducer cookie will always be zero, hence we may end up not needing
 *     cookie at all.
 *     We use WritableObjects.writeLongs() to output historyId and cookie (in that order). If we end up not needing
 *     the cookie at all, we can switch to writeLong() and use zero flags for compatibility.
 */
final class HI implements Externalizable {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private LocalHistoryIdentifier identifier;

    @SuppressWarnings("checkstyle:RedundantModifier")
    public HI() {
        // for Externalizable
    }

    HI(final LocalHistoryIdentifier identifier) {
        this.identifier = requireNonNull(identifier);
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        identifier.getClientId().writeTo(out);
        WritableObjects.writeLongs(out, identifier.getHistoryId(), identifier.getCookie());
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException {
        final var clientId = ClientIdentifier.readFrom(in);
        final byte header = WritableObjects.readLongHeader(in);
        final var historyId = WritableObjects.readFirstLong(in, header);
        final var cookie = WritableObjects.readSecondLong(in, header);
        identifier = new LocalHistoryIdentifier(clientId, historyId, cookie);
    }

    @java.io.Serial
    private Object readResolve() {
        return verifyNotNull(identifier);
    }
}
