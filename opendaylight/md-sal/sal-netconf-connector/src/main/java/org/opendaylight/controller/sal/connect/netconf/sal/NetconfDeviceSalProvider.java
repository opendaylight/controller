/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connect.netconf.sal;

import static org.opendaylight.controller.sal.connect.util.InventoryUtils.INVENTORY_ID;
import static org.opendaylight.controller.sal.connect.util.InventoryUtils.INVENTORY_NODE;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.controller.sal.core.api.data.DataBrokerService;
import org.opendaylight.controller.sal.core.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionInstance;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionService;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.impl.CompositeNodeTOImpl;
import org.opendaylight.yangtools.yang.data.impl.SimpleNodeTOImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public class NetconfDeviceSalProvider implements AutoCloseable, Provider {

    private static final Logger logger = LoggerFactory.getLogger(NetconfDeviceSalProvider.class);

    private DataBrokerService dataBroker;
    private MountProvisionInstance mountInstance;
    private final RemoteDeviceId deviceId;

    public NetconfDeviceSalProvider(final RemoteDeviceId deviceId) {
        this.deviceId = deviceId;
    }

    public synchronized DataBrokerService getDataBroker() {
        Preconditions.checkState(dataBroker != null,
                "%s: Provider %s was not initialized by sal. Cannot publish notification");
        return dataBroker;
    }

    public synchronized MountProvisionInstance getMountInstance() {
        Preconditions.checkState(mountInstance != null,
                "%s: Provider %s was not initialized by sal. Cannot publish notification");
        return mountInstance;
    }

    @Override
    public Collection<ProviderFunctionality> getProviderFunctionality() {
        return Collections.emptySet();
    }

    @Override
    public synchronized void onSessionInitiated(final Broker.ProviderSession session) {
        dataBroker = session.getService(DataBrokerService.class);
        final InstanceIdentifier path = deviceId.getPath();
        final String name = deviceId.getName();

        // TODO BUG 969 wait for nodes chema to be presnet in md-sal bedore writing to datastore
        // TODO should this initial registration be here ? maybe in facade or extract new class, updater
        final DataModificationTransaction transaction = dataBroker.beginTransaction();

        if (operationalNodeNotExisting(transaction, path)) {
            transaction.putOperationalData(path, getNodeWithId(name));
        }
        if (configurationNodeNotExisting(transaction, path)) {
            transaction.putConfigurationData(path, getNodeWithId(name));
        }

        try {
            transaction.commit().get();
        } catch (final InterruptedException e) {
            throw new RuntimeException(deviceId + ": Interrupted while waiting for response", e);
        } catch (final ExecutionException e) {
            throw new RuntimeException(deviceId + ": Read configuration data " + path + " failed", e);
        }

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

    public synchronized void close() {
        dataBroker = null;
        mountInstance = null;
    }
}
