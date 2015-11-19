/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.PoisonPill;
import akka.dispatch.OnComplete;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.cluster.datastore.exceptions.LocalShardNotFoundException;
import org.opendaylight.controller.cluster.datastore.messages.CloseDataTreeChangeListenerRegistration;
import org.opendaylight.controller.cluster.datastore.messages.RegisterDataTreeChangeListener;
import org.opendaylight.controller.cluster.datastore.messages.RegisterDataTreeChangeListenerReply;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.md.sal.dom.api.ClusteredDOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.yangtools.concepts.AbstractListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;

/**
 * Proxy class for holding required state to lazily instantiate a listener registration with an
 * asynchronously-discovered actor.
 *
 * @param <T> listener type
 */
final class DataTreeChangeListenerProxy<T extends DOMDataTreeChangeListener> extends AbstractListenerRegistration<T> {
    private static final Logger LOG = LoggerFactory.getLogger(DataTreeChangeListenerProxy.class);
    private final ActorRef dataChangeListenerActor;
    private final ActorContext actorContext;

    @GuardedBy("this")
    private ActorSelection listenerRegistrationActor;

    public DataTreeChangeListenerProxy(final ActorContext actorContext, final T listener) {
        super(listener);
        this.actorContext = Preconditions.checkNotNull(actorContext);
        this.dataChangeListenerActor = actorContext.getActorSystem().actorOf(
            DataTreeChangeListenerActor.props(getInstance()).withDispatcher(actorContext.getNotificationDispatcherPath()));
    }

    @Override
    protected synchronized void removeRegistration() {
        if (listenerRegistrationActor != null) {
            listenerRegistrationActor.tell(CloseDataTreeChangeListenerRegistration.getInstance(), ActorRef.noSender());
            listenerRegistrationActor = null;
        }

        dataChangeListenerActor.tell(PoisonPill.getInstance(), ActorRef.noSender());
    }

    void init(final String shardName, final YangInstanceIdentifier treeId) {
        Future<ActorRef> findFuture = actorContext.findLocalShardAsync(shardName);
        findFuture.onComplete(new OnComplete<ActorRef>() {
            @Override
            public void onComplete(final Throwable failure, final ActorRef shard) {
                if (failure instanceof LocalShardNotFoundException) {
                    LOG.debug("No local shard found for {} - DataTreeChangeListener {} at path {} " +
                            "cannot be registered", shardName, getInstance(), treeId);
                } else if (failure != null) {
                    LOG.error("Failed to find local shard {} - DataTreeChangeListener {} at path {} " +
                            "cannot be registered: {}", shardName, getInstance(), treeId, failure);
                } else {
                    doRegistration(shard, treeId);
                }
            }
        }, actorContext.getClientDispatcher());
    }

    private void setListenerRegistrationActor(final ActorSelection actor) {
        if (actor == null) {
            LOG.debug("Ignoring null actor on {}", this);
            return;
        }

        synchronized (this) {
            if (!isClosed()) {
                this.listenerRegistrationActor = actor;
                return;
            }
        }

        // This registration has already been closed, notify the actor
        actor.tell(CloseDataTreeChangeListenerRegistration.getInstance(), null);
    }

    private void doRegistration(final ActorRef shard, final YangInstanceIdentifier path) {

        Future<Object> future = actorContext.executeOperationAsync(shard,
                new RegisterDataTreeChangeListener(path, dataChangeListenerActor,
                        getInstance() instanceof ClusteredDOMDataTreeChangeListener),
                actorContext.getDatastoreContext().getShardInitializationTimeout());

        future.onComplete(new OnComplete<Object>(){
            @Override
            public void onComplete(final Throwable failure, final Object result) {
                if (failure != null) {
                    LOG.error("Failed to register DataTreeChangeListener {} at path {}",
                            getInstance(), path.toString(), failure);
                } else {
                    RegisterDataTreeChangeListenerReply reply = (RegisterDataTreeChangeListenerReply) result;
                    setListenerRegistrationActor(actorContext.actorSelection(
                            reply.getListenerRegistrationPath()));
                }
            }
        }, actorContext.getClientDispatcher());
    }

    @VisibleForTesting
    ActorSelection getListenerRegistrationActor() {
        return listenerRegistrationActor;
    }

    @VisibleForTesting
    ActorRef getDataChangeListenerActor() {
        return dataChangeListenerActor;
    }
}
