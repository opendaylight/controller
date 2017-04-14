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
import org.opendaylight.controller.cluster.datastore.messages.ListenerRegistrationMessage;
import org.opendaylight.yangtools.concepts.ListenerRegistration;

abstract class DelayedListenerRegistration<L extends EventListener, M extends ListenerRegistrationMessage>
        implements ListenerRegistration<L> {
    private final M registrationMessage;
    private final ActorRef registrationActor;

    @GuardedBy("this")
    private boolean closed;

    protected DelayedListenerRegistration(M registrationMessage, ActorRef registrationActor) {
        this.registrationMessage = registrationMessage;
        this.registrationActor = registrationActor;
    }

    M getRegistrationMessage() {
        return registrationMessage;
    }

    synchronized void createDelegate(final AbstractDataListenerSupport<L, M, ?> support) {
        if (!closed) {
            support.doRegistration(registrationMessage, registrationActor);
        }
    }

    @Override
    public L getInstance() {
        final ListenerRegistration<L> d = delegate;
        return d == null ? null : (L)d.getInstance();
    }

    @Override
    public synchronized void close() {
        closed = true;
    }
}
