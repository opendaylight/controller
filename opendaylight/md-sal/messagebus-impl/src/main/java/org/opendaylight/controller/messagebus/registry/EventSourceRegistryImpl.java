/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.messagebus.registry;

import static com.google.common.util.concurrent.Futures.immediateFuture;

import java.util.concurrent.Future;

import org.opendaylight.controller.messagebus.api.EventSource;
import org.opendaylight.controller.messagebus.api.EventSourceRegistry;
import org.opendaylight.controller.messagebus.app.impl.EventSourceTopology;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * @author madamjak
 *
 */
public class EventSourceRegistryImpl implements EventSourceRegistry, AutoCloseable{
    private static final Logger LOG = LoggerFactory.getLogger(EventSourceRegistryImpl.class);

    private EventSourceTopology eventSourceTopology;
    private RpcRegistration<EventSourceRegistry> serviceRegistration;

    public static EventSourceRegistryImpl create(final EventSourceTopology eventSourceTopology, final RpcProviderRegistry rpcRegistry){
        final EventSourceRegistryImpl esr = new EventSourceRegistryImpl(eventSourceTopology);
        esr.serviceRegsitration(rpcRegistry);
        LOG.info("EventSourceRegistry has been initialized");
        return esr;
    }

    private EventSourceRegistryImpl(final EventSourceTopology eventSourceTopology) {
        this.eventSourceTopology = eventSourceTopology;
    }

    private void serviceRegsitration(final RpcProviderRegistry rpcRegistry){
        serviceRegistration = rpcRegistry.addRpcImplementation(EventSourceRegistry.class, this);
    }

    @Override
    public Future<RpcResult<EventSourceRegistration>> registerEventSource(final Node node, final EventSource eventSource){
        EventSourceRegistration esr = new EventSourceRegistration(eventSource);
        this.getEventSourceTopology().register(node, eventSource);
        return immediateFuture(RpcResultBuilder.success(esr).build());
    }

    @Override
    public Future<RpcResult<Void>> unRegistreEventSource(final EventSourceRegistration esr){
        EventSource eventSource = esr.getInstance();
        this.getEventSourceTopology().unRegister(eventSource);
        return resultRpcVoid();
    }

    public EventSourceTopology getEventSourceTopology() {
        return eventSourceTopology;
    }

    @Override
    public void close() throws Exception {
        if(serviceRegistration != null){
            serviceRegistration.close();
            serviceRegistration = null;
        }
    }

    private Future<RpcResult<Void>> resultRpcVoid(){
        return immediateFuture(RpcResultBuilder.success((Void) null).build());
    }
}
