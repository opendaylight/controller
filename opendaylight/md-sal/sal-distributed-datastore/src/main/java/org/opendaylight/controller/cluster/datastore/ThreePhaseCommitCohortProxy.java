/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSelection;
import akka.dispatch.OnComplete;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.opendaylight.controller.cluster.datastore.messages.AbortTransaction;
import org.opendaylight.controller.cluster.datastore.messages.AbortTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.CanCommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CanCommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;

/**
 * ThreePhaseCommitCohortProxy represents a set of remote cohort proxies
 */
public class ThreePhaseCommitCohortProxy extends AbstractThreePhaseCommitCohort<ActorSelection> {

    private static final Logger LOG = LoggerFactory.getLogger(ThreePhaseCommitCohortProxy.class);

    private static final MessageSupplier COMMIT_MESSAGE_SUPPLIER = new MessageSupplier() {
        @Override
        public Object newMessage(String transactionId, short version) {
            return new CommitTransaction(transactionId, version).toSerializable();
        }

        @Override
        public boolean isSerializedReplyType(Object reply) {
            return CommitTransactionReply.isSerializedType(reply);
        }
    };

    private static final MessageSupplier ABORT_MESSAGE_SUPPLIER = new MessageSupplier() {
        @Override
        public Object newMessage(String transactionId, short version) {
            return new AbortTransaction(transactionId, version).toSerializable();
        }

        @Override
        public boolean isSerializedReplyType(Object reply) {
            return AbortTransactionReply.isSerializedType(reply);
        }
    };

    private final ActorContext actorContext;
    private final List<CohortInfo> cohorts;
    private final SettableFuture<Void> cohortsResolvedFuture = SettableFuture.create();
    private final String transactionId;
    private volatile OperationCallback commitOperationCallback;

    public ThreePhaseCommitCohortProxy(ActorContext actorContext, List<CohortInfo> cohorts, String transactionId) {
        this.actorContext = actorContext;
        this.cohorts = cohorts;
        this.transactionId = transactionId;

        if(cohorts.isEmpty()) {
            cohortsResolvedFuture.set(null);
        }
    }

    private ListenableFuture<Void> resolveCohorts() {
        if(cohortsResolvedFuture.isDone()) {
            return cohortsResolvedFuture;
        }

        final AtomicInteger completed = new AtomicInteger(cohorts.size());
        for(final CohortInfo info: cohorts) {
            info.getActorFuture().onComplete(new OnComplete<ActorSelection>() {
                @Override
                public void onComplete(Throwable failure, ActorSelection actor)  {
                    synchronized(completed) {
                        boolean done = completed.decrementAndGet() == 0;
                        if(failure != null) {
                            LOG.debug("Tx {}: a cohort Future failed", transactionId, failure);
                            cohortsResolvedFuture.setException(failure);
                        } else if(!cohortsResolvedFuture.isDone()) {
                            LOG.debug("Tx {}: cohort actor {} resolved", transactionId, actor);

                            info.setResolvedActor(actor);
                            if(done) {
                                LOG.debug("Tx {}: successfully resolved all cohort actors", transactionId);
                                cohortsResolvedFuture.set(null);
                            }
                        }
                    }
                }
            }, actorContext.getClientDispatcher());
        }

        return cohortsResolvedFuture;
    }

    @Override
    public ListenableFuture<Boolean> canCommit() {
        LOG.debug("Tx {} canCommit", transactionId);

        final SettableFuture<Boolean> returnFuture = SettableFuture.create();

        // The first phase of canCommit is to gather the list of cohort actor paths that will
        // participate in the commit. buildCohortPathsList combines the cohort path Futures into
        // one Future which we wait on asynchronously here. The cohort actor paths are
        // extracted from ReadyTransactionReply messages by the Futures that were obtained earlier
        // and passed to us from upstream processing. If any one fails then  we'll fail canCommit.

        Futures.addCallback(resolveCohorts(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void notUsed) {
                finishCanCommit(returnFuture);
            }

            @Override
            public void onFailure(Throwable failure) {
                returnFuture.setException(failure);
            }
        });

