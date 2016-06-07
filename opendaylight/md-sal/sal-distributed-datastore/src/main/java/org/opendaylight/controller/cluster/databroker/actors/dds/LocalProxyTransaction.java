/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import java.util.function.Consumer;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.cluster.access.commands.AbortLocalTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.CommitLocalTransactionRequest;
import org.opendaylight.controller.cluster.access.concepts.Response;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link AbstractProxyTransaction} for dispatching a transaction towards a shard leader which is co-located with
 * the client instance.
 *
 * It requires a {@link DataTreeSnapshot}, which is used to instantiated a new {@link DataTreeModification}. Operations
 * are then performed on this modification and once the transaction is submitted, the modification is sent to the shard
 * leader.
 *
 * This class is not thread-safe as usual with transactions. Since it does not interact with the backend until the
 * transaction is submitted, at which point this class gets out of the picture, this is not a cause for concern.
 *
 * @author Robert Varga
 */
@NotThreadSafe
final class LocalProxyTransaction extends AbstractProxyTransaction {
    private static final Logger LOG = LoggerFactory.getLogger(LocalProxyTransaction.class);
    private static final Consumer<Response<?, ?>> ABORT_COMPLETER = response -> {
        LOG.debug("Abort completed with {}", response);
    };

    private final TransactionIdentifier identifier;
    private DataTreeModification modification;

    LocalProxyTransaction(final DistributedDataStoreClientBehavior client,
        final TransactionIdentifier identifier, final DataTreeSnapshot snapshot) {
        super(client);
        this.identifier = Preconditions.checkNotNull(identifier);
        this.modification = snapshot.newModification();
    }

    @Override
    public TransactionIdentifier getIdentifier() {
        return identifier;
    }

    @Override
    void doDelete(final YangInstanceIdentifier path) {
        modification.delete(path);
    }

    @Override
    void doMerge(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        modification.merge(path, data);
    }

    @Override
    void doWrite(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        modification.write(path, data);
    }

    @Override
    CheckedFuture<Boolean, ReadFailedException> doExists(final YangInstanceIdentifier path) {
        return Futures.immediateCheckedFuture(modification.readNode(path).isPresent());
    }

    @Override
    CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> doRead(final YangInstanceIdentifier path) {
        return Futures.immediateCheckedFuture(modification.readNode(path));
    }

    @Override
    void doAbort() {
        client().sendRequest(nextSequence(), new AbortLocalTransactionRequest(identifier, client().self()), ABORT_COMPLETER);
        modification = new FailedDataTreeModification(() -> new IllegalStateException("Tracker has been aborted"));
    }

    @Override
    CommitLocalTransactionRequest doCommit(final boolean coordinated) {
        final CommitLocalTransactionRequest ret = new CommitLocalTransactionRequest(identifier, client().self(),
            modification, coordinated);
        modification = new FailedDataTreeModification(() -> new IllegalStateException("Tracker has been submitted"));
        return ret;
    }

    @Override
    void doSeal() {
        modification.ready();
    }
}
