/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import java.util.function.Consumer;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.cluster.access.commands.CommitLocalTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ModifyTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.PersistenceProtocol;
import org.opendaylight.controller.cluster.access.concepts.Response;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;

/**
 * A read-only specialization of {@link LocalProxyTransaction}.
 *
 * @author Robert Varga
 */
@NotThreadSafe
final class LocalReadOnlyProxyTransaction extends LocalProxyTransaction {

    private final DataTreeSnapshot snapshot;

    LocalReadOnlyProxyTransaction(final ProxyHistory parent, final TransactionIdentifier identifier,
        final DataTreeSnapshot snapshot) {
        super(parent, identifier);
        this.snapshot = Preconditions.checkNotNull(snapshot);
    }

    @Override
    boolean isSnapshotOnly() {
        return true;
    }

    @Override
    DataTreeSnapshot readOnlyView() {
        return snapshot;
    }

    @Override
    void doDelete(final YangInstanceIdentifier path) {
        throw new UnsupportedOperationException("Read-only snapshot");
    }

    @Override
    void doMerge(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        throw new UnsupportedOperationException("Read-only snapshot");
    }

    @Override
    void doWrite(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        throw new UnsupportedOperationException("Read-only snapshot");
    }

    @Override
    CommitLocalTransactionRequest commitRequest(final boolean coordinated) {
        throw new UnsupportedOperationException("Read-only snapshot");
    }

    @Override
    void doSeal() {
        // No-op
    }

    @Override
    void flushState(final AbstractProxyTransaction successor) {
        // No-op
    }

    @Override
    void applyModifyTransactionRequest(final ModifyTransactionRequest request,
            final Consumer<Response<?, ?>> callback) {
        commonModifyTransactionRequest(request, callback);
        // FIXME: BUG-8538: this is touching cohorts
        abort();
    }

    @Override
    void replayModifyTransactionRequest(final ModifyTransactionRequest request,
            final Consumer<Response<?, ?>> callback, final long enqueuedTicks) {
        commonModifyTransactionRequest(request, callback);
        // FIXME: this should go through the enqueueRequest() path
        // FIXME: BUG-8538: this is touching cohorts
        abort();
    }

    private static void commonModifyTransactionRequest(final ModifyTransactionRequest request,
            final Consumer<Response<?, ?>> callback) {
        Verify.verify(request.getModifications().isEmpty());

        final PersistenceProtocol protocol = request.getPersistenceProtocol().get();
        Verify.verify(protocol == PersistenceProtocol.ABORT);
    }
}
