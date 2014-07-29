/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connect.netconf.sal;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.connect.api.RemoteDeviceHandler;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfSessionCapabilities;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionInstance;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContextProvider;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NetconfDeviceSalFacade implements AutoCloseable, RemoteDeviceHandler<NetconfSessionCapabilities> {

    private static final Logger logger= LoggerFactory.getLogger(NetconfDeviceSalFacade.class);
    private static final YangInstanceIdentifier ROOT_PATH = YangInstanceIdentifier.builder().toInstance();

    private final RemoteDeviceId id;
    private final NetconfDeviceSalProvider salProvider;

    private final List<AutoCloseable> salRegistrations = Lists.newArrayList();

    public NetconfDeviceSalFacade(final RemoteDeviceId id, final Broker domBroker, final BindingAwareBroker bindingBroker, final BundleContext bundleContext, final ExecutorService executor) {
        this.id = id;
        this.salProvider = new NetconfDeviceSalProvider(id, executor);
        registerToSal(domBroker, bindingBroker, bundleContext);
    }

    public void registerToSal(final Broker domRegistryDependency, final BindingAwareBroker bindingBroker, final BundleContext bundleContext) {
        domRegistryDependency.registerProvider(salProvider, bundleContext);
        bindingBroker.registerProvider(salProvider, bundleContext);
    }

    @Override
    public synchronized void onNotification(final CompositeNode domNotification) {
        salProvider.getMountInstance().publish(domNotification);
    }

    @Override
    public synchronized void onDeviceConnected(final SchemaContextProvider remoteSchemaContextProvider,
                                               final NetconfSessionCapabilities netconfSessionPreferences, final RpcImplementation deviceRpc) {
        salProvider.getMountInstance().setSchemaContext(remoteSchemaContextProvider.getSchemaContext());
        salProvider.getDatastoreAdapter().updateDeviceState(true, netconfSessionPreferences.getModuleBasedCaps());
        registerDataHandlersToSal(deviceRpc, netconfSessionPreferences);
        registerRpcsToSal(deviceRpc);
    }

    @Override
    public void onDeviceDisconnected() {
        salProvider.getDatastoreAdapter().updateDeviceState(false, Collections.<QName>emptySet());
    }

    private void registerRpcsToSal(final RpcImplementation deviceRpc) {
        final MountProvisionInstance mountInstance = salProvider.getMountInstance();

        final Map<QName, String> failedRpcs = Maps.newHashMap();
        for (final RpcDefinition rpcDef : mountInstance.getSchemaContext().getOperations()) {
            try {
                salRegistrations.add(mountInstance.addRpcImplementation(rpcDef.getQName(), deviceRpc));
                logger.debug("{}: Rpc {} from netconf registered successfully", id, rpcDef.getQName());
            } catch (final Exception e) {
                // Only debug per rpc, warn for all of them at the end to pollute log a little less (e.g. routed rpcs)
                logger.debug("{}: Unable to register rpc {} from netconf device. This rpc will not be available", id,
                        rpcDef.getQName(), e);
                failedRpcs.put(rpcDef.getQName(), e.getClass() + ":" + e.getMessage());
            }
        }

        if (failedRpcs.isEmpty() == false) {
            if (logger.isDebugEnabled()) {
                logger.warn("{}: Some rpcs from netconf device were not registered: {}", id, failedRpcs);
            } else {
                logger.warn("{}: Some rpcs from netconf device were not registered: {}", id, failedRpcs.keySet());
            }
        }
    }

    private void registerDataHandlersToSal(final RpcImplementation deviceRpc,
            final NetconfSessionCapabilities netconfSessionPreferences) {
        final NetconfDeviceDataReader dataReader = new NetconfDeviceDataReader(id, deviceRpc);
        final NetconfDeviceCommitHandler commitHandler = new NetconfDeviceCommitHandler(id, deviceRpc,
                netconfSessionPreferences.isRollbackSupported());

        final MountProvisionInstance mountInstance = salProvider.getMountInstance();
        salRegistrations.add(mountInstance.registerConfigurationReader(ROOT_PATH, dataReader));
        salRegistrations.add(mountInstance.registerOperationalReader(ROOT_PATH, dataReader));
        salRegistrations.add(mountInstance.registerCommitHandler(ROOT_PATH, commitHandler));
    }

    @Override
    public void close() {
        for (final AutoCloseable reg : Lists.reverse(salRegistrations)) {
            closeGracefully(reg);
        }
        closeGracefully(salProvider);
    }

    private void closeGracefully(final AutoCloseable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (final Exception e) {
                logger.warn("{}: Ignoring exception while closing {}", id, resource, e);
            }
        }
    }
}
