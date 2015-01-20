/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connect.netconf.sal;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Set;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.inventory.rev140108.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.inventory.rev140108.NetconfNodeBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Asynchronous (Binding-aware) adapter over datastore subtree for netconf device.
 *
 * All data changes are submitted to an ExecutorService to avoid Thread blocking while sal is waiting for schema.
 */
@Deprecated
final class NetconfDeviceDatastoreAdapter implements AutoCloseable {

    private static final Logger logger  = LoggerFactory.getLogger(NetconfDeviceDatastoreAdapter.class);

    private final RemoteDeviceId id;
    private final DataBroker dataService;

    NetconfDeviceDatastoreAdapter(final RemoteDeviceId deviceId, final DataBroker dataService) {
        this.id = Preconditions.checkNotNull(deviceId);
        this.dataService = Preconditions.checkNotNull(dataService);

        initDeviceData();
    }

    public void updateDeviceState(final boolean up, final Set<QName> capabilities) {
        final org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node data = buildDataForDeviceState(
                up, capabilities, id);

        final ReadWriteTransaction transaction = dataService.newReadWriteTransaction();
        logger.trace("{}: Update device state transaction {} merging operational data started.", id, transaction.getIdentifier());
        transaction.put(LogicalDatastoreType.OPERATIONAL, id.getBindingPath(), data);
        logger.trace("{}: Update device state transaction {} merging operational data ended.", id, transaction.getIdentifier());

        commitTransaction(transaction, "update");
    }

    private void removeDeviceConfigAndState() {
        final WriteTransaction transaction = dataService.newWriteOnlyTransaction();
        logger.trace("{}: Close device state transaction {} removing all data started.", id, transaction.getIdentifier());
        transaction.delete(LogicalDatastoreType.CONFIGURATION, id.getBindingPath());
        transaction.delete(LogicalDatastoreType.OPERATIONAL, id.getBindingPath());
        logger.trace("{}: Close device state transaction {} removing all data ended.", id, transaction.getIdentifier());

        commitTransaction(transaction, "close");
    }

    private void initDeviceData() {
        final WriteTransaction transaction = dataService.newWriteOnlyTransaction();

        createNodesListIfNotPresent(transaction);

        final InstanceIdentifier<Node> path = id.getBindingPath();
        final Node nodeWithId = getNodeWithId(id);

        logger.trace("{}: Init device state transaction {} putting if absent operational data started.", id, transaction.getIdentifier());
        transaction.put(LogicalDatastoreType.OPERATIONAL, path, nodeWithId);
        logger.trace("{}: Init device state transaction {} putting operational data ended.", id, transaction.getIdentifier());

        logger.trace("{}: Init device state transaction {} putting if absent config data started.", id, transaction.getIdentifier());
        transaction.put(LogicalDatastoreType.CONFIGURATION, path, nodeWithId);
        logger.trace("{}: Init device state transaction {} putting config data ended.", id, transaction.getIdentifier());

        commitTransaction(transaction, "init");
    }

    private void createNodesListIfNotPresent(final WriteTransaction writeTx) {
        final Nodes nodes = new NodesBuilder().build();
        final InstanceIdentifier<Nodes> path = InstanceIdentifier.builder(Nodes.class).build();
        logger.trace("{}: Merging {} container to ensure its presence", id, Nodes.QNAME, writeTx.getIdentifier());
        writeTx.merge(LogicalDatastoreType.CONFIGURATION, path, nodes);
        writeTx.merge(LogicalDatastoreType.OPERATIONAL, path, nodes);
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

    @Override
    public void close() throws Exception {
        removeDeviceConfigAndState();
    }

    public static org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node buildDataForDeviceState(
            final boolean up, final Set<QName> capabilities, final RemoteDeviceId id) {

        final NodeBuilder nodeBuilder = getNodeWithIdBuilder(id);
        final NetconfNodeBuilder netconfNodeBuilder = new NetconfNodeBuilder();
        netconfNodeBuilder.setConnected(up);
        netconfNodeBuilder.setInitialCapability(FluentIterable.from(capabilities)
                .transform(new Function<QName, String>() {
                    @Override
                    public String apply(final QName input) {
                        return input.toString();
                    }
                }).toList());
        nodeBuilder.addAugmentation(NetconfNode.class, netconfNodeBuilder.build());

        return nodeBuilder.build();
    }

    private static ListenableFuture<Optional<Node>> readNodeData(
            final LogicalDatastoreType store,
            final ReadWriteTransaction transaction,
            final InstanceIdentifier<Node> path) {
        return transaction.read(store, path);
    }

    private static Node getNodeWithId(final RemoteDeviceId id) {
        final NodeBuilder nodeBuilder = getNodeWithIdBuilder(id);
        return nodeBuilder.build();
    }

    private static NodeBuilder getNodeWithIdBuilder(final RemoteDeviceId id) {
        final NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setKey(id.getBindingKey());
        nodeBuilder.setId(id.getBindingKey().getId());
        return nodeBuilder;
    }
}
