/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.messagebus.app.impl;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.mdsal.ioc.MdSAL;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.EventSourceService;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.JoinTopicInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.JoinTopicInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class Topic implements DataChangeListener {
    private final String notificationPattern;
    private final String nodeIdPattern;
    private final String topicId;
    private final MdSAL mdSal;

    public Topic(String notificationPattern, String nodeIdPattern, MdSAL mdSal) {
        Preconditions.checkNotNull(notificationPattern);
        Preconditions.checkNotNull(nodeIdPattern);
        Preconditions.checkNotNull(mdSal);

        this.notificationPattern = notificationPattern;
        this.nodeIdPattern = notificationPattern;
        this.mdSal = mdSal;

        String keyToHash = notificationPattern + nodeIdPattern;
        this.topicId = Util.md5String(keyToHash);
    }

    public String getTopicId() {
        return topicId;
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> event) {
        // TODO: Check if node is within nodeIdPattern
        // TODO: affected must return topologyNode !!!
        Node node = Util.getAffectedNode(event);
        notifyNode(node.getId());
    }

    public void notifyNode(NodeId nodeId) {
        JoinTopicInput jti = getJoinTopicInputArgument(nodeId);
        mdSal.getRpcService(EventSourceService.class).joinTopic(jti);
    }

    private JoinTopicInput getJoinTopicInputArgument(NodeId nodeId) {
        NodeRef nodeRef = MdSAL.createNodeRef(nodeId);
        JoinTopicInput jti =
                new JoinTopicInputBuilder()
                        .setNode(nodeRef.getValue())
                        .setTopicId(topicId)
                        .setNotificationPattern(notificationPattern)
                        .build();
        return jti;
    }
}
