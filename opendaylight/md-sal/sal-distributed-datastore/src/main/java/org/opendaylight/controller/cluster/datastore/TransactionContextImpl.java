/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSelection;
import akka.dispatch.Mapper;
import akka.dispatch.OnComplete;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.SettableFuture;
import java.util.List;
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.BatchedModifications;
import org.opendaylight.controller.cluster.datastore.messages.BatchedModificationsReply;
import org.opendaylight.controller.cluster.datastore.messages.CloseTransaction;
import org.opendaylight.controller.cluster.datastore.messages.DataExists;
import org.opendaylight.controller.cluster.datastore.messages.DataExistsReply;
import org.opendaylight.controller.cluster.datastore.messages.ReadData;
import org.opendaylight.controller.cluster.datastore.messages.ReadDataReply;
import org.opendaylight.controller.cluster.datastore.messages.ReadyTransaction;
import org.opendaylight.controller.cluster.datastore.messages.ReadyTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.SerializableMessage;
import org.opendaylight.controller.cluster.datastore.modification.DeleteModification;
import org.opendaylight.controller.cluster.datastore.modification.MergeModification;
import org.opendaylight.controller.cluster.datastore.modification.Modification;
import org.opendaylight.controller.cluster.datastore.modification.WriteModification;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;

public class TransactionContextImpl extends AbstractTransactionContext {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionContextImpl.class);

    private final String transactionChainId;
    private final ActorContext actorContext;
    private final ActorSelection actor;
    private final boolean isTxActorLocal;
    private final short remoteTransactionVersion;

    private final OperationCompleter operationCompleter;
    private BatchedModifications batchedModifications;

    protected TransactionContextImpl(ActorSelection actor, TransactionIdentifier identifier,
            String transactionChainId, ActorContext actorContext, SchemaContext schemaContext, boolean isTxActorLocal,
            short remoteTransactionVersion, OperationCompleter operationCompleter) {
        super(identifier);
        this.actor = actor;
        this.transactionChainId = transactionChainId;
        this.actorContext = actorContext;
        this.isTxActorLocal = isTxActorLocal;
        this.remoteTransactionVersion = remoteTransactionVersion;
        this.operationCompleter = operationCompleter;
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
        LOG.debug("Tx {} closeTransaction called", identifier);

        actorContext.sendOperationAsync(getActor(), CloseTransaction.INSTANCE.toSerializable());
    }

    @Override
    public Future<ActorSelection> readyTransaction() {
        LOG.debug("Tx {} readyTransaction called with {} previous recorded operations pending",
                identifier, recordedOperationFutures.size());

        // Send the remaining batched modifications if any.

        sendAndRecordBatchedModifications();

        // Send the ReadyTransaction message to the Tx actor.

        Future<Object> readyReplyFuture = executeOperationAsync(ReadyTransaction.INSTANCE);

        return combineRecordedOperationsFutures(readyReplyFuture);
    }

    protected Future<ActorSelection> combineRecordedOperationsFutures(final Future<Object> withLastReplyFuture) {
        // Combine all the previously recorded put/merge/delete operation reply Futures and the
        // ReadyTransactionReply Future into one Future. If any one fails then the combined
        // Future will fail. We need all prior operations and the ready operation to succeed
        // in order to attempt commit.

        List<Future<Object>> futureList = Lists.newArrayListWithCapacity(recordedOperationFutures.size() + 1);
        futureList.addAll(recordedOperationFutures);
        futureList.add(withLastReplyFuture);

        Future<Iterable<Object>> combinedFutures = akka.dispatch.Futures.sequence(futureList,
                actorContext.getClientDispatcher());

        // Transform the combined Future into a Future that returns the cohort actor path from
        // the ReadyTransactionReply. That's the end result of the ready operation.

        return combinedFutures.transform(new Mapper<Iterable<Object>, ActorSelection>() {
            @Override
            public ActorSelection checkedApply(Iterable<Object> notUsed) {
                LOG.debug("Tx {} readyTransaction: pending recorded operations succeeded",
                        identifier);

                // At this point all the Futures succeeded and we need to extract the cohort
                // actor path from the ReadyTransactionReply. For the recorded operations, they
                // don't return any data so we're only interested that they completed
                // successfully. We could be paranoid and verify the correct reply types but
                // that really should never happen so it's not worth the overhead of
                // de-serializing each reply.

                // Note the Future get call here won't block as it's complete.
                Object serializedReadyReply = withLastReplyFuture.value().get().get();
                if (serializedReadyReply instanceof ReadyTransactionReply) {
                    return actorContext.actorSelection(((ReadyTransactionReply)serializedReadyReply).getCohortPath());
                } else if(serializedReadyReply instanceof BatchedModificationsReply) {
                    return actorContext.actorSelection(((BatchedModificationsReply)serializedReadyReply).getCohortPath());
                } else if(serializedReadyReply.getClass().equals(ReadyTransactionReply.SERIALIZABLE_CLASS)) {
                    ReadyTransactionReply reply = ReadyTransactionReply.fromSerializable(serializedReadyReply);
                    String cohortPath = deserializeCohortPath(reply.getCohortPath());
                    return actorContext.actorSelection(cohortPath);
                } else {
                    // Throwing an exception here will fail the Future.
                    throw new IllegalArgumentException(String.format("%s: Invalid reply type %s",
                            identifier, serializedReadyReply.getClass()));
                }
            }
        }, TransactionProxy.SAME_FAILURE_TRANSFORMER, actorContext.getClientDispatcher());
    }

    protected String deserializeCohortPath(String cohortPath) {
        return cohortPath;
    }

    private void batchModification(Modification modification) {
        if(batchedModifications == null) {
            batchedModifications = new BatchedModifications(identifier.toString(), remoteTransactionVersion,
                    transactionChainId);
        }

        batchedModifications.addModification(modification);

        if(batchedModifications.getModifications().size() >=
                actorContext.getDatastoreContext().getShardBatchedModificationCount()) {
            sendAndRecordBatchedModifications();
        }
    }

    private void sendAndRecordBatchedModifications() {
        Future<Object> sentFuture = sendBatchedModifications();
        if(sentFuture != null) {
            recordedOperationFutures.add(sentFuture);
        }
    }

    protected Future<Object> sendBatchedModifications() {
        return sendBatchedModifications(false);
    }

    protected Future<Object> sendBatchedModifications(boolean ready) { Future<Object> sent = null;
        if(batchedModifications != null) {
            if(LOG.isDebugEnabled()) {
                LOG.debug("Tx {} sending {} batched modifications, ready: {}", identifier,
                        batchedModifications.getModifications().size(), ready);
            }

            batchedModifications.setReady(ready);
            sent = executeOperationAsync(batchedModifications);

            batchedModifications = new BatchedModifications(identifier.toString(), remoteTransactionVersion,
                    transactionChainId);
        }

        return sent;
    }

    @Override
    public void deleteData(YangInstanceIdentifier path) {
        LOG.debug("Tx {} deleteData called path = {}", identifier, path);

        batchModification(new DeleteModification(path));
    }

    @Override
    public void mergeData(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
        LOG.debug("Tx {} mergeData called path = {}", identifier, path);

        batchModification(new MergeModification(path, data));
    }

    @Override
    public void writeData(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
        LOG.debug("Tx {} writeData called path = {}", identifier, path);

        batchModification(new WriteModification(path, data));
    }

    @Override
    public void readData(
            final YangInstanceIdentifier path,final SettableFuture<Optional<NormalizedNode<?, ?>>> returnFuture ) {

        LOG.debug("Tx {} readData called path = {}", identifier, path);

        // Send the remaining batched modifications if any.

        sendAndRecordBatchedModifications();

        // If there were any previous recorded put/merge/delete operation reply Futures then we
        // must wait for them to successfully complete. This is necessary to honor the read
        // uncommitted semantics of the public API contract. If any one fails then fail the read.

        if(recordedOperationFutures.isEmpty()) {
            finishReadData(path, returnFuture);
        } else {
            LOG.debug("Tx {} readData: verifying {} previous recorded operations",
                    identifier, recordedOperationFutures.size());

            // Note: we make a copy of recordedOperationFutures to be on the safe side in case
            // Futures#sequence accesses the passed List on a different thread, as
            // recordedOperationFutures is not synchronized.

            Future<Iterable<Object>> combinedFutures = akka.dispatch.Futures.sequence(
                    Lists.newArrayList(recordedOperationFutures),
                    actorContext.getClientDispatcher());

            OnComplete<Iterable<Object>> onComplete = new OnComplete<Iterable<Object>>() {
                @Override
                public void onComplete(Throwable failure, Iterable<Object> notUsed)
                        throws Throwable {
                    if(failure != null) {
                        LOG.debug("Tx {} readData: a recorded operation failed: {}",
                                identifier, failure);
                        returnFuture.setException(new ReadFailedException(
                                "The read could not be performed because a previous put, merge,"
                                + "or delete operation failed", failure));
                    } else {
                        finishReadData(path, returnFuture);
                    }
                }
            };

            combinedFutures.onComplete(onComplete, actorContext.getClientDispatcher());
        }

    }

    private void finishReadData(final YangInstanceIdentifier path,
            final SettableFuture<Optional<NormalizedNode<?, ?>>> returnFuture) {

        LOG.debug("Tx {} finishReadData called path = {}", identifier, path);

        OnComplete<Object> onComplete = new OnComplete<Object>() {
            @Override
            public void onComplete(Throwable failure, Object readResponse) throws Throwable {
                if(failure != null) {
                    LOG.debug("Tx {} read operation failed: {}", identifier, failure);
                    returnFuture.setException(new ReadFailedException(
                            "Error reading data for path " + path, failure));

                } else {
                    LOG.debug("Tx {} read operation succeeded", identifier, failure);

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

        LOG.debug("Tx {} dataExists called path = {}", identifier, path);

        // Send the remaining batched modifications if any.

        sendAndRecordBatchedModifications();

        // If there were any previous recorded put/merge/delete operation reply Futures then we
        // must wait for them to successfully complete. This is necessary to honor the read
        // uncommitted semantics of the public API contract. If any one fails then fail this
        // request.

        if(recordedOperationFutures.isEmpty()) {
            finishDataExists(path, returnFuture);
        } else {
            LOG.debug("Tx {} dataExists: verifying {} previous recorded operations",
                    identifier, recordedOperationFutures.size());

            // Note: we make a copy of recordedOperationFutures to be on the safe side in case
            // Futures#sequence accesses the passed List on a different thread, as
            // recordedOperationFutures is not synchronized.

            Future<Iterable<Object>> combinedFutures = akka.dispatch.Futures.sequence(
                    Lists.newArrayList(recordedOperationFutures),
                    actorContext.getClientDispatcher());
            OnComplete<Iterable<Object>> onComplete = new OnComplete<Iterable<Object>>() {
                @Override
                public void onComplete(Throwable failure, Iterable<Object> notUsed)
                        throws Throwable {
                    if(failure != null) {
                        LOG.debug("Tx {} dataExists: a recorded operation failed: {}",
                                identifier, failure);
                        returnFuture.setException(new ReadFailedException(
                                "The data exists could not be performed because a previous "
                                + "put, merge, or delete operation failed", failure));
                    } else {
                        finishDataExists(path, returnFuture);
                    }
                }
            };

            combinedFutures.onComplete(onComplete, actorContext.getClientDispatcher());
        }
    }

    private void finishDataExists(final YangInstanceIdentifier path,
            final SettableFuture<Boolean> returnFuture) {

        LOG.debug("Tx {} finishDataExists called path = {}", identifier, path);

        OnComplete<Object> onComplete = new OnComplete<Object>() {
            @Override
            public void onComplete(Throwable failure, Object response) throws Throwable {
                if(failure != null) {
                    LOG.debug("Tx {} dataExists operation failed: {}", identifier, failure);
                    returnFuture.setException(new ReadFailedException(
                            "Error checking data exists for path " + path, failure));
                } else {
                    LOG.debug("Tx {} dataExists operation succeeded", identifier, failure);

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
