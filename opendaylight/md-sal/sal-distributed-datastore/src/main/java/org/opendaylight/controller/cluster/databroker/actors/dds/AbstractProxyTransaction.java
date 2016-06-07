/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import akka.actor.ActorRef;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import javax.annotation.Nullable;
import org.opendaylight.controller.cluster.access.commands.TransactionCanCommitSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionCommitSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionPreCommitSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionRequest;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.RequestFailure;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;

/**
 * Class translating transaction operations towards a particular backend shard.
 *
 * This class is not safe to access from multiple application threads, as is usual for transactions. Internal state
 * transitions coming from interactions with backend are expected to be thread-safe.
 *
 * This class interacts with the queueing mechanism in ClientActorBehavior, hence once we arrive at a decision
 * to use either a local or remote implementation, we are stuck with it. We can re-evaluate on the next transaction.
 *
 * @author Robert Varga
 */
abstract class AbstractProxyTransaction implements Identifiable<TransactionIdentifier> {
    private final DistributedDataStoreClientBehavior client;

    private boolean sealed;

    private volatile ActorRef commitCohort;

    AbstractProxyTransaction(final DistributedDataStoreClientBehavior client) {
        this.client = Preconditions.checkNotNull(client);
    }

    /**
     * Instantiate a new tracker for a transaction. This method bases its decision on which implementation to use
     * based on provided {@link ShardBackendInfo}. If no information is present, it will choose the remote
     * implementation, which is fine, as the queueing logic in ClientActorBehavior will hold on to the requests until
     * the backend is located.
     *
     * @param client Client behavior
     * @param historyId Local history identifier
     * @param transactionId Transaction identifier
     * @param backend Optional backend identifier
     * @return A new state tracker
     */
    static AbstractProxyTransaction create(final DistributedDataStoreClientBehavior client,
            final LocalHistoryIdentifier historyId, final long transactionId,
            final @Nullable ShardBackendInfo backend) {

        final java.util.Optional<DataTree> dataTree = backend == null ? java.util.Optional.empty()
                : backend.getDataTree();

        final TransactionIdentifier identifier = new TransactionIdentifier(historyId, transactionId);
        if (dataTree.isPresent()) {
            return new LocalProxyTransaction(client, identifier, dataTree.get().takeSnapshot());
        } else {
            return new RemoteProxyTransaction(client, identifier);
        }
    }

    final DistributedDataStoreClientBehavior client() {
        return client;
    }

    final void delete(final YangInstanceIdentifier path) {
        checkSealed();
        doDelete(path);
    }

    final void merge(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        checkSealed();
        doMerge(path, data);
    }

    final void write(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        checkSealed();
        doWrite(path, data);
    }

    final CheckedFuture<Boolean, ReadFailedException> exists(final YangInstanceIdentifier path) {
        checkSealed();
        return doExists(path);
    }

    final CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(final YangInstanceIdentifier path) {
        checkSealed();
        return doRead(path);
    }

    /**
     * Seal this transaction before it is either
     */
    final void seal() {
        checkSealed();
        doSeal();
        sealed = true;
    }

    private void checkSealed() {
        Preconditions.checkState(sealed, "Transaction %s has not been sealed yet", getIdentifier());
    }

    /**
     * Abort this transaction. This is invoked only for read-only transactions and will result in an explicit message
     * being sent to the backend.
     */
    final void abort() {
        checkSealed();
        doAbort();
    }

    /**
     * Commit this transaction, possibly in a coordinated fashion.
     *
     * @param coordinated True if this transaction should be coordinated across multiple participants.
     * @return Future completion
     */
    final ListenableFuture<Void> directCommit() {
        checkSealed();

        final SettableFuture<Void> ret = SettableFuture.create();
        client().sendRequest(Verify.verifyNotNull(doCommit(false)), t -> {
            if (t instanceof TransactionCommitSuccess) {
                ret.set(null);
            } else if (t instanceof RequestFailure) {
                ret.setException(((RequestFailure<?, ?>) t).getCause());
            } else {
                ret.setException(new IllegalStateException("Unhandled response " + t.getClass()));
            }
        });
        return ret;
    }

    void canCommit(final CollectingFuture<?> ret) {
        checkSealed();

        client().sendRequest(Verify.verifyNotNull(doCommit(true)), t -> {
            if (t instanceof TransactionCanCommitSuccess) {
                commitCohort = ((TransactionCanCommitSuccess) t).getCohort();
                ret.voteYes();
            } else if (t instanceof RequestFailure) {
                ret.voteNo(((RequestFailure<?, ?>) t).getCause());
            } else {
                ret.voteNo(new IllegalStateException("Unhandled response " + t.getClass()));
            }
        });
    }

    void preCommit(final CollectingFuture<?> ret) {
        checkSealed();

        client.sendRequest(null, t-> {
            if (t instanceof TransactionPreCommitSuccess) {
                ret.voteYes();
            } else if (t instanceof RequestFailure) {
                ret.voteNo(((RequestFailure<?, ?>) t).getCause());
            } else {
                ret.voteNo(new IllegalStateException("Unhandled response " + t.getClass()));
            }
        });
    }

    void doCommit(final CollectingFuture<?> ret) {
        checkSealed();

        client.sendRequest(null, t-> {
            if (t instanceof TransactionCommitSuccess) {
                ret.voteYes();
            } else if (t instanceof RequestFailure) {
                ret.voteNo(((RequestFailure<?, ?>) t).getCause());
            } else {
                ret.voteNo(new IllegalStateException("Unhandled response " + t.getClass()));
            }
        });
    }

    abstract void doDelete(final YangInstanceIdentifier path);

    abstract void doMerge(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data);

    abstract void doWrite(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data);

    abstract CheckedFuture<Boolean, ReadFailedException> doExists(final YangInstanceIdentifier path);

    abstract CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> doRead(final YangInstanceIdentifier path);

    abstract void doSeal();

    abstract void doAbort();

    abstract TransactionRequest<?> doCommit(boolean coordinated);
}
