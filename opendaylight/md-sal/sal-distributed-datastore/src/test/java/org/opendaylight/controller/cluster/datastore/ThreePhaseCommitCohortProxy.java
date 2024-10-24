/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.apache.pekko.actor.ActorSelection;
import org.apache.pekko.dispatch.OnComplete;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.AbortTransaction;
import org.opendaylight.controller.cluster.datastore.messages.AbortTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.CanCommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CanCommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.yangtools.yang.common.Empty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;

/**
 * ThreePhaseCommitCohortProxy represents a set of remote cohort proxies.
 */
@Deprecated(since = "9.0.0", forRemoval = true)
final class ThreePhaseCommitCohortProxy implements DOMStoreThreePhaseCommitCohort {
    private static final Logger LOG = LoggerFactory.getLogger(ThreePhaseCommitCohortProxy.class);

    private static final MessageSupplier COMMIT_MESSAGE_SUPPLIER = new MessageSupplier() {
        @Override
        public Object newMessage(final TransactionIdentifier transactionId, final short version) {
            return new CommitTransaction(transactionId, version).toSerializable();
        }

        @Override
        public boolean isSerializedReplyType(final Object reply) {
            return CommitTransactionReply.isSerializedType(reply);
        }
    };

    private static final MessageSupplier ABORT_MESSAGE_SUPPLIER = new MessageSupplier() {
        @Override
        public Object newMessage(final TransactionIdentifier transactionId, final short version) {
            return new AbortTransaction(transactionId, version).toSerializable();
        }

        @Override
        public boolean isSerializedReplyType(final Object reply) {
            return AbortTransactionReply.isSerializedType(reply);
        }
    };

    private static final OperationCallback NO_OP_CALLBACK = new OperationCallback() {
        @Override
        public void run() {
        }

        @Override
        public void success() {
        }

        @Override
        public void failure() {
        }

        @Override
        public void pause() {
        }

        @Override
        public void resume() {
        }
    };


    private final ActorUtils actorUtils;
    private final List<CohortInfo> cohorts;
    private final SettableFuture<Empty> cohortsResolvedFuture = SettableFuture.create();
    private final TransactionIdentifier transactionId;
    private volatile OperationCallback commitOperationCallback;

    ThreePhaseCommitCohortProxy(final ActorUtils actorUtils, final List<CohortInfo> cohorts,
            final TransactionIdentifier transactionId) {
        this.actorUtils = actorUtils;
        this.cohorts = cohorts;
        this.transactionId = requireNonNull(transactionId);

        if (cohorts.isEmpty()) {
            cohortsResolvedFuture.set(Empty.value());
        }
    }

