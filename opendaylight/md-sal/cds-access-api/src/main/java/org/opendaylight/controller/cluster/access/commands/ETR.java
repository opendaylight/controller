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

final class ETR implements ExistsTransactionRequest.SerialForm {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private ExistsTransactionRequest message;

    @SuppressWarnings("checkstyle:RedundantModifier")
    public ETR() {
        // for Externalizable
    }

    ETR(final ExistsTransactionRequest message) {
        this.message = requireNonNull(message);
    }

    @Override
    public ExistsTransactionRequest message() {
        return verifyNotNull(message);
    }

    @Override
    public void resolveTo(final ExistsTransactionRequest newMessage) {
        message = requireNonNull(newMessage);
    }

    @Override
    public Object readResolve() {
        return verifyNotNull(message);
    }
}
