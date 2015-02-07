/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.messagebus.app.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.mdsal.ioc.Callback;
import org.opendaylight.controller.mdsal.ioc.DataStore;
import org.opendaylight.controller.mdsal.ioc.Inject;
import org.opendaylight.controller.mdsal.ioc.MdSAL;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.EventSourceService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.inventory.rev140108.NetconfNode;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EventSourceManager implements DataChangeListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventSourceManager.class);
    private static final InstanceIdentifier<Node> INVENTORY_PATH = InstanceIdentifier.create(Nodes.class)
                                                                                     .child(Node.class);
    @Inject
    private DataStore dataStore;

    @Inject
    private MdSAL mdSal;

    @Inject
    private EventSourceTopology eventSourceTopology;

    @Callback(Callback.EVENT.MDSAL_READY)
    public void mdsalReady() {
        // annotation based listener registration should be implemented
        dataStore.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                                             INVENTORY_PATH,
                                             this,
                                             DataBroker.DataChangeScope.SUBTREE);

        LOGGER.info("EventSourceManager initialized.");
        //mdSal.addNotificationListener(EncapData.QNAME, new MessageTrapper());
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> event) {
        LOGGER.debug("[DataChangeEvent<InstanceIdentifier<?>, DataObject>: {}]", event);

        Node node = Util.getAffectedNode(event);
        // we listen on node tree, therefore we should rather throw IllegalStateException when node is null
        if ( node == null ) { return; }
        if ( isNetconfNode(node) == false ) { return; }
        if ( isEventSource(node) == false ) { return; }

        EventSource eventSource = new EventSource(mdSal, node.getKey().getId().getValue());
        mdSal.addRpcImplementation(node, EventSourceService.class, eventSource);
        eventSourceTopology.insert(node);
    }

    private boolean isNetconfNode(Node node)  {
        return ( node.getAugmentation(NetconfNode.class) != null );
    }

    public boolean isEventSource(Node node) {
        NetconfNode netconfNode = node.getAugmentation(NetconfNode.class);
        return isEventSource(netconfNode);
    }

    private boolean isEventSource(NetconfNode node) {
        boolean isEventSource = false;

        for (String capability : node.getInitialCapability()) {
            if(capability.contains("urn:ietf:params:xml:ns:netconf:notification")) {
                isEventSource = true;
            }
        }

        return isEventSource;
    }
}