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
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.opendaylight.controller.cluster.access.concepts.TransactionRequestIdentifier;

@Beta
public final class ModifyTransactionRequest extends TransactionRequest {
    private static final class Proxy extends AbstractRequestProxy<TransactionRequestIdentifier> {
        private List<TransactionModification> modifications;
        private PersistenceProtocol protocol;

        public Proxy() {
            modifications = ImmutableList.of();
        }

        Proxy(final TransactionRequestIdentifier identifier, final ActorRef replyTo,
                final List<TransactionModification> modifications, final PersistenceProtocol protocol) {
            super(identifier, replyTo);
            this.modifications = Preconditions.checkNotNull(modifications);
            this.protocol = protocol;
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            super.writeExternal(out);

            out.writeInt(modifications.size());
            for (TransactionModification op : modifications) {
                out.writeObject(op);
            }

            out.writeByte(PersistenceProtocol.byteValue(protocol));
         }

        @Override
        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            super.readExternal(in);

            final int size = in.readInt();
            if (size != 0) {
                modifications = new ArrayList<>(size);
                for (int i = 0; i < size; ++i) {
                    modifications.add((TransactionModification) in.readObject());
                }
            }
            protocol = PersistenceProtocol.valueOf(in.readByte());
        }

        @Override
        protected ModifyTransactionRequest readResolve() {
            return new ModifyTransactionRequest(getIdentifier(), getReplyTo(), modifications, protocol);
        }
    }

    private static final long serialVersionUID = 1L;
    private final List<TransactionModification> modifications;
    private final PersistenceProtocol protocol;

    ModifyTransactionRequest(final TransactionRequestIdentifier id, final ActorRef frontendRef,
        final List<TransactionModification> modifications, final PersistenceProtocol protocol) {
        super(id, frontendRef);
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
        return super.addToStringAttributes(toStringHelper).add("operations", modifications).add("protocol", protocol);
    }

    @Override
    protected Proxy writeReplace() {
        return new Proxy(getIdentifier(), getReplyTo(), modifications, protocol);
    }
}
