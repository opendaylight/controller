/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.messagebus.api;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

import com.google.common.util.concurrent.ListenableFuture;
/**
 * Interface represents service that is provided by infrastructure (message bus). Implementation of message bus is responsible
 * to create service that implements this interface. EventSourceRegistry service is used by Event Source Manager to register
 * or unregister event sources managed by them (see {@link org.opendaylight.controller.messagebus.api.EventSource}).
 * Event source manager can obtain reference to EventSourceRegistry by configuration subsystem.
 */
public interface EventSourceRegistry {
    /**
     * Method is used to register EventSource into message bus. Registration process add sourceNode into internal
     * register (event source topology) and register eventSource as RpcService.
     * Node key of sourceNode and {@link EventSource#getSourceNodeKey()} have to be same.
     * @param sourceNode             node represents event source
     * @param eventSource            instance of EventSource
     * @return                       Instance of EventSourceRegistration that will be used in unregistration process
     * @throws IllegalStateException If Nodekey of node is different to {@link EventSource#getSourceNodeKey()}
     */
    ListenableFuture<EventSourceRegistration> registerEventSource(final Node sourceNode, final EventSource eventSource);

    /**
     * Method is used to unregister EventSource. Event Source Manager has to keep instances of EventSourceRegistration
     * and use them for unregistration process. Unregistration process remove node form internal register and unregister
     * relevant EventSource RpcService.
     * Unregister process will NOT call methods close() of EventSourceRegistration.
     * Event Source Manager is responsible to call it if necessary.
     * Event Source Manager is responsible to release all instances relevant to unregistered event source.
     * @param esr   instance of EventSourceRegistration
     */
    ListenableFuture<Void> unRegisterEventSource(EventSourceRegistration esr);

}