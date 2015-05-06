/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.messagebus.eventsources.netconf;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.TopicId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.CreateSubscriptionInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netmod.notification.rev080714.netconf.streams.Stream;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

public class StreamNotificationTopicRegistration extends NotificationTopicRegistration {

    private static final Logger LOG = LoggerFactory.getLogger(StreamNotificationTopicRegistration.class);
    private static final NodeIdentifier STREAM_QNAME = new NodeIdentifier(QName.create(CreateSubscriptionInput.QNAME,"stream"));
    private static final SchemaPath CREATE_SUBSCRIPTION = SchemaPath.create(true, QName.create(CreateSubscriptionInput.QNAME, "create-subscription"));
    private static final NodeIdentifier START_TIME_SUBSCRIPTION = new NodeIdentifier(QName.create(CreateSubscriptionInput.QNAME,"startTime"));

    final private DOMMountPoint domMountPoint;
    final private String nodeId;
    final private NetconfEventSource netconfEventSource;
    final private Stream stream;
    private Date lastEventTime;

    private ConcurrentHashMap<SchemaPath, ListenerRegistration<NetconfEventSource>> notificationRegistrationMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<SchemaPath, ArrayList<TopicId>> notificationTopicMap = new ConcurrentHashMap<>();

    public StreamNotificationTopicRegistration(final Stream stream, final String notificationPrefix, NetconfEventSource netconfEventSource) {
        super(NotificationSourceType.NetconfDeviceStream, stream.getName().getValue(), notificationPrefix);
        this.domMountPoint = netconfEventSource.getDOMMountPoint();
        this.nodeId = netconfEventSource.getNode().getNodeId().getValue().toString();
        this.netconfEventSource = netconfEventSource;
        this.stream = stream;
        this.lastEventTime= null;
        setReplaySupported(this.stream.isReplaySupport());
        setActive(false);
    }

    void activateNotificationSource() {
        if(isActive() == false){
            LOG.info("Stream {} is not active on node {}. Will subscribe.", this.getStreamName(), this.nodeId);
            final ContainerNode input = Builders.containerBuilder().withNodeIdentifier(new NodeIdentifier(CreateSubscriptionInput.QNAME))
                    .withChild(ImmutableNodes.leafNode(STREAM_QNAME, this.getStreamName()))
                    .build();
            CheckedFuture<DOMRpcResult, DOMRpcException> csFuture = domMountPoint.getService(DOMRpcService.class).get().invokeRpc(CREATE_SUBSCRIPTION, input);
            try {
                csFuture.checkedGet();
                setActive(true);
            } catch (DOMRpcException e) {
                LOG.warn("Can not subscribe stream {} on node {}", this.getSourceName(), this.nodeId);
                setActive(false);
                return;
            }
        } else {
            LOG.info("Stream {} is now active on node {}", this.getStreamName(), this.nodeId);
        }
    }

    void reActivateNotificationSource(){
        if(isActive()){
            LOG.info("Stream {} is reactivating on node {}.", this.getStreamName(), this.nodeId);
            DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> inputBuilder =
                    Builders.containerBuilder().withNodeIdentifier(new NodeIdentifier(CreateSubscriptionInput.QNAME))
                    .withChild(ImmutableNodes.leafNode(STREAM_QNAME, this.getStreamName()));
            if(isReplaySupported() && this.getLastEventTime() != null){
                inputBuilder.withChild(ImmutableNodes.leafNode(START_TIME_SUBSCRIPTION, this.getLastEventTime()));
            }
            final ContainerNode input = inputBuilder.build();
            CheckedFuture<DOMRpcResult, DOMRpcException> csFuture = domMountPoint.getService(DOMRpcService.class).get().invokeRpc(CREATE_SUBSCRIPTION, input);
            try {
                csFuture.checkedGet();
                setActive(true);
            } catch (DOMRpcException e) {
                LOG.warn("Can not resubscribe stream {} on node {}", this.getSourceName(), this.nodeId);
                setActive(false);
                return;
            }
        }
    }

    @Override
    void deActivateNotificationSource() {
        // no operations need
    }

    private void closeStream() {
        if(isActive()){
            for(ListenerRegistration<NetconfEventSource> reg : notificationRegistrationMap.values()){
                reg.close();
            }
            notificationRegistrationMap.clear();
            notificationTopicMap.clear();
            setActive(false);
        }
    }

    private String getStreamName() {
        return getSourceName();
    }

    @Override
    ArrayList<TopicId> getNotificationTopicIds(SchemaPath notificationPath){
        return notificationTopicMap.get(notificationPath);
    }

    @Override
    boolean registerNotificationTopic(SchemaPath notificationPath, TopicId topicId){
        if(validateNotificationPath(notificationPath) == false){
            LOG.debug("Bad SchemaPath for notification try to register");
            return false;
        }
        final Optional<DOMNotificationService> notifyService = domMountPoint.getService(DOMNotificationService.class);
        if(notifyService.isPresent() == false){
            LOG.debug("DOMNotificationService is not present");
            return false;
        }
        ListenerRegistration<NetconfEventSource> registration = notifyService.get().registerNotificationListener(this.netconfEventSource,notificationPath);
        notificationRegistrationMap.put(notificationPath, registration);
        ArrayList<TopicId> topicIds = getNotificationTopicIds(notificationPath);
        if(topicIds == null){
            topicIds = new ArrayList<>();
            topicIds.add(topicId);
        } else {
            if(topicIds.contains(topicId) == false){
                topicIds.add(topicId);
            }
        }
        notificationTopicMap.put(notificationPath, topicIds);
        return true;
    }

    private boolean validateNotificationPath(SchemaPath notificationPath){
        if(notificationPath == null){
            return false;
        }
        String nameSpace = notificationPath.getLastComponent().toString();
        return nameSpace.startsWith(getNotificationUrnPrefix());
    }

    Optional<Date> getLastEventTime() {
        return Optional.fromNullable(lastEventTime);
    }


    void setLastEventTime(Date lastEventTime) {
        this.lastEventTime = lastEventTime;
    }

    @Override
    public void close() throws Exception {
        closeStream();
    }

    @Override
    void unRegisterNotificationTopic(TopicId topicId) {
        // TODO: use it when destroy topic will be implemented
    }

}
