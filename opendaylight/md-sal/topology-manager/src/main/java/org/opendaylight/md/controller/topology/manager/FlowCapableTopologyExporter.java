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

import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.binding.util.TypeSafeDataReader;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
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
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FlowCapableTopologyExporter implements //
        FlowTopologyDiscoveryListener, //
        OpendaylightInventoryListener //
{

    private final static Logger LOG = LoggerFactory.getLogger(FlowCapableTopologyExporter.class);
    public static TopologyKey topology = new TopologyKey(new TopologyId("flow:1"));

    // FIXME: Flow capable topology exporter should use transaction chaining API
    private DataProviderService dataService;

    public DataProviderService getDataService() {
        return dataService;
    }

    public void setDataService(final DataProviderService dataService) {
        this.dataService = dataService;
    }

    private InstanceIdentifier<Topology> topologyPath;

    public void start() {
        TopologyBuilder tb = new TopologyBuilder();
        tb.setKey(topology);
        topologyPath = InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class, topology).build();
        Topology top = tb.build();
        DataModificationTransaction tx = dataService.beginTransaction();
        tx.putOperationalData(topologyPath, top);
        tx.commit();
    }

    @Override
    public synchronized void onNodeRemoved(final NodeRemoved notification) {
        NodeId nodeId = toTopologyNodeId(getNodeKey(notification.getNodeRef()).getId());
        InstanceIdentifier<Node> nodeInstance = toNodeIdentifier(notification.getNodeRef());

        DataModificationTransaction tx = dataService.beginTransaction();
        tx.removeOperationalData(nodeInstance);
        removeAffectedLinks(tx, nodeId);
        try {
            tx.commit().get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Topology state export not successful. ",e);
        }
    }

    @Override
    public synchronized void onNodeUpdated(final NodeUpdated notification) {
        FlowCapableNodeUpdated fcnu = notification.getAugmentation(FlowCapableNodeUpdated.class);
        if (fcnu != null) {
            Node node = toTopologyNode(toTopologyNodeId(notification.getId()), notification.getNodeRef());
            InstanceIdentifier<Node> path = getNodePath(toTopologyNodeId(notification.getId()));
            DataModificationTransaction tx = dataService.beginTransaction();
            tx.putOperationalData(path, node);
            try {
                tx.commit().get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Topology state export not successful. ",e);
            }
        }
    }

    @Override
    public synchronized void onNodeConnectorRemoved(final NodeConnectorRemoved notification) {
        InstanceIdentifier<TerminationPoint> tpInstance = toTerminationPointIdentifier(notification
                .getNodeConnectorRef());
        TpId tpId = toTerminationPointId(getNodeConnectorKey(notification.getNodeConnectorRef()).getId());
        DataModificationTransaction tx = dataService.beginTransaction();
        tx.removeOperationalData(tpInstance);
        removeAffectedLinks(tx, tpId);
        try {
            tx.commit().get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Topology state export not successful. ",e);
        }

    }

    @Override
    public synchronized void onNodeConnectorUpdated(final NodeConnectorUpdated notification) {
        FlowCapableNodeConnectorUpdated fcncu = notification.getAugmentation(FlowCapableNodeConnectorUpdated.class);
        if (fcncu != null) {
            NodeId nodeId = toTopologyNodeId(getNodeKey(notification.getNodeConnectorRef()).getId());
            TerminationPoint point = toTerminationPoint(toTerminationPointId(notification.getId()),
                    notification.getNodeConnectorRef());
            InstanceIdentifier<TerminationPoint> path = tpPath(nodeId, point.getKey().getTpId());

            DataModificationTransaction tx = dataService.beginTransaction();
            tx.putOperationalData(path, point);
            if ((fcncu.getState() != null && fcncu.getState().isLinkDown())
                    || (fcncu.getConfiguration() != null && fcncu.getConfiguration().isPORTDOWN())) {
                removeAffectedLinks(tx, point.getTpId());
            }
            try {
                tx.commit().get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Topology state export not successful. ",e);
            }
        }
    }

    @Override
    public synchronized void onLinkDiscovered(final LinkDiscovered notification) {
        Link link = toTopologyLink(notification);
        InstanceIdentifier<Link> path = linkPath(link);
        DataModificationTransaction tx = dataService.beginTransaction();
        tx.putOperationalData(path, link);
        try {
            tx.commit().get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Topology state export not successful. ",e);
        }
    }

    @Override
    public synchronized void onLinkOverutilized(final LinkOverutilized notification) {
        // NOOP
    }

    @Override
    public synchronized void onLinkRemoved(final LinkRemoved notification) {
        InstanceIdentifier<Link> path = linkPath(toTopologyLink(notification));
        DataModificationTransaction tx = dataService.beginTransaction();
        tx.removeOperationalData(path);
        ;
    }

    @Override
    public synchronized void onLinkUtilizationNormal(final LinkUtilizationNormal notification) {
        // NOOP
    }

    private static InstanceIdentifier<Node> toNodeIdentifier(final NodeRef ref) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey invNodeKey = getNodeKey(ref);

        NodeKey nodeKey = new NodeKey(toTopologyNodeId(invNodeKey.getId()));
        return InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class, topology)
                .child(Node.class, nodeKey).build();
    }

    private InstanceIdentifier<TerminationPoint> toTerminationPointIdentifier(final NodeConnectorRef ref) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey invNodeKey = getNodeKey(ref);
        NodeConnectorKey invNodeConnectorKey = getNodeConnectorKey(ref);
        return tpPath(toTopologyNodeId(invNodeKey.getId()), toTerminationPointId(invNodeConnectorKey.getId()));
    }

    private void removeAffectedLinks(final DataModificationTransaction transaction, final NodeId id) {
        TypeSafeDataReader reader = TypeSafeDataReader.forReader(transaction);

        Topology topologyData = reader.readOperationalData(topologyPath);
        if (topologyData == null) {
            return;
        }
        for (Link link : topologyData.getLink()) {
            if (id.equals(link.getSource().getSourceNode()) || id.equals(link.getDestination().getDestNode())) {
                InstanceIdentifier<Link> path = InstanceIdentifier.builder(topologyPath)
                        .child(Link.class, link.getKey()).build();
                transaction.removeOperationalData(path);
            }
        }
    }

    private void removeAffectedLinks(final DataModificationTransaction transaction, final TpId id) {
        TypeSafeDataReader reader = TypeSafeDataReader.forReader(transaction);
        Topology topologyData = reader.readOperationalData(topologyPath);
        if (topologyData == null) {
            return;
        }
        for (Link link : topologyData.getLink()) {
            if (id.equals(link.getSource().getSourceTp()) || id.equals(link.getDestination().getDestTp())) {
                InstanceIdentifier<Link> path = InstanceIdentifier.builder(topologyPath)
                        .child(Link.class, link.getKey()).build();
                transaction.removeOperationalData(path);
            }
        }
    }

    private InstanceIdentifier<Node> getNodePath(final NodeId nodeId) {
        NodeKey nodeKey = new NodeKey(nodeId);
        return InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class, topology)
                .child(Node.class, nodeKey).build();
    }

    private InstanceIdentifier<TerminationPoint> tpPath(final NodeId nodeId, final TpId tpId) {
        NodeKey nodeKey = new NodeKey(nodeId);
        TerminationPointKey tpKey = new TerminationPointKey(tpId);
        return InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class, topology)
                .child(Node.class, nodeKey).child(TerminationPoint.class, tpKey).build();
    }

    private InstanceIdentifier<Link> linkPath(final Link link) {
        InstanceIdentifier<Link> linkInstanceId = InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, topology).child(Link.class, link.getKey()).build();
        return linkInstanceId;
    }
}
