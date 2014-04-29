/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connect.netconf.sal;

import static org.opendaylight.controller.sal.connect.util.InventoryUtils.INVENTORY_CONNECTED;
import static org.opendaylight.controller.sal.connect.util.InventoryUtils.INVENTORY_ID;
import static org.opendaylight.controller.sal.connect.util.InventoryUtils.INVENTORY_NODE;
import static org.opendaylight.controller.sal.connect.util.InventoryUtils.NETCONF_INVENTORY_INITIAL_CAPABILITY;

import java.util.List;
import java.util.Set;

import java.util.concurrent.ExecutionException;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.connect.api.RemoteDeviceSalFacade;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfSessionCapabilities;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.controller.sal.core.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionInstance;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.impl.ImmutableCompositeNode;
import org.opendaylight.yangtools.yang.data.impl.util.CompositeNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContextProvider;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public final class NetconfDeviceSalFacade implements AutoCloseable, RemoteDeviceSalFacade {
    private static final Logger logger = LoggerFactory.getLogger(NetconfDeviceTwoPhaseCommitTransaction.class);
    private static final InstanceIdentifier ROOT_PATH = InstanceIdentifier.builder().toInstance();

    private final RemoteDeviceId deviceId;
    private final NetconfDeviceSalProvider salProvider;

    private final List<AutoCloseable> salRegistrations = Lists.newArrayList();

    public NetconfDeviceSalFacade(final RemoteDeviceId id, final Broker domBroker, final BundleContext bundleContext) {
        this.deviceId = id;
        this.salProvider = new NetconfDeviceSalProvider(id);
        registerToSal(domBroker, bundleContext);
    }

    public void registerToSal(final Broker domRegistryDependency, final BundleContext bundleContext) {
        domRegistryDependency.registerProvider(salProvider, bundleContext);
    }

    @Override
    public synchronized void publishNotification(final CompositeNode domNotification) {
        salProvider.getMountInstance().publish(domNotification);
    }

    @Override
    public synchronized void initializeDeviceInSal(final SchemaContextProvider remoteSchemaContextProvider,
            final NetconfSessionCapabilities netconfSessionPreferences, final RpcImplementation deviceRpc) {
        salProvider.getMountInstance().setSchemaContext(remoteSchemaContextProvider.getSchemaContext());
        updateDeviceState(true, netconfSessionPreferences.getModuleBasedCaps());
        registerDataHandlersToSal(deviceRpc, netconfSessionPreferences);
        registerRpcsToSal(deviceRpc);
    }

    private void registerRpcsToSal(final RpcImplementation deviceRpc) {
        final MountProvisionInstance mountInstance = salProvider.getMountInstance();
        for (final RpcDefinition rpcDef : mountInstance.getSchemaContext().getOperations()) {
            try {
                salRegistrations.add(mountInstance.addRpcImplementation(rpcDef.getQName(), deviceRpc));
                logger.debug("{}: Rpc {} from netconf registered successfully", deviceId, rpcDef.getQName());
            } catch (final Exception e) {
                logger.warn("{}: Unable to register rpc {} from netconf device. This rpc will not be available.",
                        deviceId, rpcDef.getQName(), e);
            }
        }
    }

    private void registerDataHandlersToSal(final RpcImplementation deviceRpc,
            final NetconfSessionCapabilities netconfSessionPreferences) {
        final InstanceIdentifier path = deviceId.getPath();
        final NetconfDeviceDataReader dataReader = new NetconfDeviceDataReader(deviceId, deviceRpc);
        final NetconfDeviceCommitHandler commitHandler = new NetconfDeviceCommitHandler(path, deviceRpc,
                netconfSessionPreferences.isRollbackSupported());

        final MountProvisionInstance mountInstance = salProvider.getMountInstance();
        salRegistrations.add(mountInstance.registerConfigurationReader(ROOT_PATH, dataReader));
        salRegistrations.add(mountInstance.registerOperationalReader(ROOT_PATH, dataReader));
        salRegistrations.add(mountInstance.registerCommitHandler(ROOT_PATH, commitHandler));
    }

    public synchronized void updateDeviceState(final boolean up, final Set<QName> capabilities) {
        final ImmutableCompositeNode data = buildDataForDeviceState(up, capabilities, deviceId);

        final DataModificationTransaction transaction = salProvider.getDataBroker().beginTransaction();
        logger.debug("Update device state transaction {} putting operational data started.",
                transaction.getIdentifier());
        transaction.removeOperationalData(deviceId.getPath());
        transaction.putOperationalData(deviceId.getPath(), data);
        logger.debug("Update device state transaction {} putting operational data ended.", transaction.getIdentifier());

        commitTransaction(transaction, "update");
    }

    public synchronized void removeDeviceConfigAndState() {
        final DataModificationTransaction transaction = salProvider.getDataBroker().beginTransaction();
        logger.debug("Close device state transaction {} removing all data started.",
                transaction.getIdentifier());
        transaction.removeConfigurationData(deviceId.getPath());
        transaction.removeOperationalData(deviceId.getPath());
        logger.debug("Close device state transaction {} removing all data ended.", transaction.getIdentifier());

        commitTransaction(transaction, "close");
    }

    private void commitTransaction(final DataModificationTransaction transaction, final String txType) {
        // attempt commit
        final RpcResult<TransactionStatus> result;
        try {
            result = transaction.commit().get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("{}: Transaction({}) failed", deviceId, txType, e);
            throw new IllegalStateException(deviceId + " Transaction(" + txType + ") not committed correctly", e);
        }

        // verify success result + committed state
        if (isUpdateSuccessful(result)) {
            logger.debug("{}: Transaction({}) {} SUCCESSFUL", deviceId, txType, transaction.getIdentifier());
        } else {
            logger.error("{}: Transaction({}) {} FAILED!", deviceId, txType, transaction.getIdentifier());
            throw new IllegalStateException(deviceId + "  Transaction(" + txType + ") not committed correctly, Errors: " + result.getErrors());
        }
    }

    private boolean isUpdateSuccessful(final RpcResult<TransactionStatus> result) {
        return result.getResult() == TransactionStatus.COMMITED && result.isSuccessful();
    }

    public static ImmutableCompositeNode buildDataForDeviceState(final boolean up, final Set<QName> capabilities,
            final RemoteDeviceId id) {
        final CompositeNodeBuilder<ImmutableCompositeNode> it = ImmutableCompositeNode.builder();
        it.setQName(INVENTORY_NODE);
        it.addLeaf(INVENTORY_ID, id.getName());
        it.addLeaf(INVENTORY_CONNECTED, up);

        for (final QName capability : capabilities) {
            it.addLeaf(NETCONF_INVENTORY_INITIAL_CAPABILITY, capability);
        }
        return it.toInstance();
    }

    @Override
    public void close() {
        removeDeviceConfigAndState();

        for (final AutoCloseable reg : Lists.reverse(salRegistrations)) {
            closeGracefully(reg);
        }

        salProvider.close();
    }

    private void closeGracefully(final AutoCloseable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (final Exception e) {
                logger.warn("{}: Ignoring exception while closing {}", deviceId, resource, e);
            }
        }
    }
}
