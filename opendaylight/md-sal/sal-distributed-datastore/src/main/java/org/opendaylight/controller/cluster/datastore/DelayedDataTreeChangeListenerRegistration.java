/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import java.util.EventListener;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.cluster.datastore.messages.RegisterDataTreeChangeListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;

class DelayedDataTreeChangeListenerRegistration<L extends EventListener> implements ListenerRegistration<L> {
    private final RegisterDataTreeChangeListener registrationMessage;
    private final ActorRef registrationActor;

    @GuardedBy("this")
    private boolean closed;

    DelayedDataTreeChangeListenerRegistration(final RegisterDataTreeChangeListener registrationMessage,
            final ActorRef registrationActor) {
        this.registrationMessage = registrationMessage;
        this.registrationActor = registrationActor;
    }

    synchronized void doRegistration(final DataTreeChangeListenerSupport support) {
        if (!closed) {
            support.doRegistration(registrationMessage, registrationActor);
        }
    }

    @Override
    public L getInstance() {
        // ObjectRegistration annotates this method as @Nonnull but we could return null if the delegate is not set yet.
        // In reality, we do not and should not ever call this method on DelayedDataTreeChangeListenerRegistration
        // instances anyway but, since we have to provide an implementation to satisfy the interface, we throw
        // UnsupportedOperationException to honor the API contract of not returning null and to avoid a FindBugs error
        // for possibly returning null.
        throw new UnsupportedOperationException(
                "getInstance should not be called on this instance since it could be null");
    }

    @Override
    public synchronized void close() {
        closed = true;
    }
}
