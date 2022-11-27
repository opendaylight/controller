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
 * Externalizable proxy for use with {@link SkipTransactionsRequest}. It implements the Chlorine SR2 serialization
 * format.
 */
final class STR implements SkipTransactionsRequest.SerialForm {
    private static final long serialVersionUID = 1L;

    private SkipTransactionsRequest message;

    @SuppressWarnings("checkstyle:RedundantModifier")
    public STR() {
        // for Externalizable
    }

    STR(final SkipTransactionsRequest message) {
        this.message = requireNonNull(message);
    }

    @Override
    public SkipTransactionsRequest message() {
        return verifyNotNull(message);
    }

    @Override
    public void setMessage(final SkipTransactionsRequest message) {
        this.message = requireNonNull(message);
    }

    @Override
    public Object readResolve() {
        return message();
    }
}
