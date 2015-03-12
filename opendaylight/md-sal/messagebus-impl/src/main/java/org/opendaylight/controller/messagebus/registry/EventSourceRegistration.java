/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.messagebus.registry;

import org.opendaylight.controller.messagebus.api.EventSource;
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author madamjak
 *
 */
public class EventSourceRegistration extends AbstractObjectRegistration<EventSource> {
    private static final Logger LOG = LoggerFactory.getLogger(EventSourceRegistration.class);
    /**
     * @param instance
     */
    protected EventSourceRegistration(EventSource instance) {
        super(instance);
    }

    public EventSource getEventSource(){
        return (EventSource) getInstance();
    }

    @Override
    protected void removeRegistration() {
        EventSource es = getEventSource();
        try {
            getEventSource().close();
        } catch (Exception ex) {
            LOG.error("Exception while closing: {}\n Exception: {}", es, ex);
        }
    }

}
