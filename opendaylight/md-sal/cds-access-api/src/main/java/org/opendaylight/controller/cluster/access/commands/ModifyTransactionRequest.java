/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import akka.actor.ActorRef;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.TransactionRequestIdentifier;

public final class ModifyTransactionRequest extends TransactionRequest {
    public static enum FinishTransaction {
        NONE {
            @Override
            byte byteValue() {
                return 0;
            }
        },
        ABORT {
            @Override
            byte byteValue() {
                return 1;
            }
        },
        COORDINATED_COMMIT {
            @Override
            byte byteValue() {
                return 2;
            }
        },
        SIMPLE_COMMIT {
            @Override
            byte byteValue() {
                return 2;
            }
        };

        abstract byte byteValue();

        static FinishTransaction valueOf(final byte b) {
            switch (b) {
                case 0:
                    return NONE;
                case 1:
                    return ABORT;
                case 2:
                    return COORDINATED_COMMIT;
                case 3:
                    return SIMPLE_COMMIT;
                default:
                    throw new IllegalArgumentException("Invalid byte value " + b);
            }
        }
    };

    private static final class Proxy extends AbstractRequestProxy<TransactionRequestIdentifier> {
        private List<TransactionModification> modifications;
        private FinishTransaction finish;

        public Proxy() {
            modifications = ImmutableList.of();
        }

        Proxy(final TransactionRequestIdentifier identifier, final ActorRef replyTo,
                final List<TransactionModification> modifications) {
            super(identifier, replyTo);
            this.modifications = Preconditions.checkNotNull(modifications);
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            super.writeExternal(out);

            out.writeInt(modifications.size());
            for (TransactionModification op : modifications) {
                out.writeObject(op);
            }
            out.writeByte(finish == null ? 0 : finish.byteValue());
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
            finish = FinishTransaction.valueOf(in.readByte());
        }

        @Override
        protected ModifyTransactionRequest readResolve() {
            return new ModifyTransactionRequest(getIdentifier(), getReplyTo(), modifications, finish);
        }
    }

    private static final long serialVersionUID = 1L;
    private final List<TransactionModification> modifications;
    private final FinishTransaction finish;

    ModifyTransactionRequest(final TransactionRequestIdentifier id, final ActorRef frontendRef,
        final List<TransactionModification> modifications, final FinishTransaction finish) {
        super(id, frontendRef);
        this.modifications = ImmutableList.copyOf(modifications);
        this.finish = Preconditions.checkNotNull(finish);
    }

    public FinishTransaction getFinish() {
        return finish;
    }

    public List<TransactionModification> getModifications() {
        return modifications;
    }

    @Override
    public FrontendIdentifier getFrontendIdentifier() {
        return getIdentifier().getTransactionId().getFrontendId();
    }

    @Override
    protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        return super.addToStringAttributes(toStringHelper).add("operations", modifications).add("finish", finish);
    }

    @Override
    protected Proxy writeReplace() {
        return new Proxy(getIdentifier(), getReplyTo(), modifications);
    }
}
