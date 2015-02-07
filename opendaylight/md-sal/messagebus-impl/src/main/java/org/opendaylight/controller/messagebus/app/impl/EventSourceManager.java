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
import org.opendaylight.controller.mdsal.DataStore;
import org.opendaylight.controller.mdsal.MdSAL;
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
    private final DataStore dataStore;
    private final MdSAL mdSal;
    private final EventSourceTopology eventSourceTopology;

    public EventSourceManager(DataStore dataStore, MdSAL mdSal, EventSourceTopology eventSourceTopology) {
        this.dataStore = dataStore;
        this.mdSal = mdSal;
        this.eventSourceTopology = eventSourceTopology;
    }

    public void mdsalReady() {
        // annotation based listener registration should be implemented
        dataStore.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                                             INVENTORY_PATH,
                                             this,
                                             DataBroker.DataChangeScope.SUBTREE);

        LOGGER.info("EventSourceManager initialized.");
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> event) {
        LOGGER.debug("[DataChangeEvent<InstanceIdentifier<?>, DataObject>: {}]", event);

        Node node = Util.getAffectedNode(event);
        // we listen on node tree, therefore we should rather throw IllegalStateException when node is null
        if ( node == null ) {
            LOGGER.debug("OnDataChanged Event. Node is null.");
            return;
        }
        if ( isNetconfNode(node) == false ) {
            LOGGER.debug("OnDataChanged Event. Not a Netconf node.");
            return;
        }
        if ( isEventSource(node) == false ) {
            LOGGER.debug("OnDataChanged Event. Node an EventSource node.");
            return;
        }

        NetconfEventSource netconfEventSource = new NetconfEventSource(mdSal, node.getKey().getId().getValue());
        mdSal.addRpcImplementation(node, EventSourceService.class, netconfEventSource);
        eventSourceTopology.insert(node);
    }

    private boolean isNetconfNode(Node node)  {
        return node.getAugmentation(NetconfNode.class) != null ;
    }

    public boolean isEventSource(Node node) {
        NetconfNode netconfNode = node.getAugmentation(NetconfNode.class);
        return isEventSource(netconfNode);
    }

    private boolean isEventSource(NetconfNode node) {
        for (String capability : node.getInitialCapability()) {
            if(capability.startsWith("urn:ietf:params:xml:ns:netconf:notification")) {
                return true;
            }
        }

        return false;
    }
}