/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import akka.actor.ActorSelection;
import akka.dispatch.Futures;
import akka.dispatch.OnComplete;
import akka.pattern.AskTimeoutException;
import com.google.common.util.concurrent.SettableFuture;
import java.util.Optional;
import java.util.SortedSet;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.databroker.DatastoreExceptionTracker;
import org.opendaylight.controller.cluster.datastore.messages.AbstractRead;
import org.opendaylight.controller.cluster.datastore.messages.BatchedModifications;
import org.opendaylight.controller.cluster.datastore.messages.CloseTransaction;
import org.opendaylight.controller.cluster.datastore.modification.AbstractModification;
import org.opendaylight.controller.cluster.datastore.modification.DeleteModification;
import org.opendaylight.controller.cluster.datastore.modification.MergeModification;
import org.opendaylight.controller.cluster.datastore.modification.Modification;
import org.opendaylight.controller.cluster.datastore.modification.WriteModification;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;

/**
 * Redirects front-end transaction operations to a shard for processing. Instances of this class are used
 * when the destination shard is remote to the caller.
 *
 * @author Thomas Pantelis
 */
public class RemoteTransactionContext extends AbstractTransactionContext {
    private static final Logger LOG = LoggerFactory.getLogger(RemoteTransactionContext.class);

    private final ActorUtils actorUtils;
    private final ActorSelection actor;
    private final OperationLimiter limiter;

    private BatchedModifications batchedModifications;
    private int totalBatchedModificationsSent;
    private int batchPermits;
    private static DatastoreExceptionTracker datastoreExceptionTracker = DatastoreExceptionTracker.getInstance();

    /**
     * We have observed a failed modification batch. This transaction context is effectively doomed, as the backend
     * does not have a correct view of the world. If this happens, we do not limit operations but rather short-cut them
     * to a either a no-op (modifications) or a failure (reads). Once the transaction is ready, though, we send the
     * message to resynchronize with the backend, sharing a 'lost message' failure path.
     */
    private volatile Throwable failedModification;

    protected RemoteTransactionContext(final TransactionIdentifier identifier, final ActorSelection actor,
            final ActorUtils actorUtils, final short remoteTransactionVersion, final OperationLimiter limiter) {
        super(identifier, remoteTransactionVersion);
        this.limiter = requireNonNull(limiter);
        this.actor = actor;
        this.actorUtils = actorUtils;
    }

    private ActorSelection getActor() {
        return actor;
    }

    protected ActorUtils getActorUtils() {
        return actorUtils;
    }

    @Override
    public void closeTransaction() {
        LOG.debug("Tx {} closeTransaction called", getIdentifier());
        TransactionContextCleanup.untrack(this);

        actorUtils.sendOperationAsync(getActor(), new CloseTransaction(getTransactionVersion()).toSerializable());
    }

    @Override
    public Future<Object> directCommit(final Boolean havePermit) {
        LOG.debug("Tx {} directCommit called", getIdentifier());

        // Send the remaining batched modifications, if any, with the ready flag set.
        bumpPermits(havePermit);
        return sendBatchedModifications(true, true, Optional.empty());
    }

    @Override
    public Future<ActorSelection> readyTransaction(final Boolean havePermit,
            final Optional<SortedSet<String>> participatingShardNames) {
        logModificationCount();

        LOG.debug("Tx {} readyTransaction called", getIdentifier());

        // Send the remaining batched modifications, if any, with the ready flag set.

        bumpPermits(havePermit);
        Future<Object> lastModificationsFuture = sendBatchedModifications(true, false, participatingShardNames);

        return transformReadyReply(lastModificationsFuture);
    }

    private void bumpPermits(final Boolean havePermit) {
        if (Boolean.TRUE.equals(havePermit)) {
            ++batchPermits;
        }
    }

    protected Future<ActorSelection> transformReadyReply(final Future<Object> readyReplyFuture) {
        // Transform the last reply Future into a Future that returns the cohort actor path from
        // the last reply message. That's the end result of the ready operation.

        return TransactionReadyReplyMapper.transform(readyReplyFuture, actorUtils, getIdentifier());
    }

    private BatchedModifications newBatchedModifications() {
        return new BatchedModifications(getIdentifier(), getTransactionVersion());
    }

    private void batchModification(final Modification modification, final boolean havePermit) {
        incrementModificationCount();
        if (havePermit) {
            ++batchPermits;
        }

        if (batchedModifications == null) {
            batchedModifications = newBatchedModifications();
        }

        batchedModifications.addModification(modification);

        if (batchedModifications.getModifications().size()
                >= actorUtils.getDatastoreContext().getShardBatchedModificationCount()) {
            sendBatchedModifications();
        }
    }

    protected Future<Object> sendBatchedModifications() {
        return sendBatchedModifications(false, false, Optional.empty());
    }

