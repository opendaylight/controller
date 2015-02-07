/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.messagebus.app.impl;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.mdsal.MdSAL;
import org.opendaylight.controller.sal.core.api.notify.NotificationListener;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.EncapData;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.EventSourceService;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.JoinTopicInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.JoinTopicOutput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.CreateSubscriptionInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.CreateSubscriptionInputBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.NotificationsService;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.StreamNameType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.inventory.rev140108.NetconfNode;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.impl.ImmutableCompositeNode;
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetconfEventSource implements EventSourceService, NotificationListener, DataChangeListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(NetconfEventSource.class);

    private final MdSAL mdSal;
    private final String nodeId;

    private final List<String> activeStreams = new ArrayList<>();

    private Map<String, String> urnPrefixToStreamMap;

    public NetconfEventSource(MdSAL mdSal, String nodeId, Map<String, String> streamMap) {
        Preconditions.checkNotNull(mdSal);
        Preconditions.checkNotNull(nodeId);

        this.mdSal = mdSal;
        this.nodeId = nodeId;
        this.urnPrefixToStreamMap = streamMap;

        LOGGER.info("NetconfEventSource [{}] created.", nodeId);
    }

    @Override
    public Future<RpcResult<JoinTopicOutput>> joinTopic(JoinTopicInput input) {
        // TODO: implement in async way
        String notificationPattern = input.getNotificationPattern();
        List<QName> matchingNotifications = Util.expandQname(availableNotifications(), notificationPattern);
        registerNotificationListener(matchingNotifications);
        return null;
    }

    private List<QName> availableNotifications() {
        // TODO: can we just expect new schemas will not pop-up during runtime?
        Set<NotificationDefinition> availableNotifications = mdSal.getSchemaContext(nodeId).getNotifications();
        List<QName> qNs = new ArrayList<>(availableNotifications.size());
        for (NotificationDefinition nd : availableNotifications) {
            qNs.add(nd.getQName());
        }

        return qNs;
    }

    private void registerNotificationListener(List<QName> notificationsToSubscribe) {
        for (QName qName : notificationsToSubscribe) {
            startSubscription(qName);
            mdSal.addNotificationListener(nodeId, qName, this);
        }
    }

    private synchronized void startSubscription(QName qName) {
        String streamName = resolveStream(qName);

        if (streamIsActive(streamName) == false) {
            LOGGER.info("Stream {} is not active on node {}. Will subscribe.", streamName, nodeId);
            startSubscription(streamName);
        }
    }

    private synchronized void resubscribeToActiveStreams() {
        for (String streamName : activeStreams) {
            startSubscription(streamName);
        }
    }

    private synchronized void startSubscription(String streamName) {
        CreateSubscriptionInput subscriptionInput = getSubscriptionInput(streamName);
        mdSal.getRpcService(nodeId, NotificationsService.class).createSubscription(subscriptionInput);
        activeStreams.add(streamName);
    }

    private static CreateSubscriptionInput getSubscriptionInput(String streamName) {
        CreateSubscriptionInputBuilder csib = new CreateSubscriptionInputBuilder();
        csib.setStream(new StreamNameType(streamName));
        return csib.build();
    }

    private String resolveStream(QName qName) {
        String streamName = null;

        for (Map.Entry<String, String> entry : urnPrefixToStreamMap.entrySet()) {
            String nameSpace = qName.getNamespace().toString();
            String urnPrefix = entry.getKey();
            if( nameSpace.startsWith(urnPrefix) ) {
                streamName = entry.getValue();
                break;
            }
        }

        return streamName;
    }

    private boolean streamIsActive(String streamName) {
        return activeStreams.contains(streamName);
    }

    // PASS
    @Override public Set<QName> getSupportedNotifications() {
        return null;
    }

    @Override
    public void onNotification(CompositeNode notification) {
        LOGGER.info("NetconfEventSource {} received notification {}. Will publish to MD-SAL.", nodeId, notification);
        ImmutableCompositeNode payload = ImmutableCompositeNode.builder()
                .setQName(QName.create(EncapData.QNAME, "payload"))
                .add(notification).toInstance();
        ImmutableCompositeNode icn = ImmutableCompositeNode.builder()
                .setQName(EncapData.QNAME)
                .add(payload)
                .addLeaf("event-source", nodeId)
                .toInstance();

        mdSal.publishNotification(icn);
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        boolean wasConnected = false;
        boolean nowConnected = false;

        for (Map.Entry<InstanceIdentifier<?>, DataObject> changeEntry : change.getOriginalData().entrySet()) {
            if ( isNetconfNode(changeEntry) ) {
                NetconfNode nn = (NetconfNode)changeEntry.getValue();
                wasConnected = nn.isConnected();
            }
        }

        for (Map.Entry<InstanceIdentifier<?>, DataObject> changeEntry : change.getUpdatedData().entrySet()) {
            if ( isNetconfNode(changeEntry) ) {
                NetconfNode nn = (NetconfNode)changeEntry.getValue();
                nowConnected = nn.isConnected();
            }
        }

        if (wasConnected == false && nowConnected == true) {
            resubscribeToActiveStreams();
        }
    }

    private static boolean isNetconfNode(Map.Entry<InstanceIdentifier<?>, DataObject> changeEntry )  {
        return NetconfNode.class.equals(changeEntry.getKey().getTargetType());
    }

}
