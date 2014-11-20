/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.md.controller.topology.manager;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnectorUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.FlowTopologyDiscoveryListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.LinkDiscovered;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.LinkOverutilized;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.LinkRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.LinkUtilizationNormal;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.OpendaylightInventoryListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

import static org.opendaylight.md.controller.topology.manager.FlowCapableNodeMapping.getNodeConnectorKey;
import static org.opendaylight.md.controller.topology.manager.FlowCapableNodeMapping.getNodeKey;
import static org.opendaylight.md.controller.topology.manager.FlowCapableNodeMapping.toTerminationPoint;
import static org.opendaylight.md.controller.topology.manager.FlowCapableNodeMapping.toTerminationPointId;
import static org.opendaylight.md.controller.topology.manager.FlowCapableNodeMapping.toTopologyLink;
import static org.opendaylight.md.controller.topology.manager.FlowCapableNodeMapping.toTopologyNode;
import static org.opendaylight.md.controller.topology.manager.FlowCapableNodeMapping.toTopologyNodeId;

class FlowCapableTopologyExporter implements FlowTopologyDiscoveryListener, OpendaylightInventoryListener {

    private static final Logger LOG = LoggerFactory.getLogger(FlowCapableTopologyExporter.class);
    private final InstanceIdentifier<Topology> topology;
    private final OperationProcessor processor;

    FlowCapableTopologyExporter(final OperationProcessor processor,
            final InstanceIdentifier<Topology> topology) {
        this.processor = Preconditions.checkNotNull(processor);
        this.topology = Preconditions.checkNotNull(topology);
    }

