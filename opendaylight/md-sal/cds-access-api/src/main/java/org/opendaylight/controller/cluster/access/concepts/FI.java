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

/**
 * Serialization proxy for {@link FrontendIdentifier}.
 */
final class FI implements Externalizable {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private FrontendIdentifier identifier;

    @SuppressWarnings("checkstyle:RedundantModifier")
    public FI() {
        // for Externalizable
    }

    FI(final FrontendIdentifier identifier) {
        this.identifier = requireNonNull(identifier);
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException {
        identifier = new FrontendIdentifier(MemberName.readFrom(in), FrontendType.readFrom(in));
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        identifier.getMemberName().writeTo(out);
        identifier.getClientType().writeTo(out);
    }

    @java.io.Serial
    private Object readResolve() {
        return verifyNotNull(identifier);
    }
}
