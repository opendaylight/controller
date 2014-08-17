/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorPath;
import akka.actor.ActorSelection;
import akka.dispatch.Futures;
import akka.dispatch.OnComplete;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import org.opendaylight.controller.cluster.datastore.messages.AbortTransaction;
import org.opendaylight.controller.cluster.datastore.messages.AbortTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.CanCommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CanCommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.PreCommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.PreCommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scala.concurrent.Future;
import scala.runtime.AbstractFunction1;

import java.util.Collections;
import java.util.List;

/**
 * ThreePhaseCommitCohortProxy represents a set of remote cohort proxies
 */
public class ThreePhaseCommitCohortProxy implements DOMStoreThreePhaseCommitCohort{

    private static final Logger LOG = LoggerFactory.getLogger(ThreePhaseCommitCohortProxy.class);

    private final ActorContext actorContext;
    private final List<Future<ActorPath>> cohortPathFutures;
    private volatile List<ActorPath> cohortPaths;
    private final String transactionId;

    public ThreePhaseCommitCohortProxy(ActorContext actorContext,
            List<Future<ActorPath>> cohortPathFutures, String transactionId) {
        this.actorContext = actorContext;
        this.cohortPathFutures = cohortPathFutures;
        this.transactionId = transactionId;
    }

    private Future<Void> buildCohortPathsList() {

        Future<Iterable<ActorPath>> combinedFutures = Futures.sequence(cohortPathFutures,
                actorContext.getActorSystem().dispatcher());

        return combinedFutures.transform(new AbstractFunction1<Iterable<ActorPath>, Void>() {
            @Override
            public Void apply(Iterable<ActorPath> paths) {
                cohortPaths = Lists.newArrayList(paths);

                LOG.debug("Tx {} successfully built cohort path list: {}",
                        transactionId, cohortPaths);
                return null;
            }
        }, TransactionProxy.SAME_FAILURE_TRANSFORMER, actorContext.getActorSystem().dispatcher());
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

        buildCohortPathsList().onComplete(new OnComplete<Void>() {
            @Override
            public void onComplete(Throwable failure, Void notUsed) throws Throwable {
                if(failure != null) {
                    LOG.debug("Tx {}: a cohort path Future failed: {}", transactionId, failure);
                    returnFuture.setException(failure);
                } else {
                    finishCanCommit(returnFuture);
                }
            }
        }, actorContext.getActorSystem().dispatcher());

        return returnFuture;
    }

    private void finishCanCommit(final SettableFuture<Boolean> returnFuture) {

        LOG.debug("Tx {} finishCanCommit", transactionId);

        // The last phase of canCommit is to invoke all the cohort actors asynchronously to perform
        // their canCommit processing. If any one fails then we'll fail canCommit.

        Future<Iterable<Object>> combinedFuture =
                invokeCohorts(new CanCommitTransaction().toSerializable());

        combinedFuture.onComplete(new OnComplete<Iterable<Object>>() {
            @Override
            public void onComplete(Throwable failure, Iterable<Object> responses) throws Throwable {
                if(failure != null) {
                    LOG.debug("Tx {}: a canCommit cohort Future failed: {}", transactionId, failure);
                    returnFuture.setException(failure);
                    return;
                }

                boolean result = true;
                for(Object response: responses) {
                    if (response.getClass().equals(CanCommitTransactionReply.SERIALIZABLE_CLASS)) {
                        CanCommitTransactionReply reply =
                                CanCommitTransactionReply.fromSerializable(response);
                        if (!reply.getCanCommit()) {
                            result = false;
                            break;
                        }
                    } else {
                        LOG.error("Unexpected response type {}", response.getClass());
                        returnFuture.setException(new IllegalArgumentException(
                                String.format("Unexpected response type {}", response.getClass())));
                        return;
                    }
                }

                LOG.debug("Tx {}: canCommit returning result: {}", transactionId, result);

                returnFuture.set(Boolean.valueOf(result));
            }
        }, actorContext.getActorSystem().dispatcher());
    }

    private Future<Iterable<Object>> invokeCohorts(Object message) {
        List<Future<Object>> futureList = Lists.newArrayListWithCapacity(cohortPaths.size());
        for(ActorPath actorPath : cohortPaths) {

            LOG.debug("Tx {}: Sending {} to cohort {}", transactionId, message, actorPath);

            ActorSelection cohort = actorContext.actorSelection(actorPath);

            futureList.add(actorContext.executeRemoteOperationAsync(cohort, message,
                    ActorContext.ASK_DURATION));
        }

        return Futures.sequence(futureList, actorContext.getActorSystem().dispatcher());
    }

