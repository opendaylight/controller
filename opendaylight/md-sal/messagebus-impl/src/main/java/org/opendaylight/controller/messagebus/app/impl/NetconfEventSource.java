/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.messagebus.app.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMNotification;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationListener;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationPublishService;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.controller.sal.binding.api.RpcConsumerRegistry;
import org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.NotificationPattern;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.TopicNotification;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.EventSourceService;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.JoinTopicInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.JoinTopicOutput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.JoinTopicOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.CreateSubscriptionInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.NotificationsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.inventory.rev140108.NetconfNode;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.AnyXmlNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;

public class NetconfEventSource implements EventSourceService, DOMNotificationListener, DataChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(NetconfEventSource.class);

    private static final NodeIdentifier TOPIC_NOTIFICATION_ARG = new NodeIdentifier(TopicNotification.QNAME);
    private static final NodeIdentifier EVENT_SOURCE_ARG = new NodeIdentifier(QName.create(TopicNotification.QNAME, "node-id"));
    private static final NodeIdentifier PAYLOAD_ARG = new NodeIdentifier(QName.create(TopicNotification.QNAME, "payload"));

    private static final NodeIdentifier STREAM_QNAME = new NodeIdentifier(QName.create(CreateSubscriptionInput.QNAME,"stream"));
    private static final SchemaPath CREATE_SUBSCRIPTION = SchemaPath.create(true, QName.create(CreateSubscriptionInput.QNAME, "create-subscription"));


    private final String nodeId;


    private final DOMMountPoint netconfMount;
    private final DOMNotificationPublishService domPublish;
    private final NotificationsService notificationRpcService;

    private final Set<String> activeStreams = new ConcurrentSkipListSet<>();

    private final Map<String, String> urnPrefixToStreamMap;


    public NetconfEventSource(final String nodeId, final Map<String, String> streamMap, final DOMMountPoint netconfMount, final DOMNotificationPublishService publishService, final MountPoint bindingMount) {
        this.netconfMount = netconfMount;
        this.notificationRpcService = bindingMount.getService(RpcConsumerRegistry.class).get().getRpcService(NotificationsService.class);
        this.nodeId = nodeId;
        this.urnPrefixToStreamMap = streamMap;
        this.domPublish = publishService;
        LOG.info("NetconfEventSource [{}] created.", nodeId);
    }

    @Override
    public Future<RpcResult<JoinTopicOutput>> joinTopic(final JoinTopicInput input) {
        final NotificationPattern notificationPattern = input.getNotificationPattern();

        // FIXME: default language should already be regex
        final String regex = Util.wildcardToRegex(notificationPattern.getValue());

        final Pattern pattern = Pattern.compile(regex);
        final List<SchemaPath> matchingNotifications = Util.expandQname(availableNotifications(), pattern);
        registerNotificationListener(matchingNotifications);
        final JoinTopicOutput output = new JoinTopicOutputBuilder().build();
        return com.google.common.util.concurrent.Futures.immediateFuture(RpcResultBuilder.success(output).build());
    }

    private List<SchemaPath> availableNotifications() {
        // FIXME: use SchemaContextListener to get changes asynchronously
        final Set<NotificationDefinition> availableNotifications = netconfMount.getSchemaContext().getNotifications();
        final List<SchemaPath> qNs = new ArrayList<>(availableNotifications.size());
        for (final NotificationDefinition nd : availableNotifications) {
            qNs.add(nd.getPath());
        }
        return qNs;
    }

    private void registerNotificationListener(final List<SchemaPath> notificationsToSubscribe) {

        final Optional<DOMNotificationService> notifyService = netconfMount.getService(DOMNotificationService.class);
        if(notifyService.isPresent()) {
            for (final SchemaPath qName : notificationsToSubscribe) {
                startSubscription(qName);
            }
            // FIXME: Capture registration
            notifyService.get().registerNotificationListener(this, notificationsToSubscribe);
        }
    }

    private void startSubscription(final SchemaPath path) {
        final String streamName = resolveStream(path.getLastComponent());

        if (streamIsActive(streamName) == false) {
            LOG.info("Stream {} is not active on node {}. Will subscribe.", streamName, nodeId);
            startSubscription(streamName);
        }
    }

    private void resubscribeToActiveStreams() {
        for (final String streamName : activeStreams) {
            startSubscription(streamName);
        }
    }

    private synchronized void startSubscription(final String streamName) {
        final ContainerNode input = Builders.containerBuilder().withNodeIdentifier(new NodeIdentifier(CreateSubscriptionInput.QNAME))
            .withChild(ImmutableNodes.leafNode(STREAM_QNAME, streamName))
            .build();
        netconfMount.getService(DOMRpcService.class).get().invokeRpc(CREATE_SUBSCRIPTION, input);
        activeStreams.add(streamName);
    }

    private String resolveStream(final QName qName) {
        String streamName = null;

        for (final Map.Entry<String, String> entry : urnPrefixToStreamMap.entrySet()) {
            final String nameSpace = qName.getNamespace().toString();
            final String urnPrefix = entry.getKey();
            if( nameSpace.startsWith(urnPrefix) ) {
                streamName = entry.getValue();
                break;
            }
        }

        return streamName;
    }

    private boolean streamIsActive(final String streamName) {
        return activeStreams.contains(streamName);
    }

    @Override
    public void onNotification(final DOMNotification notification) {
        final ContainerNode topicNotification = Builders.containerBuilder()
                .withNodeIdentifier(TOPIC_NOTIFICATION_ARG)
                .withChild(ImmutableNodes.leafNode(EVENT_SOURCE_ARG, nodeId))
                .withChild(encapsulate(notification))
                .build();
        try {
            domPublish.putNotification(new TopicDOMNotification(topicNotification));
        } catch (final InterruptedException e) {
            throw Throwables.propagate(e);
        }
    }

    private AnyXmlNode encapsulate(final DOMNotification body) {
        // FIXME: Introduce something like AnyXmlWithNormalizedNodeData in Yangtools
        final Document doc = XmlUtil.newDocument();
        final Optional<String> namespace = Optional.of(PAYLOAD_ARG.getNodeType().getNamespace().toString());
        final Element element = XmlUtil.createElement(doc , "payload", namespace);


        final DOMResult result = new DOMResult(element);

        final SchemaContext context = netconfMount.getSchemaContext();
        final SchemaPath schemaPath = body.getType();
        try {
            NetconfMessageTransformUtil.writeNormalizedNode(body.getBody(), result, schemaPath, context);
            return Builders.anyXmlBuilder().withNodeIdentifier(PAYLOAD_ARG)
                    .withValue(new DOMSource(element))
                    .build();
        } catch (IOException | XMLStreamException e) {
            LOG.error("Unable to encapsulate notification.",e);
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        boolean wasConnected = false;
        boolean nowConnected = false;

        for (final Map.Entry<InstanceIdentifier<?>, DataObject> changeEntry : change.getOriginalData().entrySet()) {
            if ( isNetconfNode(changeEntry) ) {
                final NetconfNode nn = (NetconfNode)changeEntry.getValue();
                wasConnected = nn.isConnected();
            }
        }

        for (final Map.Entry<InstanceIdentifier<?>, DataObject> changeEntry : change.getUpdatedData().entrySet()) {
            if ( isNetconfNode(changeEntry) ) {
                final NetconfNode nn = (NetconfNode)changeEntry.getValue();
                nowConnected = nn.isConnected();
            }
        }

        if (wasConnected == false && nowConnected == true) {
            resubscribeToActiveStreams();
        }
    }

    private static boolean isNetconfNode(final Map.Entry<InstanceIdentifier<?>, DataObject> changeEntry )  {
        return NetconfNode.class.equals(changeEntry.getKey().getTargetType());
    }

}
