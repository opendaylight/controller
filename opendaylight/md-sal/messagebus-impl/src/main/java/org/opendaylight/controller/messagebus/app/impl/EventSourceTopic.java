/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.messagebus.app.impl;

import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.NotificationPattern;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.TopicId;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.DisJoinTopicInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.DisJoinTopicInputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.DisJoinTopicOutput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.EventSourceService;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.JoinTopicInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.JoinTopicInputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.JoinTopicOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.LoggerFactory;

@Deprecated(forRemoval = true)
public final class EventSourceTopic implements DataTreeChangeListener<Node>, AutoCloseable {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(EventSourceTopic.class);

    private final CopyOnWriteArraySet<InstanceIdentifier<?>> joinedEventSources = new CopyOnWriteArraySet<>();
    private final NotificationPattern notificationPattern;
    private final EventSourceService sourceService;
    private final Pattern nodeIdPattern;
    private final TopicId topicId;
    private ListenerRegistration<?> listenerRegistration;

    public static EventSourceTopic create(final NotificationPattern notificationPattern,
            final String nodeIdRegexPattern, final EventSourceTopology eventSourceTopology) {
        final EventSourceTopic est = new EventSourceTopic(notificationPattern, nodeIdRegexPattern,
                eventSourceTopology.getEventSourceService());
        est.registerListner(eventSourceTopology);
        est.notifyExistingNodes(eventSourceTopology);
        return est;
    }

    private EventSourceTopic(final NotificationPattern notificationPattern, final String nodeIdRegexPattern,
            final EventSourceService sourceService) {
        this.notificationPattern = requireNonNull(notificationPattern);
        this.sourceService = requireNonNull(sourceService);
        this.nodeIdPattern = Pattern.compile(nodeIdRegexPattern);
        this.topicId = new TopicId(getUUIDIdent());
        this.listenerRegistration = null;
        LOG.info("EventSourceTopic created - topicId {}", topicId.getValue());
    }

    public TopicId getTopicId() {
        return topicId;
    }

    @Override
    public void onDataTreeChanged(final Collection<DataTreeModification<Node>> changes) {
        for (DataTreeModification<Node> change: changes) {
            final DataObjectModification<Node> rootNode = change.getRootNode();
            switch (rootNode.getModificationType()) {
                case WRITE:
                case SUBTREE_MODIFIED:
                    final Node node = rootNode.getDataAfter();
                    if (getNodeIdRegexPattern().matcher(node.getNodeId().getValue()).matches()) {
                        notifyNode(change.getRootPath().getRootIdentifier());
                    }
                    break;
                default:
                    break;
            }
        }
    }

    public void notifyNode(final InstanceIdentifier<?> nodeId) {
        LOG.debug("Notify node: {}", nodeId);
        try {
            final RpcResult<JoinTopicOutput> rpcResultJoinTopic =
                    sourceService.joinTopic(getJoinTopicInputArgument(nodeId)).get();
            if (!rpcResultJoinTopic.isSuccessful()) {
                for (final RpcError err : rpcResultJoinTopic.getErrors()) {
                    LOG.error("Can not join topic: [{}] on node: [{}]. Error: {}", getTopicId().getValue(),
                            nodeId.toString(), err.toString());
                }
            } else {
                joinedEventSources.add(nodeId);
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Could not invoke join topic for node {}", nodeId);
        }
    }

    private void notifyExistingNodes(final EventSourceTopology eventSourceTopology) {
        LOG.debug("Notify existing nodes");
        final Pattern nodeRegex = this.nodeIdPattern;

        final FluentFuture<Optional<Topology>> future;
        try (ReadTransaction tx = eventSourceTopology.getDataBroker().newReadOnlyTransaction()) {
            future = tx.read(LogicalDatastoreType.OPERATIONAL, EventSourceTopology.EVENT_SOURCE_TOPOLOGY_PATH);
        }

        future.addCallback(new FutureCallback<Optional<Topology>>() {
            @Override
            public void onSuccess(final Optional<Topology> data) {
                if (data.isPresent()) {
                    for (final Node node : data.get().nonnullNode().values()) {
                        if (nodeRegex.matcher(node.getNodeId().getValue()).matches()) {
                            notifyNode(EventSourceTopology.EVENT_SOURCE_TOPOLOGY_PATH.child(Node.class, node.key()));
                        }
                    }
                }
            }

            @Override
            public void onFailure(final Throwable ex) {
                LOG.error("Can not notify existing nodes", ex);
            }
        }, MoreExecutors.directExecutor());
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

    private DisJoinTopicInput getDisJoinTopicInputArgument(final InstanceIdentifier<?> eventSourceNodeId) {
        final NodeRef nodeRef = new NodeRef(eventSourceNodeId);
        final DisJoinTopicInput dji = new DisJoinTopicInputBuilder()
                .setNode(nodeRef.getValue())
                .setTopicId(topicId)
                .build();
        return dji;
    }

    private void registerListner(final EventSourceTopology eventSourceTopology) {
        this.listenerRegistration = eventSourceTopology.getDataBroker().registerDataTreeChangeListener(
            DataTreeIdentifier.create(LogicalDatastoreType.OPERATIONAL,
                EventSourceTopology.EVENT_SOURCE_TOPOLOGY_PATH.child(Node.class)), this);
    }

    @Override
    public void close() {
        if (this.listenerRegistration != null) {
            this.listenerRegistration.close();
        }
        for (final InstanceIdentifier<?> eventSourceNodeId : joinedEventSources) {
            try {
                final RpcResult<DisJoinTopicOutput> result = sourceService
                        .disJoinTopic(getDisJoinTopicInputArgument(eventSourceNodeId)).get();
                if (result.isSuccessful() == false) {
                    for (final RpcError err : result.getErrors()) {
                        LOG.error("Can not destroy topic: [{}] on node: [{}]. Error: {}", getTopicId().getValue(),
                                eventSourceNodeId, err.toString());
                    }
                }
            } catch (InterruptedException | ExecutionException ex) {
                LOG.error("Can not close event source topic / destroy topic {} on node {}.", this.topicId.getValue(),
                        eventSourceNodeId, ex);
            }
        }
        joinedEventSources.clear();
    }

    private static String getUUIDIdent() {
        final UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }
}
