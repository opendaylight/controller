/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.entityownership;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import org.opendaylight.mdsal.common.api.clustering.EntityOwnershipChangeState;
import org.opendaylight.mdsal.dom.api.clustering.DOMEntity;
import org.opendaylight.mdsal.dom.api.clustering.DOMEntityOwnershipChange;
import org.opendaylight.mdsal.dom.api.clustering.DOMEntityOwnershipListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.PoisonPill;

/**
 * Managers DOMEntityOwnershipListener registrations and notifications for the DOMEntityOwnershipShard.
 *
 */
public class DOMEntityOwnershipListenerSupport {

    private static final Logger LOG = LoggerFactory.getLogger(DOMEntityOwnershipListenerSupport.class);

    private final String logId;
    private final ActorContext actorContext;
    private final Map<DOMEntityOwnershipListener, ListenerActorRefEntry> listenerActorMap = new IdentityHashMap<>();
    private final Set<DOMEntity> entitiesWithCandidateSet = new HashSet<>();
    private final Multimap<String, DOMEntityOwnershipListener> entityTypeListenerMap = HashMultimap.create();
    private volatile boolean inJeopardy = false;

    DOMEntityOwnershipListenerSupport(final ActorContext actorContext, final String logId) {
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

    boolean hasCandidateForEntity(final DOMEntity entity) {
        return entitiesWithCandidateSet.contains(entity);
    }

    void setHasCandidateForEntity(final DOMEntity entity) {
        entitiesWithCandidateSet.add(entity);
    }

    void unsetHasCandidateForEntity(final DOMEntity entity) {
        entitiesWithCandidateSet.remove(entity);
    }

    void addEntityOwnershipListener(final String entityType, final DOMEntityOwnershipListener listener) {
        LOG.debug("{}: Adding DOMEntityOwnershipListener {} for entity type {}", logId, listener, entityType);

        addListener(listener, entityType);
    }

    void removeEntityOwnershipListener(final String entityType, final DOMEntityOwnershipListener listener) {
        LOG.debug("{}: Removing DOMEntityOwnershipListener {} for entity type {}", logId, listener, entityType);

        removeListener(listener, entityType);
    }

    void notifyEntityOwnershipListeners(final DOMEntity entity, final boolean wasOwner, final boolean isOwner,
            final boolean hasOwner) {
        notifyListeners(entity, entity.getType(), wasOwner, isOwner, hasOwner);
    }

    void notifyEntityOwnershipListener(final DOMEntity entity, final boolean wasOwner, final boolean isOwner,
            final boolean hasOwner, final DOMEntityOwnershipListener listener) {
        notifyListeners(entity, wasOwner, isOwner, hasOwner, Collections.singleton(listener));
    }

    private void notifyListeners(final DOMEntity entity, final String mapKey, final boolean wasOwner,
            final boolean isOwner, final boolean hasOwner) {
        final Collection<DOMEntityOwnershipListener> listeners = entityTypeListenerMap.get(mapKey);
        if (!listeners.isEmpty()) {
            notifyListeners(entity, wasOwner, isOwner, hasOwner, listeners);
        }
    }

    private void notifyListeners(final DOMEntity entity, final boolean wasOwner, final boolean isOwner,
            final boolean hasOwner, final Collection<DOMEntityOwnershipListener> listeners) {
        final DOMEntityOwnershipChange changed = new DOMEntityOwnershipChange(entity,
                EntityOwnershipChangeState.from(wasOwner, isOwner, hasOwner), inJeopardy);
        for (final DOMEntityOwnershipListener listener : listeners) {
            final ActorRef listenerActor = listenerActorFor(listener);

            LOG.debug("{}: Notifying EntityOwnershipListenerActor {} with {}", logId, listenerActor, changed);

            listenerActor.tell(changed, ActorRef.noSender());
        }
    }

    private void addListener(final DOMEntityOwnershipListener listener, final String mapKey) {
        if (entityTypeListenerMap.put(mapKey, listener)) {
            final ListenerActorRefEntry listenerEntry = listenerActorMap.get(listener);
            if (listenerEntry == null) {
                listenerActorMap.put(listener, new ListenerActorRefEntry());
            } else {
                listenerEntry.referenceCount++;
            }
        }
    }

    private void removeListener(final DOMEntityOwnershipListener listener, final String mapKey) {
        if (entityTypeListenerMap.remove(mapKey, listener)) {
            final ListenerActorRefEntry listenerEntry = listenerActorMap.get(listener);

            LOG.debug("{}: Found {}", logId, listenerEntry);

            listenerEntry.referenceCount--;
            if (listenerEntry.referenceCount <= 0) {
                listenerActorMap.remove(listener);

                if (listenerEntry.actorRef != null) {
                    LOG.debug("Killing DOMEntityOwnershipListenerActor {}", listenerEntry.actorRef);
                    listenerEntry.actorRef.tell(PoisonPill.getInstance(), ActorRef.noSender());
                }
            }
        }
    }

    private ActorRef listenerActorFor(final DOMEntityOwnershipListener listener) {
        return listenerActorMap.get(listener).actorFor(listener);
    }

    private class ListenerActorRefEntry {
        ActorRef actorRef;
        int referenceCount = 1;

        ActorRef actorFor(final DOMEntityOwnershipListener listener) {
            if (actorRef == null) {
                actorRef = actorContext.actorOf(DOMEntityOwnershipListenerActor.props(listener));

                LOG.debug("{}: Created DOMEntityOwnershipListenerActor {} for listener {}", logId, actorRef, listener);
            }

            return actorRef;
        }

        @Override
        public String toString() {
            return "ListenerActorRefEntry [actorRef=" + actorRef + ", referenceCount=" + referenceCount + "]";
        }
    }
}
