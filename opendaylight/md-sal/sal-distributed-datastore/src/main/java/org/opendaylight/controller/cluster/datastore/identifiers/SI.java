/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.identifiers;

import static java.util.Objects.requireNonNull;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Externalizable proxy for {@link ShardIdentifier}.
 */
final class SI implements Externalizable {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private ShardIdentifier id;

    @SuppressWarnings("checkstyle:RedundantModifier")
    public SI() {
        // For Externalizable;
    }

    SI(final ShardIdentifier id) {
        this.id = requireNonNull(id);
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        id.writeTo(out);
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException {
        id = ShardIdentifier.readFrom(in);
    }

    @java.io.Serial
    private Object readResolve() {
        return requireNonNull(id);
    }
}
