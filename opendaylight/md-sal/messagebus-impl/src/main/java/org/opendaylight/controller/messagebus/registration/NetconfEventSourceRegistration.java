/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.messagebus.registration;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.mdsal.MdSAL;
import org.opendaylight.controller.messagebus.app.impl.NetconfEventSource;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.EventSourceService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetconfEventSourceRegistration extends EventSourceRegistration<Node> {

    private static final Logger LOG = LoggerFactory.getLogger(NetconfEventSourceRegistration.class);

    private static LogicalDatastoreType dataStoreType = LogicalDatastoreType.OPERATIONAL;

    public static NetconfEventSourceRegistration register(MdSAL mdSal,EventSource<Node> instance){

        NetconfEventSourceRegistration esr = new NetconfEventSourceRegistration(instance);
        esr.registration( mdSal);
        LOG.debug("EventSource {} was registered sucessfuly.",instance.getSource());
        return esr;

    }

    private ListenerRegistration<DataChangeListener> listenerRegistration;
    private BindingAwareBroker.RoutedRpcRegistration<EventSourceService> rpcRegistration;
    private final NetconfEventSource netconfEnventSource;
    
    private NetconfEventSourceRegistration(EventSource<Node> instance) {
        super(instance);
        if(NetconfEventSource.class.equals(instance.getClass()) == false){
            throw new IllegalStateException("Bad type of instance, NetconfEventSource is expected");
        }
        netconfEnventSource = (NetconfEventSource) instance;
    }

    private void registration(MdSAL mdSal){

        listenerRegistration = mdSal.getDataBroker().registerDataChangeListener(dataStoreType,
                netconfEnventSource.getInstanceIdentifier(), netconfEnventSource, DataBroker.DataChangeScope.SUBTREE);

        rpcRegistration = mdSal.getBindingAwareContext().addRoutedRpcImplementation(EventSourceService.class, netconfEnventSource);
        rpcRegistration.registerPath(netconfEnventSource.getRpcPathBaseIdentity(), netconfEnventSource.getRpcPathInstanceIdentifier());
    }

    public EventSource<Node> getEventSource(){
        return getInstance();
    }

    @Override
    public void removeRegistration() {

        EventSource<Node> instance = getInstance();

        if(listenerRegistration != null){
            listenerRegistration.close();
        }
        if(rpcRegistration != null){
            rpcRegistration.unregisterPath(instance.getRpcPathBaseIdentity(), instance.getRpcPathInstanceIdentifier());;
        }
        LOG.debug("EventSource {} was unregistered sucessfuly.",instance.getSource());
    }

}