/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.japi.Creator;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
import org.opendaylight.controller.cluster.datastore.messages.CloseDataTreeChangeListenerRegistration;
import org.opendaylight.controller.cluster.datastore.messages.CloseDataTreeChangeListenerRegistrationReply;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;

/**
 * Actor co-located with a shard. It exists only to terminate the registration when
 * asked to do so via {@link CloseDataTreeChangeListenerRegistration}.
 */
public final class DataTreeChangeListenerRegistrationActor extends AbstractUntypedActor {
    private final ListenerRegistration<DOMDataTreeChangeListener> registration;

    public DataTreeChangeListenerRegistrationActor(final ListenerRegistration<DOMDataTreeChangeListener> registration) {
        this.registration = Preconditions.checkNotNull(registration);
    }

    @Override
    protected void handleReceive(Object message) throws Exception {
        if (message instanceof CloseDataTreeChangeListenerRegistration) {
            registration.close();
            getSender().tell(CloseDataTreeChangeListenerRegistrationReply.getInstance(), getSelf());
            getSelf().tell(PoisonPill.getInstance(), getSelf());
        }
    }

    public static Props props(final ListenerRegistration<DOMDataTreeChangeListener> registration) {
        return Props.create(new DataTreeChangeListenerRegistrationCreator(registration));
    }

    private static final class DataTreeChangeListenerRegistrationCreator implements Creator<DataTreeChangeListenerRegistrationActor> {
        private static final long serialVersionUID = 1L;
        final ListenerRegistration<DOMDataTreeChangeListener> registration;

        DataTreeChangeListenerRegistrationCreator(ListenerRegistration<DOMDataTreeChangeListener> registration) {
            this.registration = Preconditions.checkNotNull(registration);
        }

        @Override
        public DataTreeChangeListenerRegistrationActor create() {
            return new DataTreeChangeListenerRegistrationActor(registration);
        }
    }
}
