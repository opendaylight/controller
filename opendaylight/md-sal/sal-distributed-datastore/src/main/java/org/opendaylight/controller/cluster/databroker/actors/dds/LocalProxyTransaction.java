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
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.cluster.access.commands.AbortLocalTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.CommitLocalTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ModifyTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionDelete;
import org.opendaylight.controller.cluster.access.commands.TransactionMerge;
import org.opendaylight.controller.cluster.access.commands.TransactionModification;
import org.opendaylight.controller.cluster.access.commands.TransactionRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionWrite;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
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
    @GuardedBy("this")
    private ConnectedClientConnection nextConnection;

    LocalProxyTransaction(final ProxyHistory parent, final TransactionIdentifier identifier,
        final DataTreeSnapshot snapshot) {
        super(parent);
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
        sendRequest(new AbortLocalTransactionRequest(identifier, localActor()), ABORT_COMPLETER);
        modification = new FailedDataTreeModification(() -> new IllegalStateException("Tracker has been aborted"));
    }

    @Override
    CommitLocalTransactionRequest doCommit(final boolean coordinated) {
        final CommitLocalTransactionRequest ret = new CommitLocalTransactionRequest(identifier, localActor(),
            modification, coordinated);
        modification = new FailedDataTreeModification(() -> new IllegalStateException("Tracker has been submitted"));
        return ret;
    }

    @Override
    void doSeal() {
        modification.ready();
        if (nextConnection != null) {
            LOG.debug("Resuming replay of {} to connection {}", this, nextConnection);

            // FIXME: check if the connection is still local and talks to the same data tree. Based on that check,
            //        there are three possible outcomes:
            //        - happy path, where reconnect has resolved to the same backend (unrestarted) and we do not have
            //          to do anything
            //        - local connection, where we need to acquire a new modification, replay the one we have on top
            //          of it and keep working in local mode
            //        - remote connection, where we need to replay the modifications, effectively issuing events
            //          the user has emitted

            throw new UnsupportedOperationException();
        }
    }

    private void applyModifyTransactionRequest(final ModifyTransactionRequest request) {
        for (TransactionModification mod : request.getModifications()) {
            if (mod instanceof TransactionWrite) {
                modification.write(mod.getPath(), ((TransactionWrite)mod).getData());
            } else if (mod instanceof TransactionMerge) {
                modification.merge(mod.getPath(), ((TransactionMerge)mod).getData());
            } else if (mod instanceof TransactionDelete) {
                modification.delete(mod.getPath());
            } else {
                throw new IllegalArgumentException("Unsupported modification " + mod);
            }
        }
    }

    @Override
    void handleForwardedRemoteRequest(final TransactionRequest<?> request) {
        LOG.debug("Applying forwaded request {}", request);

        if (request instanceof ModifyTransactionRequest) {
            applyModifyTransactionRequest((ModifyTransactionRequest) request);
        } else {
            throw new IllegalArgumentException("Unhandled request " + request);
        }
    }

    @Override
    void forwardToRemote(final RemoteProxyTransaction successor, final TransactionRequest<?> request,
            final Consumer<Response<?, ?>> callback) throws RequestException {
        // TODO Auto-generated method stub

    }

    @Override
    void forwardToLocal(final LocalProxyTransaction successor, final TransactionRequest<?> request,
            final Consumer<Response<?, ?>> callback) throws RequestException {
        // TODO Auto-generated method stub

    }
}
