/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.topology;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMNotification;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.connect.api.RemoteDeviceHandler;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfSessionPreferences;
import org.opendaylight.controller.sal.connect.netconf.sal.NetconfDeviceDataBroker;
import org.opendaylight.controller.sal.connect.netconf.sal.NetconfDeviceNotificationService;
import org.opendaylight.controller.sal.connect.netconf.sal.NetconfDeviceSalProvider;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopologyMountPointFacade implements AutoCloseable, RemoteDeviceHandler<NetconfSessionPreferences> {

    private static final Logger LOG = LoggerFactory.getLogger(TopologyMountPointFacade.class);

    private final RemoteDeviceId id;
    private final Broker domBroker;
    private final BindingAwareBroker bindingBroker;
    private final long defaultRequestTimeoutMillis;

    private ProviderSession domProviderRegistration;

    private SchemaContext remoteSchemaContext = null;
    private NetconfSessionPreferences netconfSessionPreferences = null;
    private DOMRpcService deviceRpc = null;
    private final NetconfDeviceSalProvider salProvider;

    private final ArrayList<RemoteDeviceHandler<NetconfSessionPreferences>> connectionStatusListeners = new ArrayList<>();

    public TopologyMountPointFacade(final RemoteDeviceId id, final Broker domBroker,
            final BindingAwareBroker bindingBroker, final long defaultRequestTimeoutMillis) {

        this.id = id;
        this.domBroker = domBroker;
        this.bindingBroker = bindingBroker;
        this.defaultRequestTimeoutMillis = defaultRequestTimeoutMillis;
        this.salProvider = new NetconfDeviceSalProvider(id);
        registerToSal(domBroker, bindingBroker);
    }

    public void registerToSal(final Broker domRegistryDependency, final BindingAwareBroker bindingBroker) {
        domProviderRegistration = domRegistryDependency.registerProvider(salProvider);
        bindingBroker.registerProvider(salProvider);
    }

    @Override
    public void onDeviceConnected(final SchemaContext remoteSchemaContext,
            final NetconfSessionPreferences netconfSessionPreferences, final DOMRpcService deviceRpc) {
        // prepare our prerequisites for mountpoint
        this.remoteSchemaContext = remoteSchemaContext;
        this.netconfSessionPreferences = netconfSessionPreferences;
        this.deviceRpc = deviceRpc;
        for (final RemoteDeviceHandler<NetconfSessionPreferences> listener : connectionStatusListeners) {
            listener.onDeviceConnected(remoteSchemaContext, netconfSessionPreferences, deviceRpc);
        }
    }

    @Override
    public void onDeviceDisconnected() {
        salProvider.getMountInstance().onTopologyDeviceDisconnected();
        for (final RemoteDeviceHandler<NetconfSessionPreferences> listener : connectionStatusListeners) {
            listener.onDeviceDisconnected();
        }
    }

    @Override
    public void onDeviceFailed(final Throwable throwable) {
        salProvider.getMountInstance().onTopologyDeviceDisconnected();
        for (final RemoteDeviceHandler<NetconfSessionPreferences> listener : connectionStatusListeners) {
            listener.onDeviceFailed(throwable);
        }
    }

    @Override
    public void onNotification(final DOMNotification domNotification) {
        salProvider.getMountInstance().publish(domNotification);
    }

    public void registerMountPoint() {
        Preconditions.checkNotNull(id);
        Preconditions.checkNotNull(remoteSchemaContext);
        Preconditions.checkNotNull(netconfSessionPreferences);

        final DOMDataBroker netconfDeviceDataBroker = new NetconfDeviceDataBroker(id, remoteSchemaContext, deviceRpc,
                netconfSessionPreferences, defaultRequestTimeoutMillis);
        final NetconfDeviceNotificationService notificationService = new NetconfDeviceNotificationService();

        salProvider.getMountInstance().onTopologyDeviceConnected(remoteSchemaContext, netconfDeviceDataBroker,
                deviceRpc, notificationService);
    }

    public void unregisterMountPoint() {
        salProvider.getMountInstance().onTopologyDeviceDisconnected();
    }

    public void registerConnectionStatusListener(final RemoteDeviceHandler<NetconfSessionPreferences> listener) {
        connectionStatusListeners.add(listener);
    }

    @Override
    public void close() {
        if (domProviderRegistration != null) {
            domProviderRegistration.close();
            domProviderRegistration = null;
        }
        closeGracefully(salProvider);
    }

    private void closeGracefully(final AutoCloseable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (final Exception e) {
                LOG.warn("{}: Ignoring exception while closing {}", id, resource, e);
            }
        }
    }
}
