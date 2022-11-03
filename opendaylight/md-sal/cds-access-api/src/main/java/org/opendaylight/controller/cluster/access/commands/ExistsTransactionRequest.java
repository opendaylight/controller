/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import akka.actor.ActorRef;
import java.io.Serial;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * A transaction request to query if a particular path exists in the current view of a particular transaction.
 */
public final class ExistsTransactionRequest extends AbstractReadPathTransactionRequest<ExistsTransactionRequest> {
    @Serial
    private static final long serialVersionUID = 1L;

    public ExistsTransactionRequest(final @NonNull TransactionIdentifier identifier, final long sequence,
            final @NonNull  ActorRef replyTo, final @NonNull YangInstanceIdentifier path, final boolean snapshotOnly) {
        super(identifier, sequence, replyTo, path, snapshotOnly);
    }

    private ExistsTransactionRequest(final ExistsTransactionRequest request, final ABIVersion version) {
        super(request, version);
    }

    @Override
    protected ExistsTransactionRequest cloneAsVersion(final ABIVersion version) {
        return new ExistsTransactionRequest(this, version);
    }

    @Override
    protected ExistsTransactionRequestProxyV1 externalizableProxy(final ABIVersion version) {
        return new ExistsTransactionRequestProxyV1(this);
    }
}
