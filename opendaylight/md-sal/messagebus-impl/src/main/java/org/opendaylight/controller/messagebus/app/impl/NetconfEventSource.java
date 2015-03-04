/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.messagebus.app.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.mdsal.MdSAL;
import org.opendaylight.controller.messagebus.registration.EventSource;
import org.opendaylight.controller.sal.core.api.notify.NotificationListener;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.NotificationPattern;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.TopicNotification;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.JoinTopicInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.JoinTopicOutput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.CreateSubscriptionInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.CreateSubscriptionInputBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.NotificationsService;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.StreamNameType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.inventory.rev140108.NetconfNode;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.BaseIdentity;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.impl.ImmutableCompositeNode;
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public class NetconfEventSource implements EventSource<Node>,  NotificationListener, DataChangeListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(NetconfEventSource.class);

    private final MdSAL mdSal;
    private final String nodeId;
    private final Node node;
    private final List<String> activeStreams = new ArrayList<>();

    private final Map<String, String> urnPrefixToStreamMap;
    
    public NetconfEventSource(final MdSAL mdSal, final Node node, final Map<String, String> streamMap){
      Preconditions.checkNotNull(mdSal);
      Preconditions.checkNotNull(node);
      Preconditions.checkNotNull(streamMap);
      this.mdSal = mdSal;
      this.nodeId =  node.getKey().getId().getValue();
      this.node = node;
      this.urnPrefixToStreamMap = streamMap;
      LOGGER.info("NetconfEventSource [{}] created.", nodeId);
    }

    @Override
    public Future<RpcResult<JoinTopicOutput>> joinTopic(final JoinTopicInput input) {
        final NotificationPattern notificationPattern = input.getNotificationPattern();

        // FIXME: default language should already be regex
        final String regex = Util.wildcardToRegex(notificationPattern.getValue());

        final Pattern pattern = Pattern.compile(regex);
        List<QName> matchingNotifications = Util.expandQname(availableNotifications(), pattern);
        registerNotificationListener(matchingNotifications);
        return null;
    }

    private List<QName> availableNotifications() {
        // FIXME: use SchemaContextListener to get changes asynchronously
        Set<NotificationDefinition> availableNotifications = mdSal.getSchemaContext(nodeId).getNotifications();
        List<QName> qNs = new ArrayList<>(availableNotifications.size());
        for (NotificationDefinition nd : availableNotifications) {
            qNs.add(nd.getQName());
        }

        return qNs;
    }

    private void registerNotificationListener(final List<QName> notificationsToSubscribe) {
        for (QName qName : notificationsToSubscribe) {
            startSubscription(qName);
            // FIXME: do not lose this registration
            final ListenerRegistration<NotificationListener> reg = mdSal.addNotificationListener(nodeId, qName, this);
        }
    }

    private synchronized void startSubscription(final QName qName) {
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

    private synchronized void startSubscription(final String streamName) {
        CreateSubscriptionInput subscriptionInput = getSubscriptionInput(streamName);
        mdSal.getRpcService(nodeId, NotificationsService.class).createSubscription(subscriptionInput);
        activeStreams.add(streamName);
    }

    private static CreateSubscriptionInput getSubscriptionInput(final String streamName) {
        CreateSubscriptionInputBuilder csib = new CreateSubscriptionInputBuilder();
        csib.setStream(new StreamNameType(streamName));
        return csib.build();
    }

    private String resolveStream(final QName qName) {
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

    private boolean streamIsActive(final String streamName) {
        return activeStreams.contains(streamName);
    }

    // PASS
    @Override public Set<QName> getSupportedNotifications() {
        return null;
    }

    @SuppressWarnings("deprecation")
	@Override
    public void onNotification(final CompositeNode notification) {
        LOGGER.info("NetconfEventSource {} received notification {}. Will publish to MD-SAL.", nodeId, notification);
        ImmutableCompositeNode payload = ImmutableCompositeNode.builder()
                .setQName(QName.create(TopicNotification.QNAME, "payload"))
                .add(notification).toInstance();
        ImmutableCompositeNode icn = ImmutableCompositeNode.builder()
                .setQName(TopicNotification.QNAME)
                .add(payload)
                .addLeaf("event-source", nodeId)
                .toInstance();

        mdSal.publishNotification(icn);
    }

    @Override
    public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
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

    private static boolean isNetconfNode(final Map.Entry<InstanceIdentifier<?>, DataObject> changeEntry )  {
        return NetconfNode.class.equals(changeEntry.getKey().getTargetType());
    }

	@Override
	public InstanceIdentifier<?> getInstanceIdentifier() {
		InstanceIdentifier<NetconfNode> nodeInstanceIdentifier =
                InstanceIdentifier.create(Nodes.class)
                        .child(Node.class, node.getKey())
                        .augmentation(NetconfNode.class);
		return nodeInstanceIdentifier;
	}

	@Override
	public Node getSource() {
		return this.node;
	}

	@Override
	public Class<? extends BaseIdentity> getRpcPathBaseIdentity() {
		return NodeContext.class;
	}

	@Override
	public InstanceIdentifier<?> getRpcPathInstanceIdentifier() {
		NodeRef nodeRef = createNodeRef(node.getId());
		return nodeRef.getValue();
	}

	private NodeRef createNodeRef(NodeId nodeId) {
        NodeKey nodeKey = new NodeKey(nodeId);
        InstanceIdentifier<Node> path = InstanceIdentifier
                .builder(Nodes.class)
                .child(Node.class, nodeKey)
                .build();
        return new NodeRef(path);
    }
	
}
