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
 * Serialization proxy for {@link ClientIdentifier}.
 */
final class CI implements ClientIdentifier.SerialForm {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private ClientIdentifier identifier;

    @SuppressWarnings("checkstyle:RedundantModifier")
    public CI() {
        // for Externalizable
    }

    CI(final ClientIdentifier identifier) {
        this.identifier = requireNonNull(identifier);
    }

    @Override
    public ClientIdentifier identifier() {
        return verifyNotNull(identifier);
    }

    @Override
    public void setIdentifier(final ClientIdentifier identifier) {
        this.identifier = requireNonNull(identifier);
    }

    @Override
    public Object readResolve() {
        return identifier();
    }
}
