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
import org.opendaylight.controller.cluster.datastore.messages.CloseDataChangeListenerRegistration;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * ListenerRegistrationProxy acts as a proxy for a ListenerRegistration that was done on a remote shard
 * <p>
 * Registering a DataChangeListener on the Data Store creates a new instance of the ListenerRegistrationProxy
 * The ListenerRegistrationProxy talks to a remote ListenerRegistration actor.
 * </p>
 */
public class DataChangeListenerRegistrationProxy implements ListenerRegistration {
    private final ActorSelection listenerRegistrationActor;
    private final AsyncDataChangeListener listener;
    private final ActorRef dataChangeListenerActor;

    public <L extends AsyncDataChangeListener<InstanceIdentifier, NormalizedNode<?, ?>>>
    DataChangeListenerRegistrationProxy(
        ActorSelection listenerRegistrationActor,
        L listener, ActorRef dataChangeListenerActor) {
        this.listenerRegistrationActor = listenerRegistrationActor;
        this.listener = listener;
        this.dataChangeListenerActor = dataChangeListenerActor;
    }

    @Override
    public Object getInstance() {
        return listener;
    }

    @Override
    public void close() {
        listenerRegistrationActor.tell(new CloseDataChangeListenerRegistration(), null);
        dataChangeListenerActor.tell(PoisonPill.getInstance(), null);
    }
}
