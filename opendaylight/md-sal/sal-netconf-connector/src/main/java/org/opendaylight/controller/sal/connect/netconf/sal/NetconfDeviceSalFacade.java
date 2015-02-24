/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connect.netconf.sal;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.connect.api.RemoteDeviceHandler;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfDeviceCapabilities;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfSessionPreferences;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NetconfDeviceSalFacade implements AutoCloseable, RemoteDeviceHandler<NetconfSessionPreferences> {

    private static final Logger logger= LoggerFactory.getLogger(NetconfDeviceSalFacade.class);

    private final RemoteDeviceId id;
    private final NetconfDeviceSalProvider salProvider;

    private final List<AutoCloseable> salRegistrations = Lists.newArrayList();

    public NetconfDeviceSalFacade(final RemoteDeviceId id, final Broker domBroker, final BindingAwareBroker bindingBroker, final BundleContext bundleContext) {
        this.id = id;
        this.salProvider = new NetconfDeviceSalProvider(id);
        registerToSal(domBroker, bindingBroker, bundleContext);
    }

    public void registerToSal(final Broker domRegistryDependency, final BindingAwareBroker bindingBroker, final BundleContext bundleContext) {
        domRegistryDependency.registerProvider(salProvider, bundleContext);
        bindingBroker.registerProvider(salProvider, bundleContext);
    }

    @Override
    public synchronized void onNotification(final ContainerNode domNotification) {
        salProvider.getMountInstance().publish(domNotification);
    }

    @Override
    public synchronized void onDeviceConnected(final SchemaContext schemaContext,
                                               final NetconfSessionPreferences netconfSessionPreferences, final DOMRpcService deviceRpc) {

        final DOMDataBroker domBroker = new NetconfDeviceDataBroker(id, schemaContext, deviceRpc, netconfSessionPreferences);

        final NetconfDeviceNotificationService notificationService = new NetconfDeviceNotificationService();

        salProvider.getMountInstance().onDeviceConnected(schemaContext, domBroker, deviceRpc, notificationService);
        salProvider.getDatastoreAdapter().updateDeviceState(true, netconfSessionPreferences.getModuleBasedCaps());
        salProvider.getMountInstance().onTopologyDeviceConnected(schemaContext, domBroker, deviceRpc, notificationService);
        salProvider.getTopologyDatastoreAdapter().updateDeviceData(true, netconfSessionPreferences.getNetconfDeviceCapabilities());
    }

    @Override
    public synchronized void onDeviceDisconnected() {
        salProvider.getDatastoreAdapter().updateDeviceState(false, Collections.<QName>emptySet());
        salProvider.getTopologyDatastoreAdapter().updateDeviceData(false, new NetconfDeviceCapabilities());
        salProvider.getMountInstance().onDeviceDisconnected();
        salProvider.getMountInstance().onTopologyDeviceDisconnected();
    }

    @Override
    public void onDeviceFailed(final Throwable throwable) {
        salProvider.getTopologyDatastoreAdapter().setDeviceAsFailed(throwable);
        salProvider.getMountInstance().onDeviceDisconnected();
        salProvider.getMountInstance().onTopologyDeviceDisconnected();
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
