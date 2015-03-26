/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.messagebus.api;

import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Instance of EventSourceRegistration is returned by {@link EventSourceRegistry#registerEventSource(Node, EventSource)}
 * and it is used to unregister EventSource.
 * {@link EventSourceRegistry#unRegisterEventSource(EventSourceRegistration) never will call close()
 * method of EventSourceRegistration instances.
 *
 */
public class EventSourceRegistration extends AbstractObjectRegistration<EventSource> {
    private static final Logger LOG = LoggerFactory.getLogger(EventSourceRegistration.class);
    /**
     * @param instance of EventSource that has been registered by {@link EventSourceRegistry#registerEventSource(Node, EventSource)}
     */
    public EventSourceRegistration(EventSource instance) {
        super(instance);
    }

    @Override
    protected void removeRegistration() {
        EventSource es = getInstance();
        try {
            es.close();
        } catch (Exception ex) {
            LOG.error("Exception while closing: {}\n Exception: {}", es, ex);
        }
    }

}
