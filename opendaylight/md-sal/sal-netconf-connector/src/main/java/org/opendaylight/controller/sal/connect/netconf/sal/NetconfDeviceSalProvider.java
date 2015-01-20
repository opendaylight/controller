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
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.controller.sal.core.api.RpcProvisionRegistry;
import org.opendaylight.controller.sal.core.api.notify.NotificationPublishService;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class NetconfDeviceSalProvider implements AutoCloseable, Provider, BindingAwareProvider {

    private static final Logger logger = LoggerFactory.getLogger(NetconfDeviceSalProvider.class);

    private final RemoteDeviceId id;
    private final ExecutorService executor;
    private volatile NetconfDeviceDatastoreAdapter datastoreAdapter;
    private MountInstance mountInstance;

    private volatile NetconfDeviceTopologyAdapter topologyDatastoreAdapter;

    public NetconfDeviceSalProvider(final RemoteDeviceId deviceId, final ExecutorService executor) {
        this.id = deviceId;
        this.executor = executor;
    }

    public MountInstance getMountInstance() {
        Preconditions.checkState(mountInstance != null,
                "%s: Mount instance was not initialized by sal. Cannot get mount instance", id);
        return mountInstance;
    }

    public NetconfDeviceDatastoreAdapter getDatastoreAdapter() {
        Preconditions.checkState(datastoreAdapter != null,
                "%s: Sal provider %s was not initialized by sal. Cannot get datastore adapter", id);
        return datastoreAdapter;
    }

    public NetconfDeviceTopologyAdapter getTopologyDatastoreAdapter() {
        Preconditions.checkState(topologyDatastoreAdapter != null,
                "%s: Sal provider %s was not initialized by sal. Cannot get topology datastore adapter", id);
        return topologyDatastoreAdapter;
    }

    @Override
    public void onSessionInitiated(final Broker.ProviderSession session) {
        logger.debug("{}: (BI)Session with sal established {}", id, session);

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
        logger.debug("{}: Session with sal established {}", id, session);

        final DataBroker dataBroker = session.getSALService(DataBroker.class);
        datastoreAdapter = new NetconfDeviceDatastoreAdapter(id, dataBroker);

        topologyDatastoreAdapter = new NetconfDeviceTopologyAdapter(id, dataBroker);
    }

    public void close() throws Exception {
        mountInstance.close();
        datastoreAdapter.close();
        datastoreAdapter = null;
    }

    static final class MountInstance implements AutoCloseable {

        private DOMMountPointService mountService;
        private final RemoteDeviceId id;
        private ObjectRegistration<DOMMountPoint> registration;
        private NotificationPublishService notificationSerivce;

        MountInstance(final DOMMountPointService mountService, final RemoteDeviceId id) {
            this.mountService = Preconditions.checkNotNull(mountService);
            this.id = Preconditions.checkNotNull(id);
        }

        synchronized void onDeviceConnected(final SchemaContext initialCtx,
                final DOMDataBroker broker, final RpcProvisionRegistry rpc,
                final NotificationPublishService notificationSerivce) {

            Preconditions.checkNotNull(mountService, "Closed");
            Preconditions.checkState(registration == null, "Already initialized");

            final DOMMountPointService.DOMMountPointBuilder mountBuilder = mountService.createMountPoint(id.getPath());
            mountBuilder.addInitialSchemaContext(initialCtx);

            mountBuilder.addService(DOMDataBroker.class, broker);
            mountBuilder.addService(RpcProvisionRegistry.class, rpc);
            this.notificationSerivce = notificationSerivce;
            mountBuilder.addService(NotificationPublishService.class, notificationSerivce);

            registration = mountBuilder.register();
        }

        synchronized void onDeviceDisconnected() {
            if(registration == null) {
                return;
            }

            try {
                registration.close();
            } catch (final Exception e) {
                // Only log and ignore
                logger.warn("Unable to unregister mount instance for {}. Ignoring exception", id.getPath(), e);
            } finally {
                registration = null;
            }
        }

        @Override
        synchronized public void close() throws Exception {
            if(registration != null) {
                onDeviceDisconnected();
            }
            mountService = null;
        }

        public synchronized void publish(final CompositeNode domNotification) {
            Preconditions.checkNotNull(notificationSerivce, "Device not set up yet, cannot handle notification {}", domNotification);
            notificationSerivce.publish(domNotification);
        }
    }

}
