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
 * Serialization proxy for {@link RequestEnvelope}.
 */
final class RE implements RequestEnvelope.SerialForm {
    private static final long serialVersionUID = 1L;

    private RequestEnvelope envelope;

    @SuppressWarnings("checkstyle:RedundantModifier")
    public RE() {
        // for Externalizable
    }

    RE(final RequestEnvelope envelope) {
        this.envelope = requireNonNull(envelope);
    }

    @Override
    public RequestEnvelope envelope() {
        return verifyNotNull(envelope);
    }

    @Override
    public void setEnvelope(final RequestEnvelope envelope) {
        this.envelope = requireNonNull(envelope);
    }

    @Override
    public Object readResolve() {
        return envelope();
    }
}
