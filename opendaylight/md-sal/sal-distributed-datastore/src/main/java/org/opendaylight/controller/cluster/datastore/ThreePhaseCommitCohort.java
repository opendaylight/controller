/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Creator;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.opendaylight.controller.cluster.datastore.messages.AbortTransaction;
import org.opendaylight.controller.cluster.datastore.messages.AbortTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.CanCommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CanCommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.ForwardedCommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.PreCommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.PreCommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.modification.CompositeModification;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;

public class ThreePhaseCommitCohort extends AbstractUntypedActor {
    private final DOMStoreThreePhaseCommitCohort cohort;
    private final ActorRef shardActor;
    private final CompositeModification modification;

    public ThreePhaseCommitCohort(DOMStoreThreePhaseCommitCohort cohort,
        ActorRef shardActor, CompositeModification modification) {

        this.cohort = cohort;
        this.shardActor = shardActor;
        this.modification = modification;
    }

    private final LoggingAdapter log =
        Logging.getLogger(getContext().system(), this);

    public static Props props(final DOMStoreThreePhaseCommitCohort cohort,
        final ActorRef shardActor, final CompositeModification modification) {
        return Props.create(new ThreePhaseCommitCohortCreator(cohort, shardActor, modification));
    }

    @Override
    public void handleReceive(Object message) throws Exception {
        if (message.getClass()
            .equals(CanCommitTransaction.SERIALIZABLE_CLASS)) {
            canCommit(new CanCommitTransaction());
        } else if (message.getClass()
            .equals(PreCommitTransaction.SERIALIZABLE_CLASS)) {
            preCommit(new PreCommitTransaction());
        } else if (message.getClass()
            .equals(CommitTransaction.SERIALIZABLE_CLASS)) {
            commit(new CommitTransaction());
        } else if (message.getClass()
            .equals(AbortTransaction.SERIALIZABLE_CLASS)) {
            abort(new AbortTransaction());
        } else {
            unknownMessage(message);
        }
    }

    private void abort(AbortTransaction message) {
        final ListenableFuture<Void> future = cohort.abort();
        final ActorRef sender = getSender();
        final ActorRef self = getSelf();

        Futures.addCallback(future, new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void v) {
                sender
                    .tell(new AbortTransactionReply().toSerializable(),
                        self);
            }

            @Override
            public void onFailure(Throwable t) {
                LOG.error(t, "An exception happened during abort");
                sender
                    .tell(new akka.actor.Status.Failure(t), self);
            }
        });
    }

    private void commit(CommitTransaction message) {
        // Forward the commit to the shard
        log.debug("Forward commit transaction to Shard {} ", shardActor);
        shardActor.forward(new ForwardedCommitTransaction(cohort, modification),
            getContext());

        getContext().parent().tell(PoisonPill.getInstance(), getSelf());

    }

    private void preCommit(PreCommitTransaction message) {
        final ListenableFuture<Void> future = cohort.preCommit();
        final ActorRef sender = getSender();
        final ActorRef self = getSelf();
        Futures.addCallback(future, new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void v) {
                sender
                    .tell(new PreCommitTransactionReply().toSerializable(),
                        self);
            }

            @Override
            public void onFailure(Throwable t) {
                LOG.error(t, "An exception happened during pre-commit");
                sender
                    .tell(new akka.actor.Status.Failure(t), self);
            }
        });

    }

    private void canCommit(CanCommitTransaction message) {
        final ListenableFuture<Boolean> future = cohort.canCommit();
        final ActorRef sender = getSender();
        final ActorRef self = getSelf();
        Futures.addCallback(future, new FutureCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean canCommit) {
                sender.tell(new CanCommitTransactionReply(canCommit)
                    .toSerializable(), self);
            }

            @Override
            public void onFailure(Throwable t) {
                LOG.error(t, "An exception happened during canCommit");
                sender
                    .tell(new akka.actor.Status.Failure(t), self);
            }
        });
    }

    private static class ThreePhaseCommitCohortCreator implements Creator<ThreePhaseCommitCohort> {
        final DOMStoreThreePhaseCommitCohort cohort;
        final ActorRef shardActor;
        final CompositeModification modification;

        ThreePhaseCommitCohortCreator(DOMStoreThreePhaseCommitCohort cohort,
                ActorRef shardActor, CompositeModification modification) {
            this.cohort = cohort;
            this.shardActor = shardActor;
            this.modification = modification;
        }

        @Override
        public ThreePhaseCommitCohort create() throws Exception {
            return new ThreePhaseCommitCohort(cohort, shardActor, modification);
        }
    }
}
