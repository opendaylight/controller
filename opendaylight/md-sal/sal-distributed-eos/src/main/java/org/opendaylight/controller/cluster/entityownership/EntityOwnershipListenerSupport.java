/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.entityownership;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.Holding;
import org.opendaylight.mdsal.eos.common.api.EntityOwnershipChangeState;
import org.opendaylight.mdsal.eos.dom.api.DOMEntity;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipChange;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages EntityOwnershipListener registrations and notifications for the EntityOwnershipShard. This class is
 * thread-safe.
 *
 * @author Thomas Pantelis
 */
class EntityOwnershipListenerSupport extends EntityOwnershipChangePublisher {
    private static final Logger LOG = LoggerFactory.getLogger(EntityOwnershipListenerSupport.class);

    private final String logId;
    private final ActorContext actorContext;
    private final ReadWriteLock listenerLock = new ReentrantReadWriteLock();

    @GuardedBy("listenerLock")
    private final Map<DOMEntityOwnershipListener, ListenerActorRefEntry> listenerActorMap = new IdentityHashMap<>();

    @GuardedBy("listenerLock")
    private final Multimap<String, DOMEntityOwnershipListener> entityTypeListenerMap = HashMultimap.create();

    private volatile boolean inJeopardy = false;

    EntityOwnershipListenerSupport(final ActorContext actorContext, final String logId) {
        this.actorContext = actorContext;
        this.logId = logId;
    }

    @Override
    String getLogId() {
        return logId;
    }

    /**
     * Set the in-jeopardy flag and indicate its previous state.
     *
     * @param inJeopardy new value of the in-jeopardy flag
     * @return Previous value of the flag.
     */
    @SuppressWarnings("checkstyle:hiddenField")
    boolean setInJeopardy(final boolean inJeopardy) {
        final boolean wasInJeopardy = this.inJeopardy;
        this.inJeopardy = inJeopardy;
        return wasInJeopardy;
    }

    void addEntityOwnershipListener(final String entityType, final DOMEntityOwnershipListener listener) {
        LOG.debug("{}: Adding EntityOwnershipListener {} for entity type {}", logId, listener, entityType);

        listenerLock.writeLock().lock();
        try {
            if (entityTypeListenerMap.put(entityType, listener)) {
                ListenerActorRefEntry listenerEntry = listenerActorMap.get(listener);
                if (listenerEntry == null) {
                    listenerActorMap.put(listener, new ListenerActorRefEntry(listener));
                } else {
                    listenerEntry.referenceCount++;
                }
            }
        } finally {
            listenerLock.writeLock().unlock();
        }
    }

    void removeEntityOwnershipListener(final String entityType, final DOMEntityOwnershipListener listener) {
        LOG.debug("{}: Removing EntityOwnershipListener {} for entity type {}", logId, listener, entityType);

        listenerLock.writeLock().lock();
        try {
            if (entityTypeListenerMap.remove(entityType, listener)) {
                ListenerActorRefEntry listenerEntry = listenerActorMap.get(listener);

                LOG.debug("{}: Found {}", logId, listenerEntry);

                listenerEntry.referenceCount--;
                if (listenerEntry.referenceCount <= 0) {
                    listenerActorMap.remove(listener);

                    if (listenerEntry.actorRef != null) {
                        LOG.debug("Killing EntityOwnershipListenerActor {}", listenerEntry.actorRef);
                        listenerEntry.actorRef.tell(PoisonPill.getInstance(), ActorRef.noSender());
                    }
                }
            }
        } finally {
            listenerLock.writeLock().unlock();
        }
    }

    @Override
    void notifyEntityOwnershipListeners(final DOMEntity entity, final boolean wasOwner, final boolean isOwner,
            final boolean hasOwner) {
        listenerLock.readLock().lock();
        try {
            Collection<DOMEntityOwnershipListener> listeners = entityTypeListenerMap.get(entity.getType());
            if (!listeners.isEmpty()) {
                notifyListeners(entity, wasOwner, isOwner, hasOwner,
                        listeners.stream().map(listenerActorMap::get).collect(Collectors.toList()));
            }
        } finally {
            listenerLock.readLock().unlock();
        }
    }

    void notifyEntityOwnershipListener(final DOMEntity entity, final boolean wasOwner, final boolean isOwner,
            final boolean hasOwner, final DOMEntityOwnershipListener listener) {
        listenerLock.readLock().lock();
        try {
            notifyListeners(entity, wasOwner, isOwner, hasOwner, ImmutableList.of(listenerActorMap.get(listener)));
        } finally {
            listenerLock.readLock().unlock();
        }
    }

    @Holding("listenerLock")
    private void notifyListeners(final DOMEntity entity, final boolean wasOwner, final boolean isOwner,
            final boolean hasOwner, final Collection<ListenerActorRefEntry> listenerEntries) {
        DOMEntityOwnershipChange changed = new DOMEntityOwnershipChange(entity,
                EntityOwnershipChangeState.from(wasOwner, isOwner, hasOwner), inJeopardy);
        for (ListenerActorRefEntry entry: listenerEntries) {
            ActorRef listenerActor = entry.actorFor();

            LOG.debug("{}: Notifying EntityOwnershipListenerActor {} with {}", logId, listenerActor, changed);

            listenerActor.tell(changed, ActorRef.noSender());
        }
    }

    private class ListenerActorRefEntry {
        final DOMEntityOwnershipListener listener;

        @GuardedBy("listenerLock")
        ActorRef actorRef;

        @GuardedBy("listenerLock")
        int referenceCount = 1;

        ListenerActorRefEntry(final DOMEntityOwnershipListener listener) {
            this.listener = listener;
        }

        ActorRef actorFor() {
            if (actorRef == null) {
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
