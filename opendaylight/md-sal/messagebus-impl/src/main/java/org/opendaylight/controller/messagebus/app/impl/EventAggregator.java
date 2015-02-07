/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.messagebus.app.impl;

import java.util.List;
import java.util.concurrent.Future;
import org.opendaylight.controller.mdsal.ioc.Callback;
import org.opendaylight.controller.mdsal.ioc.Inject;
import org.opendaylight.controller.mdsal.ioc.MdSAL;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.CreateTopicInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.CreateTopicOutput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.CreateTopicOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.DestroyTopicInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.EventAggregatorService;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.Node1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: implement topic created notification
public class EventAggregator implements EventAggregatorService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventAggregator.class);

    @Inject
    private MdSAL mdSAL;

    @Inject
    private EventSourceTopology eventSourceTopology;

    @Callback(Callback.EVENT.MDSAL_READY)
    public void mdsalReady() {
        mdSAL.addRpcImplementation(EventAggregatorService.class, this);
    }

    @Override
    public Future<RpcResult<CreateTopicOutput>> createTopic(CreateTopicInput input) {
        LOGGER.info("Received Topic creation request: NotificationPattern -> {}, NodeIdPattern -> {}",
                input.getNotificationPattern(),
                input.getNodeIdPattern());

        Topic topic = new Topic(input.getNotificationPattern(), input.getNodeIdPattern(), mdSAL);

        //# Make sure we capture all nodes from now on
        eventSourceTopology.registerDataChangeListener(topic);

        //# Notify existing nodes
        //# Code reader note: Context of Node type is NetworkTopology
        List<Node> nodes = eventSourceTopology.snapshot();
        for (Node node : nodes) {
            NodeId nodeIdToNotify = node.getAugmentation(Node1.class).getEventSourceNode();
            topic.notifyNode(nodeIdToNotify);
        }

        CreateTopicOutput cto = new CreateTopicOutputBuilder()
                .setTopicId(topic.getTopicId())
                .build();

        return Util.resultFor(cto);
    }

    @Override
    public Future<RpcResult<Void>> destroyTopic(DestroyTopicInput input) {
        // 1. UNREGISTER DATA CHANGE LISTENER -> ?
        // 2. CLOSE TOPIC
        return null;
    }
}
