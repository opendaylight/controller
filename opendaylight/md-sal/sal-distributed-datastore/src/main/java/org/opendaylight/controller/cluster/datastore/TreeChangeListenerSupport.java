/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collection;
import akka.actor.ActorSelection;
import org.opendaylight.controller.cluster.datastore.messages.EnableNotification;
import org.opendaylight.controller.cluster.datastore.messages.RegisterTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class TreeChangeListenerSupport extends DelegateFactory<RegisterTreeChangeListener, ListenerRegistration<DOMDataTreeChangeListener>> {
    private static final Logger LOG = LoggerFactory.getLogger(TreeChangeListenerSupport.class);
    private final Collection<ActorSelection> actors = new ArrayList<>();
    private final Shard shard;

    TreeChangeListenerSupport(final Shard shard) {
        this.shard = Preconditions.checkNotNull(shard);
    }

    @Override
    ListenerRegistration<DOMDataTreeChangeListener> createDelegate(RegisterTreeChangeListener message) {
        ActorSelection dataChangeListenerPath = shard.getContext().system().actorSelection(
            message.getDataTreeChangeListenerPath().path());

        // Notify the listener if notifications should be enabled or not
        // If this shard is the leader then it will enable notifications else
        // it will not
        dataChangeListenerPath.tell(new EnableNotification(true), shard.getSelf());

        // Now store a reference to the data change listener so it can be notified
        // at a later point if notifications should be enabled or disabled
        actors.add(dataChangeListenerPath);

        DOMDataTreeChangeListener listener = new DataTreeChangeListenerProxy(dataChangeListenerPath);

        LOG.debug("{}: Registering for path {}", shard.persistenceId(), message.getPath());

        return shard.getDataStore().registerTreeChangeListener(message.getPath(), listener);
    }

}
