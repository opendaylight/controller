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

/**
 * Serialization proxy for {@link LocalHistoryIdentifier}.
 */
final class HI implements LocalHistoryIdentifier.SerialForm {
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
    public LocalHistoryIdentifier identifier() {
        return verifyNotNull(identifier);
    }

    @Override
    public void setIdentifier(final LocalHistoryIdentifier identifier) {
        this.identifier = requireNonNull(identifier);
    }

    @Override
    public Object readResolve() {
        return identifier();
    }
}
