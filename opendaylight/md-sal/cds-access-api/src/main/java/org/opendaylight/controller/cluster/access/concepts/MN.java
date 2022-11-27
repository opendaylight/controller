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
 * Serialization proxy for {@link MemberName}.
 */
final class MN implements MemberName.SerialForm {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private MemberName name;

    @SuppressWarnings("checkstyle:RedundantModifier")
    public MN() {
        // for Externalizable
    }

    MN(final MemberName name) {
        this.name = requireNonNull(name);
    }

    @Override
    public MemberName name() {
        return verifyNotNull(name);
    }

    @Override
    public void setName(final MemberName name) {
        this.name = requireNonNull(name);
    }

    @Override
    public Object readResolve() {
        return name();
    }
}