    protected Future<Object> sendBatchedModifications(final boolean ready, final boolean doCommitOnReady,
            final Optional<SortedSet<String>> participatingShardNames) {
        Future<Object> sent = null;
        if (ready || batchedModifications != null && !batchedModifications.getModifications().isEmpty()) {
            if (batchedModifications == null) {
                batchedModifications = newBatchedModifications();
            }

            LOG.debug("Tx {} sending {} batched modifications, ready: {}", getIdentifier(),
                    batchedModifications.getModifications().size(), ready);

            batchedModifications.setDoCommitOnReady(doCommitOnReady);
            batchedModifications.setTotalMessagesSent(++totalBatchedModificationsSent);

            final BatchedModifications toSend = batchedModifications;
            final int permitsToRelease = batchPermits;
            batchPermits = 0;

            if (ready) {
                batchedModifications.setReady(participatingShardNames);
                batchedModifications.setDoCommitOnReady(doCommitOnReady);
                batchedModifications = null;
            } else {
                batchedModifications = newBatchedModifications();

                final Throwable failure = failedModification;
                if (failure != null) {
                    // We have observed a modification failure, it does not make sense to send this batch. This speeds
                    // up the time when the application could be blocked due to messages timing out and operation
                    // limiter kicking in.
                    LOG.debug("Tx {} modifications previously failed, not sending a non-ready batch", getIdentifier());
                    limiter.release(permitsToRelease);
                    return Futures.failed(failure);
                }
            }

            sent = actorUtils.executeOperationAsync(getActor(), toSend.toSerializable(),
                actorUtils.getTransactionCommitOperationTimeout());
            sent.onComplete(new OnComplete<>() {
                @Override
                public void onComplete(final Throwable failure, final Object success) {
                    if (failure != null) {
                        LOG.debug("Tx {} modifications failed", getIdentifier(), failure);
                        failedModification = failure;
                    } else {
                        LOG.debug("Tx {} modifications completed with {}", getIdentifier(), success);
                    }
                    limiter.release(permitsToRelease);
                }
            }, actorUtils.getClientDispatcher());
        }

        return sent;
    }

    @Override
    public void executeDelete(final YangInstanceIdentifier path, final Boolean havePermit) {
        LOG.debug("Tx {} executeDelete called path = {}", getIdentifier(), path);
        executeModification(new DeleteModification(path), havePermit);
    }

    @Override
    public void executeMerge(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data,
            final Boolean havePermit) {
        LOG.debug("Tx {} executeMerge called path = {}", getIdentifier(), path);
        executeModification(new MergeModification(path, data), havePermit);
    }

    @Override
    public void executeWrite(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data,
            final Boolean havePermit) {
        LOG.debug("Tx {} executeWrite called path = {}", getIdentifier(), path);
        executeModification(new WriteModification(path, data), havePermit);
    }

    private void executeModification(final AbstractModification modification, final Boolean havePermit) {
        final boolean permitToRelease;
        if (havePermit == null) {
            permitToRelease = failedModification == null && acquireOperation();
        } else {
            permitToRelease = havePermit.booleanValue();
        }

        batchModification(modification, permitToRelease);
    }

    @Override
    public <T> void executeRead(final AbstractRead<T> readCmd, final SettableFuture<T> returnFuture,
            final Boolean havePermit) {
        LOG.debug("Tx {} executeRead {} called path = {}", getIdentifier(), readCmd.getClass().getSimpleName(),
                readCmd.getPath());

        final Throwable failure = failedModification;
        if (failure != null) {
            // If we know there was a previous modification failure, we must not send a read request, as it risks
            // returning incorrect data. We check this before acquiring an operation simply because we want the app
            // to complete this transaction as soon as possible.
            returnFuture.setException(new ReadFailedException("Previous modification failed, cannot "
                    + readCmd.getClass().getSimpleName() + " for path " + readCmd.getPath(), failure));
            return;
        }

        // Send any batched modifications. This is necessary to honor the read uncommitted semantics of the
        // public API contract.

        final boolean permitToRelease = havePermit == null ? acquireOperation() : havePermit.booleanValue();
        sendBatchedModifications();

        OnComplete<Object> onComplete = new OnComplete<>() {
            @Override
            public void onComplete(final Throwable failure, final Object response) {
                // We have previously acquired an operation, now release it, no matter what happened
                if (permitToRelease) {
                    limiter.release();
                }

                if (failure != null) {
                    // The AskTimeoutException is handled specially, that the count is been maintained and monitored.
                    // Alarm will be raised if the count hits the configured threshold level.
                    if (failure instanceof AskTimeoutException) {
                        String transactionClassName = this.getClass().getName();
                        String exceptionCounterKey = datastoreExceptionTracker.getExceptionTrackerCounterName(failure,
                                transactionClassName);
                        datastoreExceptionTracker.incrementAskTimeoutExceptionCounter(exceptionCounterKey);
                    }
                    LOG.debug("Tx {} {} operation failed", getIdentifier(), readCmd.getClass().getSimpleName(),
                        failure);

                    returnFuture.setException(new ReadFailedException("Error checking "
                        + readCmd.getClass().getSimpleName() + " for path " + readCmd.getPath(), failure));
                } else {
                    LOG.debug("Tx {} {} operation succeeded", getIdentifier(), readCmd.getClass().getSimpleName());
                    readCmd.processResponse(response, returnFuture);
                }
            }
        };

        final Future<Object> future = actorUtils.executeOperationAsync(getActor(),
            readCmd.asVersion(getTransactionVersion()).toSerializable(), actorUtils.getOperationTimeout());
        future.onComplete(onComplete, actorUtils.getClientDispatcher());
    }

    /**
     * Acquire operation from the limiter if the hand-off has completed. If the hand-off is still ongoing, this method
     * does nothing.
     *
     * @return True if a permit was successfully acquired, false otherwise
     */
    private boolean acquireOperation() {
        checkState(isOperationHandOffComplete(),
            "Attempted to acquire execute operation permit for transaction %s on actor %s during handoff",
            getIdentifier(), actor);

        if (limiter.acquire()) {
            return true;
        }

        LOG.warn("Failed to acquire execute operation permit for transaction {} on actor {}", getIdentifier(), actor);
        return false;
    }

    @Override
    public boolean usesOperationLimiting() {
        return true;
    }
}
