/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.messagebus.app.impl;

import com.google.common.base.Preconditions;
import java.util.Map;
import java.util.regex.Pattern;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.NotificationPattern;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.TopicId;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.EventSourceService;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.JoinTopicInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.JoinTopicInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.LoggerFactory;

public class EventSourceTopic implements DataChangeListener {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(EventSourceTopic.class);
    private final NotificationPattern notificationPattern;
    private final EventSourceService sourceService;
    private final Pattern nodeIdPattern;
    private final TopicId topicId;

    public EventSourceTopic(final NotificationPattern notificationPattern, final String nodeIdPattern, final EventSourceService eventSource) {
        this.notificationPattern = Preconditions.checkNotNull(notificationPattern);
        this.sourceService = eventSource;

        // FIXME: regex should be the language of nodeIdPattern
        final String regex = Util.wildcardToRegex(nodeIdPattern);
        this.nodeIdPattern = Pattern.compile(regex);


        // FIXME: We need to perform some salting in order to make
        //        the topic IDs less predictable.
        this.topicId = new TopicId(Util.md5String(notificationPattern + nodeIdPattern));
    }

    public TopicId getTopicId() {
        return topicId;
    }

    @Override
    public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> event) {
                for (final Map.Entry<InstanceIdentifier<?>, DataObject> changeEntry : event.getUpdatedData().entrySet()) {
            if (changeEntry.getValue() instanceof Node) {
                final Node node = (Node) changeEntry.getValue();
                if (nodeIdPattern.matcher(node.getId().getValue()).matches()) {
                    notifyNode(changeEntry.getKey());
                }
            }
        }
    }

    public void notifyNode(final InstanceIdentifier<?> nodeId) {
        try {
            sourceService.joinTopic(getJoinTopicInputArgument(nodeId));
        } catch (final Exception e) {
            LOG.error("Could not invoke join topic for node %s", nodeId);
        }
    }

    private JoinTopicInput getJoinTopicInputArgument(final InstanceIdentifier<?> path) {
        final NodeRef nodeRef = new NodeRef(path);
        final JoinTopicInput jti =
                new JoinTopicInputBuilder()
                        .setNode(nodeRef.getValue())
                        .setTopicId(topicId)
                        .setNotificationPattern(notificationPattern)
                        .build();
        return jti;
    }


}
