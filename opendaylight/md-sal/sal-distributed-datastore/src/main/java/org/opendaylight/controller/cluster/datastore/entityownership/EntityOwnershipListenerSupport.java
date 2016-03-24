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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import org.opendaylight.controller.md.sal.common.api.clustering.Entity;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipChange;
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

    private final String logId;
    private final ActorContext actorContext;
    private final Map<EntityOwnershipListener, ListenerActorRefEntry> listenerActorMap = new IdentityHashMap<>();
    private final Set<Entity> entitiesWithCandidateSet = new HashSet<>();
    private final Multimap<String, EntityOwnershipListener> entityTypeListenerMap = HashMultimap.create();
    private volatile boolean inJeopardy = false;

    EntityOwnershipListenerSupport(ActorContext actorContext, String logId) {
        this.actorContext = actorContext;
        this.logId = logId;
    }

    String getLogId() {
        return logId;
    }

    /**
     * Set the in-jeopardy flag and indicate its previous state.
     *
     * @param inJeopardy new value of the in-jeopardy flag
     * @return Previous value of the flag.
     */
    boolean setInJeopardy(final boolean inJeopardy) {
        final boolean wasInJeopardy = this.inJeopardy;
        this.inJeopardy = inJeopardy;
        return wasInJeopardy;
    }

    boolean hasCandidateForEntity(Entity entity) {
        return entitiesWithCandidateSet.contains(entity);
    }

    void setHasCandidateForEntity(Entity entity) {
        entitiesWithCandidateSet.add(entity);
    }

    void unsetHasCandidateForEntity(Entity entity) {
        entitiesWithCandidateSet.remove(entity);
    }

    void addEntityOwnershipListener(String entityType, EntityOwnershipListener listener) {
        LOG.debug("{}: Adding EntityOwnershipListener {} for entity type {}", logId, listener, entityType);

        addListener(listener, entityType, entityTypeListenerMap);
    }

    void removeEntityOwnershipListener(String entityType, EntityOwnershipListener listener) {
        LOG.debug("{}: Removing EntityOwnershipListener {} for entity type {}", logId, listener, entityType);

        removeListener(listener, entityType, entityTypeListenerMap);
    }

    void notifyEntityOwnershipListeners(Entity entity, boolean wasOwner, boolean isOwner, boolean hasOwner) {
        notifyListeners(entity, entity.getType(), wasOwner, isOwner, hasOwner, entityTypeListenerMap);
    }

    void notifyEntityOwnershipListener(Entity entity, boolean wasOwner, boolean isOwner, boolean hasOwner,
            EntityOwnershipListener listener) {
        notifyListeners(entity, wasOwner, isOwner, hasOwner, Arrays.asList(listener));
    }

    private <T> void notifyListeners(Entity entity, T mapKey, boolean wasOwner, boolean isOwner, boolean hasOwner,
            Multimap<T, EntityOwnershipListener> listenerMap) {
        Collection<EntityOwnershipListener> listeners = listenerMap.get(mapKey);
        if(!listeners.isEmpty()) {
            notifyListeners(entity, wasOwner, isOwner, hasOwner, listeners);
        }
    }

    private void notifyListeners(Entity entity, boolean wasOwner, boolean isOwner, boolean hasOwner,
            Collection<EntityOwnershipListener> listeners) {
        EntityOwnershipChange changed = new EntityOwnershipChange(entity, wasOwner, isOwner, hasOwner, inJeopardy);
        for(EntityOwnershipListener listener: listeners) {
            ActorRef listenerActor = listenerActorFor(listener);

            LOG.debug("{}: Notifying EntityOwnershipListenerActor {} with {}", logId, listenerActor, changed);

            listenerActor.tell(changed, ActorRef.noSender());
        }
    }

    private <T> void addListener(EntityOwnershipListener listener, T mapKey,
            Multimap<T, EntityOwnershipListener> toListenerMap) {
        if(toListenerMap.put(mapKey, listener)) {
            ListenerActorRefEntry listenerEntry = listenerActorMap.get(listener);
            if(listenerEntry == null) {
                listenerActorMap.put(listener, new ListenerActorRefEntry());
            } else {
                listenerEntry.referenceCount++;
            }
        }
    }

    private <T> void removeListener(EntityOwnershipListener listener, T mapKey,
            Multimap<T, EntityOwnershipListener> fromListenerMap) {
        if(fromListenerMap.remove(mapKey, listener)) {
            ListenerActorRefEntry listenerEntry = listenerActorMap.get(listener);

            LOG.debug("{}: Found {}", logId, listenerEntry);

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

    private ActorRef listenerActorFor(EntityOwnershipListener listener) {
        return listenerActorMap.get(listener).actorFor(listener);
    }

    private class ListenerActorRefEntry {
        ActorRef actorRef;
        int referenceCount = 1;

        ActorRef actorFor(EntityOwnershipListener listener) {
            if(actorRef == null) {
                actorRef = actorContext.actorOf(EntityOwnershipListenerActor.props(listener));

                LOG.debug("{}: Created EntityOwnershipListenerActor {} for listener {}", logId, actorRef, listener);
            }

            return actorRef;
        }

        @Override
        public String toString() {
            return "ListenerActorRefEntry [actorRef=" + actorRef + ", referenceCount=" + referenceCount + "]";
        }
    }
}
