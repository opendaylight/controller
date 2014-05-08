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

import java.util.concurrent.ExecutionException;

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
        return Props.create(new Creator<ThreePhaseCommitCohort>() {
            @Override
            public ThreePhaseCommitCohort create() throws Exception {
                return new ThreePhaseCommitCohort(cohort, shardActor,
                    modification);
            }
        });
    }


    @Override
    public void handleReceive(Object message) throws Exception {
        if (message instanceof CanCommitTransaction) {
            canCommit((CanCommitTransaction) message);
        } else if (message instanceof PreCommitTransaction) {
            preCommit((PreCommitTransaction) message);
        } else if (message instanceof CommitTransaction) {
            commit((CommitTransaction) message);
        } else if (message instanceof AbortTransaction) {
            abort((AbortTransaction) message);
        }
    }

    private void abort(AbortTransaction message) {
        final ListenableFuture<Void> future = cohort.abort();
        final ActorRef sender = getSender();
        final ActorRef self = getSelf();

        future.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    future.get();
                    sender.tell(new AbortTransactionReply(), self);
                } catch (InterruptedException | ExecutionException e) {
                    log.error(e, "An exception happened when aborting");
                }
            }
        }, getContext().dispatcher());
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

        future.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    future.get();
                    sender.tell(new PreCommitTransactionReply(), self);
                } catch (InterruptedException | ExecutionException e) {
                    log.error(e, "An exception happened when preCommitting");
                }
            }
        }, getContext().dispatcher());

    }

    private void canCommit(CanCommitTransaction message) {
        final ListenableFuture<Boolean> future = cohort.canCommit();
        final ActorRef sender = getSender();
        final ActorRef self = getSelf();

        future.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    Boolean canCommit = future.get();
                    sender.tell(new CanCommitTransactionReply(canCommit), self);
                } catch (InterruptedException | ExecutionException e) {
                    log.error(e, "An exception happened when aborting");
                }
            }
        }, getContext().dispatcher());

    }
}
