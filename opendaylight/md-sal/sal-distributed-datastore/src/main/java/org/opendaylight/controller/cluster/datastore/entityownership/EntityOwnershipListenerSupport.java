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
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import org.opendaylight.mdsal.eos.common.api.EntityOwnershipChangeState;
import org.opendaylight.mdsal.eos.dom.api.DOMEntity;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipChange;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipListener;
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
    private final Map<DOMEntityOwnershipListener, ListenerActorRefEntry> listenerActorMap = new IdentityHashMap<>();
    private final Set<DOMEntity> entitiesWithCandidateSet = new HashSet<>();
    private final Multimap<String, DOMEntityOwnershipListener> entityTypeListenerMap = HashMultimap.create();
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

    boolean hasCandidateForEntity(DOMEntity entity) {
        return entitiesWithCandidateSet.contains(entity);
    }

    void setHasCandidateForEntity(DOMEntity entity) {
        entitiesWithCandidateSet.add(entity);
    }

    void unsetHasCandidateForEntity(DOMEntity entity) {
        entitiesWithCandidateSet.remove(entity);
    }

    void addEntityOwnershipListener(String entityType, DOMEntityOwnershipListener listener) {
        LOG.debug("{}: Adding EntityOwnershipListener {} for entity type {}", logId, listener, entityType);

        addListener(listener, entityType);
    }

    void removeEntityOwnershipListener(String entityType, DOMEntityOwnershipListener listener) {
        LOG.debug("{}: Removing EntityOwnershipListener {} for entity type {}", logId, listener, entityType);

        removeListener(listener, entityType);
    }

    void notifyEntityOwnershipListeners(DOMEntity entity, boolean wasOwner, boolean isOwner, boolean hasOwner) {
        notifyListeners(entity, entity.getType(), wasOwner, isOwner, hasOwner);
    }

    void notifyEntityOwnershipListener(DOMEntity entity, boolean wasOwner, boolean isOwner, boolean hasOwner,
            DOMEntityOwnershipListener listener) {
        notifyListeners(entity, wasOwner, isOwner, hasOwner, Collections.singleton(listener));
    }

    private void notifyListeners(DOMEntity entity, String mapKey, boolean wasOwner, boolean isOwner, boolean hasOwner) {
        Collection<DOMEntityOwnershipListener> listeners = entityTypeListenerMap.get(mapKey);
        if(!listeners.isEmpty()) {
            notifyListeners(entity, wasOwner, isOwner, hasOwner, listeners);
        }
    }

    private void notifyListeners(DOMEntity entity, boolean wasOwner, boolean isOwner, boolean hasOwner,
            Collection<DOMEntityOwnershipListener> listeners) {
        DOMEntityOwnershipChange changed = new DOMEntityOwnershipChange(entity,
                EntityOwnershipChangeState.from(wasOwner, isOwner, hasOwner), inJeopardy);
        for(DOMEntityOwnershipListener listener: listeners) {
            ActorRef listenerActor = listenerActorFor(listener);

            LOG.debug("{}: Notifying EntityOwnershipListenerActor {} with {}", logId, listenerActor, changed);

            listenerActor.tell(changed, ActorRef.noSender());
        }
    }

    private void addListener(DOMEntityOwnershipListener listener, String mapKey) {
        if (entityTypeListenerMap.put(mapKey, listener)) {
            ListenerActorRefEntry listenerEntry = listenerActorMap.get(listener);
            if(listenerEntry == null) {
                listenerActorMap.put(listener, new ListenerActorRefEntry());
            } else {
                listenerEntry.referenceCount++;
            }
        }
    }

    private void removeListener(DOMEntityOwnershipListener listener, String mapKey) {
        if (entityTypeListenerMap.remove(mapKey, listener)) {
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

    private ActorRef listenerActorFor(DOMEntityOwnershipListener listener) {
        return listenerActorMap.get(listener).actorFor(listener);
    }

    private class ListenerActorRefEntry {
        ActorRef actorRef;
        int referenceCount = 1;

        ActorRef actorFor(DOMEntityOwnershipListener listener) {
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
