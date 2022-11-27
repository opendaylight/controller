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
 * Serialization proxy for {@link FrontendType}.
 */
final class FT implements FrontendType.SerialForm {
    private static final long serialVersionUID = 1L;

    private FrontendType type;

    @SuppressWarnings("checkstyle:RedundantModifier")
    public FT() {
        // for Externalizable
    }

    FT(final FrontendType type) {
        this.type = requireNonNull(type);
    }

    @Override
    public FrontendType type() {
        return verifyNotNull(type);
    }

    @Override
    public void setType(final FrontendType type) {
        this.type = requireNonNull(type);
    }

    @Override
    public Object readResolve() {
        return type();
    }
}
