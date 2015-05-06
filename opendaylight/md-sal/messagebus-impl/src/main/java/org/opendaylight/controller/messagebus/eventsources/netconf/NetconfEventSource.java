/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.messagebus.eventsources.netconf;

import static com.google.common.util.concurrent.Futures.immediateFuture;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMEvent;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMNotification;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationListener;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationPublishService;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationService;
import org.opendaylight.controller.messagebus.app.impl.TopicDOMNotification;
import org.opendaylight.controller.messagebus.app.impl.Util;
import org.opendaylight.controller.messagebus.spi.EventSource;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.NotificationPattern;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.TopicId;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.TopicNotification;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.JoinTopicInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.JoinTopicOutput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.JoinTopicOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.JoinTopicStatus;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netmod.notification.rev080714.Netconf;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netmod.notification.rev080714.netconf.Streams;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netmod.notification.rev080714.netconf.streams.Stream;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
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
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.CheckedFuture;

public class NetconfEventSource implements EventSource, DOMNotificationListener {

    private static final Logger LOG = LoggerFactory.getLogger(NetconfEventSource.class);

    private static final NodeIdentifier TOPIC_NOTIFICATION_ARG = new NodeIdentifier(TopicNotification.QNAME);
    private static final NodeIdentifier EVENT_SOURCE_ARG = new NodeIdentifier(QName.create(TopicNotification.QNAME, "node-id"));
    private static final NodeIdentifier TOPIC_ID_ARG = new NodeIdentifier(QName.create(TopicNotification.QNAME, "topic-id"));
    private static final NodeIdentifier PAYLOAD_ARG = new NodeIdentifier(QName.create(TopicNotification.QNAME, "payload"));
    private static final String ConnectionNotificationSourceName = "ConnectionNotificationSource";

    private final String nodeId;
    private final Node node;

    private final DOMMountPoint netconfMount;
    private final MountPoint mountPoint;
    private final DOMNotificationPublishService domPublish;

    private final Map<String, String> urnPrefixToStreamMap; // key = urnPrefix, value = StreamName
    private final List<NotificationTopicRegistration> notificationTopicRegistrationList = new ArrayList<>();

    public NetconfEventSource(final Node node, final Map<String, String> streamMap, final DOMMountPoint netconfMount, final MountPoint mountPoint, final DOMNotificationPublishService publishService) {
        this.netconfMount = Preconditions.checkNotNull(netconfMount);
        this.mountPoint = Preconditions.checkNotNull(mountPoint);
        this.node = Preconditions.checkNotNull(node);
        this.urnPrefixToStreamMap = Preconditions.checkNotNull(streamMap);
        this.domPublish = Preconditions.checkNotNull(publishService);
        this.nodeId = node.getNodeId().getValue();
        this.initializeNotificationTopicRegistrationList();

        LOG.info("NetconfEventSource [{}] created.", this.nodeId);
    }

    private void initializeNotificationTopicRegistrationList() {
        notificationTopicRegistrationList.add(new ConnectionNotificationTopicRegistration(ConnectionNotificationSourceName, this));
        Optional<Map<String, Stream>> streamMap = getAvailableStreams();
        if(streamMap.isPresent()){
            for (String urnPrefix : this.urnPrefixToStreamMap.keySet()) {
                final String streamName = this.urnPrefixToStreamMap.get(urnPrefix);
                if(streamMap.get().containsKey(streamName)){
                    notificationTopicRegistrationList.add(new StreamNotificationTopicRegistration(streamMap.get().get(streamName),urnPrefix, this));
                }
            }
        }
    }

    private Optional<Map<String, Stream>> getAvailableStreams(){

        Map<String,Stream> streamMap = null;
        InstanceIdentifier<Streams> pathStream = InstanceIdentifier.builder(Netconf.class).child(Streams.class).build();
        Optional<DataBroker> dataBroker = this.mountPoint.getService(DataBroker.class);

        if(dataBroker.isPresent()){

            ReadOnlyTransaction tx = dataBroker.get().newReadOnlyTransaction();
            CheckedFuture<Optional<Streams>, ReadFailedException> checkFeature = tx.read(LogicalDatastoreType.OPERATIONAL,pathStream);

            try {
                Optional<Streams> streams = checkFeature.checkedGet();
                if(streams.isPresent()){
                    streamMap = new HashMap<>();
                    for(Stream stream : streams.get().getStream()){
                        streamMap.put(stream.getName().getValue(), stream);
                    }
                }
            } catch (ReadFailedException e) {
                LOG.warn("Can not read streams for node {}",this.nodeId);
            }

        }

        return Optional.fromNullable(streamMap);
    }

    @Override
    public Future<RpcResult<JoinTopicOutput>> joinTopic(final JoinTopicInput input) {

        final NotificationPattern notificationPattern = input.getNotificationPattern();
        final List<SchemaPath> matchingNotifications = getMatchingNotifications(notificationPattern);
        return registerTopic(input.getTopicId(),matchingNotifications);

    }

