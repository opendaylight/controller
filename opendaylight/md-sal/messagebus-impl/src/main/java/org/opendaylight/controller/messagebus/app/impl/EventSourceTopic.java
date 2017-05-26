/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.messagebus.app.impl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.NotificationPattern;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.TopicId;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.DisJoinTopicInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.DisJoinTopicInputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.EventSourceService;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.JoinTopicInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.JoinTopicInputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.JoinTopicOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.LoggerFactory;

public class EventSourceTopic implements DataChangeListener, AutoCloseable {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(EventSourceTopic.class);
    private final NotificationPattern notificationPattern;
    private final EventSourceService sourceService;
    private final Pattern nodeIdPattern;
    private final TopicId topicId;
    private ListenerRegistration<DataChangeListener> listenerRegistration;
    private final CopyOnWriteArraySet<InstanceIdentifier<?>> joinedEventSources = new CopyOnWriteArraySet<>();

    public static EventSourceTopic create(final NotificationPattern notificationPattern, final String nodeIdRegexPattern, final EventSourceTopology eventSourceTopology){
        final EventSourceTopic est = new EventSourceTopic(notificationPattern, nodeIdRegexPattern, eventSourceTopology.getEventSourceService());
        est.registerListner(eventSourceTopology);
        est.notifyExistingNodes(eventSourceTopology);
        return est;
    }

    private EventSourceTopic(final NotificationPattern notificationPattern, final String nodeIdRegexPattern, final EventSourceService sourceService) {
        this.notificationPattern = Preconditions.checkNotNull(notificationPattern);
        this.sourceService = Preconditions.checkNotNull(sourceService);
        this.nodeIdPattern = Pattern.compile(nodeIdRegexPattern);
        this.topicId = new TopicId(getUUIDIdent());
        this.listenerRegistration = null;
        LOG.info("EventSourceTopic created - topicId {}", topicId.getValue());
    }

    public TopicId getTopicId() {
        return topicId;
    }

    @Override
    public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> event) {

        for (final Map.Entry<InstanceIdentifier<?>, DataObject> createdEntry : event.getCreatedData().entrySet()) {
            if (createdEntry.getValue() instanceof Node) {
                final Node node = (Node) createdEntry.getValue();
                LOG.debug("Create node...");
                if (getNodeIdRegexPattern().matcher(node.getNodeId().getValue()).matches()) {
                    LOG.debug("Matched...");
                    notifyNode(EventSourceTopology.EVENT_SOURCE_TOPOLOGY_PATH.child(Node.class, node.getKey()));
                }
            }
        }

        for (final Map.Entry<InstanceIdentifier<?>, DataObject> changeEntry : event.getUpdatedData().entrySet()) {
            if (changeEntry.getValue() instanceof Node) {
                final Node node = (Node) changeEntry.getValue();
                if (getNodeIdRegexPattern().matcher(node.getNodeId().getValue()).matches()) {
                    notifyNode(changeEntry.getKey());
                }
            }
        }
    }

    public void notifyNode(final InstanceIdentifier<?> nodeId) {
        LOG.debug("Notify node: {}", nodeId);
        try {
            final RpcResult<JoinTopicOutput> rpcResultJoinTopic = sourceService.joinTopic(getJoinTopicInputArgument(nodeId)).get();
            if(rpcResultJoinTopic.isSuccessful() == false){
                for(final RpcError err : rpcResultJoinTopic.getErrors()){
                    LOG.error("Can not join topic: [{}] on node: [{}]. Error: {}",getTopicId().getValue(),nodeId.toString(),err.toString());
                }
            } else {
                joinedEventSources.add(nodeId);
            }
        } catch (final Exception e) {
            LOG.error("Could not invoke join topic for node {}", nodeId);
        }
    }

    private void notifyExistingNodes(final EventSourceTopology eventSourceTopology){
        LOG.debug("Notify existing nodes");
        final Pattern nodeRegex = this.nodeIdPattern;

        final ReadOnlyTransaction tx = eventSourceTopology.getDataBroker().newReadOnlyTransaction();
        final CheckedFuture<Optional<Topology>, ReadFailedException> future =
                tx.read(LogicalDatastoreType.OPERATIONAL, EventSourceTopology.EVENT_SOURCE_TOPOLOGY_PATH);

        Futures.addCallback(future, new FutureCallback<Optional<Topology>>(){

            @Override
            public void onSuccess(final Optional<Topology> data) {
                if(data.isPresent()) {
                     final List<Node> nodes = data.get().getNode();
                     if(nodes != null){
                        for (final Node node : nodes) {
                             if (nodeRegex.matcher(node.getNodeId().getValue()).matches()) {
                                 notifyNode(EventSourceTopology.EVENT_SOURCE_TOPOLOGY_PATH.child(Node.class, node.getKey()));
                             }
                         }
                     }
                }
                tx.close();
            }

            @Override
            public void onFailure(final Throwable t) {
                LOG.error("Can not notify existing nodes", t);
                tx.close();
            }

        });

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

    public Pattern getNodeIdRegexPattern() {
        return nodeIdPattern;
    }

    private DisJoinTopicInput getDisJoinTopicInputArgument(final InstanceIdentifier<?> eventSourceNodeId){
        final NodeRef nodeRef = new NodeRef(eventSourceNodeId);
        final DisJoinTopicInput dji = new DisJoinTopicInputBuilder()
                .setNode(nodeRef.getValue())
                .setTopicId(topicId)
                .build();
        return dji;
    }

    private void registerListner(final EventSourceTopology eventSourceTopology) {
        this.listenerRegistration =
                eventSourceTopology.getDataBroker().registerDataChangeListener(
                        LogicalDatastoreType.OPERATIONAL,
                        EventSourceTopology.EVENT_SOURCE_TOPOLOGY_PATH,
                        this,
                        DataBroker.DataChangeScope.SUBTREE);
    }

    @Override
    public void close() {
        if(this.listenerRegistration != null){
            this.listenerRegistration.close();
        }
        for(final InstanceIdentifier<?> eventSourceNodeId : joinedEventSources){
            try {
                final RpcResult<Void> result = sourceService.disJoinTopic(getDisJoinTopicInputArgument(eventSourceNodeId)).get();
                if(result.isSuccessful() == false){
                    for(final RpcError err : result.getErrors()){
                        LOG.error("Can not destroy topic: [{}] on node: [{}]. Error: {}",getTopicId().getValue(),eventSourceNodeId,err.toString());
                    }
                }
            } catch (InterruptedException | ExecutionException ex) {
                LOG.error("Can not close event source topic / destroy topic {} on node {}.", this.topicId.getValue(), eventSourceNodeId, ex);
            }
        }
        joinedEventSources.clear();
    }

    private static String getUUIDIdent(){
        final UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }
}