    @Override
    public ListenableFuture<Void> preCommit() {
        return voidOperation("preCommit",  new PreCommitTransaction().toSerializable(),
                PreCommitTransactionReply.SERIALIZABLE_CLASS, true);
    }

    @Override
    public ListenableFuture<Void> abort() {
        // Note - we pass false for propagateException. In the front-end data broker, this method
        // is called when one of the 3 phases fails with an exception. We'd rather have that
        // original exception propagated to the client. If our abort fails and we propagate the
        // exception then that exception will supersede and suppress the original exception. But
        // it's the original exception that is the root cause and of more interest to the client.

        return voidOperation("abort", new AbortTransaction().toSerializable(),
                AbortTransactionReply.SERIALIZABLE_CLASS, false);
    }

    @Override
    public ListenableFuture<Void> commit() {
        return voidOperation("commit",  new CommitTransaction().toSerializable(),
                CommitTransactionReply.SERIALIZABLE_CLASS, true);
    }

    private ListenableFuture<Void> voidOperation(final String operationName, final Object message,
            final Class<?> expectedResponseClass, final boolean propagateException) {

        LOG.debug("Tx {} {}", transactionId, operationName);

        final SettableFuture<Void> returnFuture = SettableFuture.create();

        // The cohort actor list should already be built at this point by the canCommit phase but,
        // if not for some reason, we'll try to build it here.

        if(cohortPaths != null) {
            finishVoidOperation(operationName, message, expectedResponseClass, propagateException,
                    returnFuture);
        } else {
            buildCohortPathsList().onComplete(new OnComplete<Void>() {
                @Override
                public void onComplete(Throwable failure, Void notUsed) throws Throwable {
                    if(failure != null) {
                        LOG.debug("Tx {}: a {} cohort path Future failed: {}", transactionId,
                                operationName, failure);

                        if(propagateException) {
                            returnFuture.setException(failure);
                        } else {
                            returnFuture.set(null);
                        }
                    } else {
                        finishVoidOperation(operationName, message, expectedResponseClass,
                                propagateException, returnFuture);
                    }
                }
            }, actorContext.getActorSystem().dispatcher());
        }

        return returnFuture;
    }

    private void finishVoidOperation(final String operationName, final Object message,
            final Class<?> expectedResponseClass, final boolean propagateException,
            final SettableFuture<Void> returnFuture) {

        LOG.debug("Tx {} finish {}", transactionId, operationName);

        Future<Iterable<Object>> combinedFuture = invokeCohorts(message);

        combinedFuture.onComplete(new OnComplete<Iterable<Object>>() {
            @Override
            public void onComplete(Throwable failure, Iterable<Object> responses) throws Throwable {

                Throwable exceptionToPropagate = failure;
                if(exceptionToPropagate == null) {
                    for(Object response: responses) {
                        if(!response.getClass().equals(expectedResponseClass)) {
                            exceptionToPropagate = new IllegalArgumentException(
                                    String.format("Unexpected response type {}",
                                            response.getClass()));
                            break;
                        }
                    }
                }

                if(exceptionToPropagate != null) {
                    LOG.debug("Tx {}: a {} cohort Future failed: {}", transactionId,
                            operationName, exceptionToPropagate);

                    if(propagateException) {
                        // We don't log the exception here to avoid redundant logging since we're
                        // propagating to the caller in MD-SAL core who will log it.
                        returnFuture.setException(exceptionToPropagate);
                    } else {
                        // Since the caller doesn't want us to propagate the exception we'll also
                        // not log it normally. But it's usually not good to totally silence
                        // exceptions so we'll log it to debug level.
                        LOG.debug(String.format("%s failed",  message.getClass().getSimpleName()),
                                exceptionToPropagate);
                        returnFuture.set(null);
                    }
                } else {
                    LOG.debug("Tx {}: {} succeeded", transactionId, operationName);
                    returnFuture.set(null);
                }
            }
        }, actorContext.getActorSystem().dispatcher());
    }

    @VisibleForTesting
    List<Future<ActorPath>> getCohortPathFutures() {
        return Collections.unmodifiableList(cohortPathFutures);
    }
}
