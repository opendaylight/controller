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
import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.access.concepts.TransactionRequestIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;

@Beta
public final class CommitLocalTransactionRequest extends AbstractLocalTransactionRequest {
    private static final long serialVersionUID = 1L;
    private final DataTreeModification mod;
    private final boolean coordinated;

    public CommitLocalTransactionRequest(final TransactionRequestIdentifier identifier, final ActorRef replyTo,
        final DataTreeModification mod, final boolean coordinated) {
        super(identifier, replyTo);
        this.mod = Preconditions.checkNotNull(mod);
        this.coordinated = coordinated;
    }

    public DataTreeModification getModification() {
        return mod;
    }

    public boolean isCoordinated() {
        return coordinated;
    }
}