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
import akka.actor.PoisonPill;
import akka.dispatch.OnComplete;
import akka.util.Timeout;
import org.opendaylight.controller.cluster.datastore.messages.CloseDataChangeListenerRegistration;
import org.opendaylight.controller.cluster.datastore.messages.RegisterChangeListener;
import org.opendaylight.controller.cluster.datastore.messages.RegisterChangeListenerReply;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import scala.concurrent.Future;

/**
 * ListenerRegistrationProxy acts as a proxy for a ListenerRegistration that was done on a remote shard
 * <p>
 * Registering a DataChangeListener on the Data Store creates a new instance of the ListenerRegistrationProxy
 * The ListenerRegistrationProxy talks to a remote ListenerRegistration actor.
 * </p>
 */
@SuppressWarnings("rawtypes")
public class DataChangeListenerRegistrationProxy implements ListenerRegistration {

    private static final Logger LOG = LoggerFactory.getLogger(DataChangeListenerRegistrationProxy.class);

    public static final int REGISTER_DATA_CHANGE_LISTENER_TIMEOUT_FACTOR = 24; // 24 times the usual operation timeout

    private volatile ActorSelection listenerRegistrationActor;
    private final AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>> listener;
    private ActorRef dataChangeListenerActor;
    private final String shardName;
    private final ActorContext actorContext;
    private boolean closed = false;

    public <L extends AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>>>
                                                              DataChangeListenerRegistrationProxy (
            String shardName, ActorContext actorContext, L listener) {
        this.shardName = shardName;
        this.actorContext = actorContext;
        this.listener = listener;
    }

    @VisibleForTesting
    ActorSelection getListenerRegistrationActor() {
        return listenerRegistrationActor;
    }

    @VisibleForTesting
    ActorRef getDataChangeListenerActor() {
        return dataChangeListenerActor;
    }

    @Override
    public Object getInstance() {
        return listener;
    }

    private void setListenerRegistrationActor(ActorSelection listenerRegistrationActor) {
        if(listenerRegistrationActor == null) {
            return;
        }

        boolean sendCloseMessage = false;
        synchronized(this) {
            if(closed) {
                sendCloseMessage = true;
            } else {
                this.listenerRegistrationActor = listenerRegistrationActor;
            }
        }

        if(sendCloseMessage) {
            listenerRegistrationActor.tell(new
                CloseDataChangeListenerRegistration().toSerializable(), null);
        }
    }

    public void init(final YangInstanceIdentifier path, AsyncDataBroker.DataChangeScope scope) {

        dataChangeListenerActor = actorContext.getActorSystem().actorOf(
                DataChangeListener.props(listener));

        Optional<ActorRef> shard = actorContext.findLocalShard(shardName);

        // if shard is NOT local
        if (!shard.isPresent()) {
            LOG.debug("No local shard for shardName {} was found - DataChangeListener {} cannot be registered",
                    shardName, listener);
            return;
        }

        Future<Object> future = actorContext.executeOperationAsync(shard.get(),
                new RegisterChangeListener(path, dataChangeListenerActor.path(), scope),
                new Timeout(actorContext.getOperationDuration().$times(
                        REGISTER_DATA_CHANGE_LISTENER_TIMEOUT_FACTOR)));

        future.onComplete(new OnComplete<Object>(){
            @Override
            public void onComplete(Throwable failure, Object result) {
                if(failure != null){
                    LOG.error("Failed to register DataChangeListener {} at path {}",
                            listener, path.toString(), failure);
                    return;
                }

                RegisterChangeListenerReply reply = (RegisterChangeListenerReply) result;
                setListenerRegistrationActor(actorContext.actorSelection(
                        reply.getListenerRegistrationPath()));
            }
        }, actorContext.getActorSystem().dispatcher());
    }

    @Override
    public void close() {

        boolean sendCloseMessage;
        synchronized(this) {
            sendCloseMessage = !closed && listenerRegistrationActor != null;
            closed = true;
        }

        if(sendCloseMessage) {
            listenerRegistrationActor.tell(new CloseDataChangeListenerRegistration().toSerializable(),
                    ActorRef.noSender());
            listenerRegistrationActor = null;
        }

        if(dataChangeListenerActor != null) {
            dataChangeListenerActor.tell(PoisonPill.getInstance(), ActorRef.noSender());
            dataChangeListenerActor = null;
        }
    }
}
