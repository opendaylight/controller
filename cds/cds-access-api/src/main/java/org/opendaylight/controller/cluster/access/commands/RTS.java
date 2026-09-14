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
import java.util.Optional;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeDataInput;

/**
 * Externalizable proxy for use with {@link ReadTransactionSuccess}. It implements the Chlorine SR2 serialization
 * format.
 */
final class RTS implements TransactionSuccess.SerialForm<ReadTransactionSuccess> {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private ReadTransactionSuccess message;

    @SuppressWarnings("checkstyle:RedundantModifier")
    public RTS() {
        // for Externalizable
    }

    RTS(final ReadTransactionSuccess message) {
        this.message = requireNonNull(message);
    }

    @Override
    public ReadTransactionSuccess message() {
        return verifyNotNull(message);
    }

    @Override
    public void setMessage(final ReadTransactionSuccess message) {
        this.message = requireNonNull(message);
    }

    @Override
    public ReadTransactionSuccess readExternal(final ObjectInput in, final TransactionIdentifier target,
            final long sequence) throws IOException {
        final Optional<NormalizedNode> data;
        if (in.readBoolean()) {
            data = Optional.of(NormalizedNodeDataInput.newDataInput(in).readNormalizedNode());
        } else {
            data = Optional.empty();
        }
        return new ReadTransactionSuccess(target, sequence, data);
    }

    @Override
    public void writeExternal(final ObjectOutput out, final ReadTransactionSuccess msg) throws IOException {
        TransactionSuccess.SerialForm.super.writeExternal(out, msg);

        final var data = msg.getData();
        if (data.isPresent()) {
            out.writeBoolean(true);
            try (var nnout = msg.getVersion().getStreamVersion().newDataOutput(out)) {
                nnout.writeNormalizedNode(data.orElseThrow());
            }
        } else {
            out.writeBoolean(false);
        }
    }

    @Override
    public Object readResolve() {
        return message();
    }
}
