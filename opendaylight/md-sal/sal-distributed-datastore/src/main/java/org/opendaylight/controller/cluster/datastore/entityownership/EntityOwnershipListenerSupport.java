/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.entityownership;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import org.opendaylight.controller.cluster.datastore.entityownership.messages.EntityOwnershipChanged;
import org.opendaylight.controller.md.sal.common.api.clustering.Entity;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages EntityOwnershipListener registrations and notifications for the EntityOwnershipShard.
 *
 * @author Thomas Pantelis
 */
class EntityOwnershipListenerSupport {
    private static final Logger LOG = LoggerFactory.getLogger(EntityOwnershipListenerSupport.class);

    private final ActorContext actorContext;
    private final Map<EntityOwnershipListener, ListenerActorRefEntry> listenerActorMap = new IdentityHashMap<>();
    private final Multimap<Entity, EntityOwnershipListener> entityListenerMap = HashMultimap.create();

    EntityOwnershipListenerSupport(ActorContext actorContext) {
        this.actorContext = actorContext;
    }

    void addEntityOwnershipListener(Entity entity, EntityOwnershipListener listener) {
        LOG.debug("Adding EntityOwnershipListener {} for {}", listener, entity);

        if(entityListenerMap.put(entity, listener)) {
            ListenerActorRefEntry listenerEntry = listenerActorMap.get(listener);
            if(listenerEntry == null) {
                listenerActorMap.put(listener, new ListenerActorRefEntry());
            } else {
                listenerEntry.referenceCount++;
            }
        }
    }

    void removeEntityOwnershipListener(Entity entity, EntityOwnershipListener listener) {
        LOG.debug("Removing EntityOwnershipListener {} for {}", listener, entity);

        if(entityListenerMap.remove(entity, listener)) {
            ListenerActorRefEntry listenerEntry = listenerActorMap.get(listener);

            LOG.debug("Found {}", listenerEntry);

            listenerEntry.referenceCount--;
            if(listenerEntry.referenceCount <= 0) {
                listenerActorMap.remove(listener);

                if(listenerEntry.actorRef != null) {
                    LOG.debug("Killing EntityOwnershipListenerActor {}", listenerEntry.actorRef);
                    listenerEntry.actorRef.tell(PoisonPill.getInstance(), ActorRef.noSender());
                }
            }
        }
    }

    void notifyEntityOwnershipListeners(Entity entity, boolean wasOwner, boolean isOwner) {
        Collection<EntityOwnershipListener> listeners = entityListenerMap.get(entity);
        if(listeners.isEmpty()) {
            return;
        }

        EntityOwnershipChanged changed = new EntityOwnershipChanged(entity, wasOwner, isOwner);
        for(EntityOwnershipListener listener: listeners) {
            ActorRef listenerActor = listenerActorFor(listener);

            LOG.debug("Notifying EntityOwnershipListenerActor {} with {}", listenerActor,changed);

            listenerActor.tell(changed, ActorRef.noSender());
        }
    }

    private ActorRef listenerActorFor(EntityOwnershipListener listener) {
        return listenerActorMap.get(listener).actorFor(listener);
    }

    private class ListenerActorRefEntry {
        ActorRef actorRef;
        int referenceCount = 1;

        ActorRef actorFor(EntityOwnershipListener listener) {
            if(actorRef == null) {
                actorRef = actorContext.actorOf(EntityOwnershipListenerActor.props(listener));

                LOG.debug("Created EntityOwnershipListenerActor {} for listener {}", actorRef, listener);
            }

            return actorRef;
        }

        @Override
        public String toString() {
            return "ListenerActorRefEntry [actorRef=" + actorRef + ", referenceCount=" + referenceCount + "]";
        }
    }
}
