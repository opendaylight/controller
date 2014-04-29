/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connect.netconf.sal;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.inventory.rev140108.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.inventory.rev140108.NetconfNodeBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Asynchronous (Binding-aware) adapter over datastore subtree for netconf device.
 *
 * All data changes are submitted to an ExecutorService to avoid Thread blocking while sal is waiting for schema.
 */
final class NetconfDeviceDatastoreAdapter implements AutoCloseable {

    private static final Logger logger  = LoggerFactory.getLogger(NetconfDeviceDatastoreAdapter.class);

    private final RemoteDeviceId id;
    private final DataProviderService dataService;
    private final ListeningExecutorService executor;

    NetconfDeviceDatastoreAdapter(final RemoteDeviceId deviceId, final DataProviderService dataService,
            final ExecutorService executor) {
        this.id = Preconditions.checkNotNull(deviceId);
        this.dataService = Preconditions.checkNotNull(dataService);
        this.executor = MoreExecutors.listeningDecorator(Preconditions.checkNotNull(executor));

        // Initial data change scheduled
        submitDataChangeToExecutor(this.executor, new Runnable() {
            @Override
            public void run() {
                initDeviceData();
            }
        }, deviceId);
    }

    public void updateDeviceState(final boolean up, final Set<QName> capabilities) {
        submitDataChangeToExecutor(this.executor, new Runnable() {
            @Override
            public void run() {
                updateDeviceStateInternal(up, capabilities);
            }
        }, id);
    }

    private void updateDeviceStateInternal(final boolean up, final Set<QName> capabilities) {
        final org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node data = buildDataForDeviceState(
                up, capabilities, id);

        final DataModificationTransaction transaction = dataService.beginTransaction();
        logger.trace("{}: Update device state transaction {} putting operational data started.", id, transaction.getIdentifier());
        transaction.removeOperationalData(id.getBindingPath());
        transaction.putOperationalData(id.getBindingPath(), data);
        logger.trace("{}: Update device state transaction {} putting operational data ended.", id, transaction.getIdentifier());

        commitTransaction(transaction, "update");
    }

    private void removeDeviceConfigAndState() {
        final DataModificationTransaction transaction = dataService.beginTransaction();
        logger.trace("{}: Close device state transaction {} removing all data started.", id, transaction.getIdentifier());
        transaction.removeConfigurationData(id.getBindingPath());
        transaction.removeOperationalData(id.getBindingPath());
        logger.trace("{}: Close device state transaction {} removing all data ended.", id, transaction.getIdentifier());

        commitTransaction(transaction, "close");
    }

    private void initDeviceData() {
        final DataModificationTransaction transaction = dataService.beginTransaction();

        final InstanceIdentifier<Node> path = id.getBindingPath();

        final Node nodeWithId = getNodeWithId(id);
        if (operationalNodeNotExisting(transaction, path)) {
            transaction.putOperationalData(path, nodeWithId);
        }
        if (configurationNodeNotExisting(transaction, path)) {
            transaction.putConfigurationData(path, nodeWithId);
        }

        commitTransaction(transaction, "init");
    }

    private void commitTransaction(final DataModificationTransaction transaction, final String txType) {
        // attempt commit
        final RpcResult<TransactionStatus> result;
        try {
            result = transaction.commit().get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("{}: Transaction({}) failed", id, txType, e);
            throw new IllegalStateException(id + " Transaction(" + txType + ") not committed correctly", e);
        }

        // verify success result + committed state
        if (isUpdateSuccessful(result)) {
            logger.trace("{}: Transaction({}) {} SUCCESSFUL", id, txType, transaction.getIdentifier());
        } else {
            logger.error("{}: Transaction({}) {} FAILED!", id, txType, transaction.getIdentifier());
            throw new IllegalStateException(id + "  Transaction(" + txType + ") not committed correctly, " +
                    "Errors: " + result.getErrors());
        }
    }

    @Override
    public void close() throws Exception {
        // Remove device data from datastore
        submitDataChangeToExecutor(executor, new Runnable() {
            @Override
            public void run() {
                removeDeviceConfigAndState();
            }
        }, id);
    }

    private static boolean isUpdateSuccessful(final RpcResult<TransactionStatus> result) {
        return result.getResult() == TransactionStatus.COMMITED && result.isSuccessful();
    }

    private static void submitDataChangeToExecutor(final ListeningExecutorService executor, final Runnable r,
            final RemoteDeviceId id) {
        // Submit data change
        final ListenableFuture<?> f = executor.submit(r);
        // Verify update execution
        Futures.addCallback(f, new FutureCallback<Object>() {
            @Override
            public void onSuccess(final Object result) {
                logger.debug("{}: Device data updated successfully", id);
            }

            @Override
            public void onFailure(final Throwable t) {
                logger.warn("{}: Device data update failed", id, t);
            }
        });
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

    private static boolean configurationNodeNotExisting(final DataModificationTransaction transaction,
            final InstanceIdentifier<Node> path) {
        return null == transaction.readConfigurationData(path);
    }

    private static boolean operationalNodeNotExisting(final DataModificationTransaction transaction,
            final InstanceIdentifier<Node> path) {
        return null == transaction.readOperationalData(path);
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
