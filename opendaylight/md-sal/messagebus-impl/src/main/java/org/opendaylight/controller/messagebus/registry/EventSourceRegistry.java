/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.messagebus.registry;

import static com.google.common.util.concurrent.Futures.immediateFuture;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;

import org.opendaylight.controller.messagebus.api.EventSource;
import org.opendaylight.controller.messagebus.api.EventSourceManager;
import org.opendaylight.controller.messagebus.app.impl.EventSourceTopology;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
/**
 * @author madamjak
 *
 */
public class EventSourceRegistry implements EventSourceRegistryService, AutoCloseable{
    private static final Logger LOG = LoggerFactory.getLogger(EventSourceRegistry.class);

    private EventSourceTopology eventSourceTopology;
    private final CopyOnWriteArrayList<EventSourceManager> eventSourceManagerList;
    private RpcRegistration<EventSourceRegistryService> serviceRegistration;
    
    public static EventSourceRegistry create(final EventSourceTopology eventSourceTopology, final RpcProviderRegistry rpcRegistry){
        final EventSourceRegistry esr = new EventSourceRegistry(eventSourceTopology);
        esr.serviceRegsitration(rpcRegistry);
        return esr;
    }
    
    private EventSourceRegistry(final EventSourceTopology eventSourceTopology) {
        this.eventSourceTopology = eventSourceTopology;
        this.eventSourceManagerList = new CopyOnWriteArrayList<EventSourceManager>();
    }

    private void serviceRegsitration(final RpcProviderRegistry rpcRegistry){
        serviceRegistration = rpcRegistry.addRpcImplementation(EventSourceRegistryService.class, this);
    }
    
    @Override
    public Future<RpcResult<Void>> registerEventSourceManager(EventSourceManager eventSourceManager){
        Preconditions.checkNotNull(eventSourceManager);
        eventSourceManagerList.addIfAbsent(eventSourceManager);
        LOG.info("EventSourceManager {} has been registered", eventSourceManager);
        return resultRpcVoid();
    }

    @Override
    public Future<RpcResult<Void>> unRegisterEventSourceManager(EventSourceManager eventSourceManager){
        Preconditions.checkNotNull(eventSourceManager);
        if(eventSourceManagerList.contains(eventSourceManager) == false){
            return resultRpcVoid();
        }
        for(EventSourceRegistration esr : eventSourceManager.getEventSourceRegistrations()){
            unRegistreEventSource(esr);
        }
        eventSourceManagerList.remove(eventSourceManager);
        LOG.info("EventSourceManager {} has been unregistered", eventSourceManager);
        return resultRpcVoid();
    }

    @Override
    public Future<RpcResult<EventSourceRegistration>> registerEventSource(EventSource eventSource){
        EventSourceRegistration esr = new EventSourceRegistration(eventSource);
        this.getEventSourceTopology().register(eventSource);
        return immediateFuture(RpcResultBuilder.success(esr).build());
    }

    @Override
    public Future<RpcResult<Void>> unRegistreEventSource(EventSourceRegistration esr){
        EventSource eventSource = esr.getEventSource();
        this.getEventSourceTopology().unRegister(eventSource);
        esr.removeRegistration();
        return resultRpcVoid();
    }

    public EventSourceTopology getEventSourceTopology() {
        return eventSourceTopology;
    }
    
    @Override
    public void close() throws Exception {
        for(EventSourceManager esm : eventSourceManagerList){
            unRegisterEventSourceManager(esm);
        }
        if(serviceRegistration != null){
            serviceRegistration.close();
        }
    }

    private Future<RpcResult<Void>> resultRpcVoid(){
        return immediateFuture(RpcResultBuilder.success((Void) null).build());
    }
}
