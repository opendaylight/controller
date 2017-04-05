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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;

/**
 * Request to commit a local transaction. Since local transactions do not introduce state on the backend until they
 * are ready, this message carries a complete set of modifications.
 *
 * @author Robert Varga
 */
@Beta
public final class CommitLocalTransactionRequest
        extends AbstractLocalTransactionRequest<CommitLocalTransactionRequest> {
    private static final long serialVersionUID = 1L;

    @SuppressFBWarnings(value = "SE_BAD_FIELD", justification = "This field is not Serializable but this class "
            + "implements writeReplace to delegate serialization to a Proxy class and thus instances of this class "
            + "aren't serialized. FindBugs does not recognize this.")
    private final DataTreeModification mod;
    private final Exception delayedFailure;
    private final boolean coordinated;

    public CommitLocalTransactionRequest(@Nonnull final TransactionIdentifier identifier, final long sequence,
            @Nonnull final ActorRef replyTo, @Nonnull final DataTreeModification mod,
            @Nullable final Exception delayedFailure, final boolean coordinated) {
        super(identifier, sequence, replyTo);
        this.mod = Preconditions.checkNotNull(mod);
        this.delayedFailure = delayedFailure;
        this.coordinated = coordinated;
    }

    /**
     * Return the delayed error detected on the frontend. If this error is present, it will be reported as the result
     * of the first step of the commit process.
     *
     * @return Delayed failure, if present.
     */
    public Optional<Exception> getDelayedFailure() {
        return Optional.ofNullable(delayedFailure);
    }

    public DataTreeModification getModification() {
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
        return super.addToStringAttributes(toStringHelper).add("coordinated", coordinated)
                .add("delayedError", delayedFailure);
    }
}
