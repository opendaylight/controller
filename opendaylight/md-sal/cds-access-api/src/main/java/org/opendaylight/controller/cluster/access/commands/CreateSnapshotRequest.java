/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import akka.actor.ActorRef;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

/**
 * Create a new snapshot. Unlike transactions, which allow modification of state, snapshots provide only read-type
 * operations.
 *
 * @author Robert Varga
 */
public final class CreateSnapshotRequest extends TransactionRequest<CreateSnapshotRequest> {
    private static final long serialVersionUID = 1L;

    public CreateSnapshotRequest(final TransactionIdentifier identifier, final long sequence, final ActorRef replyTo) {
        super(identifier, sequence, replyTo);
    }

    CreateSnapshotRequest(final CreateSnapshotRequest request, final ABIVersion version) {
        super(request, version);
    }

    @Override
    protected AbstractTransactionRequestProxy<CreateSnapshotRequest> externalizableProxy(final ABIVersion version) {
        return new CreateSnapshotRequestProxyV1(this);
    }

    @Override
    protected CreateSnapshotRequest cloneAsVersion(final ABIVersion targetVersion) {
        return new CreateSnapshotRequest(this, targetVersion);
    }

}
