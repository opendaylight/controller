/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import static java.util.Objects.requireNonNull;

import akka.actor.ActorRef;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeDataInput;
import org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeDataOutput;
import org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeStreamVersion;
import org.opendaylight.yangtools.yang.data.impl.schema.ReusableImmutableNormalizedNodeStreamWriter;

/**
 * Externalizable proxy for use with {@link ExistsTransactionRequest}. It implements the initial (Boron) serialization
 * format.
 *
 * @author Robert Varga
 */
final class ModifyTransactionRequestProxyV1 extends AbstractTransactionRequestProxy<ModifyTransactionRequest> {
    private static final long serialVersionUID = 1L;

    private List<TransactionModification> modifications;
    private Optional<PersistenceProtocol> protocol;
    private transient NormalizedNodeStreamVersion streamVersion;

    // checkstyle flags the public modifier as redundant however it is explicitly needed for Java serialization to
    // be able to create instances via reflection.
    @SuppressWarnings("checkstyle:RedundantModifier")
    public ModifyTransactionRequestProxyV1() {
        // For Externalizable
    }

    ModifyTransactionRequestProxyV1(final ModifyTransactionRequest request) {
        super(request);
        this.modifications = requireNonNull(request.getModifications());
        this.protocol = request.getPersistenceProtocol();
        this.streamVersion = request.getVersion().getStreamVersion();
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);

        protocol = Optional.ofNullable(PersistenceProtocol.readFrom(in));

        final int size = in.readInt();
        if (size != 0) {
            modifications = new ArrayList<>(size);
            final NormalizedNodeDataInput nnin = NormalizedNodeDataInput.newDataInput(in);
            final ReusableImmutableNormalizedNodeStreamWriter writer =
                    ReusableImmutableNormalizedNodeStreamWriter.create();
            for (int i = 0; i < size; ++i) {
                modifications.add(TransactionModification.readFrom(nnin, writer));
            }
        } else {
            modifications = ImmutableList.of();
        }
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        super.writeExternal(out);

        out.writeByte(PersistenceProtocol.byteValue(protocol.orElse(null)));
        out.writeInt(modifications.size());
        if (!modifications.isEmpty()) {
            try (NormalizedNodeDataOutput nnout = streamVersion.newDataOutput(out)) {
                for (TransactionModification op : modifications) {
                    op.writeTo(nnout);
                }
            }
        }
    }

    @Override
    protected ModifyTransactionRequest createRequest(final TransactionIdentifier target, final long sequence,
            final ActorRef replyTo) {
        return new ModifyTransactionRequest(target, sequence, replyTo, modifications, protocol.orElse(null));
    }
}
