/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.impl.forward;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.opendaylight.controller.sal.binding.api.mount.MountProviderInstance;
import org.opendaylight.controller.sal.binding.api.mount.MountProviderService;
import org.opendaylight.controller.sal.binding.impl.MountPointManagerImpl;
import org.opendaylight.controller.sal.binding.impl.RootBindingAwareBroker;
import org.opendaylight.controller.sal.binding.impl.connect.dom.BindingDomConnectorDeployer;
import org.opendaylight.controller.sal.binding.impl.connect.dom.BindingIndependentConnector;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionInstance;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionListener;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.impl.codec.DeserializationException;

public class DomForwardedBindingBrokerImpl extends RootBindingAwareBroker implements DomForwardedBroker {

    private ProviderSession domProviderContext;
    private BindingIndependentConnector connector;

    private MountProvisionService domMountService;

    private final MountPointForwardingManager forwardingMountManager;

    private ConcurrentMap<InstanceIdentifier<?>, BindingIndependentConnector> connectors = new ConcurrentHashMap<>();
    private ConcurrentMap<InstanceIdentifier<?>, org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier> forwarded = new ConcurrentHashMap<>();
    private ListenerRegistration<MountProvisionListener> domListenerRegistration;
    private ListenerRegistration<org.opendaylight.controller.sal.binding.api.mount.MountProviderService.MountProvisionListener> baListenerRegistration;


    public DomForwardedBindingBrokerImpl(final String instanceName) {
        super(instanceName);
        forwardingMountManager = new MountPointForwardingManager(getMountManager());
        setMountManager(forwardingMountManager);
    }

    @Override
    public BindingIndependentConnector getConnector() {
        return connector;
    }

    @Override
    public ProviderSession getDomProviderContext() {
        return domProviderContext;
    }

    @Override
    public void setConnector(final BindingIndependentConnector connector) {
        this.connector = connector;
    }

    @Override
    public void setDomProviderContext(final ProviderSession domProviderContext) {
        this.domProviderContext = domProviderContext;
    }

    @Override
    public void startForwarding() {
        BindingDomConnectorDeployer.startDataForwarding(getConnector(), getDataBroker(), getDomProviderContext());
        BindingDomConnectorDeployer.startRpcForwarding(getConnector(), getRpcProviderRegistry(),
                getDomProviderContext());
        BindingDomConnectorDeployer.startNotificationForwarding(getConnector(), getNotificationBroker(),
                getDomProviderContext());
        startMountpointForwarding();
    }

    private void startMountpointForwarding() {
        domMountService = getDomProviderContext().getService(MountProvisionService.class);
        if (domMountService != null) {
            domListenerRegistration = domMountService.registerProvisionListener(forwardingMountManager);
            baListenerRegistration = getMountManager().registerProvisionListener(forwardingMountManager);
        }
    }

    public MountProvisionService getDomMountService() {
        return domMountService;
    }

    public void setDomMountService(final MountProvisionService domMountService) {
        this.domMountService = domMountService;
    }

    private void tryToDeployConnector(final InstanceIdentifier<?> baPath,
            final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier biPath) {
        final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier previous = forwarded.putIfAbsent(baPath, biPath);
        if (previous != null) {
            return;
        }
        final MountProviderInstance baMountPoint = getMountManager().createOrGetMountPoint(baPath);
        final MountProvisionInstance domMountPoint = domMountService.createOrGetMountPoint(biPath);
        final BindingIndependentConnector connector = createForwarder(baPath, baMountPoint, domMountPoint);
        connectors.put(baPath, connector);
    }

    private BindingIndependentConnector createForwarder(final InstanceIdentifier<?> path, final MountProviderInstance baMountPoint,
            final MountProvisionInstance domMountPoint) {
        final BindingIndependentConnector mountConnector = BindingDomConnectorDeployer.createConnector(getConnector());

        BindingDomConnectorDeployer.startDataForwarding(mountConnector, baMountPoint, domMountPoint);
        BindingDomConnectorDeployer.startRpcForwarding(mountConnector, baMountPoint, domMountPoint);
        BindingDomConnectorDeployer.startNotificationForwarding(mountConnector, baMountPoint, domMountPoint);
        // connector.setDomNotificationBroker(domMountPoint);
        return mountConnector;
    }

