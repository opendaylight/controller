/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.messagebus.registry;

import static com.google.common.util.concurrent.Futures.immediateFuture;

import org.opendaylight.controller.messagebus.api.EventSource;
import org.opendaylight.controller.messagebus.api.EventSourceRegistration;
import org.opendaylight.controller.messagebus.api.EventSourceRegistry;
import org.opendaylight.controller.messagebus.app.impl.EventSourceTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListenableFuture;
/**
 * @author madamjak
 *
 */
public class EventSourceRegistryImpl implements EventSourceRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(EventSourceRegistryImpl.class);

    private EventSourceTopology eventSourceTopology;

    public EventSourceRegistryImpl(final EventSourceTopology eventSourceTopology) {
        this.eventSourceTopology = eventSourceTopology;
        LOG.info("EventSourceRegistry has been initialized");
    }

    @Override
    public ListenableFuture<EventSourceRegistration> registerEventSource(final Node node, final EventSource eventSource){
        EventSourceRegistration esr = new EventSourceRegistration(eventSource);
        this.getEventSourceTopology().register(node, eventSource);
        return immediateFuture(esr);
    }

    @Override
    public ListenableFuture<Void> unRegisterEventSource(final EventSourceRegistration esr){
        EventSource eventSource = esr.getInstance();
        this.getEventSourceTopology().unRegister(eventSource);
        return immediateFuture((Void) null);
    }

    public EventSourceTopology getEventSourceTopology() {
        return eventSourceTopology;
    }

}
