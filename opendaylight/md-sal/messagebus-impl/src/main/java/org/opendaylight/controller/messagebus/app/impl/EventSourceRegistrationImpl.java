/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.messagebus.app.impl;

import org.opendaylight.controller.messagebus.spi.EventSource;
import org.opendaylight.controller.messagebus.spi.EventSourceRegistration;
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;

import com.google.common.base.Preconditions;


class EventSourceRegistrationImpl <T extends EventSource> extends AbstractObjectRegistration<T> implements EventSourceRegistration<T>{

    private final EventSourceTopology eventSourceTopology;

    /**
     * @param instance of EventSource that has been registered by {@link EventSourceRegistryImpl#registerEventSource(Node, EventSource)}
     */
    public EventSourceRegistrationImpl(T instance, EventSourceTopology eventSourceTopology) {
        super(instance);
        this.eventSourceTopology = Preconditions.checkNotNull(eventSourceTopology);
    }

    @Override
    protected void removeRegistration() {
        this.eventSourceTopology.unRegister(getInstance());
    }

}