    public synchronized void tryToDeployDomForwarder(final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier domPath) {
        InstanceIdentifier<?> baPath;
        try {
            baPath = connector.getMappingService().fromDataDom(domPath);
            final BindingIndependentConnector potentialConnector = connectors.get(baPath);
            if (potentialConnector != null) {
                return;
            }
            tryToDeployConnector(baPath, domPath);
        } catch (final DeserializationException e) {

        }
    }

    public synchronized void tryToDeployBindingForwarder(final InstanceIdentifier<?> baPath) {
        final BindingIndependentConnector potentialConnector = connectors.get(baPath);
        if (potentialConnector != null) {
            return;
        }
        final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier domPath = connector.getMappingService().toDataDom(baPath);
        tryToDeployConnector(baPath, domPath);
    }

    public synchronized void undeployBindingForwarder(final InstanceIdentifier<?> baPath) {
        // FIXME: Implement closeMountPoint
    }

    public synchronized void undeployDomForwarder(final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier biPath) {
        // FIXME: Implement closeMountPoint
    }

    private class MountPointForwardingManager implements MountProviderService,MountProvisionListener,
        org.opendaylight.controller.sal.binding.api.mount.MountProviderService.MountProvisionListener {

        final MountProviderService delegate;

        public MountPointForwardingManager(final MountProviderService impl) {
            delegate = impl;
        }

        /**
         * Invokes {@link MountPointManagerImpl#createMountPoint}. This will create binding
         * mount point, from which we will get callback to {@link #onMountPointCreated(InstanceIdentifier)}
         * which will trigger connecting to DOM mount point.
         *
         */
        @Override
        public MountProviderInstance createMountPoint(final InstanceIdentifier<?> path) {
            return delegate.createMountPoint(path);
        }

        /**
         * Invokes {@link MountPointManagerImpl#createOrGetMountPoint}. This will get existing
         * binding  mount point (which is already DOM forwarded),
         * or creates new one.will get callback to {@link #onMountPointCreated(InstanceIdentifier)}
         * which will trigger connecting to DOM mount point.
         */
        @Override
        public MountProviderInstance createOrGetMountPoint(final InstanceIdentifier<?> path) {
            return delegate.createOrGetMountPoint(path);
        }

        @Override
        public ListenerRegistration<MountProvisionListener> registerProvisionListener(final MountProvisionListener arg0) {
            return delegate.registerProvisionListener(arg0);
        }

        /**
         *
         * This will try to get existing mount point from underlaying implementation
         * and if the Binding Mount Point is not present, we will try to get DOM
         * Mount point. If DOM mount point exists, we will trigger deployment of
         * forwarder for DOM Mountpoint, Which will result in creation of binding
         * mount point in underlying Mount point manager.
         *
         */
        @Override
        public MountProviderInstance getMountPoint(final InstanceIdentifier<?> path) {
            final MountProviderInstance binding = delegate.getMountPoint(path);
            if(binding != null) {
                return binding;
            }
            final YangInstanceIdentifier domPath = connector.getMappingService().toDataDom(path);
            final MountProvisionInstance domMount = domMountService.getMountPoint(domPath);
            if(domMount != null) {
                // we create Binding Mount point and connector, if connector is already created
                // resulting operation is noop. Delegate will be updated.
                tryToDeployDomForwarder(domPath);
                return delegate.getMountPoint(path);
            }
            return null;
        }

        @Override
        public void onMountPointCreated(final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier path) {
            tryToDeployDomForwarder(path);
        }

        @Override
        public void onMountPointRemoved(final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier path) {
            undeployDomForwarder(path);
        }

        @Override
        public void onMountPointCreated(final InstanceIdentifier<?> path) {
            tryToDeployBindingForwarder(path);
        }

        @Override
        public void onMountPointRemoved(final InstanceIdentifier<?> path) {
            undeployBindingForwarder(path);
        }
    }

}
