/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import akka.actor.ActorRef;
import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.concepts.SliceableMessage;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeDataInput;
import org.opendaylight.yangtools.yang.data.impl.schema.ReusableImmutableNormalizedNodeStreamWriter;

/**
 * A transaction request to apply a particular set of operations on top of the current transaction. This message is
 * used to also finish a transaction by specifying a {@link PersistenceProtocol}.
 *
 * @author Robert Varga
 */
@Beta
public final class ModifyTransactionRequest extends TransactionRequest<ModifyTransactionRequest>
        implements SliceableMessage {
    interface SerialForm extends TransactionRequest.SerialForm<ModifyTransactionRequest> {


        @Override
        default ModifyTransactionRequest readExternal(final ObjectInput in, final TransactionIdentifier target,
                final long sequence, final ActorRef replyTo) throws IOException {

            final var protocol = Optional.ofNullable(PersistenceProtocol.readFrom(in));
            final int size = in.readInt();
            final List<TransactionModification> modifications;
            if (size != 0) {
                modifications = new ArrayList<>(size);
                final var nnin = NormalizedNodeDataInput.newDataInput(in);
                final var writer = ReusableImmutableNormalizedNodeStreamWriter.create();
                for (int i = 0; i < size; ++i) {
                    modifications.add(TransactionModification.readFrom(nnin, writer));
                }
            } else {
                modifications = ImmutableList.of();
            }

            return new ModifyTransactionRequest(target, sequence, replyTo, modifications, protocol.orElse(null));
        }

        @Override
        default void writeExternal(final ObjectOutput out, final ModifyTransactionRequest msg) throws IOException {
            TransactionRequest.SerialForm.super.writeExternal(out, msg);

            out.writeByte(PersistenceProtocol.byteValue(msg.getPersistenceProtocol().orElse(null)));

            final var modifications = msg.getModifications();
            out.writeInt(modifications.size());
            if (!modifications.isEmpty()) {
                try (var nnout = msg.getVersion().getStreamVersion().newDataOutput(out)) {
                    for (var op : modifications) {
                        op.writeTo(nnout);
                    }
                }
            }
        }
    }

    private static final long serialVersionUID = 1L;

    @SuppressFBWarnings(value = "SE_BAD_FIELD", justification = "This field is not Serializable but this class "
            + "implements writeReplace to delegate serialization to a Proxy class and thus instances of this class "
            + "aren't serialized. FindBugs does not recognize this.")
    private final List<TransactionModification> modifications;
    private final PersistenceProtocol protocol;

    private ModifyTransactionRequest(final ModifyTransactionRequest request, final ABIVersion version) {
        super(request, version);
        modifications = request.modifications;
        protocol = request.protocol;
    }

    ModifyTransactionRequest(final TransactionIdentifier target, final long sequence, final ActorRef replyTo,
        final List<TransactionModification> modifications, final PersistenceProtocol protocol) {
        super(target, sequence, replyTo);
        this.modifications = ImmutableList.copyOf(modifications);
        this.protocol = protocol;
    }

    public Optional<PersistenceProtocol> getPersistenceProtocol() {
        return Optional.ofNullable(protocol);
    }

    public List<TransactionModification> getModifications() {
        return modifications;
    }

    @Override
    protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        return super.addToStringAttributes(toStringHelper).add("modifications", modifications.size())
                .add("protocol", protocol);
    }

    @Override
    protected SerialForm externalizableProxy(final ABIVersion version) {
        return ABIVersion.MAGNESIUM.lt(version) ? new MTR(this) : new ModifyTransactionRequestProxyV1(this);
    }

    @Override
    protected ModifyTransactionRequest cloneAsVersion(final ABIVersion version) {
        return new ModifyTransactionRequest(this, version);
    }
}
