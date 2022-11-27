/*
 * Copyright (c) 2022 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

/**
 * Externalizable proxy for use with {@link LocalHistorySuccess}. It implements the Chlorine SR2 serialization format.
 */
final class HS implements LocalHistorySuccess.SerialForm {
    private static final long serialVersionUID = 1L;

    private LocalHistorySuccess message;

    @SuppressWarnings("checkstyle:RedundantModifier")
    public HS() {
        // for Externalizable
    }

    HS(final LocalHistorySuccess message) {
        this.message = requireNonNull(message);
    }

    @Override
    public LocalHistorySuccess message() {
        return verifyNotNull(message);
    }

    @Override
    public void setMessage(final LocalHistorySuccess message) {
        this.message = requireNonNull(message);
    }

    @Override
    public Object readResolve() {
        return message();
    }
}
