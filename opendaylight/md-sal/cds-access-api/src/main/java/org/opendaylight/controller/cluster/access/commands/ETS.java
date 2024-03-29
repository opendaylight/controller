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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

/**
 * Externalizable proxy for use with {@link ExistsTransactionSuccess}. It implements the Chlorine SR2 serialization
 * format.
 */
final class ETS implements TransactionSuccess.SerialForm<ExistsTransactionSuccess> {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private ExistsTransactionSuccess message;

    @SuppressWarnings("checkstyle:RedundantModifier")
    public ETS() {
        // for Externalizable
    }

    ETS(final ExistsTransactionSuccess message) {
        this.message = requireNonNull(message);
    }

    @Override
    public ExistsTransactionSuccess message() {
        return verifyNotNull(message);
    }

    @Override
    public void setMessage(final ExistsTransactionSuccess message) {
        this.message = requireNonNull(message);
    }

    @Override
    public void writeExternal(final ObjectOutput out, final ExistsTransactionSuccess msg) throws IOException {
        out.writeBoolean(msg.getExists());
    }

    @Override
    public ExistsTransactionSuccess readExternal(final ObjectInput in, final TransactionIdentifier target,
            final long sequence) throws IOException {
        return new ExistsTransactionSuccess(target, sequence, in.readBoolean());
    }

    @Override
    public Object readResolve() {
        return message();
    }
}
