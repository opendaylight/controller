/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.md.controller.topology.manager;

import static org.opendaylight.md.controller.topology.manager.FlowCapableNodeMapping.getNodeConnectorKey;
import static org.opendaylight.md.controller.topology.manager.FlowCapableNodeMapping.getNodeKey;
import static org.opendaylight.md.controller.topology.manager.FlowCapableNodeMapping.toTerminationPoint;
import static org.opendaylight.md.controller.topology.manager.FlowCapableNodeMapping.toTerminationPointId;
import static org.opendaylight.md.controller.topology.manager.FlowCapableNodeMapping.toTopologyLink;
import static org.opendaylight.md.controller.topology.manager.FlowCapableNodeMapping.toTopologyNode;
import static org.opendaylight.md.controller.topology.manager.FlowCapableNodeMapping.toTopologyNodeId;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
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

class FlowCapableTopologyExporter implements FlowTopologyDiscoveryListener, OpendaylightInventoryListener {
    private final InstanceIdentifier<Topology> topology;
    private final OperationProcessor processor;

    FlowCapableTopologyExporter(final OperationProcessor processor, final InstanceIdentifier<Topology> topology) {
        this.processor = Preconditions.checkNotNull(processor);
        this.topology = Preconditions.checkNotNull(topology);
    }

    @Override
    public void onNodeRemoved(final NodeRemoved notification) {
        processor.enqueueOperation(new TopologyOperation() {
            @Override
            public void applyOperation(final DataModificationTransaction transaction) {
                NodeId nodeId = toTopologyNodeId(getNodeKey(notification.getNodeRef()).getId());
                InstanceIdentifier<Node> nodeInstance = toNodeIdentifier(notification.getNodeRef());
                transaction.removeOperationalData(nodeInstance);
            }
        });
    }

    @Override
    public void onNodeUpdated(final NodeUpdated notification) {
        FlowCapableNodeUpdated fcnu = notification.getAugmentation(FlowCapableNodeUpdated.class);
        if (fcnu != null) {
            processor.enqueueOperation(new TopologyOperation() {
                @Override
                public void applyOperation(final DataModificationTransaction transaction) {
                    Node node = toTopologyNode(toTopologyNodeId(notification.getId()), notification.getNodeRef());
                    InstanceIdentifier<Node> path = getNodePath(toTopologyNodeId(notification.getId()));
                    transaction.putOperationalData(path, node);
                }
            });
        }
    }

    @Override
    public void onNodeConnectorRemoved(final NodeConnectorRemoved notification) {
        processor.enqueueOperation(new TopologyOperation() {
            @Override
            public void applyOperation(final DataModificationTransaction transaction) {
                InstanceIdentifier<TerminationPoint> tpInstance = toTerminationPointIdentifier(notification
                        .getNodeConnectorRef());
                TpId tpId = toTerminationPointId(getNodeConnectorKey(notification.getNodeConnectorRef()).getId());

                transaction.removeOperationalData(tpInstance);
            }
        });
    }

    @Override
    public void onNodeConnectorUpdated(final NodeConnectorUpdated notification) {
        final FlowCapableNodeConnectorUpdated fcncu = notification.getAugmentation(FlowCapableNodeConnectorUpdated.class);
        if (fcncu != null) {
            processor.enqueueOperation(new TopologyOperation() {
                @Override
                public void applyOperation(final DataModificationTransaction transaction) {
                    NodeId nodeId = toTopologyNodeId(getNodeKey(notification.getNodeConnectorRef()).getId());
                    TerminationPoint point = toTerminationPoint(toTerminationPointId(notification.getId()),
                            notification.getNodeConnectorRef());
                    InstanceIdentifier<TerminationPoint> path = tpPath(nodeId, point.getKey().getTpId());

                    transaction.putOperationalData(path, point);
                }
            });
        }
    }

    @Override
    public void onLinkDiscovered(final LinkDiscovered notification) {
        processor.enqueueOperation(new TopologyOperation() {
            @Override
            public void applyOperation(final DataModificationTransaction transaction) {
                Link link = toTopologyLink(notification);
                InstanceIdentifier<Link> path = linkPath(link);
                transaction.putOperationalData(path, link);
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
            public void applyOperation(final DataModificationTransaction transaction) {
                transaction.removeOperationalData(linkPath(toTopologyLink(notification)));
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