        return returnFuture;
    }

    private void finishCanCommit(final SettableFuture<Boolean> returnFuture) {
        LOG.debug("Tx {} finishCanCommit", transactionId);

        // For empty transactions return immediately
        if(cohorts.size() == 0){
            LOG.debug("Tx {}: canCommit returning result true", transactionId);
            returnFuture.set(Boolean.TRUE);
            return;
        }

        commitOperationCallback = new TransactionRateLimitingCallback(actorContext);
        commitOperationCallback.run();

        final Iterator<CohortInfo> iterator = cohorts.iterator();

        final OnComplete<Object> onComplete = new OnComplete<Object>() {
            @Override
            public void onComplete(Throwable failure, Object response) {
                if (failure != null) {
                    LOG.debug("Tx {}: a canCommit cohort Future failed", transactionId, failure);

                    returnFuture.setException(failure);
                    commitOperationCallback.failure();
                    return;
                }

                // Only the first call to pause takes effect - subsequent calls before resume are no-ops. So
                // this means we'll only time the first transaction canCommit which should be fine.
                commitOperationCallback.pause();

                boolean result = true;
                if (CanCommitTransactionReply.isSerializedType(response)) {
                    CanCommitTransactionReply reply = CanCommitTransactionReply.fromSerializable(response);

                    LOG.debug("Tx {}: received {}", transactionId, response);

                    if (!reply.getCanCommit()) {
                        result = false;
                    }
                } else {
                    LOG.error("Unexpected response type {}", response.getClass());
                    returnFuture.setException(new IllegalArgumentException(
                            String.format("Unexpected response type %s", response.getClass())));
                    return;
                }

                if(iterator.hasNext() && result) {
                    sendCanCommitTransaction(iterator.next(), this);
                } else {
                    LOG.debug("Tx {}: canCommit returning result: {}", transactionId, result);
                    returnFuture.set(Boolean.valueOf(result));
                }

            }
        };

        sendCanCommitTransaction(iterator.next(), onComplete);
    }

    private void sendCanCommitTransaction(CohortInfo toCohortInfo, OnComplete<Object> onComplete) {
        CanCommitTransaction message = new CanCommitTransaction(transactionId, toCohortInfo.getActorVersion());

        if(LOG.isDebugEnabled()) {
            LOG.debug("Tx {}: sending {} to {}", transactionId, message, toCohortInfo.getResolvedActor());
        }

        Future<Object> future = actorContext.executeOperationAsync(toCohortInfo.getResolvedActor(),
                message.toSerializable(), actorContext.getTransactionCommitOperationTimeout());
        future.onComplete(onComplete, actorContext.getClientDispatcher());
    }

    private Future<Iterable<Object>> invokeCohorts(MessageSupplier messageSupplier) {
        List<Future<Object>> futureList = Lists.newArrayListWithCapacity(cohorts.size());
        for(CohortInfo cohort : cohorts) {
            Object message = messageSupplier.newMessage(transactionId, cohort.getActorVersion());

            if(LOG.isDebugEnabled()) {
                LOG.debug("Tx {}: Sending {} to cohort {}", transactionId, message , cohort);
            }

            futureList.add(actorContext.executeOperationAsync(cohort.getResolvedActor(), message,
                    actorContext.getTransactionCommitOperationTimeout()));
        }

        return akka.dispatch.Futures.sequence(futureList, actorContext.getClientDispatcher());
    }

    @Override
    public ListenableFuture<Void> preCommit() {
        // We don't need to do anything here - preCommit is done atomically with the commit phase
        // by the shard.
        return IMMEDIATE_VOID_SUCCESS;
    }

    @Override
    public ListenableFuture<Void> abort() {
        // Note - we pass false for propagateException. In the front-end data broker, this method
        // is called when one of the 3 phases fails with an exception. We'd rather have that
        // original exception propagated to the client. If our abort fails and we propagate the
        // exception then that exception will supersede and suppress the original exception. But
        // it's the original exception that is the root cause and of more interest to the client.

        return voidOperation("abort", ABORT_MESSAGE_SUPPLIER,
                AbortTransactionReply.class, false, OperationCallback.NO_OP_CALLBACK);
    }

    @Override
    public ListenableFuture<Void> commit() {
        OperationCallback operationCallback = commitOperationCallback != null ? commitOperationCallback :
            OperationCallback.NO_OP_CALLBACK;

        return voidOperation("commit", COMMIT_MESSAGE_SUPPLIER,
                CommitTransactionReply.class, true, operationCallback);
    }

    private static boolean successfulFuture(ListenableFuture<Void> future) {
        if(!future.isDone()) {
            return false;
        }

        try {
            future.get();
            return true;
        } catch(Exception e) {
            return false;
        }
    }

    private ListenableFuture<Void> voidOperation(final String operationName,
            final MessageSupplier messageSupplier, final Class<?> expectedResponseClass,
            final boolean propagateException, final OperationCallback callback) {
        LOG.debug("Tx {} {}", transactionId, operationName);

        final SettableFuture<Void> returnFuture = SettableFuture.create();

        // The cohort actor list should already be built at this point by the canCommit phase but,
        // if not for some reason, we'll try to build it here.

        ListenableFuture<Void> future = resolveCohorts();
        if(successfulFuture(future)) {
            finishVoidOperation(operationName, messageSupplier, expectedResponseClass, propagateException,
                    returnFuture, callback);
        } else {
            Futures.addCallback(future, new FutureCallback<Void>() {
                @Override
                public void onSuccess(Void notUsed) {
                    finishVoidOperation(operationName, messageSupplier, expectedResponseClass,
                            propagateException, returnFuture, callback);
                }

                @Override
                public void onFailure(Throwable failure) {
                    LOG.debug("Tx {}: a {} cohort path Future failed: {}", transactionId, operationName, failure);

                    if(propagateException) {
                        returnFuture.setException(failure);
                    } else {
                        returnFuture.set(null);
                    }
                }
            });
        }

        return returnFuture;
    }

    private void finishVoidOperation(final String operationName, MessageSupplier messageSupplier,
                                     final Class<?> expectedResponseClass, final boolean propagateException,
                                     final SettableFuture<Void> returnFuture, final OperationCallback callback) {
        LOG.debug("Tx {} finish {}", transactionId, operationName);

        callback.resume();

        Future<Iterable<Object>> combinedFuture = invokeCohorts(messageSupplier);

        combinedFuture.onComplete(new OnComplete<Iterable<Object>>() {
            @Override
            public void onComplete(Throwable failure, Iterable<Object> responses) throws Throwable {
                Throwable exceptionToPropagate = failure;
                if(exceptionToPropagate == null) {
                    for(Object response: responses) {
                        if(!response.getClass().equals(expectedResponseClass)) {
                            exceptionToPropagate = new IllegalArgumentException(
                                    String.format("Unexpected response type %s", response.getClass()));
                            break;
                        }
                    }
                }

                if(exceptionToPropagate != null) {
                    LOG.debug("Tx {}: a {} cohort Future failed", transactionId, operationName, exceptionToPropagate);
                    if(propagateException) {
                        // We don't log the exception here to avoid redundant logging since we're
                        // propagating to the caller in MD-SAL core who will log it.
                        returnFuture.setException(exceptionToPropagate);
                    } else {
                        // Since the caller doesn't want us to propagate the exception we'll also
                        // not log it normally. But it's usually not good to totally silence
                        // exceptions so we'll log it to debug level.
                        returnFuture.set(null);
                    }

                    callback.failure();
                } else {
                    LOG.debug("Tx {}: {} succeeded", transactionId, operationName);

                    returnFuture.set(null);

                    callback.success();
                }
            }
        }, actorContext.getClientDispatcher());
    }

    @Override
    List<Future<ActorSelection>> getCohortFutures() {
        List<Future<ActorSelection>> cohortFutures = new ArrayList<>(cohorts.size());
        for(CohortInfo info: cohorts) {
            cohortFutures.add(info.getActorFuture());
        }

        return cohortFutures;
    }

    static class CohortInfo {
        private final Future<ActorSelection> actorFuture;
        private volatile ActorSelection resolvedActor;
        private final Supplier<Short> actorVersionSupplier;

        CohortInfo(Future<ActorSelection> actorFuture, Supplier<Short> actorVersionSupplier) {
            this.actorFuture = actorFuture;
            this.actorVersionSupplier = actorVersionSupplier;
        }

        Future<ActorSelection> getActorFuture() {
            return actorFuture;
        }

        ActorSelection getResolvedActor() {
            return resolvedActor;
        }

        void setResolvedActor(ActorSelection resolvedActor) {
            this.resolvedActor = resolvedActor;
        }

        short getActorVersion() {
            Preconditions.checkState(resolvedActor != null,
                    "getActorVersion cannot be called until the actor is resolved");
            return actorVersionSupplier.get();
        }
    }

    private interface MessageSupplier {
        Object newMessage(String transactionId, short version);
        boolean isSerializedReplyType(Object reply);
    }
}