    private ListenableFuture<Empty> resolveCohorts() {
        if (cohortsResolvedFuture.isDone()) {
            return cohortsResolvedFuture;
        }

        final AtomicInteger completed = new AtomicInteger(cohorts.size());
        final Object lock = new Object();
        for (final CohortInfo info: cohorts) {
            info.getActorFuture().onComplete(new OnComplete<ActorSelection>() {
                @Override
                public void onComplete(final Throwable failure, final ActorSelection actor)  {
                    synchronized (lock) {
                        boolean done = completed.decrementAndGet() == 0;
                        if (failure != null) {
                            LOG.debug("Tx {}: a cohort Future failed", transactionId, failure);
                            cohortsResolvedFuture.setException(failure);
                        } else if (!cohortsResolvedFuture.isDone()) {
                            LOG.debug("Tx {}: cohort actor {} resolved", transactionId, actor);

                            info.setResolvedActor(actor);
                            if (done) {
                                LOG.debug("Tx {}: successfully resolved all cohort actors", transactionId);
                                cohortsResolvedFuture.set(Empty.value());
                            }
                        }
                    }
                }
            }, actorUtils.getClientDispatcher());
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

        Futures.addCallback(resolveCohorts(), new FutureCallback<>() {
            @Override
            public void onSuccess(final Empty result) {
                finishCanCommit(returnFuture);
            }

            @Override
            public void onFailure(final Throwable failure) {
                returnFuture.setException(failure);
            }
        }, MoreExecutors.directExecutor());

        return returnFuture;
    }

    private void finishCanCommit(final SettableFuture<Boolean> returnFuture) {
        LOG.debug("Tx {} finishCanCommit", transactionId);

        // For empty transactions return immediately
        if (cohorts.size() == 0) {
            LOG.debug("Tx {}: canCommit returning result true", transactionId);
            returnFuture.set(Boolean.TRUE);
            return;
        }

        commitOperationCallback = new TransactionRateLimitingCallback(actorUtils);
        commitOperationCallback.run();

        final Iterator<CohortInfo> iterator = cohorts.iterator();

        final OnComplete<Object> onComplete = new OnComplete<>() {
            @Override
            public void onComplete(final Throwable failure, final Object response) {
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

                if (iterator.hasNext() && result) {
                    sendCanCommitTransaction(iterator.next(), this);
                } else {
                    LOG.debug("Tx {}: canCommit returning result: {}", transactionId, result);
                    returnFuture.set(result);
                }

            }
        };

        sendCanCommitTransaction(iterator.next(), onComplete);
    }

    private void sendCanCommitTransaction(final CohortInfo toCohortInfo, final OnComplete<Object> onComplete) {
        CanCommitTransaction message = new CanCommitTransaction(transactionId, toCohortInfo.getActorVersion());

        LOG.debug("Tx {}: sending {} to {}", transactionId, message, toCohortInfo.getResolvedActor());

        Future<Object> future = actorUtils.executeOperationAsync(toCohortInfo.getResolvedActor(),
                message.toSerializable(), actorUtils.getTransactionCommitOperationTimeout());
        future.onComplete(onComplete, actorUtils.getClientDispatcher());
    }

    private Future<Iterable<Object>> invokeCohorts(final MessageSupplier messageSupplier) {
        List<Future<Object>> futureList = new ArrayList<>(cohorts.size());
        for (CohortInfo cohort : cohorts) {
            Object message = messageSupplier.newMessage(transactionId, cohort.getActorVersion());

            LOG.debug("Tx {}: Sending {} to cohort {}", transactionId, message , cohort.getResolvedActor());

            futureList.add(actorUtils.executeOperationAsync(cohort.getResolvedActor(), message,
                    actorUtils.getTransactionCommitOperationTimeout()));
        }

        return org.apache.pekko.dispatch.Futures.sequence(futureList, actorUtils.getClientDispatcher());
    }

    @Override
    public ListenableFuture<Empty> preCommit() {
        // We don't need to do anything here - preCommit is done atomically with the commit phase by the shard.
        return Empty.immediateFuture();
    }

    @Override
    public ListenableFuture<Empty> abort() {
        // Note - we pass false for propagateException. In the front-end data broker, this method
        // is called when one of the 3 phases fails with an exception. We'd rather have that
        // original exception propagated to the client. If our abort fails and we propagate the
        // exception then that exception will supersede and suppress the original exception. But
        // it's the original exception that is the root cause and of more interest to the client.

        return operation("abort", Empty.value(), ABORT_MESSAGE_SUPPLIER, AbortTransactionReply.class, false,
            NO_OP_CALLBACK);
    }

    @Override
    public ListenableFuture<? extends CommitInfo> commit() {
        OperationCallback operationCallback = commitOperationCallback != null ? commitOperationCallback :
            NO_OP_CALLBACK;

        return operation("commit", CommitInfo.empty(), COMMIT_MESSAGE_SUPPLIER, CommitTransactionReply.class, true,
            operationCallback);
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private static boolean successfulFuture(final ListenableFuture<?> future) {
        if (!future.isDone()) {
            return false;
        }

        try {
            future.get();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private <T> ListenableFuture<T> operation(final String operationName, final T futureValue,
            final MessageSupplier messageSupplier, final Class<?> expectedResponseClass,
            final boolean propagateException, final OperationCallback callback) {
        LOG.debug("Tx {} {}", transactionId, operationName);

        final SettableFuture<T> returnFuture = SettableFuture.create();

        // The cohort actor list should already be built at this point by the canCommit phase but,
        // if not for some reason, we'll try to build it here.

        ListenableFuture<Empty> future = resolveCohorts();
        if (successfulFuture(future)) {
            finishOperation(operationName, messageSupplier, expectedResponseClass, propagateException, returnFuture,
                futureValue, callback);
        } else {
            Futures.addCallback(future, new FutureCallback<>() {
                @Override
                public void onSuccess(final Empty result) {
                    finishOperation(operationName, messageSupplier, expectedResponseClass, propagateException,
                        returnFuture, futureValue, callback);
                }

                @Override
                public void onFailure(final Throwable failure) {
                    LOG.debug("Tx {}: a {} cohort path Future failed", transactionId, operationName, failure);

                    if (propagateException) {
                        returnFuture.setException(failure);
                    } else {
                        returnFuture.set(futureValue);
                    }
                }
            }, MoreExecutors.directExecutor());
        }

        return returnFuture;
    }

    private <T> void finishOperation(final String operationName, final MessageSupplier messageSupplier,
                                     final Class<?> expectedResponseClass, final boolean propagateException,
                                     final SettableFuture<T> returnFuture, final T futureValue,
                                     final OperationCallback callback) {
        LOG.debug("Tx {} finish {}", transactionId, operationName);

        callback.resume();

        Future<Iterable<Object>> combinedFuture = invokeCohorts(messageSupplier);

        combinedFuture.onComplete(new OnComplete<Iterable<Object>>() {
            @Override
            public void onComplete(final Throwable failure, final Iterable<Object> responses) {
                Throwable exceptionToPropagate = failure;
                if (exceptionToPropagate == null) {
                    for (Object response: responses) {
                        if (!response.getClass().equals(expectedResponseClass)) {
                            exceptionToPropagate = new IllegalArgumentException(
                                    String.format("Unexpected response type %s", response.getClass()));
                            break;
                        }
                    }
                }

                if (exceptionToPropagate != null) {
                    LOG.debug("Tx {}: a {} cohort Future failed", transactionId, operationName, exceptionToPropagate);
                    if (propagateException) {
                        // We don't log the exception here to avoid redundant logging since we're
                        // propagating to the caller in MD-SAL core who will log it.
                        returnFuture.setException(exceptionToPropagate);
                    } else {
                        // Since the caller doesn't want us to propagate the exception we'll also
                        // not log it normally. But it's usually not good to totally silence
                        // exceptions so we'll log it to debug level.
                        returnFuture.set(futureValue);
                    }

                    callback.failure();
                } else {
                    LOG.debug("Tx {}: {} succeeded", transactionId, operationName);

                    returnFuture.set(futureValue);

                    callback.success();
                }
            }
        }, actorUtils.getClientDispatcher());
    }

    static class CohortInfo {
        private final Future<ActorSelection> actorFuture;
        private final Supplier<Short> actorVersionSupplier;

        private volatile ActorSelection resolvedActor;

        CohortInfo(final Future<ActorSelection> actorFuture, final Supplier<Short> actorVersionSupplier) {
            this.actorFuture = actorFuture;
            this.actorVersionSupplier = actorVersionSupplier;
        }

        Future<ActorSelection> getActorFuture() {
            return actorFuture;
        }

        ActorSelection getResolvedActor() {
            return resolvedActor;
        }

        void setResolvedActor(final ActorSelection resolvedActor) {
            this.resolvedActor = resolvedActor;
        }

        short getActorVersion() {
            checkState(resolvedActor != null, "getActorVersion cannot be called until the actor is resolved");
            return actorVersionSupplier.get();
        }
    }

    private interface MessageSupplier {
        Object newMessage(TransactionIdentifier transactionId, short version);

        boolean isSerializedReplyType(Object reply);
    }
}