    private synchronized Future<RpcResult<JoinTopicOutput>> registerTopic(final TopicId topicId, final List<SchemaPath> notificationsToSubscribe){

        JoinTopicStatus joinTopicStatus = JoinTopicStatus.Down;
        if(notificationsToSubscribe != null && notificationsToSubscribe.isEmpty() == false){
            final Optional<DOMNotificationService> notifyService = getDOMMountPoint().getService(DOMNotificationService.class);
            if(notifyService.isPresent()){
                int subscribedStreams = 0;
                for(SchemaPath schemaNotification : notificationsToSubscribe){
                   for(NotificationTopicRegistration reg : notificationTopicRegistrationList){
                      LOG.info("Source of notification {} is activating, TopicId {}", reg.getSourceName(), topicId.getValue() );
                      reg.activateNotificationSource();
                      boolean regSuccess = reg.registerNotificationTopic(schemaNotification, topicId);
                      if(regSuccess){
                         subscribedStreams = subscribedStreams +1;
                      }
                   }
                }
                if(subscribedStreams > 0){
                    joinTopicStatus = JoinTopicStatus.Up;
                }
            }
        }

        final JoinTopicOutput output = new JoinTopicOutputBuilder().setStatus(joinTopicStatus).build();
        return immediateFuture(RpcResultBuilder.success(output).build());

    }

    public void reActivateStreams(){
        for (NotificationTopicRegistration reg : notificationTopicRegistrationList) {
           LOG.info("Source of notification {} is reactivating on node {}", reg.getSourceName(), this.nodeId);
            reg.reActivateNotificationSource();
        }
    }

    public void deActivateStreams(){
        for (NotificationTopicRegistration reg : notificationTopicRegistrationList) {
           LOG.info("Source of notification {} is deactivating on node {}", reg.getSourceName(), this.nodeId);
            reg.deActivateNotificationSource();
        }
    }

    @Override
    public void onNotification(final DOMNotification notification) {
        LOG.info("Notification {} has been arrived...",notification.getType());
        SchemaPath notificationPath = notification.getType();
        Date notificationEventTime = null;
        if(notification instanceof DOMEvent){
            notificationEventTime = ((DOMEvent) notification).getEventTime();
        }
        for(NotificationTopicRegistration notifReg : notificationTopicRegistrationList){
            ArrayList<TopicId> topicIdsForNotification = notifReg.getNotificationTopicIds(notificationPath);
            if(topicIdsForNotification != null && topicIdsForNotification.isEmpty() == false){

                if(notifReg instanceof StreamNotificationTopicRegistration){
                    StreamNotificationTopicRegistration streamReg = (StreamNotificationTopicRegistration)notifReg;
                    streamReg.setLastEventTime(notificationEventTime);
                }

                for(TopicId topicId : topicIdsForNotification){
                    publishNotification(notification, topicId);
                    LOG.info("Notification {} has been published for TopicId {}",notification.getType(), topicId.getValue());
                }

            }
        }
    }

    private void publishNotification(final DOMNotification notification, TopicId topicId){
         final ContainerNode topicNotification = Builders.containerBuilder()
                 .withNodeIdentifier(TOPIC_NOTIFICATION_ARG)
                 .withChild(ImmutableNodes.leafNode(TOPIC_ID_ARG, topicId))
                 .withChild(ImmutableNodes.leafNode(EVENT_SOURCE_ARG, this.nodeId))
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

        final SchemaContext context = getDOMMountPoint().getSchemaContext();
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

    private List<SchemaPath> getMatchingNotifications(NotificationPattern notificationPattern){
        // FIXME: default language should already be regex
        final String regex = Util.wildcardToRegex(notificationPattern.getValue());

        final Pattern pattern = Pattern.compile(regex);
        List<SchemaPath> availableNotifications = getAvailableNotifications();
        if(availableNotifications == null || availableNotifications.isEmpty()){
            return null;
        }
        return Util.expandQname(availableNotifications, pattern);
    }

    @Override
    public void close() throws Exception {
        for(NotificationTopicRegistration streamReg : notificationTopicRegistrationList){
            streamReg.close();
        }
    }

    @Override
    public NodeKey getSourceNodeKey(){
        return getNode().getKey();
    }

    @Override
    public List<SchemaPath> getAvailableNotifications() {

        final List<SchemaPath> availNotifList = new ArrayList<>();
        // add Event Source Connection status notification
        availNotifList.add(ConnectionNotificationTopicRegistration.EVENT_SOURCE_STATUS_PATH);

        // FIXME: use SchemaContextListener to get changes asynchronously
        final Set<NotificationDefinition> availableNotifications = getDOMMountPoint().getSchemaContext().getNotifications();
        // add all known notifications from netconf device
        for (final NotificationDefinition nd : availableNotifications) {
            availNotifList.add(nd.getPath());
        }
        return availNotifList;
    }

    public Node getNode() {
        return node;
    }

    DOMMountPoint getDOMMountPoint() {
        return netconfMount;
    }

    MountPoint getMountPoint() {
        return mountPoint;
    }

    NetconfNode getNetconfNode(){
        return node.getAugmentation(NetconfNode.class);
    }

}
