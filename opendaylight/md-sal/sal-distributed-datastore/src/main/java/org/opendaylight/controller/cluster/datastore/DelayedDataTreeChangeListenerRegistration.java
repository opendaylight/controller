/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import org.apache.pekko.actor.ActorRef;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.opendaylight.controller.cluster.datastore.messages.RegisterDataTreeChangeListener;
import org.opendaylight.yangtools.concepts.Registration;

class DelayedDataTreeChangeListenerRegistration implements Registration {
    private final RegisterDataTreeChangeListener registrationMessage;
    private final ActorRef registrationActor;

    private @GuardedBy("this") boolean closed;

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
    public synchronized void close() {
        closed = true;
    }
}
