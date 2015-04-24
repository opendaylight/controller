/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSelection;
import akka.dispatch.Futures;
import akka.dispatch.OnComplete;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
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
import scala.runtime.AbstractFunction1;

/**
 * ThreePhaseCommitCohortProxy represents a set of remote cohort proxies
 */
public class ThreePhaseCommitCohortProxy extends AbstractThreePhaseCommitCohort<ActorSelection> {

    private static final Logger LOG = LoggerFactory.getLogger(ThreePhaseCommitCohortProxy.class);

    private final ActorContext actorContext;
    private final List<Future<ActorSelection>> cohortFutures;
    private volatile List<ActorSelection> cohorts;
    private final String transactionId;

    public ThreePhaseCommitCohortProxy(ActorContext actorContext,
            List<Future<ActorSelection>> cohortFutures, String transactionId) {
        this.actorContext = actorContext;
        this.cohortFutures = cohortFutures;
        this.transactionId = transactionId;
    }

    private Future<Void> buildCohortList() {

        Future<Iterable<ActorSelection>> combinedFutures = Futures.sequence(cohortFutures,
                actorContext.getClientDispatcher());

        return combinedFutures.transform(new AbstractFunction1<Iterable<ActorSelection>, Void>() {
            @Override
            public Void apply(Iterable<ActorSelection> actorSelections) {
                cohorts = Lists.newArrayList(actorSelections);
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Tx {} successfully built cohort path list: {}",
                        transactionId, cohorts);
                }
                return null;
            }
        }, TransactionReadyReplyMapper.SAME_FAILURE_TRANSFORMER, actorContext.getClientDispatcher());
    }

    @Override
    public ListenableFuture<Boolean> canCommit() {
        if(LOG.isDebugEnabled()) {
            LOG.debug("Tx {} canCommit", transactionId);
        }
        final SettableFuture<Boolean> returnFuture = SettableFuture.create();

        // The first phase of canCommit is to gather the list of cohort actor paths that will
        // participate in the commit. buildCohortPathsList combines the cohort path Futures into
        // one Future which we wait on asynchronously here. The cohort actor paths are
        // extracted from ReadyTransactionReply messages by the Futures that were obtained earlier
        // and passed to us from upstream processing. If any one fails then  we'll fail canCommit.

        buildCohortList().onComplete(new OnComplete<Void>() {
            @Override
            public void onComplete(Throwable failure, Void notUsed) throws Throwable {
                if(failure != null) {
                    if(LOG.isDebugEnabled()) {
                        LOG.debug("Tx {}: a cohort Future failed: {}", transactionId, failure);
                    }
                    returnFuture.setException(failure);
                } else {
                    finishCanCommit(returnFuture);
                }
            }
        }, actorContext.getClientDispatcher());

        return returnFuture;
    }

    private void finishCanCommit(final SettableFuture<Boolean> returnFuture) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("Tx {} finishCanCommit", transactionId);
        }

        // For empty transactions return immediately
        if(cohorts.size() == 0){
            if(LOG.isDebugEnabled()) {
                LOG.debug("Tx {}: canCommit returning result: {}", transactionId, true);
            }
            returnFuture.set(Boolean.TRUE);
            return;
        }

        final Object message = new CanCommitTransaction(transactionId).toSerializable();

        final Iterator<ActorSelection> iterator = cohorts.iterator();

        final OnComplete<Object> onComplete = new OnComplete<Object>() {
            @Override
            public void onComplete(Throwable failure, Object response) throws Throwable {
                if (failure != null) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Tx {}: a canCommit cohort Future failed: {}", transactionId, failure);
                    }
                    returnFuture.setException(failure);
                    return;
                }

                boolean result = true;
                if (response.getClass().equals(CanCommitTransactionReply.SERIALIZABLE_CLASS)) {
                    CanCommitTransactionReply reply =
                            CanCommitTransactionReply.fromSerializable(response);
                    if (!reply.getCanCommit()) {
                        result = false;
                    }
                } else {
                    LOG.error("Unexpected response type {}", response.getClass());
                    returnFuture.setException(new IllegalArgumentException(
                            String.format("Unexpected response type %s", response.getClass())));
                    return;
                }

                if(iterator.hasNext() && result){
                    Future<Object> future = actorContext.executeOperationAsync(iterator.next(), message,
                            actorContext.getTransactionCommitOperationTimeout());
                    future.onComplete(this, actorContext.getClientDispatcher());
                } else {
                    if(LOG.isDebugEnabled()) {
                        LOG.debug("Tx {}: canCommit returning result: {}", transactionId, result);
                    }
                    returnFuture.set(Boolean.valueOf(result));
                }

            }
        };

        Future<Object> future = actorContext.executeOperationAsync(iterator.next(), message,
                actorContext.getTransactionCommitOperationTimeout());
        future.onComplete(onComplete, actorContext.getClientDispatcher());
    }

    private Future<Iterable<Object>> invokeCohorts(Object message) {
        List<Future<Object>> futureList = Lists.newArrayListWithCapacity(cohorts.size());
        for(ActorSelection cohort : cohorts) {
            if(LOG.isDebugEnabled()) {
                LOG.debug("Tx {}: Sending {} to cohort {}", transactionId, message, cohort);
            }
            futureList.add(actorContext.executeOperationAsync(cohort, message, actorContext.getTransactionCommitOperationTimeout()));
        }

        return Futures.sequence(futureList, actorContext.getClientDispatcher());
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

        return voidOperation("abort", new AbortTransaction(transactionId).toSerializable(),
                AbortTransactionReply.SERIALIZABLE_CLASS, false);
    }

    @Override
    public ListenableFuture<Void> commit() {
        OperationCallback operationCallback = cohortFutures.isEmpty() ? OperationCallback.NO_OP_CALLBACK :
                new TransactionRateLimitingCallback(actorContext);

        return voidOperation("commit", new CommitTransaction(transactionId).toSerializable(),
                CommitTransactionReply.SERIALIZABLE_CLASS, true, operationCallback);
    }

    private ListenableFuture<Void> voidOperation(final String operationName, final Object message,
                                                 final Class<?> expectedResponseClass, final boolean propagateException) {
        return voidOperation(operationName, message, expectedResponseClass, propagateException,
                OperationCallback.NO_OP_CALLBACK);
    }

    private ListenableFuture<Void> voidOperation(final String operationName, final Object message,
                                                 final Class<?> expectedResponseClass, final boolean propagateException, final OperationCallback callback) {

        if(LOG.isDebugEnabled()) {
            LOG.debug("Tx {} {}", transactionId, operationName);
        }
        final SettableFuture<Void> returnFuture = SettableFuture.create();

        // The cohort actor list should already be built at this point by the canCommit phase but,
        // if not for some reason, we'll try to build it here.

        if(cohorts != null) {
            finishVoidOperation(operationName, message, expectedResponseClass, propagateException,
                    returnFuture, callback);
        } else {
            buildCohortList().onComplete(new OnComplete<Void>() {
                @Override
                public void onComplete(Throwable failure, Void notUsed) throws Throwable {
                    if(failure != null) {
                        if(LOG.isDebugEnabled()) {
                            LOG.debug("Tx {}: a {} cohort path Future failed: {}", transactionId,
                                operationName, failure);
                        }
                        if(propagateException) {
                            returnFuture.setException(failure);
                        } else {
                            returnFuture.set(null);
                        }
                    } else {
                        finishVoidOperation(operationName, message, expectedResponseClass,
                                propagateException, returnFuture, callback);
                    }
                }
            }, actorContext.getClientDispatcher());
        }

        return returnFuture;
    }

    private void finishVoidOperation(final String operationName, final Object message,
                                     final Class<?> expectedResponseClass, final boolean propagateException,
                                     final SettableFuture<Void> returnFuture, final OperationCallback callback) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("Tx {} finish {}", transactionId, operationName);
        }

        callback.run();

        Future<Iterable<Object>> combinedFuture = invokeCohorts(message);

        combinedFuture.onComplete(new OnComplete<Iterable<Object>>() {
            @Override
            public void onComplete(Throwable failure, Iterable<Object> responses) throws Throwable {

                Throwable exceptionToPropagate = failure;
                if(exceptionToPropagate == null) {
                    for(Object response: responses) {
                        if(!response.getClass().equals(expectedResponseClass)) {
                            exceptionToPropagate = new IllegalArgumentException(
                                    String.format("Unexpected response type %s",
                                            response.getClass()));
                            break;
                        }
                    }
                }

                if(exceptionToPropagate != null) {

                    if(LOG.isDebugEnabled()) {
                        LOG.debug("Tx {}: a {} cohort Future failed: {}", transactionId,
                            operationName, exceptionToPropagate);
                    }
                    if(propagateException) {
                        // We don't log the exception here to avoid redundant logging since we're
                        // propagating to the caller in MD-SAL core who will log it.
                        returnFuture.setException(exceptionToPropagate);
                    } else {
                        // Since the caller doesn't want us to propagate the exception we'll also
                        // not log it normally. But it's usually not good to totally silence
                        // exceptions so we'll log it to debug level.
                        if(LOG.isDebugEnabled()) {
                            LOG.debug(String.format("%s failed", message.getClass().getSimpleName()),
                                exceptionToPropagate);
                        }
                        returnFuture.set(null);
                    }

                    callback.failure();
                } else {

                    if(LOG.isDebugEnabled()) {
                        LOG.debug("Tx {}: {} succeeded", transactionId, operationName);
                    }
                    returnFuture.set(null);

                    callback.success();
                }
            }
        }, actorContext.getClientDispatcher());
    }

    @Override
    List<Future<ActorSelection>> getCohortFutures() {
        return Collections.unmodifiableList(cohortFutures);
    }
}
