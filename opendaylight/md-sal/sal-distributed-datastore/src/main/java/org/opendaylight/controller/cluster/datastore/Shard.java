/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Creator;
import akka.persistence.Persistent;
import akka.persistence.UntypedProcessor;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransactionChain;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransactionChainReply;
import org.opendaylight.controller.cluster.datastore.messages.ForwardedCommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.RegisterChangeListener;
import org.opendaylight.controller.cluster.datastore.messages.RegisterChangeListenerReply;
import org.opendaylight.controller.cluster.datastore.messages.UpdateSchemaContext;
import org.opendaylight.controller.cluster.datastore.modification.Modification;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

/**
 * A Shard represents a portion of the logical data tree <br/>
 * <p>
 * Our Shard uses InMemoryDataStore as it's internal representation and delegates all requests it
 * </p>
 */
public class Shard extends UntypedProcessor {

    public static final String DEFAULT_NAME = "default";

    private final ListeningExecutorService storeExecutor =
        MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(2));

    private final InMemoryDOMDataStore store;

    private final Map<Modification, DOMStoreThreePhaseCommitCohort>
        modificationToCohort = new HashMap<>();

    private final LoggingAdapter log =
        Logging.getLogger(getContext().system(), this);

    private Shard(String name) {
        store = new InMemoryDOMDataStore(name, storeExecutor);
    }

    public static Props props(final String name) {
        return Props.create(new Creator<Shard>() {

            @Override
            public Shard create() throws Exception {
                return new Shard(name);
            }

        });
    }

    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof CreateTransactionChain) {
            createTransactionChain();
        } else if (message instanceof RegisterChangeListener) {
            registerChangeListener((RegisterChangeListener) message);
        } else if (message instanceof UpdateSchemaContext) {
            updateSchemaContext((UpdateSchemaContext) message);
        } else if (message instanceof ForwardedCommitTransaction) {
            handleForwardedCommit((ForwardedCommitTransaction) message);
        } else if (message instanceof Persistent) {
            commit((Persistent) message);
        }
    }

    private void commit(Persistent message) {
        Modification modification = (Modification) message.payload();
        DOMStoreThreePhaseCommitCohort cohort =
            modificationToCohort.remove(modification);
        if (cohort == null) {
            log.error(
                "Could not find cohort for modification : " + modification);
            return;
        }
        final ListenableFuture<Void> future = cohort.commit();
        final ActorRef sender = getSender();
        final ActorRef self = getSelf();
        future.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    future.get();
                    sender.tell(new CommitTransactionReply(), self);
                } catch (InterruptedException | ExecutionException e) {
                    log.error(e, "An exception happened when committing");
                }
            }
        }, getContext().dispatcher());
    }

    private void handleForwardedCommit(ForwardedCommitTransaction message) {
        log.info("received forwarded transaction");
        modificationToCohort
            .put(message.getModification(), message.getCohort());
        getSelf().forward(Persistent.create(message.getModification()),
            getContext());
    }

    private void updateSchemaContext(UpdateSchemaContext message) {
        store.onGlobalContextUpdated(message.getSchemaContext());
    }

    private void registerChangeListener(
        RegisterChangeListener registerChangeListener) {

        ActorSelection listenerRegistrationActor = getContext()
            .system().actorSelection(registerChangeListener.getDataChangeListenerPath());

        AsyncDataChangeListener<InstanceIdentifier, NormalizedNode<?, ?>>
            listener = new ListenerProxy(listenerRegistrationActor);

        org.opendaylight.yangtools.concepts.ListenerRegistration<AsyncDataChangeListener<InstanceIdentifier, NormalizedNode<?, ?>>>
            registration =
            store.registerChangeListener(registerChangeListener.getPath(),
                listener, registerChangeListener.getScope());
        ActorRef listenerRegistration =
            getContext().actorOf(ListenerRegistration.props(registration));
        getSender()
            .tell(new RegisterChangeListenerReply(listenerRegistration.path()),
                getSelf());
    }

    private void createTransactionChain() {
        DOMStoreTransactionChain chain = store.createTransactionChain();
        ActorRef transactionChain =
            getContext().actorOf(ShardTransactionChain.props(chain));
        getSender()
            .tell(new CreateTransactionChainReply(transactionChain.path()),
                getSelf());
    }
}
