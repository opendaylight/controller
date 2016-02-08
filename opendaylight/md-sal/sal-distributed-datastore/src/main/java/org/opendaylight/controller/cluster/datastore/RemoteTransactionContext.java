/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSelection;
import akka.dispatch.OnComplete;
import akka.util.Timeout;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.SettableFuture;
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.AbstractRead;
import org.opendaylight.controller.cluster.datastore.messages.BatchedModifications;
import org.opendaylight.controller.cluster.datastore.messages.CloseTransaction;
import org.opendaylight.controller.cluster.datastore.messages.SerializableMessage;
import org.opendaylight.controller.cluster.datastore.modification.AbstractModification;
import org.opendaylight.controller.cluster.datastore.modification.Modification;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
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

    private final ActorContext actorContext;
    private final ActorSelection actor;
    private final OperationLimiter limiter;

    private BatchedModifications batchedModifications;
    private int totalBatchedModificationsSent;

    protected RemoteTransactionContext(TransactionIdentifier identifier, ActorSelection actor,
            ActorContext actorContext, short remoteTransactionVersion, OperationLimiter limiter) {
        super(identifier, remoteTransactionVersion);
        this.limiter = Preconditions.checkNotNull(limiter);
        this.actor = actor;
        this.actorContext = actorContext;
    }

    private Future<Object> completeOperation(Future<Object> operationFuture){
        operationFuture.onComplete(limiter, actorContext.getClientDispatcher());
        return operationFuture;
    }

    private ActorSelection getActor() {
        return actor;
    }

    protected ActorContext getActorContext() {
        return actorContext;
    }

    protected Future<Object> executeOperationAsync(SerializableMessage msg, Timeout timeout) {
        return completeOperation(actorContext.executeOperationAsync(getActor(), msg.toSerializable(), timeout));
    }

    @Override
    public void closeTransaction() {
        LOG.debug("Tx {} closeTransaction called", getIdentifier());
        TransactionContextCleanup.untrack(this);

        actorContext.sendOperationAsync(getActor(), new CloseTransaction(getTransactionVersion()).toSerializable());
    }

    @Override
    public Future<Object> directCommit() {
        LOG.debug("Tx {} directCommit called", getIdentifier());

        // Send the remaining batched modifications, if any, with the ready flag set.

        return sendBatchedModifications(true, true);
    }

    @Override
    public Future<ActorSelection> readyTransaction() {
        logModificationCount();

        LOG.debug("Tx {} readyTransaction called", getIdentifier());

        // Send the remaining batched modifications, if any, with the ready flag set.

        Future<Object> lastModificationsFuture = sendBatchedModifications(true, false);

        return transformReadyReply(lastModificationsFuture);
    }

    protected Future<ActorSelection> transformReadyReply(final Future<Object> readyReplyFuture) {
        // Transform the last reply Future into a Future that returns the cohort actor path from
        // the last reply message. That's the end result of the ready operation.

        return TransactionReadyReplyMapper.transform(readyReplyFuture, actorContext, getIdentifier());
    }

    private BatchedModifications newBatchedModifications() {
        return new BatchedModifications(getIdentifier().toString(), getTransactionVersion(),
                getIdentifier().getChainId());
    }

    private void batchModification(Modification modification) {
        incrementModificationCount();
        if(batchedModifications == null) {
            batchedModifications = newBatchedModifications();
        }

        batchedModifications.addModification(modification);

        if(batchedModifications.getModifications().size() >=
                actorContext.getDatastoreContext().getShardBatchedModificationCount()) {
            sendBatchedModifications();
        }
    }

    protected Future<Object> sendBatchedModifications() {
        return sendBatchedModifications(false, false);
    }

    protected Future<Object> sendBatchedModifications(boolean ready, boolean doCommitOnReady) {
        Future<Object> sent = null;
        if(ready || (batchedModifications != null && !batchedModifications.getModifications().isEmpty())) {
            if(batchedModifications == null) {
                batchedModifications = newBatchedModifications();
            }

            if(LOG.isDebugEnabled()) {
                LOG.debug("Tx {} sending {} batched modifications, ready: {}", getIdentifier(),
                        batchedModifications.getModifications().size(), ready);
            }

            batchedModifications.setReady(ready);
            batchedModifications.setDoCommitOnReady(doCommitOnReady);
            batchedModifications.setTotalMessagesSent(++totalBatchedModificationsSent);
            sent = executeOperationAsync(batchedModifications, actorContext.getTransactionCommitOperationTimeout());

            if(ready) {
                batchedModifications = null;
            } else {
                batchedModifications = newBatchedModifications();
            }
        }

        return sent;
    }

    @Override
    public void executeModification(AbstractModification modification) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("Tx {} executeModification {} called path = {}", getIdentifier(), modification.getClass()
                    .getSimpleName(), modification.getPath());
        }

        acquireOperation();
        batchModification(modification);
    }

    @Override
    public <T> void executeRead(final AbstractRead<T> readCmd, final SettableFuture<T> returnFuture) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("Tx {} executeRead {} called path = {}", getIdentifier(), readCmd.getClass().getSimpleName(),
                    readCmd.getPath());
        }

        // Send any batched modifications. This is necessary to honor the read uncommitted semantics of the
        // public API contract.

        acquireOperation();
        sendBatchedModifications();

        OnComplete<Object> onComplete = new OnComplete<Object>() {
            @Override
            public void onComplete(Throwable failure, Object response) throws Throwable {
                if(failure != null) {
                    if(LOG.isDebugEnabled()) {
                        LOG.debug("Tx {} {} operation failed: {}", getIdentifier(), readCmd.getClass().getSimpleName(),
                                failure);
                    }
                    returnFuture.setException(new ReadFailedException("Error checking " + readCmd.getClass().getSimpleName()
                            + " for path " + readCmd.getPath(), failure));
                } else {
                    if(LOG.isDebugEnabled()) {
                        LOG.debug("Tx {} {} operation succeeded", getIdentifier(), readCmd.getClass().getSimpleName());
                    }
                    readCmd.processResponse(response, returnFuture);
                }
            }
        };

        Future<Object> future = executeOperationAsync(readCmd.asVersion(getTransactionVersion()),
                actorContext.getOperationTimeout());

        future.onComplete(onComplete, actorContext.getClientDispatcher());
    }

    /**
     * Acquire operation from the limiter if the hand-off has completed. If
     * the hand-off is still ongoing, this method does nothing.
     */
    private final void acquireOperation() {
        if (isOperationHandOffComplete()) {
            limiter.acquire();
        }
    }

    @Override
    public boolean usesOperationLimiting() {
        return true;
    }
}
