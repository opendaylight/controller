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
import com.google.common.base.Optional;
import com.google.common.util.concurrent.SettableFuture;
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.BatchedModifications;
import org.opendaylight.controller.cluster.datastore.messages.CloseTransaction;
import org.opendaylight.controller.cluster.datastore.messages.DataExists;
import org.opendaylight.controller.cluster.datastore.messages.DataExistsReply;
import org.opendaylight.controller.cluster.datastore.messages.ReadData;
import org.opendaylight.controller.cluster.datastore.messages.ReadDataReply;
import org.opendaylight.controller.cluster.datastore.messages.SerializableMessage;
import org.opendaylight.controller.cluster.datastore.modification.DeleteModification;
import org.opendaylight.controller.cluster.datastore.modification.MergeModification;
import org.opendaylight.controller.cluster.datastore.modification.Modification;
import org.opendaylight.controller.cluster.datastore.modification.WriteModification;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
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

    private final ActorContext actorContext;
    private final ActorSelection actor;
    private final boolean isTxActorLocal;
    private final short remoteTransactionVersion;

    private final OperationLimiter operationCompleter;
    private BatchedModifications batchedModifications;
    private int totalBatchedModificationsSent;

    protected RemoteTransactionContext(ActorSelection actor, TransactionIdentifier identifier,
            ActorContext actorContext, boolean isTxActorLocal,
            short remoteTransactionVersion, OperationLimiter limiter) {
        super(identifier);
        this.actor = actor;
        this.actorContext = actorContext;
        this.isTxActorLocal = isTxActorLocal;
        this.remoteTransactionVersion = remoteTransactionVersion;
        this.operationCompleter = limiter;
    }

    private Future<Object> completeOperation(Future<Object> operationFuture){
        operationFuture.onComplete(this.operationCompleter, actorContext.getClientDispatcher());
        return operationFuture;
    }


    private ActorSelection getActor() {
        return actor;
    }

    protected ActorContext getActorContext() {
        return actorContext;
    }

    protected short getRemoteTransactionVersion() {
        return remoteTransactionVersion;
    }

    protected Future<Object> executeOperationAsync(SerializableMessage msg) {
        return completeOperation(actorContext.executeOperationAsync(getActor(), isTxActorLocal ? msg : msg.toSerializable()));
    }

    @Override
    public void closeTransaction() {
        LOG.debug("Tx {} closeTransaction called", getIdentifier());
        TransactionContextCleanup.untrack(this);

        actorContext.sendOperationAsync(getActor(), CloseTransaction.INSTANCE.toSerializable());
    }

    @Override
    public boolean supportsDirectCommit() {
        return true;
    }

    @Override
    public Future<Object> directCommit() {
        LOG.debug("Tx {} directCommit called", getIdentifier());

        // Send the remaining batched modifications, if any, with the ready flag set.

        return sendBatchedModifications(true, true);
    }

    @Override
    public Future<ActorSelection> readyTransaction() {
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
        return new BatchedModifications(getIdentifier().toString(), remoteTransactionVersion, getIdentifier().getChainId());
    }

    private void batchModification(Modification modification) {
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
            sent = executeOperationAsync(batchedModifications);

            if(ready) {
                batchedModifications = null;
            } else {
                batchedModifications = newBatchedModifications();
            }
        }

        return sent;
    }

    @Override
    public void deleteData(YangInstanceIdentifier path) {
        LOG.debug("Tx {} deleteData called path = {}", getIdentifier(), path);

        batchModification(new DeleteModification(path));
    }

    @Override
    public void mergeData(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
        LOG.debug("Tx {} mergeData called path = {}", getIdentifier(), path);

        batchModification(new MergeModification(path, data));
    }

    @Override
    public void writeData(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
        LOG.debug("Tx {} writeData called path = {}", getIdentifier(), path);

        batchModification(new WriteModification(path, data));
    }

    @Override
    public void readData(final YangInstanceIdentifier path,
            final SettableFuture<Optional<NormalizedNode<?, ?>>> returnFuture ) {

        LOG.debug("Tx {} readData called path = {}", getIdentifier(), path);

        // Send any batched modifications. This is necessary to honor the read uncommitted semantics of the
        // public API contract.

        sendBatchedModifications();

        OnComplete<Object> onComplete = new OnComplete<Object>() {
            @Override
            public void onComplete(Throwable failure, Object readResponse) throws Throwable {
                if(failure != null) {
                    LOG.debug("Tx {} read operation failed: {}", getIdentifier(), failure);
                    returnFuture.setException(new ReadFailedException(
                            "Error reading data for path " + path, failure));

                } else {
                    LOG.debug("Tx {} read operation succeeded", getIdentifier(), failure);

                    if (readResponse instanceof ReadDataReply) {
                        ReadDataReply reply = (ReadDataReply) readResponse;
                        returnFuture.set(Optional.<NormalizedNode<?, ?>>fromNullable(reply.getNormalizedNode()));

                    } else if (ReadDataReply.isSerializedType(readResponse)) {
                        ReadDataReply reply = ReadDataReply.fromSerializable(readResponse);
                        returnFuture.set(Optional.<NormalizedNode<?, ?>>fromNullable(reply.getNormalizedNode()));

                    } else {
                        returnFuture.setException(new ReadFailedException(
                            "Invalid response reading data for path " + path));
                    }
                }
            }
        };

        Future<Object> readFuture = executeOperationAsync(new ReadData(path));

        readFuture.onComplete(onComplete, actorContext.getClientDispatcher());
    }

    @Override
    public void dataExists(final YangInstanceIdentifier path, final SettableFuture<Boolean> returnFuture) {

        LOG.debug("Tx {} dataExists called path = {}", getIdentifier(), path);

        // Send any batched modifications. This is necessary to honor the read uncommitted semantics of the
        // public API contract.

        sendBatchedModifications();

        OnComplete<Object> onComplete = new OnComplete<Object>() {
            @Override
            public void onComplete(Throwable failure, Object response) throws Throwable {
                if(failure != null) {
                    LOG.debug("Tx {} dataExists operation failed: {}", getIdentifier(), failure);
                    returnFuture.setException(new ReadFailedException(
                            "Error checking data exists for path " + path, failure));
                } else {
                    LOG.debug("Tx {} dataExists operation succeeded", getIdentifier(), failure);

                    if (response instanceof DataExistsReply) {
                        returnFuture.set(Boolean.valueOf(((DataExistsReply) response).exists()));

                    } else if (response.getClass().equals(DataExistsReply.SERIALIZABLE_CLASS)) {
                        returnFuture.set(Boolean.valueOf(DataExistsReply.fromSerializable(response).exists()));

                    } else {
                        returnFuture.setException(new ReadFailedException(
                                "Invalid response checking exists for path " + path));
                    }
                }
            }
        };

        Future<Object> future = executeOperationAsync(new DataExists(path));

        future.onComplete(onComplete, actorContext.getClientDispatcher());
    }
}
