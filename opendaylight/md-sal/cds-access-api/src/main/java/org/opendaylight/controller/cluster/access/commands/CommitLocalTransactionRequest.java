/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects.ToStringHelper;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.util.Optional;
import org.apache.pekko.actor.ActorRef;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeModification;

/**
 * Request to commit a local transaction. Since local transactions do not introduce state on the backend until they
 * are ready, this message carries a complete set of modifications.
 */
public final class CommitLocalTransactionRequest
        extends AbstractLocalTransactionRequest<CommitLocalTransactionRequest> {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private final @NonNull DataTreeModification mod;
    private final @Nullable Exception delayedFailure;
    private final boolean coordinated;

    @NonNullByDefault
    public CommitLocalTransactionRequest(final TransactionIdentifier identifier, final long sequence,
            final ActorRef replyTo, final DataTreeModification mod, final @Nullable Exception delayedFailure,
            final boolean coordinated) {
        super(identifier, sequence, replyTo);
        this.mod = requireNonNull(mod);
        this.delayedFailure = delayedFailure;
        this.coordinated = coordinated;
    }

    /**
     * Return the delayed error detected on the frontend. If this error is present, it will be reported as the result
     * of the first step of the commit process.
     *
     * @return Delayed failure, of present
     */
    public @Nullable Exception delayedFailure() {
        return delayedFailure;
    }

    /**
     * Return the delayed error detected on the frontend. If this error is present, it will be reported as the result
     * of the first step of the commit process.
     *
     * @return Delayed failure, if present.
     * @deprecated Use {@link #delayedFailure()} instead.
     */
    @Deprecated(since = "11.0.2", forRemoval = true)
    public Optional<Exception> getDelayedFailure() {
        return Optional.ofNullable(delayedFailure);
    }

    public @NonNull DataTreeModification getModification() {
        return mod;
    }

    /**
     * Indicate if this is a coordinated, multi-backend request. If this method returns true, the backend must
     * act as a cohort participating in the three-phase commit (3PC) protocol to commit this transaction.  If this
     * method returns false, the backend should proceed to commit the transaction and respond once the commit completes
     * or fails to complete.
     *
     * @return Indication of coordinated commit.
     */
    public boolean isCoordinated() {
        return coordinated;
    }

    @Override
    protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        return super.addToStringAttributes(toStringHelper)
            .add("coordinated", coordinated)
            .add("delayedError", delayedFailure);
    }

    @java.io.Serial
    private void readObject(final ObjectInputStream stream) throws IOException, ClassNotFoundException {
        throwNSE();
    }

    @java.io.Serial
    private void readObjectNoData() throws ObjectStreamException {
        throwNSE();
    }

    @java.io.Serial
    private void writeObject(final ObjectOutputStream stream) throws IOException {
        throwNSE();
    }
}