    @Override
    public void onNodeRemoved(final NodeRemoved notification) {

        final NodeId nodeId = toTopologyNodeId(getNodeKey(notification.getNodeRef()).getId());
        final InstanceIdentifier<Node> nodeInstance = toNodeIdentifier(notification.getNodeRef());

        processor.enqueueOperation(new TopologyOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction transaction) {
                    removeAffectedLinks(nodeId, transaction);
                    transaction.delete(LogicalDatastoreType.OPERATIONAL, nodeInstance);
            }

            @Override
            public String toString() {
                return "onNodeRemoved";
            }
        });
    }

    @Override
    public void onNodeUpdated(final NodeUpdated notification) {
        FlowCapableNodeUpdated fcnu = notification.getAugmentation(FlowCapableNodeUpdated.class);
        if (fcnu != null) {
            processor.enqueueOperation(new TopologyOperation() {
                @Override
                public void applyOperation(final ReadWriteTransaction transaction) {
                    final Node node = toTopologyNode(toTopologyNodeId(notification.getId()), notification.getNodeRef());
                    final InstanceIdentifier<Node> path = getNodePath(toTopologyNodeId(notification.getId()));
                    transaction.merge(LogicalDatastoreType.OPERATIONAL, path, node, true);
                }

                @Override
                public String toString() {
                    return "onNodeUpdated";
                }
            });
        }
    }

    @Override
    public void onNodeConnectorRemoved(final NodeConnectorRemoved notification) {

        final InstanceIdentifier<TerminationPoint> tpInstance = toTerminationPointIdentifier(
                notification.getNodeConnectorRef());

        final InstanceIdentifier<Node> node = tpInstance.firstIdentifierOf(Node.class);

        final TpId tpId = toTerminationPointId(getNodeConnectorKey(
                notification.getNodeConnectorRef()).getId());

        processor.enqueueOperation(new TopologyOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction transaction) {
                Optional<Node> nodeOptional = Optional.absent();
                try {
                    nodeOptional = transaction.read(LogicalDatastoreType.OPERATIONAL, node).checkedGet();
                } catch (ReadFailedException e) {
                    LOG.error("Error occured when trying to read NodeConnector ", e);
                }
                if (nodeOptional.isPresent()) {
                    removeAffectedLinks(tpId, transaction);
                    transaction.delete(LogicalDatastoreType.OPERATIONAL, tpInstance);
                }
            }

            @Override
            public String toString() {
                return "onNodeConnectorRemoved";
            }
        });
    }

    @Override
    public void onNodeConnectorUpdated(final NodeConnectorUpdated notification) {
        final FlowCapableNodeConnectorUpdated fcncu = notification.getAugmentation(
                FlowCapableNodeConnectorUpdated.class);
        if (fcncu != null) {
            processor.enqueueOperation(new TopologyOperation() {
                @Override
                public void applyOperation(final ReadWriteTransaction transaction) {
                    final NodeId nodeId = toTopologyNodeId(getNodeKey(notification.getNodeConnectorRef()).getId());
                    TerminationPoint point = toTerminationPoint(toTerminationPointId(notification.getId()),
                            notification.getNodeConnectorRef());
                    final InstanceIdentifier<TerminationPoint> path = tpPath(nodeId, point.getKey().getTpId());
                    transaction.merge(LogicalDatastoreType.OPERATIONAL, path, point, true);
                    if ((fcncu.getState() != null && fcncu.getState().isLinkDown())
                            || (fcncu.getConfiguration() != null && fcncu.getConfiguration().isPORTDOWN())) {
                        removeAffectedLinks(point.getTpId(), transaction);
                    }
                }

                @Override
                public String toString() {
                    return "onNodeConnectorUpdated";
                }
            });
        }
    }

    @Override
    public void onLinkDiscovered(final LinkDiscovered notification) {
        processor.enqueueOperation(new TopologyOperation() {
            @Override
            public void applyOperation(final ReadWriteTransaction transaction) {
                final Link link = toTopologyLink(notification);
                final InstanceIdentifier<Link> path = linkPath(link);
                transaction.merge(LogicalDatastoreType.OPERATIONAL, path, link, true);
            }

            @Override
            public String toString() {
                return "onLinkDiscovered";
            }
        });
    }

    @Override
    public void onLinkOverutilized(final LinkOverutilized notification) {
        // NOOP
    }

    @Override
    public void onLinkRemoved(final LinkRemoved notification) {
        processor.enqueueOperation(new TopologyOperation() {
            @Override
            public void applyOperation(final ReadWriteTransaction transaction) {
                Optional<Link> linkOptional = Optional.absent();
                try {
                    // read that checks if link exists (if we do not do this we might get an exception on delete)
                    linkOptional = transaction.read(LogicalDatastoreType.OPERATIONAL,
                            linkPath(toTopologyLink(notification))).checkedGet();
                } catch (ReadFailedException e) {
                    LOG.error("Error occured when trying to read Link ", e);
                }
                if (linkOptional.isPresent()) {
                    transaction.delete(LogicalDatastoreType.OPERATIONAL, linkPath(toTopologyLink(notification)));
                }
            }

            @Override
            public String toString() {
                return "onLinkRemoved";
            }
        });
    }

    @Override
    public void onLinkUtilizationNormal(final LinkUtilizationNormal notification) {
        // NOOP
    }

    private InstanceIdentifier<Node> toNodeIdentifier(final NodeRef ref) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey invNodeKey = getNodeKey(ref);
        NodeKey nodeKey = new NodeKey(toTopologyNodeId(invNodeKey.getId()));
        return topology.child(Node.class, nodeKey);
    }

    private InstanceIdentifier<TerminationPoint> toTerminationPointIdentifier(final NodeConnectorRef ref) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey invNodeKey = getNodeKey(ref);
        NodeConnectorKey invNodeConnectorKey = getNodeConnectorKey(ref);
        return tpPath(toTopologyNodeId(invNodeKey.getId()), toTerminationPointId(invNodeConnectorKey.getId()));
    }

    private void removeAffectedLinks(final NodeId id, final ReadWriteTransaction transaction) {
        Optional<Topology> topologyOptional = Optional.absent();
        try {
            topologyOptional = transaction.read(LogicalDatastoreType.OPERATIONAL, topology).checkedGet();
        } catch (ReadFailedException e) {
            LOG.error("Error reading topology data for topology {}", topology, e);
        }
        if (topologyOptional.isPresent()) {
            removeAffectedLinks(id, topologyOptional, transaction);
        }
    }

    private void removeAffectedLinks(final NodeId id, Optional<Topology> topologyOptional, ReadWriteTransaction transaction) {
        if (!topologyOptional.isPresent()) {
            return;
        }

        List<Link> linkList = topologyOptional.get().getLink() != null ?
                topologyOptional.get().getLink() : Collections.<Link> emptyList();
        for (Link link : linkList) {
            if (id.equals(link.getSource().getSourceNode()) ||
                    id.equals(link.getDestination().getDestNode())) {
                transaction.delete(LogicalDatastoreType.OPERATIONAL, linkPath(link));
            }
        }
    }

    private void removeAffectedLinks(final TpId id, final ReadWriteTransaction transaction) {
        Optional<Topology> topologyOptional = Optional.absent();
        try {
            topologyOptional = transaction.read(LogicalDatastoreType.OPERATIONAL, topology).checkedGet();
        } catch (ReadFailedException e) {
            LOG.error("Error reading topology data for topology {}", topology, e);
        }
        if (topologyOptional.isPresent()) {
            removeAffectedLinks(id, topologyOptional, transaction);
        }
    }

    private void removeAffectedLinks(final TpId id, Optional<Topology> topologyOptional, ReadWriteTransaction transaction) {
        if (!topologyOptional.isPresent()) {
            return;
        }

        List<Link> linkList = topologyOptional.get().getLink() != null
                ? topologyOptional.get().getLink() : Collections.<Link> emptyList();
        for (Link link : linkList) {
            if (id.equals(link.getSource().getSourceTp()) ||
                    id.equals(link.getDestination().getDestTp())) {
                transaction.delete(LogicalDatastoreType.OPERATIONAL, linkPath(link));
            }
        }
    }

    private InstanceIdentifier<Node> getNodePath(final NodeId nodeId) {
        return topology.child(Node.class, new NodeKey(nodeId));
    }

    private InstanceIdentifier<TerminationPoint> tpPath(final NodeId nodeId, final TpId tpId) {
        NodeKey nodeKey = new NodeKey(nodeId);
        TerminationPointKey tpKey = new TerminationPointKey(tpId);
        return topology.child(Node.class, nodeKey).child(TerminationPoint.class, tpKey);
    }

    private InstanceIdentifier<Link> linkPath(final Link link) {
        return topology.child(Link.class, link.getKey());
    }
}
