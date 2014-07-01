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
import akka.actor.ActorSystem;
import akka.util.Timeout;
import org.opendaylight.controller.cluster.datastore.messages.FindPrimary;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryFound;
import org.opendaylight.controller.cluster.datastore.messages.RegisterChangeListener;
import org.opendaylight.controller.cluster.datastore.messages.RegisterChangeListenerReply;
import org.opendaylight.controller.cluster.datastore.messages.UpdateSchemaContext;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;

import static akka.pattern.Patterns.ask;

/**
 *
 */
public class DistributedDataStore implements DOMStore, SchemaContextListener {

    private static final Logger
        LOG = LoggerFactory.getLogger(DistributedDataStore.class);

    final FiniteDuration ASK_DURATION = Duration.create(5, TimeUnit.SECONDS);
    final Duration AWAIT_DURATION = Duration.create(5, TimeUnit.SECONDS);

    private final ActorRef shardManager;
    private final ActorSystem actorSystem;
    private final String type;


    public DistributedDataStore(ActorSystem actorSystem, String type) {
        this.actorSystem = actorSystem;
        this.type = type;
        shardManager = actorSystem.actorOf(ShardManager.props(type));
    }

    @Override
    public <L extends AsyncDataChangeListener<InstanceIdentifier, NormalizedNode<?, ?>>> ListenerRegistration<L> registerChangeListener(
        InstanceIdentifier path, L listener,
        AsyncDataBroker.DataChangeScope scope) {

        ActorSelection primary = findPrimary();

        ActorRef dataChangeListenerActor = actorSystem.actorOf(DataChangeListener.props());

        Object result =
            getResult(primary, new RegisterChangeListener(path, dataChangeListenerActor.path(),
                AsyncDataBroker.DataChangeScope.BASE), ASK_DURATION);

        RegisterChangeListenerReply reply = (RegisterChangeListenerReply) result;
        return new ListenerRegistrationProxy(reply.getListenerRegistrationPath());
    }

    private ActorSelection findPrimary() {
        Object result = getResult(shardManager, new FindPrimary(Shard.DEFAULT_NAME), ASK_DURATION);

        if(result instanceof PrimaryFound){
            PrimaryFound found = (PrimaryFound) result;
            LOG.error("Primary found {}", found.getPrimaryPath());

            return actorSystem.actorSelection(found.getPrimaryPath());
        }
        throw new RuntimeException("primary was not found");
    }

    private Object getResult(ActorRef actor, Object message, FiniteDuration duration){
        Future<Object> future =
            ask(actor, message, new Timeout(duration));

        try {
            return Await.result(future, AWAIT_DURATION);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Object getResult(ActorSelection actor, Object message, FiniteDuration duration){
        Future<Object> future =
            ask(actor, message, new Timeout(duration));

        try {
            return Await.result(future, AWAIT_DURATION);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public DOMStoreTransactionChain createTransactionChain() {
        return new TransactionChainProxy();
    }

    @Override
    public DOMStoreReadTransaction newReadOnlyTransaction() {
        return new TransactionProxy();
    }

    @Override
    public DOMStoreWriteTransaction newWriteOnlyTransaction() {
        return new TransactionProxy();
    }

    @Override
    public DOMStoreReadWriteTransaction newReadWriteTransaction() {
        return new TransactionProxy();
    }

    @Override public void onGlobalContextUpdated(SchemaContext schemaContext) {
        shardManager.tell(new UpdateSchemaContext(schemaContext), null);
    }
}
