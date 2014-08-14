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

import java.util.Collections;
import java.util.List;

/**
 * ThreePhaseCommitCohortProxy represents a set of remote cohort proxies
 */
public class ThreePhaseCommitCohortProxy implements DOMStoreThreePhaseCommitCohort{

    private static final Logger LOG = LoggerFactory.getLogger(DistributedDataStore.class);

    private final ActorContext actorContext;
    private final List<ActorPath> cohortPaths;
    private final String transactionId;

    public ThreePhaseCommitCohortProxy(ActorContext actorContext, List<ActorPath> cohortPaths,
            String transactionId) {
        this.actorContext = actorContext;
        this.cohortPaths = cohortPaths;
        this.transactionId = transactionId;
    }

    @Override
    public ListenableFuture<Boolean> canCommit() {
        LOG.debug("txn {} canCommit", transactionId);

        Future<Iterable<Object>> combinedFuture =
                invokeCohorts(new CanCommitTransaction().toSerializable());

        final SettableFuture<Boolean> returnFuture = SettableFuture.create();

        combinedFuture.onComplete(new OnComplete<Iterable<Object>>() {
            @Override
            public void onComplete(Throwable failure, Iterable<Object> responses) throws Throwable {
                if(failure != null) {
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

                returnFuture.set(Boolean.valueOf(result));
            }
        }, actorContext.getActorSystem().dispatcher());

        return returnFuture;
    }

    private Future<Iterable<Object>> invokeCohorts(Object message) {
        List<Future<Object>> futureList = Lists.newArrayListWithCapacity(cohortPaths.size());
        for(ActorPath actorPath : cohortPaths) {

            LOG.debug("txn {} Sending {} to {}", transactionId, message, actorPath);

            ActorSelection cohort = actorContext.actorSelection(actorPath);

            futureList.add(actorContext.executeRemoteOperationAsync(cohort, message,
                    ActorContext.ASK_DURATION));
        }

        return Futures.sequence(futureList, actorContext.getActorSystem().dispatcher());
    }

    @Override
    public ListenableFuture<Void> preCommit() {
        LOG.debug("txn {} preCommit", transactionId);
        return voidOperation(new PreCommitTransaction().toSerializable(),
                PreCommitTransactionReply.SERIALIZABLE_CLASS, true);
    }

    @Override
    public ListenableFuture<Void> abort() {
        LOG.debug("txn {} abort", transactionId);

        // Note - we pass false for propagateException. In the front-end data broker, this method
        // is called when one of the 3 phases fails with an exception. We'd rather have that
        // original exception propagated to the client. If our abort fails and we propagate the
        // exception then that exception will supersede and suppress the original exception. But
        // it's the original exception that is the root cause and of more interest to the client.

        return voidOperation(new AbortTransaction().toSerializable(),
                AbortTransactionReply.SERIALIZABLE_CLASS, false);
    }

    @Override
    public ListenableFuture<Void> commit() {
        LOG.debug("txn {} commit", transactionId);
        return voidOperation(new CommitTransaction().toSerializable(),
                CommitTransactionReply.SERIALIZABLE_CLASS, true);
    }

    private ListenableFuture<Void> voidOperation(final Object message,
            final Class<?> expectedResponseClass, final boolean propagateException) {

        Future<Iterable<Object>> combinedFuture = invokeCohorts(message);

        final SettableFuture<Void> returnFuture = SettableFuture.create();

        combinedFuture.onComplete(new OnComplete<Iterable<Object>>() {
            @Override
            public void onComplete(Throwable failure, Iterable<Object> responses) throws Throwable {

                Throwable exceptionToPropagate = failure;
                if(exceptionToPropagate == null) {
                    for(Object response: responses) {
                        if(!response.getClass().equals(expectedResponseClass)) {
                            LOG.error("Unexpected response type {}", response.getClass());
                            exceptionToPropagate = new IllegalArgumentException(
                                    String.format("Unexpected response type {}",
                                            response.getClass()));
                            break;
                        }
                    }
                }

                if(exceptionToPropagate != null) {
                    if(propagateException) {
                        returnFuture.setException(exceptionToPropagate);
                    } else {
                        LOG.debug(String.format("%s failed",  message.getClass().getSimpleName()),
                                exceptionToPropagate);
                        returnFuture.set(null);
                    }
                } else {
                    returnFuture.set(null);
                }
            }
        }, actorContext.getActorSystem().dispatcher());

        return returnFuture;
    }

    public List<ActorPath> getCohortPaths() {
        return Collections.unmodifiableList(this.cohortPaths);
    }
}
