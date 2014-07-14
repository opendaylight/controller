/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connect.netconf.sal;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionInstance;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionService;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class NetconfDeviceSalProvider implements AutoCloseable, Provider, BindingAwareProvider {

    private static final Logger logger = LoggerFactory.getLogger(NetconfDeviceSalProvider.class);

    private final RemoteDeviceId id;
    private final ExecutorService executor;
    private volatile MountProvisionInstance mountInstance;
    private volatile NetconfDeviceDatastoreAdapter datastoreAdapter;

    public NetconfDeviceSalProvider(final RemoteDeviceId deviceId, final ExecutorService executor) {
        this.id = deviceId;
        this.executor = executor;
    }

    public MountProvisionInstance getMountInstance() {
        Preconditions.checkState(mountInstance != null,
                "%s: Sal provider was not initialized by sal. Cannot get mount instance", id);
        return mountInstance;
    }

    public NetconfDeviceDatastoreAdapter getDatastoreAdapter() {
        Preconditions.checkState(datastoreAdapter != null,
                "%s: Sal provider %s was not initialized by sal. Cannot get datastore adapter", id);
        return datastoreAdapter;
    }

    @Override
    public void onSessionInitiated(final Broker.ProviderSession session) {
        final MountProvisionService mountService = session.getService(MountProvisionService.class);
        if (mountService != null) {
            mountInstance = mountService.createOrGetMountPoint(id.getPath());
        }

        logger.debug("{}: (BI)Session with sal established {}", id, session);
    }

    @Override
    public Collection<Provider.ProviderFunctionality> getProviderFunctionality() {
        return Collections.emptySet();
    }

    @Override
    public Collection<? extends RpcService> getImplementations() {
        return Collections.emptySet();
    }

    @Override
    public Collection<? extends BindingAwareProvider.ProviderFunctionality> getFunctionality() {
        return Collections.emptySet();
    }

    @Override
    public void onSessionInitiated(final BindingAwareBroker.ProviderContext session) {
        final DataProviderService dataBroker = session.getSALService(DataProviderService.class);
        datastoreAdapter = new NetconfDeviceDatastoreAdapter(id, dataBroker, executor);

        logger.debug("{}: Session with sal established {}", id, session);
    }

    @Override
    public void onSessionInitialized(final BindingAwareBroker.ConsumerContext session) {
    }

    public void close() throws Exception {
        mountInstance = null;
        datastoreAdapter.close();
        datastoreAdapter = null;
    }

}
