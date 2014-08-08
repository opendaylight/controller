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
import com.google.common.util.concurrent.ListeningExecutorService;

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

/**
 * ThreePhaseCommitCohortProxy represents a set of remote cohort proxies
 */
public class ThreePhaseCommitCohortProxy implements
    DOMStoreThreePhaseCommitCohort{

    private static final Logger
        LOG = LoggerFactory.getLogger(DistributedDataStore.class);

    private final ActorContext actorContext;
    private final List<ActorPath> cohortPaths;
    private final ListeningExecutorService executor;
    private final String transactionId;


    public ThreePhaseCommitCohortProxy(ActorContext actorContext,
        List<ActorPath> cohortPaths,
        String transactionId,
        ListeningExecutorService executor) {

        this.actorContext = actorContext;
        this.cohortPaths = cohortPaths;
        this.transactionId = transactionId;
        this.executor = executor;
    }

    @Override public ListenableFuture<Boolean> canCommit() {
        Callable<Boolean> call = new Callable<Boolean>() {

            @Override
            public Boolean call() throws Exception {
                for(ActorPath actorPath : cohortPaths){
                    ActorSelection cohort = actorContext.actorSelection(actorPath);

                    try {
                        Object response =
                                actorContext.executeRemoteOperation(cohort,
                                        new CanCommitTransaction().toSerializable(),
                                        ActorContext.ASK_DURATION);

                        if (response.getClass().equals(CanCommitTransactionReply.SERIALIZABLE_CLASS)) {
                            CanCommitTransactionReply reply =
                                    CanCommitTransactionReply.fromSerializable(response);
                            if (!reply.getCanCommit()) {
                                return false;
                            }
                        }
                    } catch(RuntimeException e){
                        // FIXME : Need to properly handle this
                        LOG.error("Unexpected Exception", e);
                        return false;
                    }
                }

                return true;
            }
        };

        return executor.submit(call);
    }

    @Override public ListenableFuture<Void> preCommit() {
        return voidOperation(new PreCommitTransaction().toSerializable(), PreCommitTransactionReply.SERIALIZABLE_CLASS);
    }

    @Override public ListenableFuture<Void> abort() {
        return voidOperation(new AbortTransaction().toSerializable(), AbortTransactionReply.SERIALIZABLE_CLASS);
    }

    @Override public ListenableFuture<Void> commit() {
        return voidOperation(new CommitTransaction().toSerializable(), CommitTransactionReply.SERIALIZABLE_CLASS);
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

        return executor.submit(call);
    }

    public List<ActorPath> getCohortPaths() {
        return Collections.unmodifiableList(this.cohortPaths);
    }
}
