/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.connect.netconf.sal;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfDeviceCapabilities;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeFields.ConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.netconf.node.fields.AvailableCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.netconf.node.fields.UnavailableCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.netconf.node.fields.UnavailableCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.netconf.node.fields.unavailable.capabilities.UnavailableCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.netconf.node.fields.unavailable.capabilities.UnavailableCapability.FailureReason;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.netconf.node.fields.unavailable.capabilities.UnavailableCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.network.topology.topology.topology.types.TopologyNetconf;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetconfDeviceTopologyAdapter implements AutoCloseable {

    public static final Logger logger = LoggerFactory.getLogger(NetconfDeviceTopologyAdapter.class);
    public static final Function<Entry<QName, FailureReason>, UnavailableCapability> UNAVAILABLE_CAPABILITY_TRANSFORMER = new Function<Entry<QName, FailureReason>, UnavailableCapability>() {
        @Override
        public UnavailableCapability apply(final Entry<QName, FailureReason> input) {
            return new UnavailableCapabilityBuilder()
                    .setCapability(input.getKey().toString())
                    .setFailureReason(input.getValue()).build();
        }
    };
    public static final Function<QName, String> AVAILABLE_CAPABILITY_TRANSFORMER = new Function<QName, String>() {
        @Override
        public String apply(QName qName) {
            return qName.toString();
        }
    };

    private final RemoteDeviceId id;
    private final DataBroker dataService;

    private final InstanceIdentifier<NetworkTopology> networkTopologyPath;
    private final KeyedInstanceIdentifier<Topology, TopologyKey> topologyListPath;

    NetconfDeviceTopologyAdapter(final RemoteDeviceId id, final DataBroker dataService) {
        this.id = id;
        this.dataService = dataService;

        this.networkTopologyPath = InstanceIdentifier.builder(NetworkTopology.class).build();
        this.topologyListPath = networkTopologyPath.child(Topology.class, new TopologyKey(new TopologyId(TopologyNetconf.QNAME.getLocalName())));

        initDeviceData();
    }

     private void initDeviceData() {
        final WriteTransaction writeTx = dataService.newWriteOnlyTransaction();

        createNetworkTopologyIfNotPresent(writeTx);

        final InstanceIdentifier<Node> path = id.getTopologyBindingPath();
        NodeBuilder nodeBuilder = getNodeIdBuilder(id);
        NetconfNodeBuilder netconfNodeBuilder = new NetconfNodeBuilder();
        netconfNodeBuilder.setConnectionStatus(ConnectionStatus.Connecting);
        netconfNodeBuilder.setHost(id.getHost());
        netconfNodeBuilder.setPort(new PortNumber(id.getAddress().getPort()));
        nodeBuilder.addAugmentation(NetconfNode.class, netconfNodeBuilder.build());
        Node node = nodeBuilder.build();

        logger.trace("{}: Init device state transaction {} putting if absent operational data started.", id, writeTx.getIdentifier());
        writeTx.put(LogicalDatastoreType.OPERATIONAL, path, node);
        logger.trace("{}: Init device state transaction {} putting operational data ended.", id, writeTx.getIdentifier());

        logger.trace("{}: Init device state transaction {} putting if absent config data started.", id, writeTx.getIdentifier());
        writeTx.put(LogicalDatastoreType.CONFIGURATION, path, getNodeWithId(id));
        logger.trace("{}: Init device state transaction {} putting config data ended.", id, writeTx.getIdentifier());

        commitTransaction(writeTx, "init");
    }

    public void updateDeviceData(boolean up, NetconfDeviceCapabilities capabilities) {
        final Node data = buildDataForNetconfNode(up, capabilities);

        final WriteTransaction writeTx = dataService.newWriteOnlyTransaction();
        logger.trace("{}: Update device state transaction {} merging operational data started.", id, writeTx.getIdentifier());
        writeTx.put(LogicalDatastoreType.OPERATIONAL, id.getTopologyBindingPath(), data);
        logger.trace("{}: Update device state transaction {} merging operational data ended.", id, writeTx.getIdentifier());

        commitTransaction(writeTx, "update");
    }

    public void setDeviceAsFailed() {
        final NetconfNode netconfNode = new NetconfNodeBuilder().setConnectionStatus(ConnectionStatus.UnableToConnect).build();
        final Node data = getNodeIdBuilder(id).addAugmentation(NetconfNode.class, netconfNode).build();

        final WriteTransaction writeTx = dataService.newWriteOnlyTransaction();
        logger.trace("{}: Setting device state as failed {} putting operational data started.", id, writeTx.getIdentifier());
        writeTx.put(LogicalDatastoreType.OPERATIONAL, id.getTopologyBindingPath(), data);
        logger.trace("{}: Setting device state as failed {} putting operational data ended.", id, writeTx.getIdentifier());

        commitTransaction(writeTx, "update-failed-device");
    }

    private Node buildDataForNetconfNode(boolean up, NetconfDeviceCapabilities capabilities) {
        List<String> capabilityList = new ArrayList<>();
        capabilityList.addAll(capabilities.getNonModuleBasedCapabilities());
        capabilityList.addAll(FluentIterable.from(capabilities.getResolvedCapabilities()).transform(AVAILABLE_CAPABILITY_TRANSFORMER).toList());
        final AvailableCapabilitiesBuilder avCapabalitiesBuilder = new AvailableCapabilitiesBuilder();
        avCapabalitiesBuilder.setAvailableCapability(capabilityList);

        final UnavailableCapabilities unavailableCapabilities =
                new UnavailableCapabilitiesBuilder().setUnavailableCapability(FluentIterable.from(capabilities.getUnresolvedCapabilites().entrySet())
                        .transform(UNAVAILABLE_CAPABILITY_TRANSFORMER).toList()).build();

        final NetconfNodeBuilder netconfNodeBuilder = new NetconfNodeBuilder()
                .setHost(id.getHost())
                .setPort(new PortNumber(id.getAddress().getPort()))
                .setConnectionStatus(up ? ConnectionStatus.Connected : ConnectionStatus.Connecting)
                .setAvailableCapabilities(avCapabalitiesBuilder.build())
                .setUnavailableCapabilities(unavailableCapabilities);

        final NodeBuilder nodeBuilder = getNodeIdBuilder(id);
        final Node node = nodeBuilder.addAugmentation(NetconfNode.class, netconfNodeBuilder.build()).build();

        return node;
    }

    public void removeDeviceConfiguration() {
        final WriteTransaction writeTx = dataService.newWriteOnlyTransaction();

        logger.trace("{}: Close device state transaction {} removing all data started.", id, writeTx.getIdentifier());
        writeTx.delete(LogicalDatastoreType.CONFIGURATION, id.getTopologyBindingPath());
        writeTx.delete(LogicalDatastoreType.OPERATIONAL, id.getTopologyBindingPath());
        logger.trace("{}: Close device state transaction {} removing all data ended.", id, writeTx.getIdentifier());

        commitTransaction(writeTx, "close");
    }

    private void createNetworkTopologyIfNotPresent(final WriteTransaction writeTx) {

        final NetworkTopology networkTopology = new NetworkTopologyBuilder().build();
        logger.trace("{}: Merging {} container to ensure its presence", id, networkTopology.QNAME, writeTx.getIdentifier());
        writeTx.merge(LogicalDatastoreType.CONFIGURATION, networkTopologyPath, networkTopology);
        writeTx.merge(LogicalDatastoreType.OPERATIONAL, networkTopologyPath, networkTopology);

        final Topology topology = new TopologyBuilder().setTopologyId(new TopologyId(TopologyNetconf.QNAME.getLocalName())).build();
        logger.trace("{}: Merging {} container to ensure its presence", id, topology.QNAME, writeTx.getIdentifier());
        writeTx.merge(LogicalDatastoreType.CONFIGURATION, topologyListPath, topology);
        writeTx.merge(LogicalDatastoreType.OPERATIONAL, topologyListPath, topology);
    }

    private void commitTransaction(final WriteTransaction transaction, final String txType) {
        logger.trace("{}: Committing Transaction {}:{}", id, txType, transaction.getIdentifier());
        final CheckedFuture<Void, TransactionCommitFailedException> result = transaction.submit();

        Futures.addCallback(result, new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                logger.trace("{}: Transaction({}) {} SUCCESSFUL", id, txType, transaction.getIdentifier());
            }

            @Override
            public void onFailure(final Throwable t) {
                logger.error("{}: Transaction({}) {} FAILED!", id, txType, transaction.getIdentifier(), t);
                throw new IllegalStateException(id + "  Transaction(" + txType + ") not committed correctly", t);
            }
        });

    }

    private static Node getNodeWithId(final RemoteDeviceId id) {
        final NodeBuilder builder = getNodeIdBuilder(id);
        return builder.build();
    }

    private static NodeBuilder getNodeIdBuilder(final RemoteDeviceId id) {
        final NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setKey(new NodeKey(new NodeId(id.getName())));
        return nodeBuilder;
    }

    @Override
    public void close() throws Exception {
        removeDeviceConfiguration();
    }
}
