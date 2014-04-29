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

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.controller.sal.core.api.data.DataBrokerService;
import org.opendaylight.controller.sal.core.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionInstance;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionService;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.impl.CompositeNodeTOImpl;
import org.opendaylight.yangtools.yang.data.impl.ImmutableCompositeNode;
import org.opendaylight.yangtools.yang.data.impl.SimpleNodeTOImpl;
import org.opendaylight.yangtools.yang.data.impl.util.CompositeNodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public class NetconfDeviceSalProvider implements AutoCloseable, Provider {

    private static final Logger logger = LoggerFactory.getLogger(NetconfDeviceSalProvider.class);

    private DataBrokerService dataBroker;
    private MountProvisionInstance mountInstance;
    private NetconfDeviceDatastoreAdapter datastoreAdapter;

    private final RemoteDeviceId deviceId;

    public NetconfDeviceSalProvider(final RemoteDeviceId deviceId) {
        this.deviceId = deviceId;
    }

    public synchronized MountProvisionInstance getMountInstance() {
        Preconditions.checkState(mountInstance != null,
                "%s: Provider %s was not initialized by sal. Cannot publish notification");
        return mountInstance;
    }

    public synchronized NetconfDeviceDatastoreAdapter getDatastoreAdapter() {
        Preconditions.checkState(datastoreAdapter != null,
                "%s: Provider %s was not initialized by sal. Cannot publish notification");
        return datastoreAdapter;
    }

    @Override
    public Collection<ProviderFunctionality> getProviderFunctionality() {
        return Collections.emptySet();
    }

    @Override
    public synchronized void onSessionInitiated(final Broker.ProviderSession session) {
        dataBroker = session.getService(DataBrokerService.class);
        final InstanceIdentifier path = deviceId.getPath();

        // TODO BUG 969 wait for nodes chema to be presnet in md-sal bedore writing to datastore
        datastoreAdapter = new NetconfDeviceDatastoreAdapter(deviceId, dataBroker);

        final MountProvisionService mountService = session.getService(MountProvisionService.class);
        if (mountService != null) {
            mountInstance = mountService.createOrGetMountPoint(path);
        }

        logger.debug("{}: Session with sal established {}", deviceId, session);
    }

    private static CompositeNode getNodeWithId(final String name) {
        final SimpleNodeTOImpl id = new SimpleNodeTOImpl<>(INVENTORY_ID, null, name);
        return new CompositeNodeTOImpl(INVENTORY_NODE, null, Collections.<Node<?>> singletonList(id));
    }

    private static boolean configurationNodeNotExisting(final DataModificationTransaction transaction,
                                                        final InstanceIdentifier path) {
        return null == transaction.readConfigurationData(path);
    }

    private static boolean operationalNodeNotExisting(final DataModificationTransaction transaction,
                                                      final InstanceIdentifier path) {
        return null == transaction.readOperationalData(path);
    }

    public synchronized void close() throws Exception {
        dataBroker = null;
        mountInstance = null;
        datastoreAdapter.close();
        datastoreAdapter = null;
    }

    static final class NetconfDeviceDatastoreAdapter implements AutoCloseable {

        private final RemoteDeviceId deviceId;
        private final DataBrokerService dataBroker;

        NetconfDeviceDatastoreAdapter(final RemoteDeviceId deviceId, final DataBrokerService dataBroker) {
            this.deviceId = deviceId;
            this.dataBroker = dataBroker;
            initDeviceData();
        }

        public synchronized void updateDeviceState(final boolean up, final Set<QName> capabilities) {
            final ImmutableCompositeNode data = buildDataForDeviceState(up, capabilities, deviceId);

            final DataModificationTransaction transaction = dataBroker.beginTransaction();
            logger.debug("Update device state transaction {} putting operational data started.",
                    transaction.getIdentifier());
            transaction.removeOperationalData(deviceId.getPath());
            transaction.putOperationalData(deviceId.getPath(), data);
            logger.debug("Update device state transaction {} putting operational data ended.", transaction.getIdentifier());

            commitTransaction(transaction, "update");
        }

        public static ImmutableCompositeNode buildDataForDeviceState(final boolean up, final Set<QName> capabilities,
                                                                     final RemoteDeviceId id) {
            final CompositeNodeBuilder<ImmutableCompositeNode> it = ImmutableCompositeNode.builder();
            it.setQName(INVENTORY_NODE);
            it.addLeaf(INVENTORY_ID, id.getName());
            it.addLeaf(INVENTORY_CONNECTED, up);

            for (final QName capability : capabilities) {
                it.addLeaf(NETCONF_INVENTORY_INITIAL_CAPABILITY, capability.toString());
            }
            return it.toInstance();
        }

        private synchronized void removeDeviceConfigAndState() {
            final DataModificationTransaction transaction = dataBroker.beginTransaction();
            logger.debug("Close device state transaction {} removing all data started.",
                    transaction.getIdentifier());
            transaction.removeConfigurationData(deviceId.getPath());
            transaction.removeOperationalData(deviceId.getPath());
            logger.debug("Close device state transaction {} removing all data ended.", transaction.getIdentifier());

            commitTransaction(transaction, "close");
        }

        private void initDeviceData() {
            final DataModificationTransaction transaction = dataBroker.beginTransaction();

            final InstanceIdentifier path = deviceId.getPath();
            final String name = deviceId.getName();

            if (operationalNodeNotExisting(transaction, path)) {
                transaction.putOperationalData(path, getNodeWithId(name));
            }
            if (configurationNodeNotExisting(transaction, path)) {
                transaction.putConfigurationData(path, getNodeWithId(name));
            }

            commitTransaction(transaction, "init");
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

        @Override
        public void close() throws Exception {
            removeDeviceConfigAndState();
        }
    }
}
