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
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.dom.api.DOMNotification;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class NetconfDeviceSalProvider implements AutoCloseable, Provider, BindingAwareProvider {

    private static final Logger LOG = LoggerFactory.getLogger(NetconfDeviceSalProvider.class);

    private final RemoteDeviceId id;
    private MountInstance mountInstance;

    private volatile NetconfDeviceTopologyAdapter topologyDatastoreAdapter;

    public NetconfDeviceSalProvider(final RemoteDeviceId deviceId) {
        this.id = deviceId;
    }

    public MountInstance getMountInstance() {
        Preconditions.checkState(mountInstance != null,
                "%s: Mount instance was not initialized by sal. Cannot get mount instance", id);
        return mountInstance;
    }

    public NetconfDeviceTopologyAdapter getTopologyDatastoreAdapter() {
        Preconditions.checkState(topologyDatastoreAdapter != null,
                "%s: Sal provider %s was not initialized by sal. Cannot get topology datastore adapter", id);
        return topologyDatastoreAdapter;
    }

    @Override
    public void onSessionInitiated(final Broker.ProviderSession session) {
        LOG.debug("{}: (BI)Session with sal established {}", id, session);

        final DOMMountPointService mountService = session.getService(DOMMountPointService.class);
        if (mountService != null) {
            mountInstance = new MountInstance(mountService, id);
        }
    }

    @Override
    public Collection<Provider.ProviderFunctionality> getProviderFunctionality() {
        return Collections.emptySet();
    }

    @Override
    public void onSessionInitiated(final BindingAwareBroker.ProviderContext session) {
        LOG.debug("{}: Session with sal established {}", id, session);

        final DataBroker dataBroker = session.getSALService(DataBroker.class);

        topologyDatastoreAdapter = new NetconfDeviceTopologyAdapter(id, dataBroker);
    }

    public void close() throws Exception {
        mountInstance.close();
        topologyDatastoreAdapter.close();
        topologyDatastoreAdapter = null;
    }

    static final class MountInstance implements AutoCloseable {

        private DOMMountPointService mountService;
        private final RemoteDeviceId id;
        private NetconfDeviceNotificationService notificationService;

        private ObjectRegistration<DOMMountPoint> topologyRegistration;

        MountInstance(final DOMMountPointService mountService, final RemoteDeviceId id) {
            this.mountService = Preconditions.checkNotNull(mountService);
            this.id = Preconditions.checkNotNull(id);
        }

        synchronized void onTopologyDeviceConnected(final SchemaContext initialCtx,
                                                    final DOMDataBroker broker, final DOMRpcService rpc,
                                                    final NetconfDeviceNotificationService notificationService) {

            Preconditions.checkNotNull(mountService, "Closed");
            Preconditions.checkState(topologyRegistration == null, "Already initialized");

            final DOMMountPointService.DOMMountPointBuilder mountBuilder = mountService.createMountPoint(id.getTopologyPath());
            mountBuilder.addInitialSchemaContext(initialCtx);

            mountBuilder.addService(DOMDataBroker.class, broker);
            mountBuilder.addService(DOMRpcService.class, rpc);
            mountBuilder.addService(DOMNotificationService.class, notificationService);
            this.notificationService = notificationService;

            topologyRegistration = mountBuilder.register();
            LOG.debug("{}: TOPOLOGY Mountpoint exposed into MD-SAL {}", id,
                    topologyRegistration);

        }

        synchronized void onTopologyDeviceDisconnected() {
            if(topologyRegistration == null) {
                LOG.trace(
                        "{}: Not removing TOPOLOGY mountpoint from MD-SAL, mountpoint was not registered yet",
                        id);
                return;
            }

            try {
                topologyRegistration.close();
            } catch (final Exception e) {
                // Only log and ignore
                LOG.warn(
                        "Unable to unregister mount instance for {}. Ignoring exception",
                        id.getTopologyPath(), e);
            } finally {
                LOG.debug("{}: TOPOLOGY Mountpoint removed from MD-SAL {}",
                        id, topologyRegistration);
                topologyRegistration = null;
            }
        }

        @Override
        synchronized public void close() throws Exception {
            onTopologyDeviceDisconnected();
            mountService = null;
        }

        public synchronized void publish(final DOMNotification domNotification) {
            Preconditions.checkNotNull(notificationService, "Device not set up yet, cannot handle notification {}", domNotification);
            notificationService.publishNotification(domNotification);
        }
    }

}
