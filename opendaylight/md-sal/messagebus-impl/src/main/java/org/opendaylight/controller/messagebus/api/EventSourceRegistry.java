/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.messagebus.api;

import org.opendaylight.controller.messagebus.app.impl.EventSourceTopology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * EventSourceRegistry service is used by event source managers to register
 * event sources managed by them (see {@link EventSource}). Event source manager
 * can obtain reference to EventSourceRegistry by configuration subsystem.
 */
public class EventSourceRegistry {

    private static final Logger LOG = LoggerFactory
            .getLogger(EventSourceRegistry.class);
    final EventSourceTopology eventSourceTopology;

    /**
     * Method is used to register EventSource into message bus. Registration
     * process add event source into internal register (event source topology) and
     * register eventSource as RpcService.
     * Event source manager can call {@link EventSourceRegistration#close()}
     * to unregister event source.
     * @param eventSource  instance of EventSource
     * @return             Instance of EventSourceRegistration
     */
    public EventSourceRegistry(EventSourceTopology eventSourceTopology) {

        this.eventSourceTopology = Preconditions.checkNotNull(eventSourceTopology);
        LOG.info("EventSourceRegistry has been initialized");

    }

    public EventSourceRegistration registerEventSource(final EventSource eventSource) {
        EventSourceRegistration esr = new EventSourceRegistration(eventSource, this);
        this.eventSourceTopology.register(eventSource);
        return esr;
    }

    protected final void unRegisterEventSource(final EventSource eventSource){
        this.eventSourceTopology.unRegister(eventSource);
    }
}