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
import com.google.common.base.Preconditions;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.cluster.datastore.exceptions.LocalShardNotFoundException;
import org.opendaylight.controller.cluster.datastore.messages.CloseDataTreeChangeListenerRegistration;
import org.opendaylight.controller.cluster.datastore.messages.RegisterTreeChangeListener;
import org.opendaylight.controller.cluster.datastore.messages.RegisterTreeChangeListenerReply;
import org.opendaylight.controller.cluster.datastore.messages.UpdateSchemaContext;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.yangtools.concepts.AbstractListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;

final class DOMDataTreeChangeListenerProxy<T extends DOMDataTreeChangeListener> extends AbstractListenerRegistration<T> {
    private static final Logger LOG = LoggerFactory.getLogger(DOMDataTreeChangeListenerProxy.class);
    private final ActorRef dataChangeListenerActor;
    private final DistributedDataStore parent;
    private final ActorContext actorContext;

    @GuardedBy("this")
    private ActorSelection listenerRegistrationActor;

    public DOMDataTreeChangeListenerProxy(final DistributedDataStore parent, final ActorContext actorContext, final T listener) {
        super(listener);
        this.actorContext = Preconditions.checkNotNull(actorContext);
        this.parent = Preconditions.checkNotNull(parent);
        this.dataChangeListenerActor = actorContext.getActorSystem().actorOf(
            DataTreeChangeListener.props(actorContext.getSchemaContext(), getInstance()).withDispatcher(actorContext.getNotificationDispatcherPath()));
    }

    @Override
    protected void removeRegistration() {
        synchronized (this) {
            if (listenerRegistrationActor != null) {
                listenerRegistrationActor.tell(CloseDataTreeChangeListenerRegistration.getInstance(), ActorRef.noSender());
                listenerRegistrationActor = null;
            }

            dataChangeListenerActor.tell(PoisonPill.getInstance(), ActorRef.noSender());
        }

        // Must not hold lock, as updateContext() can cause deadlock
        parent.removeTreeChangeListener(this);
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

    synchronized void updateContext(final UpdateSchemaContext message) {
        if (!isClosed()) {
            dataChangeListenerActor.tell(message, ActorRef.noSender());
        }
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
                new RegisterTreeChangeListener(path, dataChangeListenerActor.path()),
                actorContext.getDatastoreContext().getShardInitializationTimeout());

        future.onComplete(new OnComplete<Object>(){
            @Override
            public void onComplete(final Throwable failure, final Object result) {
                if (failure != null) {
                    LOG.error("Failed to register DataTreeChangeListener {} at path {}",
                            getInstance(), path.toString(), failure);
                } else {
                    RegisterTreeChangeListenerReply reply = (RegisterTreeChangeListenerReply) result;
                    setListenerRegistrationActor(actorContext.actorSelection(
                            reply.getListenerRegistrationPath()));
                }
            }
        }, actorContext.getClientDispatcher());
    }
}
