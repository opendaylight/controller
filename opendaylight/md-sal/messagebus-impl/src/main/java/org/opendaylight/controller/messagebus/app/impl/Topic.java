/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.messagebus.app.impl;

import com.google.common.base.Preconditions;
import java.util.regex.Pattern;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.mdsal.MdSAL;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.NotificationPattern;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.TopicId;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.EventSourceService;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.JoinTopicInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.JoinTopicInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.LoggerFactory;

public class Topic implements DataChangeListener {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(Topic.class);
    private final NotificationPattern notificationPattern;
    private final Pattern nodeIdPattern;
    private final TopicId topicId;
    private final MdSAL mdSal;

    public Topic(final NotificationPattern notificationPattern, final String nodeIdPattern, final MdSAL mdSal) {
        this.notificationPattern = Preconditions.checkNotNull(notificationPattern);

        // FIXME: regex should be the language of nodeIdPattern
        final String regex = Util.wildcardToRegex(nodeIdPattern);
        this.nodeIdPattern = Pattern.compile(regex);
        this.mdSal = Preconditions.checkNotNull(mdSal);

        // FIXME: We need to perform some salting in order to make
        //        the topic IDs less predictable.
        this.topicId = new TopicId(Util.md5String(notificationPattern + nodeIdPattern));
    }

    public TopicId getTopicId() {
        return topicId;
    }

    @Override
    public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> event) {
        // TODO: affected must return topologyNode !!!
        final Node node = Util.getAffectedNode(event);
        if (nodeIdPattern.matcher(node.getId().getValue()).matches()) {
            notifyNode(node.getId());
        } else {
            LOG.debug("Skipping node {}", node.getId());
        }
    }

    public void notifyNode(final NodeId nodeId) {
        JoinTopicInput jti = getJoinTopicInputArgument(nodeId);
        EventSourceService ess = mdSal.getRpcService(EventSourceService.class);
        Preconditions.checkState(ess != null, "EventSourceService is not registered");

        ess.joinTopic(jti);
    }

    private JoinTopicInput getJoinTopicInputArgument(final NodeId nodeId) {
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
