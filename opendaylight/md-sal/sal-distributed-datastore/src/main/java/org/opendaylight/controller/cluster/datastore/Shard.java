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
import akka.serialization.Serialization;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard.ShardMBeanFactory;
import org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard.ShardStats;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransactionChain;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransactionChainReply;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.ForwardedCommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.PeerAddressResolved;
import org.opendaylight.controller.cluster.datastore.messages.RegisterChangeListener;
import org.opendaylight.controller.cluster.datastore.messages.RegisterChangeListenerReply;
import org.opendaylight.controller.cluster.datastore.messages.UpdateSchemaContext;
import org.opendaylight.controller.cluster.datastore.modification.Modification;
import org.opendaylight.controller.cluster.datastore.modification.MutableCompositeModification;
import org.opendaylight.controller.cluster.raft.RaftActor;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

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
public class Shard extends RaftActor {

    public static final String DEFAULT_NAME = "default";

    private final ListeningExecutorService storeExecutor =
        MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(2));

    private final InMemoryDOMDataStore store;

    private final Map<Object, DOMStoreThreePhaseCommitCohort>
        modificationToCohort = new HashMap<>();

    private final LoggingAdapter LOG =
        Logging.getLogger(getContext().system(), this);

    // By default persistent will be true and can be turned off using the system
    // property persistent
    private final boolean persistent;

    private final String name;

    private volatile SchemaContext schemaContext;

    private final ShardStats shardMBean;

    private Shard(String name, Map<String, String> peerAddresses) {
        super(name, peerAddresses);

        this.name = name;

        String setting = System.getProperty("shard.persistent");

        this.persistent = !"false".equals(setting);

        LOG.info("Creating shard : {} persistent : {}", name, persistent);

        store = new InMemoryDOMDataStore(name, storeExecutor);

        shardMBean = ShardMBeanFactory.getShardStatsMBean(name);

    }

    public static Props props(final String name, final Map<String, String> peerAddresses) {
        return Props.create(new Creator<Shard>() {

            @Override
            public Shard create() throws Exception {
                return new Shard(name, peerAddresses);
            }

        });
    }


    @Override public void onReceiveCommand(Object message){
        LOG.debug("Received message {} from {}", message.getClass().toString(), getSender());

        if (message.getClass().equals(CreateTransactionChain.SERIALIZABLE_CLASS)) {
            if(isLeader()) {
                createTransactionChain();
            } else if(getLeader() != null){
                getLeader().forward(message, getContext());
            }
        } else if (message.getClass().equals(RegisterChangeListener.SERIALIZABLE_CLASS)) {
            registerChangeListener(RegisterChangeListener.fromSerializable(getContext().system(), message));
        } else if (message instanceof UpdateSchemaContext) {
            updateSchemaContext((UpdateSchemaContext) message);
        } else if (message instanceof ForwardedCommitTransaction) {
            handleForwardedCommit((ForwardedCommitTransaction) message);
        } else if (message.getClass().equals(CreateTransaction.SERIALIZABLE_CLASS)) {
            if(isLeader()) {
                createTransaction(CreateTransaction.fromSerializable(message));
            } else if(getLeader() != null){
                getLeader().forward(message, getContext());
            }
        } else if (message instanceof PeerAddressResolved){
            PeerAddressResolved resolved = (PeerAddressResolved) message;
            setPeerAddress(resolved.getPeerId(), resolved.getPeerAddress());
        } else {
          super.onReceiveCommand(message);
        }
    }

    private void createTransaction(CreateTransaction createTransaction) {
        DOMStoreReadWriteTransaction transaction =
            store.newReadWriteTransaction();
        String transactionId = "shard-" + createTransaction.getTransactionId();
        LOG.info("Creating transaction : {} " , transactionId);
        ActorRef transactionActor = getContext().actorOf(
            ShardTransaction.props(transaction, getSelf(), schemaContext), transactionId);

        getSender()
            .tell(new CreateTransactionReply(Serialization.serializedActorPath(transactionActor), createTransaction.getTransactionId()).toSerializable(),
                getSelf());
    }

    private void commit(final ActorRef sender, Object serialized) {
        Modification modification = MutableCompositeModification.fromSerializable(serialized, schemaContext);
        DOMStoreThreePhaseCommitCohort cohort =
            modificationToCohort.remove(serialized);
        if (cohort == null) {
            LOG.error(
                "Could not find cohort for modification : " + modification);
            LOG.info("Writing modification using a new transaction");
            modification.apply(store.newReadWriteTransaction());
            return;
        }

        final ListenableFuture<Void> future = cohort.commit();
        shardMBean.incrementCommittedTransactionCount();
        final ActorRef self = getSelf();
        future.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    future.get();

                    if(sender != null) {
                        sender
                            .tell(new CommitTransactionReply().toSerializable(),
                                self);
                    } else {
                        LOG.error("sender is null ???");
                    }
                } catch (InterruptedException | ExecutionException e) {
                    // FIXME : Handle this properly
                    LOG.error(e, "An exception happened when committing");
                }
            }
        }, getContext().dispatcher());
    }

    private void handleForwardedCommit(ForwardedCommitTransaction message) {
        Object serializedModification = message.getModification().toSerializable();

        modificationToCohort
            .put(serializedModification , message.getCohort());

        if(persistent) {
            this.persistData(getSender(), "identifier", new CompositeModificationPayload(serializedModification));
        } else {
            this.commit(getSender(), serializedModification);
        }
    }

    private void updateSchemaContext(UpdateSchemaContext message) {
        this.schemaContext = message.getSchemaContext();
        store.onGlobalContextUpdated(message.getSchemaContext());
    }

    private void registerChangeListener(
        RegisterChangeListener registerChangeListener) {

        LOG.debug("registerDataChangeListener for " + registerChangeListener.getPath());


        ActorSelection dataChangeListenerPath = getContext()
            .system().actorSelection(
                registerChangeListener.getDataChangeListenerPath());

        AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>>
            listener = new DataChangeListenerProxy(schemaContext,dataChangeListenerPath);

        org.opendaylight.yangtools.concepts.ListenerRegistration<AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>>>
            registration =
            store.registerChangeListener(registerChangeListener.getPath(),
                listener, registerChangeListener.getScope());
        ActorRef listenerRegistration =
            getContext().actorOf(
                DataChangeListenerRegistration.props(registration));

        LOG.debug("registerDataChangeListener sending reply, listenerRegistrationPath = " + listenerRegistration.path().toString());

        getSender()
            .tell(new RegisterChangeListenerReply(listenerRegistration.path()).toSerializable(),
                getSelf());
    }

    private void createTransactionChain() {
        DOMStoreTransactionChain chain = store.createTransactionChain();
        ActorRef transactionChain =
            getContext().actorOf(
                ShardTransactionChain.props(chain, schemaContext));
        getSender()
            .tell(new CreateTransactionChainReply(transactionChain.path())
                .toSerializable(),
                getSelf());
    }

    @Override protected void applyState(ActorRef clientActor, String identifier,
        Object data) {

        if(data instanceof CompositeModificationPayload){
            Object modification =
                ((CompositeModificationPayload) data).getModification();
            commit(clientActor, modification);
        } else {
            LOG.error("Unknown state received {}", data);
        }

    }

    @Override protected Object createSnapshot() {
        throw new UnsupportedOperationException("createSnapshot");
    }

    @Override protected void applySnapshot(Object snapshot) {
        throw new UnsupportedOperationException("applySnapshot");
    }

    @Override public String persistenceId() {
        return this.name;
    }
}
