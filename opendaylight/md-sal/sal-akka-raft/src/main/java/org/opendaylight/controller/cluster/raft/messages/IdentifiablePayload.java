/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages;

import static com.google.common.base.Verify.verifyNotNull;

import java.io.Externalizable;
import org.opendaylight.controller.cluster.raft.persisted.MigratedSerializable;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.concepts.Identifier;

public abstract class IdentifiablePayload<T extends Identifier> extends Payload
        implements Identifiable<T>, MigratedSerializable {
    private static final long serialVersionUID = 1L;

    private final boolean legacy;

    protected IdentifiablePayload(final boolean legacy) {
        this.legacy = legacy;
    }

    @Override
    public final boolean isMigrated() {
        return legacy;
    }

    @Override
    // FIXME: remove once legacy is gone
    public final int serializedSize() {
        return legacy ? legacySize() : newSize();
    }

    protected abstract int legacySize();

    // FIXME: rename implementations to serializedSize() once legacy is gone
    protected abstract int newSize();

    @Override
    public final Object writeReplace() {
        return verifyNotNull(legacy ? legacyProxy() : proxy());
    }

    protected abstract Externalizable legacyProxy();

    protected abstract Externalizable proxy();
}
