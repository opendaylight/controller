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
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;

/**
 * Request to commit a local transaction. Since local transactions do not introduce state on the backend until they
 * are ready, this message carries a complete set of modifications.
 *
 * @author Robert Varga
 */
@Beta
public final class CommitLocalTransactionRequest extends AbstractLocalTransactionRequest<CommitLocalTransactionRequest> {
    private static final long serialVersionUID = 1L;
    private final DataTreeModification mod;
    private final boolean coordinated;

    public CommitLocalTransactionRequest(final @Nonnull TransactionIdentifier identifier,
            final @Nonnull ActorRef replyTo, final @Nonnull DataTreeModification mod, final boolean coordinated) {
        super(identifier, 0, replyTo);
        this.mod = Preconditions.checkNotNull(mod);
        this.coordinated = coordinated;
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
        return super.addToStringAttributes(toStringHelper).add("coordinated", coordinated);
    }
}