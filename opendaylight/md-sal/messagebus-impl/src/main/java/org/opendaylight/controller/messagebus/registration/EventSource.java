/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.messagebus.registration;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.EventSourceService;
import org.opendaylight.yangtools.yang.binding.BaseIdentity;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * All sources of events (notifications) have to implement this interface. 
 * Implementations of methods give necessary information for registration sources of events for message-bus
 * @author madamjak
 *
 * @param <T>
 */
public interface EventSource<T> extends EventSourceService {
    /**
     * InstanceIdentifier is used for DataChangeListener registration
     * 
     * @return
     */
    InstanceIdentifier<?> getInstanceIdentifier();

    /**
     * Source is used to registration of EventSource in an EventSource Topology
     * 
     * @return
     */
    T getSource();

    /**
     * It represents base identity for routed RPC registration
     * 
     * @return
     */
    Class<? extends BaseIdentity> getRpcPathBaseIdentity();

    /**
     * It is used to registration path in RPC registration process
     * 
     * @return
     */
    InstanceIdentifier<?> getRpcPathInstanceIdentifier();
}