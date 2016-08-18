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
import java.util.function.Consumer;
import org.opendaylight.controller.cluster.access.commands.TransactionAbortRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionAbortSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionCanCommitSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionCommitSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionDoCommitRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionPreCommitRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionPreCommitSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionRequest;
import org.opendaylight.controller.cluster.access.concepts.RequestFailure;
import org.opendaylight.controller.cluster.access.concepts.Response;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

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

    private long sequence;
    private boolean sealed;

    AbstractProxyTransaction(final DistributedDataStoreClientBehavior client) {
        this.client = Preconditions.checkNotNull(client);
    }

    final ActorRef localActor() {
        return client.self();
    }

    final long nextSequence() {
        return sequence++;
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

    final void sendRequest(final TransactionRequest<?> request, final Consumer<Response<?, ?>> completer) {
        client.sendRequest(request, completer);
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
    final ListenableFuture<Boolean> directCommit() {
        checkSealed();

        final SettableFuture<Boolean> ret = SettableFuture.create();
        sendRequest(Verify.verifyNotNull(doCommit(false)), t -> {
            if (t instanceof TransactionCommitSuccess) {
                ret.set(Boolean.TRUE);
            } else if (t instanceof RequestFailure) {
                ret.setException(((RequestFailure<?, ?>) t).getCause());
            } else {
                ret.setException(new IllegalStateException("Unhandled response " + t.getClass()));
            }
        });
        return ret;
    }

    void abort(final VotingFuture<Void> ret) {
        checkSealed();

        sendRequest(new TransactionAbortRequest(getIdentifier(), nextSequence(), localActor()), t -> {
            if (t instanceof TransactionAbortSuccess) {
                ret.voteYes();
            } else if (t instanceof RequestFailure) {
                ret.voteNo(((RequestFailure<?, ?>) t).getCause());
            } else {
                ret.voteNo(new IllegalStateException("Unhandled response " + t.getClass()));
            }
        });
    }

    void canCommit(final VotingFuture<?> ret) {
        checkSealed();

        sendRequest(Verify.verifyNotNull(doCommit(true)), t -> {
            if (t instanceof TransactionCanCommitSuccess) {
                ret.voteYes();
            } else if (t instanceof RequestFailure) {
                ret.voteNo(((RequestFailure<?, ?>) t).getCause());
            } else {
                ret.voteNo(new IllegalStateException("Unhandled response " + t.getClass()));
            }
        });
    }

    void preCommit(final VotingFuture<?> ret) {
        checkSealed();

        sendRequest(new TransactionPreCommitRequest(getIdentifier(), nextSequence(), localActor()), t-> {
            if (t instanceof TransactionPreCommitSuccess) {
                ret.voteYes();
            } else if (t instanceof RequestFailure) {
                ret.voteNo(((RequestFailure<?, ?>) t).getCause());
            } else {
                ret.voteNo(new IllegalStateException("Unhandled response " + t.getClass()));
            }
        });
    }

    void doCommit(final VotingFuture<?> ret) {
        checkSealed();

        sendRequest(new TransactionDoCommitRequest(getIdentifier(), nextSequence(), localActor()), t-> {
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
