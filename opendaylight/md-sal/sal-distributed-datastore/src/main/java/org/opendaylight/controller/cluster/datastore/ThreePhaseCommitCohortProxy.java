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
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import org.opendaylight.controller.cluster.datastore.exceptions.TimeoutException;
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

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

/**
 * ThreePhaseCommitCohortProxy represents a set of remote cohort proxies
 */
public class ThreePhaseCommitCohortProxy implements
    DOMStoreThreePhaseCommitCohort{

    private static final Logger
        LOG = LoggerFactory.getLogger(DistributedDataStore.class);

    private final ActorContext actorContext;
    private final List<ActorPath> cohortPaths;
    private final ExecutorService executor;
    private final String transactionId;


    public ThreePhaseCommitCohortProxy(ActorContext actorContext,
        List<ActorPath> cohortPaths,
        String transactionId,
        ExecutorService executor) {

        this.actorContext = actorContext;
        this.cohortPaths = cohortPaths;
        this.transactionId = transactionId;
        this.executor = executor;
    }

    @Override public ListenableFuture<Boolean> canCommit() {
        Callable<Boolean> call = new Callable() {

            @Override public Boolean call() throws Exception {
            for(ActorPath actorPath : cohortPaths){
                ActorSelection cohort = actorContext.actorSelection(actorPath);

                try {
                    Object response =
                        actorContext.executeRemoteOperation(cohort,
                            new CanCommitTransaction(),
                            ActorContext.ASK_DURATION);

                    if (response instanceof CanCommitTransactionReply) {
                        CanCommitTransactionReply reply =
                            (CanCommitTransactionReply) response;
                        if (!reply.getCanCommit()) {
                            return false;
                        }
                    }
                } catch(RuntimeException e){
                    LOG.error("Unexpected Exception", e);
                    return false;
                }


            }
            return true;
            }
        };

        ListenableFutureTask<Boolean>
            future = ListenableFutureTask.create(call);

        executor.submit(future);

        return future;
    }

    @Override public ListenableFuture<Void> preCommit() {
        return voidOperation(new PreCommitTransaction(), PreCommitTransactionReply.class);
    }

    @Override public ListenableFuture<Void> abort() {
        return voidOperation(new AbortTransaction(), AbortTransactionReply.class);
    }

    @Override public ListenableFuture<Void> commit() {
        return voidOperation(new CommitTransaction(), CommitTransactionReply.class);
    }

    private ListenableFuture<Void> voidOperation(final Object message, final Class expectedResponseClass){
        Callable<Void> call = new Callable<Void>() {

            @Override public Void call() throws Exception {
                for(ActorPath actorPath : cohortPaths){
                    ActorSelection cohort = actorContext.actorSelection(actorPath);

                    try {
                        Object response =
                            actorContext.executeRemoteOperation(cohort,
                                message,
                                ActorContext.ASK_DURATION);

                        if (response != null && !response.getClass()
                            .equals(expectedResponseClass)) {
                            throw new RuntimeException(
                                String.format(
                                    "did not get the expected response \n\t\t expected : %s \n\t\t actual   : %s",
                                    expectedResponseClass.toString(),
                                    response.getClass().toString())
                            );
                        }
                    } catch(TimeoutException e){
                        LOG.error(String.format("A timeout occurred when processing operation : %s", message));
                    }
                }
                return null;
            }
        };

        ListenableFutureTask<Void>
            future = ListenableFutureTask.create(call);

        executor.submit(future);

        return future;

    }

    public List<ActorPath> getCohortPaths() {
        return Collections.unmodifiableList(this.cohortPaths);
    }
}
